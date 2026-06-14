package com.markedusduplicate.deckard.mascot

import android.annotation.SuppressLint
import android.content.Context
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
