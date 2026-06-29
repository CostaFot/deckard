package com.markedusduplicate.deckard.mascot

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.core.net.toUri
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.deckard.di.AccessibilityScreenText
import com.markedusduplicate.deckard.di.OcrContentScreenText
import com.markedusduplicate.deckard.mascot.DeckardOverlayService.Companion.detectText
import com.markedusduplicate.deckard.poc.PocNavOverlayView
import com.markedusduplicate.deckard.slop.DetectSlopUseCase
import com.markedusduplicate.deckard.slop.MIN_WORDS_TO_DETECT
import com.markedusduplicate.deckard.slop.ScreenReadResult
import com.markedusduplicate.deckard.slop.ScreenTextReader
import com.markedusduplicate.deckard.slop.SlopCheck
import com.markedusduplicate.logging.logDebug
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hosts the floating Deckard mascot in a system overlay window so it lives over every app. Deckard is
 * hidden until summoned by a left→right swipe on the [DeckardEdgeHandleView] tab pinned to the left
 * edge; summoning reads the text on the current screen (via the `@AccessibilityScreenText`
 * [ScreenTextReader] — accessibility-tree extraction), judges whether it's AI-generated "slop" via
 * [DetectSlopUseCase] (backed by
 * [com.markedusduplicate.deckard.slop.AiDetectorRepository]), and shows the verdict in a speech
 * bubble, then auto-hides. **Long-pressing** the tab runs the alternative `@OcrContentScreenText`
 * read instead — a screenshot the model isolates the main post out of. Tapping the mascot re-runs the
 * a11y check; tapping the bubble dismisses it.
 *
 * Text can also be judged **without reading the screen**: [detectText] (driven by the share-sheet
 * [com.markedusduplicate.deckard.ui.activity.ShareTextActivity]) feeds already-captured text straight
 * into the same verdict flow and pops the mascot up to speak it — auto-starting the service if needed.
 *
 * An overlay service has no bind callbacks and no decor view, so it drives its own
 * [LifecycleRegistry] to RESUMED and sets the view-tree owners directly on the overlay view — both
 * required for Compose to compose and recompose.
 *
 * Requires the draw-over-apps permission (checked here) and the accessibility service enabled (for
 * reading the screen). Started/stopped from the setup screen; runs as a plain started service for now.
 */
