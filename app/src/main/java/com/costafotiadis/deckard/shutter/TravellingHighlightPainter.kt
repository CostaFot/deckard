package com.costafotiadis.deckard.shutter

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/**
 * A band of ink tracked round the edge of the screen, the way a highlighter is drawn along a line —
 * one quick lap on the shutter, then slow laps for as long as the model is reading.
 *
 * The one variant that reads as *reading* rather than as *flash*, which is the honest thing to say
 * about a wait that is spent looking at a picture of your screen.
 */
internal object TravellingHighlightPainter : ShutterPainter {

    private val INSET = 6.dp
    private val CORNER = 28.dp
    private val WIDTH = 10.dp
    private const val LENGTH = 0.22f
    private const val PEAK = 0.55f

    /**
     * A gradient along a path needs a shader. Stacking a handful of sub-segments with rising alpha
     * fakes the taper for the cost of a few more strokes, which is the cheaper of the two next to a
     * GPU already running an inference.
     *
     * They are butt-capped, and that is not a detail: round caps overlap their neighbours by half a
     * stroke at every join, and translucent strokes that overlap composite darker — so the band
     * comes out as a string of beads instead of one taper.
     */
    private const val TAPER_STEPS = 10

    override fun DrawScope.paint(frame: ShutterFrame, ink: Color) {
        val head: Float
        val length: Float
        val peak: Float

        when (frame.phase) {
            ShutterPhase.Snap -> {
                head = FastOutSlowInEasing.transform(frame.progress)
                length = LENGTH * (0.4f + 0.6f * frame.progress)
                peak = PEAK * (frame.progress / 0.3f).coerceIn(0f, 1f)
            }

            ShutterPhase.Working -> {
                head = frame.loop
                length = LENGTH
                peak = PEAK * (0.75f + 0.25f * breathe(frame.loop))
            }

            ShutterPhase.Release -> {
                head = frame.loop
                length = LENGTH * (1f - frame.progress)
                peak = PEAK * (1f - frame.progress)
            }
        }

        if (length <= 0f || peak <= 0f) return

        val perimeter = perimeterPath(size, INSET.toPx() * frame.scale, CORNER.toPx() * frame.scale)
        val measure = PathMeasure().apply { setPath(perimeter, true) }
        val width = WIDTH.toPx() * frame.scale

        repeat(TAPER_STEPS) { step ->
            val near = (step + 1f) / TAPER_STEPS
            val far = step.toFloat() / TAPER_STEPS
            strokeSegment(
                measure = measure,
                start = head - length * (1f - far),
                end = head - length * (1f - near),
                color = ink.copy(alpha = peak * near * near),
                width = width,
                cap = StrokeCap.Butt,
            )
        }
    }
}
