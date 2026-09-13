package com.markedusduplicate.deckard.mascot

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.KeyEvent
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markedusduplicate.deckard.R
import com.markedusduplicate.design.theme.AppTheme
import com.markedusduplicate.textresource.asString
import kotlinx.coroutines.flow.StateFlow

/**
 * The floating mascot itself: a draggable [DeckardMascot] with, beneath him, whatever he currently
 * has to say — a tailed bubble while he reads, or the full [SlopReportCard] once he's judged.
 * Rendered into a `WindowManager` overlay by [DeckardOverlayService], so the content is wrap-sized
 * and the background stays transparent — only the mascot and his bubble occupy (and intercept
 * touches in) the window; everything else passes through to the app beneath.
 */
@SuppressLint("ViewConstructor")
class DeckardComposeView(
    context: Context,
    private val state: StateFlow<DeckardState>,
    private val onTap: () -> Unit,
    private val onDrag: (dx: Float, dy: Float) -> Unit,
    private val onDismiss: () -> Unit,
    private val onViewAnalysis: (url: String) -> Unit,
    private val onCopyLink: (url: String) -> Unit,
) : AbstractComposeView(context) {

    init {
        id = R.id.deckardComposeView
    }

    /**
     * Back callback for the modern path (Android 13+). The app opts into predictive back
     * (`enableOnBackInvokedCallback=true`), so on API 33+ the framework routes back through the
     * window's [OnBackInvokedDispatcher] instead of [KEYCODE_BACK][KeyEvent.KEYCODE_BACK]. Held so it
     * can be unregistered on detach.
     */
    private var backCallback: OnBackInvokedCallback? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerBackCallback()
    }

    override fun onDetachedFromWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) unregisterBackCallback()
        super.onDetachedFromWindow()
    }

    /**
     * Register the back callback for the window's lifetime. It fires only while the overlay window is
     * the back target, i.e. while [DeckardOverlayService] has it focusable (solely when Deckard is
     * showing) — so back closes the mascot, not the app beneath it. Focusability (not registration)
     * gates when it fires.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun registerBackCallback() {
        val dispatcher = findOnBackInvokedDispatcher() ?: return
        val callback = OnBackInvokedCallback { onDismiss() }
        dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback)
        backCallback = callback
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun unregisterBackCallback() {
        backCallback?.let { findOnBackInvokedDispatcher()?.unregisterOnBackInvokedCallback(it) }
        backCallback = null
    }

    /**
     * Back-gesture dismissal, legacy fallback. Used on API 30–32 (pre-[OnBackInvokedDispatcher]) and
     * if no dispatcher is available; on API 33+ with [backCallback] registered the framework consumes
     * back via the dispatcher and never delivers [KEYCODE_BACK][KeyEvent.KEYCODE_BACK] here, so the
     * two paths don't double-fire. Reaches here only while the window is focusable.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            onDismiss()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    @Composable
    override fun Content() {
        val s by state.collectAsStateWithLifecycle()
        AppTheme {
            AnimatedVisibility(
                visible = s != DeckardState.Hidden,
                enter = fadeIn(tween(140)) +
                        scaleIn(tween(180), initialScale = 0.88f, transformOrigin = MascotOrigin),
                exit = fadeOut(tween(100)) +
                        scaleOut(tween(120), targetScale = 0.9f, transformOrigin = MascotOrigin),
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(OVERLAY_SHADOW_ROOM),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    DeckardMascot(onTap = onTap, onDrag = onDrag, onDismiss = onDismiss)

                    when (val current = s) {
                        DeckardState.Hidden -> Unit
                        is DeckardState.Thinking -> DeckardThinkingBubble(
                            text = DeckardVoice.thinking(current.how).asString(),
                        )

                        is DeckardState.Unavailable ->
                            DeckardBubble(text = DeckardVoice.setback(current.reason).asString())

                        is DeckardState.Verdict -> SlopReportCard(
                            verdict = current.verdict,
                            note = DeckardVoice.remark(current.verdict).asString(),
                            onViewAnalysis = onViewAnalysis,
                            onCopyLink = onCopyLink,
                        )
                    }
                }
            }
        }
    }
}

/** Deckard grows out of his own plate in the top-left, not out of the middle of the report. */
private val MascotOrigin = TransformOrigin(0.08f, 0f)

/**
 * Breathing room inside the overlay window for the mascot's and the card's shadows. The window is
 * wrap-content, so without it the shadow is clipped square at the window edge.
 */
private val OVERLAY_SHADOW_ROOM = 14.dp
