package com.costafotiadis.deckard.shutter

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The report card's own stamp, at the size of the whole screen: the same rule tilted the same degree
 * or two off the grid, pressed onto the display and lifted again when there is a verdict to print.
 *
 * Drawn in ink rather than in a [StampInk][com.costafotiadis.design.theme.StampInks], because during
 * the wait the verdict has no colour yet — it is a blank stamp, and it stays blank until Pangram
 * says otherwise. On release it collapses toward the left-edge midpoint, which is where the card is
 * about to appear, so the wait turns into the verdict instead of being replaced by it.
 */
internal object ScreenStampPainter : ShutterPainter {

    /** The tilt of [com.costafotiadis.deckard.mascot.SlopReportCard]'s stamp, to the degree. */
    private const val TILT = -1.8f

    private val RULE_INSET = 20.dp
    private val RULE_WIDTH = 3.dp
    private val RULE_CORNER = 18.dp
    private val HAIRLINE_GAP = 6.dp
    private val HAIRLINE_WIDTH = 1.dp

    override fun DrawScope.paint(frame: ShutterFrame, ink: Color) {
        fun px(value: Dp) = value.toPx() * frame.scale

        val reveal: Float
        val press: Float
        val alpha: Float
        val pivot: Offset

        when (frame.phase) {
            ShutterPhase.Snap -> {
                val eased = FastOutSlowInEasing.transform(frame.progress)
                reveal = eased
                press = 1.06f - 0.06f * eased
                alpha = eased
                pivot = center
            }

            ShutterPhase.Working -> {
                reveal = 1f
                press = 1f + 0.006f * sway(frame.loop)
                alpha = 0.62f + 0.18f * breathe(frame.loop)
                pivot = center
            }

            ShutterPhase.Release -> {
                val eased = FastOutSlowInEasing.transform(frame.progress)
                reveal = 1f
                press = 1f - 0.14f * eased
                alpha = 1f - eased
                pivot = Offset(0f, size.height / 2f)
            }
        }

        if (alpha <= 0f) return

        rotate(degrees = TILT, pivot = center) {
            scale(scale = press, pivot = pivot) {
                val rule = perimeterPath(size, px(RULE_INSET), px(RULE_CORNER))
                strokeBothWays(
                    measure = PathMeasure().apply { setPath(rule, true) },
                    fraction = reveal,
                    color = ink.copy(alpha = alpha * 0.85f),
                    width = px(RULE_WIDTH),
                )

                val hairline = perimeterPath(
                    size = size,
                    inset = px(RULE_INSET) + px(HAIRLINE_GAP),
                    corner = px(RULE_CORNER) - px(HAIRLINE_GAP),
                )
                strokeBothWays(
                    measure = PathMeasure().apply { setPath(hairline, true) },
                    fraction = reveal,
                    color = ink.copy(alpha = alpha * 0.35f),
                    width = px(HAIRLINE_WIDTH),
                )
            }
        }
    }
}
