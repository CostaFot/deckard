package com.markedusduplicate.deckard.mascot

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.markedusduplicate.deckard.R
import com.markedusduplicate.design.theme.AppTheme

/**
 * A slim, always-present tab pinned to the left edge. A left→right swipe across it summons Deckard
 * ([onSummon]) — the "back-gesture, but on one spot only" the feature is named after. A **long-press**
 * on the tab triggers [onLongPress] instead: the slower screenshot + OCR "pick the post" read.
 *
 * The catch on Android 10+: the screen edges are reserved for the system back gesture, which would
 * otherwise eat our swipe. We declare this view's bounds via [setSystemGestureExclusionRects] so the
 * system yields that small region to us. The exclusion budget is ~200dp tall per edge, so the tab is
 * kept short.
 */
@SuppressLint("ViewConstructor")
class DeckardEdgeHandleView(
    context: Context,
    private val onSummon: () -> Unit,
    private val onLongPress: () -> Unit,
) : AbstractComposeView(context) {

    init {
        id = R.id.deckardEdgeHandleView
        addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            systemGestureExclusionRects = listOf(Rect(0, 0, right - left, bottom - top))
        }
    }

    @Composable
    override fun Content() {
        val view = LocalView.current
        AppTheme {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(140.dp)
                    .pointerInput(Unit) {
                        val threshold = 48.dp.toPx()
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = { if (total > threshold) onSummon() },
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                total += amount
                            },
                        )
                    }
                    // A stationary hold fires the long-press; a swipe moves past touch slop and
                    // cancels it, falling through to the horizontal-drag summon above.
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onLongPress()
                            },
                        )
                    },
                contentAlignment = Alignment.CenterStart,
            ) {
                // the visible nub; the whole 36dp-wide box is the swipe target
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .width(6.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                )
            }
        }
    }
}
