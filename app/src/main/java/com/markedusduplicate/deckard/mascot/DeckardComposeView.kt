package com.markedusduplicate.deckard.mascot

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.KeyEvent
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markedusduplicate.deckard.R
import com.markedusduplicate.design.theme.AppTheme
import kotlinx.coroutines.flow.StateFlow

/**
 * The floating mascot itself: a draggable 🧙 emoji with a speech bubble that shows whatever the LLM
 * just said. Rendered into a `WindowManager` overlay by [DeckardOverlayService], so the content is
 * wrap-sized and the background stays transparent — only the emoji and bubble occupy (and intercept
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
            if (s == DeckardState.Hidden) return@AppTheme
            Box(modifier = Modifier.wrapContentSize()) {
                Column(
                    modifier = Modifier.wrapContentSize(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "🧙",
                        fontSize = 40.sp,
                        modifier = Modifier
                            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
                            .pointerInput(Unit) {
                                detectDragGestures { change, amount ->
                                    change.consume()
                                    onDrag(amount.x, amount.y)
                                }
                            },
                    )

                    when (val current = s) {
                        DeckardState.Hidden -> Unit
                        DeckardState.Thinking -> Bubble(text = "🤔 …")
                        is DeckardState.Speaking -> Bubble(text = current.remark)
                        is DeckardState.Unavailable -> Bubble(text = current.reason)
                        is DeckardState.Verdict -> SlopReportCard(
                            verdict = current.verdict,
                            onViewAnalysis = onViewAnalysis,
                            onCopyLink = onCopyLink,
                        )
                    }
                }

                CloseButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = 2.dp,
        modifier = modifier.size(28.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Close",
            modifier = Modifier.padding(6.dp),
        )
    }
}

@Composable
private fun Bubble(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 4.dp,
        modifier = Modifier.widthIn(max = 240.dp),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
