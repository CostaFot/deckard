package com.costafotiadis.deckard.shutter

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min

/**
 * The screen's outline as a path that starts at the **left-edge midpoint** and runs clockwise.
 *
 * That start point is the only reason this is built by hand instead of calling
 * [Path.addRoundRect], whose start is wherever Skia puts it: the left-edge midpoint is where the
 * summon tab lives, so every effect that travels the perimeter leaves from the place Deckard was
 * summoned from. It also makes "trace both ways at once" a one-liner — the first half of this path
 * runs up and over, the last half runs down and under, and they meet opposite.
 */
internal fun perimeterPath(size: Size, inset: Float, corner: Float): Path {
    val left = inset
    val top = inset
    val right = size.width - inset
    val bottom = size.height - inset
    val radius = min(corner, min(right - left, bottom - top) / 2f).coerceAtLeast(0f)
    val diameter = radius * 2f
    val midY = (top + bottom) / 2f

    return Path().apply {
        moveTo(left, midY)
        lineTo(left, top + radius)
        arcTo(Rect(left, top, left + diameter, top + diameter), 180f, 90f, false)
        lineTo(right - radius, top)
        arcTo(Rect(right - diameter, top, right, top + diameter), 270f, 90f, false)
        lineTo(right, bottom - radius)
        arcTo(Rect(right - diameter, bottom - diameter, right, bottom), 0f, 90f, false)
        lineTo(left + radius, bottom)
        arcTo(Rect(left, bottom - diameter, left + diameter, bottom), 90f, 90f, false)
        close()
    }
}

/**
 * Stroke the piece of [measure] between the [start] and [end] fractions of its length.
 *
 * Both fractions wrap rather than clamp, so a travelling segment can be asked for the piece either
 * side of the path's start point and gets it in two strokes instead of being flattened against the
 * beginning.
 */
internal fun DrawScope.strokeSegment(
    measure: PathMeasure,
    start: Float,
    end: Float,
    color: Color,
    width: Float,
    cap: StrokeCap = StrokeCap.Round,
) {
    val span = end - start
    if (span <= 0f || color.alpha <= 0f || width <= 0f) return
    val length = measure.length
    if (length <= 0f) return

    val stroke = Stroke(width = width, cap = cap)
    if (span >= 1f) {
        drawPath(segment(measure, 0f, length), color = color, style = stroke)
        return
    }

    val from = wrap(start) * length
    val to = from + span * length
    if (to <= length) {
        drawPath(segment(measure, from, to), color = color, style = stroke)
        return
    }
    drawPath(segment(measure, from, length), color = color, style = stroke)
    drawPath(segment(measure, 0f, to - length), color = color, style = stroke)
}

private fun wrap(fraction: Float): Float = fraction - floor(fraction)

private fun segment(measure: PathMeasure, from: Float, to: Float): Path =
    Path().also { measure.getSegment(from, to, it, true) }

/**
 * Trace a closed path out from its start point in both directions at once, [fraction] of the way to
 * the point opposite. Both the crop marks and the stamp draw themselves on this way, which is why
 * [perimeterPath] starts where it does.
 */
internal fun DrawScope.strokeBothWays(
    measure: PathMeasure,
    fraction: Float,
    color: Color,
    width: Float,
) {
    val half = (fraction.coerceIn(0f, 1f)) / 2f
    if (half <= 0f) return
    strokeSegment(measure, 0f, half, color, width, StrokeCap.Butt)
    strokeSegment(measure, 1f - half, 1f, color, width, StrokeCap.Butt)
}

/** 0 → 1 → 0 over one lap, eased so it reads as breathing rather than blinking. */
internal fun breathe(loop: Float): Float = (1f - cos(loop * 2f * PI.toFloat())) / 2f

/** [breathe] mapped to -1 → 1 → -1, for anything that swings either side of a resting value. */
internal fun sway(loop: Float): Float = breathe(loop) * 2f - 1f