@AndroidEntryPoint
class DeckardOverlayService :
    android.app.Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner {

    @Inject
    @AccessibilityScreenText
    lateinit var accessibilityReader: ScreenTextReader

    @Inject
    @OcrContentScreenText
    lateinit var screenshotOcrReader: ScreenTextReader

    @Inject
    lateinit var detectSlopUseCase: DetectSlopUseCase

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore = ViewModelStore()

    @Inject
    lateinit var overlayViewModelFactory: OverlayViewModelFactory

    /** Hand the overlay composition our singleton-graph-backed factory, so `viewModel()` resolves DI'd VMs. */
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = overlayViewModelFactory

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val scope by lazy { CoroutineScope(dispatcherProvider.ui + SupervisorJob()) }

    private val state = MutableStateFlow<DeckardState>(DeckardState.Hidden)

    private val windowManager by lazy { getSystemService(WindowManager::class.java) }

    private fun overlayParams(buildGravity: Int, x: Int, y: Int) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = buildGravity
        this.x = x
        this.y = y
    }

    // The mascot is summoned next to the edge tab, so it speaks from the left-centre.
    private val layoutParams by lazy {
        overlayParams(Gravity.LEFT or Gravity.CENTER_VERTICAL, x = dp(44), y = 0)
    }
    private val handleParams by lazy {
        overlayParams(Gravity.LEFT or Gravity.CENTER_VERTICAL, x = 0, y = 0)
    }

    // POC Nav3 panel, pinned to the right edge.
    private val pocParams by lazy {
        overlayParams(Gravity.RIGHT or Gravity.CENTER_VERTICAL, x = 0, y = 0)
    }

    private var overlayView: DeckardComposeView? = null
    private var edgeHandleView: DeckardEdgeHandleView? = null
    private var pocView: PocNavOverlayView? = null
    private var tapJob: Job? = null

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        if (!Settings.canDrawOverlays(this)) {
            logDebug { "deckard: no draw-over permission, stopping" }
            stopSelf()
            return
        }

        val view = DeckardComposeView(
            context = this,
            state = state.asStateFlow(),
            onTap = ::summonViaAccessibilityRead,
            onDrag = ::onDrag,
            onDismiss = ::dismiss,
            onViewAnalysis = ::openAnalysis,
            onCopyLink = ::copyLink,
        ).also(::attachOwners)
        overlayView = view
        windowManager.addView(view, layoutParams)

        val handle = DeckardEdgeHandleView(
            context = this,
            onSummon = ::summonViaAccessibilityRead,
            onLongPress = ::summonViaScreenshotOcr,
        ).also(::attachOwners)
        edgeHandleView = handle
        windowManager.addView(handle, handleParams)

        val poc = PocNavOverlayView(
            context = this,
            viewModelFactory = overlayViewModelFactory,
            onClose = ::removePocPanel,
        ).also(::attachOwners)
        pocView = poc
        windowManager.addView(poc, pocParams)

        isRunning = true
        logDebug { "deckard overlay + edge handle + poc nav panel added" }
    }

    private fun removePocPanel() {
        pocView?.let { runCatching { windowManager.removeView(it) } }
        pocView = null
    }

    /** Make the service the owner of [view]'s tree so Compose can find a lifecycle / saved state. */
    private fun attachOwners(view: android.view.View) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DETECT_TEXT) {
            intent.getStringExtra(EXTRA_TEXT)?.takeIf { it.isNotBlank() }?.let(::runDetectionOnText)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Summon Deckard via the a11y-tree read (swipe / tap): fast, no model, per-app extractors. */
    private fun summonViaAccessibilityRead() = runDetection(accessibilityReader)

    /** Summon Deckard via the screenshot + OCR "pick the post" read (long-press): slower, model-backed. */
    private fun summonViaScreenshotOcr() = runDetection(screenshotOcrReader)

    /** Show Deckard, read the screen's text with [reader], and surface the verdict (stays until closed). */
    private fun runDetection(reader: ScreenTextReader) = runDetecting { readScreenAndJudge(reader) }

    /** Show Deckard and judge already-captured [text] (e.g. text shared into the app) — no screen read. */
    private fun runDetectionOnText(text: String) = runDetecting { judge(text) }

    /** Show Deckard, run [produce] to get a verdict, and surface it (stays until closed). */
    private fun runDetecting(produce: suspend () -> DeckardState) {
        tapJob?.cancel()
        setOverlayFocusable(true)
        tapJob = scope.launch {
            state.value = DeckardState.Thinking
            state.value = produce()
        }
    }

    private fun dismiss() {
        tapJob?.cancel()
        state.value = DeckardState.Hidden
        setOverlayFocusable(false)
    }

    /**
     * Toggle the mascot window's focusability. Focusable so it captures the back key/gesture (to
     * [dismiss]) only while Deckard is showing; otherwise [WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE]
     * is restored so key/IME focus stays with the app beneath. Touch pass-through is unaffected
     * (governed by `FLAG_NOT_TOUCH_MODAL`).
     */
    private fun setOverlayFocusable(focusable: Boolean) {
        val view = overlayView ?: return
        layoutParams.flags = if (focusable) {
            layoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            layoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        runCatching { windowManager.updateViewLayout(view, layoutParams) }
    }

    private suspend fun readScreenAndJudge(reader: ScreenTextReader): DeckardState =
        when (val result = reader.read()) {
            is ScreenReadResult.Unavailable -> DeckardState.Unavailable(result.reason)
            is ScreenReadResult.Text -> judge(result.value)
        }

    /** Run [text] through the detector and map the outcome to a mascot state. */
    private suspend fun judge(text: String): DeckardState {
        logDebug { "slop: judging ${text.length} chars" }
        return when (val check = detectSlopUseCase(text)) {
            is SlopCheck.Judged -> DeckardState.Verdict(check.verdict)
            SlopCheck.NotEnoughText ->
                DeckardState.Unavailable("Not enough text here to judge — I need about $MIN_WORDS_TO_DETECT words.")

            SlopCheck.Failed -> DeckardState.Unavailable("Couldn't reach the slop oracle")
        }
    }

    private fun openAnalysis(url: String) {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        dismiss()
    }

    private fun copyLink(url: String) {
        getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("Pangram result", url))
    }

    private fun onDrag(dx: Float, dy: Float) {
        val view = overlayView ?: return
        layoutParams.x += dx.toInt()
        layoutParams.y += dy.toInt()
        windowManager.updateViewLayout(view, layoutParams)
    }

    override fun onDestroy() {
        isRunning = false
        tapJob?.cancel()
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        edgeHandleView?.let { runCatching { windowManager.removeView(it) } }
        removePocPanel()
        overlayView = null
        edgeHandleView = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_DETECT_TEXT = "com.markedusduplicate.deckard.action.DETECT_TEXT"
        private const val EXTRA_TEXT = "com.markedusduplicate.deckard.extra.TEXT"

        /** True while the overlay is up; read by the setup screen to drive the start/stop toggle. */
        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            context.startService(Intent(context, DeckardOverlayService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DeckardOverlayService::class.java))
        }

        /**
         * Judge [text] directly (e.g. text shared into the app via the share sheet) and surface the
         * verdict through the mascot. Starts the service if it isn't already up, so a share auto-summons
         * Deckard. Requires the draw-over-apps permission; no accessibility/screen read is involved.
         */
        fun detectText(context: Context, text: String) {
            context.startService(
                Intent(context, DeckardOverlayService::class.java)
                    .setAction(ACTION_DETECT_TEXT)
                    .putExtra(EXTRA_TEXT, text),
            )
        }
    }
}
