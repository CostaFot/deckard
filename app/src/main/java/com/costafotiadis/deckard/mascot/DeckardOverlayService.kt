package com.costafotiadis.deckard.mascot

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.core.content.ContextCompat
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
import com.costafotiadis.common.FlagProvider
import com.costafotiadis.common.coroutine.DispatcherProvider
import com.costafotiadis.deckard.R
import com.costafotiadis.deckard.di.OcrContentScreenText
import com.costafotiadis.deckard.llm.nano.NanoBench
import com.costafotiadis.deckard.mascot.DeckardOverlayService.Companion.detectText
import com.costafotiadis.deckard.shutter.DeckardShutterView
import com.costafotiadis.deckard.shutter.ShutterEffect
import com.costafotiadis.deckard.shutter.ShutterEffectStore
import com.costafotiadis.deckard.slop.DetectSlopUseCase
import com.costafotiadis.deckard.slop.ScreenReadResult
import com.costafotiadis.deckard.slop.ScreenTextReader
import com.costafotiadis.deckard.slop.SlopCheck
import com.costafotiadis.logging.logDebug
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Hosts the floating Deckard mascot in a system overlay window so it lives over every app. Deckard is
 * hidden until summoned by a **long-press** on the [DeckardEdgeHandleView] tab pinned to the left
 * edge; summoning photographs the current screen and has the on-device model isolate the main post
 * out of it (the `@OcrContentScreenText` [ScreenTextReader]), judges whether it's AI-generated
 * "slop" via [DetectSlopUseCase] (backed by [com.costafotiadis.deckard.slop.AiDetectorRepository]),
 * and shows the verdict beneath him until the X dismisses it. The mascot himself takes no tap: he
 * would be in the picture the summon takes.
 *
 * A summon also puts up a third, untouchable window for the length of the read — the
 * [com.costafotiadis.deckard.shutter.ShutterEffect] the settings screen has chosen — because a read
 * that photographs your screen should say so, and because the vision inference it is covering takes
 * seconds. It goes up inside the reader's own "done with the screen" callback and not before:
 * anything drawn earlier is in the picture.
 *
 * Text can also be judged **without reading the screen**: [detectText] (driven by the share-sheet
 * [com.costafotiadis.deckard.ui.activity.ShareTextActivity]) feeds already-captured text straight
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
    @OcrContentScreenText
    lateinit var screenshotOcrReader: ScreenTextReader

    @Inject
    lateinit var detectSlopUseCase: DetectSlopUseCase

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    lateinit var shutterEffects: ShutterEffectStore

    @Inject
    lateinit var flagProvider: FlagProvider

    @Inject
    lateinit var nanoBench: NanoBench

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

    /**
     * The shutter effect's own window: the whole display, and untouchable so it cannot take a
     * gesture from the app it is drawn over.
     *
     * `FLAG_LAYOUT_IN_SCREEN` and the cutout mode are load-bearing rather than tidy-minded. Without
     * them the window stops at the system bars, and an effect that runs round the edges of the
     * screen but not the actual edges of the screen has nothing left to be.
     */
    private val shutterParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
    }

    // The mascot is summoned next to the edge tab, so it speaks from the left-centre.
    private val layoutParams by lazy {
        overlayParams(Gravity.LEFT or Gravity.CENTER_VERTICAL, x = dp(30), y = 0)
    }
    private val handleParams by lazy {
        overlayParams(Gravity.LEFT or Gravity.CENTER_VERTICAL, x = 0, y = 0)
    }

    private var overlayView: DeckardComposeView? = null
    private var edgeHandleView: DeckardEdgeHandleView? = null
    private var shutterView: DeckardShutterView? = null
    private val shutterRunning = MutableStateFlow(false)
    private var tapJob: Job? = null

    /**
     * Which detection is the current one. A second summon cancels the first, and the first's teardown
     * then runs *after* the second has already opened its own shutter — so a run only closes the
     * shutter if it is still the run that owns it. Getting this wrong leaves a full-screen window
     * over every app the user opens.
     */
    private var runToken = 0L

    /**
     * Debug builds only: plays the chosen effect over whatever app happens to be in front, with no
     * screenshot and no model behind it.
     *
     * An effect drawn over an arbitrary app cannot be judged in a preview or over the setup screen —
     * the edges only read correctly over the thing they are actually covering — and the read that
     * normally triggers one costs a vision inference and needs a 3GB model on the device. So there
     * is a way to just fire it: `scripts/deckard shutter`.
     *
     * Exported, because `adb shell am broadcast` runs as a different uid — which is why it is
     * registered at all only when [FlagProvider.isDebugEnabled], and why it does nothing but draw.
     */
    private val shutterPreviewReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = previewShutter()
    }

    /** Debug only, like [shutterPreviewReceiver]: one Gemini Nano read under a chosen configuration, timed. */
    private val nanoBenchReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val spec = NanoBench.spec(intent ?: return)
            scope.launch { nanoBench.run(spec) }
        }
    }

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
            onDrag = ::onDrag,
            onDismiss = ::dismiss,
            onViewAnalysis = ::openAnalysis,
            onCopyLink = ::copyLink,
        ).also(::attachOwners)
        overlayView = view
        windowManager.addView(view, layoutParams)

        val handle = DeckardEdgeHandleView(
            context = this,
            onLongPress = ::summon,
        ).also(::attachOwners)
        edgeHandleView = handle
        windowManager.addView(handle, handleParams)

        if (flagProvider.isDebugEnabled) {
            ContextCompat.registerReceiver(
                this,
                shutterPreviewReceiver,
                IntentFilter(ACTION_PREVIEW_SHUTTER),
                ContextCompat.RECEIVER_EXPORTED,
            )
            ContextCompat.registerReceiver(
                this,
                nanoBenchReceiver,
                IntentFilter(NanoBench.ACTION),
                ContextCompat.RECEIVER_EXPORTED,
            )
        }

        isRunning = true
        logDebug { "deckard overlay + edge handle added" }
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

    /** Show Deckard, read the screen with the screenshot + OCR "pick the post" read, and surface the verdict. */
    private fun summon() = runDetecting { readScreenAndJudge() }

    /** Show Deckard and judge already-captured [text] (e.g. text shared into the app) — no screen read. */
    private fun runDetectionOnText(text: String) = runDetecting {
        show(DeckardState.Thinking(ReadMethod.SharedText))
        judge(text)
    }

    /**
     * Run [produce] to get a verdict and surface it (stays until closed).
     *
     * The `finally` is what guarantees the shutter is let go of: a verdict, a setback, or a
     * cancellation part-way through all end the same way, which is the only reason a window nothing
     * can touch is safe to put over the whole screen.
     */
    private fun runDetecting(produce: suspend () -> DeckardState) {
        tapJob?.cancel()
        val token = ++runToken
        tapJob = scope.launch {
            try {
                show(produce())
            } finally {
                withContext(NonCancellable) { closeShutter(token) }
            }
        }
    }

    /**
     * Put Deckard on screen in [newState], taking window focus so the back key/gesture dismisses him.
     * Nothing calls this until whoever is reading the screen has finished with it — see
     * [ScreenTextReader.read].
     */
    private fun show(newState: DeckardState) {
        setOverlayFocusable(true)
        state.value = newState
    }

    private fun dismiss() {
        tapJob?.cancel()
        state.value = DeckardState.Hidden
        setOverlayFocusable(false)
        closeShutter(runToken)
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

    /**
     * Read the screen, then judge what comes back. Deckard stays off-screen until the reader reports
     * it is done with the screen itself: he would otherwise be in the shot the vision model is asked
     * to read the post out of.
     */
    private suspend fun readScreenAndJudge(): DeckardState =
        when (val result = screenshotOcrReader.read(::onScreenCaptured)) {
            is ScreenReadResult.Unavailable -> DeckardState.Unavailable(NoVerdict.CouldNotRead(result.reason))
            is ScreenReadResult.Text -> judge(result.value)
        }

    /** The screen is ours again: say it was photographed, and put Deckard on it. */
    private fun onScreenCaptured() {
        openShutter()
        show(DeckardState.Thinking(ReadMethod.Screenshot))
    }

    /**
     * Put the effect's window up and start it running. Called from the reader's own "done with the
     * screen" callback and never a moment sooner: anything drawn before the shutter is in the JPEG
     * the vision model is asked to read the post out of.
     */
    private fun openShutter() {
        val effect = shutterEffects.selected.value
        if (effect == ShutterEffect.None) return

        if (shutterView?.effect != effect) removeShutterView()
        shutterRunning.value = true
        if (shutterView != null) return

        val view = DeckardShutterView(
            context = this,
            effect = effect,
            running = shutterRunning.asStateFlow(),
            onFinished = ::removeShutterView,
        ).also(::attachOwners)
        shutterView = view.takeIf { runCatching { windowManager.addView(it, shutterParams) }.isSuccess }
    }

    /** Let the effect go, if [token] is still the run that opened it. The window leaves on its own. */
    private fun closeShutter(token: Long) {
        if (token != runToken) return
        shutterRunning.value = false
    }

    /** The release has finished playing, so there is finally nothing left to draw. */
    private fun removeShutterView() {
        shutterView?.let { runCatching { windowManager.removeView(it) } }
        shutterView = null
        shutterRunning.value = false
    }

    /** Play the effect on its own, for as long as a short read would have taken. */
    private fun previewShutter() {
        val token = ++runToken
        scope.launch {
            openShutter()
            delay(PREVIEW_HOLD_MILLIS)
            closeShutter(token)
        }
    }

    /** Run [text] through the detector and map the outcome to a mascot state. */
    private suspend fun judge(text: String): DeckardState {
        logDebug { "slop: judging ${text.length} chars" }
        return when (val check = detectSlopUseCase(text)) {
            is SlopCheck.Judged -> DeckardState.Verdict(check.verdict)
            SlopCheck.NotEnoughText -> DeckardState.Unavailable(NoVerdict.NotEnoughText)
            SlopCheck.Failed -> DeckardState.Unavailable(NoVerdict.DetectorUnreachable)
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
            ?.setPrimaryClip(ClipData.newPlainText(getString(R.string.card_clip_label), url))
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
        if (flagProvider.isDebugEnabled) {
            runCatching { unregisterReceiver(shutterPreviewReceiver) }
            runCatching { unregisterReceiver(nanoBenchReceiver) }
        }
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        edgeHandleView?.let { runCatching { windowManager.removeView(it) } }
        removeShutterView()
        overlayView = null
        edgeHandleView = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_DETECT_TEXT = "com.costafotiadis.deckard.action.DETECT_TEXT"
        private const val EXTRA_TEXT = "com.costafotiadis.deckard.extra.TEXT"

        /** Debug only — see [shutterPreviewReceiver]. Named by `scripts/deckard shutter`. */
        private const val ACTION_PREVIEW_SHUTTER =
            "com.costafotiadis.deckard.action.PREVIEW_SHUTTER"

        /** About as long as a vision inference on a warm engine, which is what it stands in for. */
        private const val PREVIEW_HOLD_MILLIS = 2_600L

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
