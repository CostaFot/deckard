package com.costafotiadis.deckard.shutter

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Registration marks, the way a page goes to print: four corner marks slide in from outside the
 * screen and a hairline draws itself round the perimeter, out from the summon tab in both
 * directions, closing opposite.
 *
 * The most literal reading of what this product is. The screen is the page and Deckard is marking it
 * up, so the announcement is the same mark a printer puts on a proof — which also makes it the
 * cheapest of the four to draw, four short strokes and a hairline.
 */
internal object CropMarksPainter : ShutterPainter {

    private val INSET = 12.dp
    private val ARM = 28.dp
    private val MARK_WIDTH = 2.dp
    private val HAIRLINE_WIDTH = 1.dp
    private val TRAVEL = 18.dp

    override fun DrawScope.paint(frame: ShutterFrame, ink: Color) {
        fun px(value: Dp) = value.toPx() * frame.scale

        val marks: Float
        val trace: Float
        val markAlpha: Float
        val hairAlpha: Float
        val tick: Float?

        when (frame.phase) {
            ShutterPhase.Snap -> {
                marks = FastOutSlowInEasing.transform((frame.progress / 0.7f).coerceIn(0f, 1f))
                trace = ((frame.progress - 0.45f) / 0.55f).coerceIn(0f, 1f)
                markAlpha = (frame.progress / 0.35f).coerceIn(0f, 1f)
                hairAlpha = 0.32f * trace
                tick = null
            }

            ShutterPhase.Working -> {
                marks = 1f
                trace = 1f
                markAlpha = 1f
                hairAlpha = 0.18f + 0.24f * breathe(frame.loop)
                tick = frame.loop
            }

            ShutterPhase.Release -> {
                val out = FastOutLinearInEasing.transform(frame.progress)
                marks = 1f - out
                trace = 1f
                markAlpha = 1f - out
                hairAlpha = 0.30f * (1f - out)
                tick = null
            }
        }

        val perimeter = perimeterPath(size, px(INSET), px(INSET))
        val measure = PathMeasure().apply { setPath(perimeter, true) }

        strokeBothWays(measure, trace, ink.copy(alpha = hairAlpha * 0.9f), px(HAIRLINE_WIDTH))

        if (tick != null) {
            strokeSegment(
                measure = measure,
                start = tick - TICK_LENGTH,
                end = tick,
                color = ink.copy(alpha = 0.55f),
                width = px(HAIRLINE_WIDTH) * 2f,
            )
        }

        if (markAlpha > 0f) {
            drawCornerMarks(
                inset = px(INSET),
                arm = px(ARM),
                width = px(MARK_WIDTH),
                drift = px(TRAVEL) * (1f - marks),
                color = ink.copy(alpha = markAlpha * 0.92f),
            )
        }
    }

    private fun DrawScope.drawCornerMarks(
        inset: Float,
        arm: Float,
        width: Float,
        drift: Float,
        color: Color,
    ) {
        val left = inset - drift
        val top = inset - drift
        val right = size.width - inset + drift
        val bottom = size.height - inset + drift

        fun mark(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(color, Offset(x, y), Offset(x + arm * dx, y), width, StrokeCap.Square)
            drawLine(color, Offset(x, y), Offset(x, y + arm * dy), width, StrokeCap.Square)
        }

        mark(left, top, 1f, 1f)
        mark(right, top, -1f, 1f)
        mark(right, bottom, -1f, -1f)
        mark(left, bottom, 1f, -1f)
    }

    private const val TICK_LENGTH = 0.12f
}
