package com.costafotiadis.deckard.mascot

import android.annotation.SuppressLint
import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
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
import com.costafotiadis.deckard.R
import com.costafotiadis.design.theme.AppTheme

/**
 * A slim, always-present tab pinned to the left edge. A **long-press** on it (with a haptic tick)
 * summons Deckard via [onLongPress]: the screenshot + OCR "pick the post" read.
 *
 * The tab no longer claims its patch of the edge from the system back gesture: a hold never moves,
 * so the gesture navigation has nothing to take, and a swipe across the tab is the app's back
 * gesture like anywhere else on the edge.
 */
@SuppressLint("ViewConstructor")
class DeckardEdgeHandleView(
    context: Context,
    private val onLongPress: () -> Unit,
) : AbstractComposeView(context) {

    init {
        id = R.id.deckardEdgeHandleView
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
                        detectTapGestures(
                            onLongPress = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onLongPress()
                            },
                        )
                    },
                contentAlignment = Alignment.CenterStart,
            ) {
                // the visible nub; the whole 36dp-wide box is the hold target
                Box(
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .width(6.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)),
                )
            }
        }
    }
}
