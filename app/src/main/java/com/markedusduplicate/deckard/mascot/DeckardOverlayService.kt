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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.common.result.fold
import com.markedusduplicate.deckard.di.OcrScreenText
import com.markedusduplicate.deckard.slop.DetectSlopUseCase
import com.markedusduplicate.deckard.slop.ScreenReadResult
import com.markedusduplicate.deckard.slop.ScreenTextReader
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
 * edge; summoning reads the text on the current screen (via the `@OcrScreenText` [ScreenTextReader]
 * — screenshot OCR), judges whether it's AI-generated "slop" via [DetectSlopUseCase] (backed by
 * [com.markedusduplicate.deckard.slop.AiDetectorRepository]), and shows the verdict in a speech
 * bubble, then auto-hides. Tapping the mascot re-runs the check; tapping the bubble dismisses it.
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
    SavedStateRegistryOwner {

    @Inject
    @OcrScreenText
    lateinit var screenTextReader: ScreenTextReader

    @Inject
    lateinit var detectSlopUseCase: DetectSlopUseCase

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore = ViewModelStore()

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

    private var overlayView: DeckardComposeView? = null
    private var edgeHandleView: DeckardEdgeHandleView? = null
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
            onTap = ::detectSlopNow,
            onDrag = ::onDrag,
            onDismiss = ::dismiss,
            onViewAnalysis = ::openAnalysis,
            onCopyLink = ::copyLink,
        ).also(::attachOwners)
        overlayView = view
        windowManager.addView(view, layoutParams)

        val handle = DeckardEdgeHandleView(
            context = this,
            onSummon = ::detectSlopNow,
        ).also(::attachOwners)
        edgeHandleView = handle
        windowManager.addView(handle, handleParams)

        isRunning = true
        logDebug { "deckard overlay + edge handle added" }
    }

    /** Make the service the owner of [view]'s tree so Compose can find a lifecycle / saved state. */
    private fun attachOwners(view: android.view.View) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    /** Summon Deckard: show him, read the screen's text, and surface the verdict (stays until closed). */
    private fun detectSlopNow() {
        tapJob?.cancel()
        tapJob = scope.launch {
            state.value = DeckardState.Thinking
            state.value = detectSlop()
        }
    }

    private fun dismiss() {
        tapJob?.cancel()
        state.value = DeckardState.Hidden
    }

    private suspend fun detectSlop(): DeckardState =
        when (val result = screenTextReader.read()) {
            is ScreenReadResult.Unavailable -> DeckardState.Unavailable(result.reason)
            is ScreenReadResult.Text -> {
                logDebug { "slop: captured ${result.value.length} chars" }
                detectSlopUseCase(result.value).fold(
                    ifError = { DeckardState.Unavailable("Couldn't reach the slop oracle") },
                    ifSuccess = { DeckardState.Verdict(it) },
                )
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
        overlayView = null
        edgeHandleView = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
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
    }
}
