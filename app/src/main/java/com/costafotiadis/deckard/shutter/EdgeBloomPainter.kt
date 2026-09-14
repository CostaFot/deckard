package com.costafotiadis.deckard.shutter

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Ink bleeding in from all four edges, swelling on the shutter and breathing while the model reads.
 *
 * The closest honest translation of Gemini's wobble with the colour taken out, and the one variant
 * that is a field rather than a mark — so it is also the heaviest to draw, four full-edge gradients
 * a frame, which is worth knowing when judging it against a live inference rather than an idle
 * screen.
 */
internal object EdgeBloomPainter : ShutterPainter {

    private val DEPTH = 56.dp
    private const val ALPHA = 0.22f

    override fun DrawScope.paint(frame: ShutterFrame, ink: Color) {
        val depthFactor: Float
        val alphaFactor: Float

        when (frame.phase) {
            ShutterPhase.Snap -> {
                val eased = FastOutSlowInEasing.transform(frame.progress)
                depthFactor = eased * (1f + 0.15f * sin(frame.progress * PI.toFloat()))
                alphaFactor = eased * (1f + 0.35f * sin(frame.progress * PI.toFloat()))
            }

            ShutterPhase.Working -> {
                depthFactor = 1f + 0.08f * sway(frame.loop)
                alphaFactor = 1f + 0.18f * sway(frame.loop)
            }

            ShutterPhase.Release -> {
                val left = 1f - FastOutLinearInEasing.transform(frame.progress)
                depthFactor = left
                alphaFactor = left
            }
        }

        val depth = (DEPTH.toPx() * frame.scale * depthFactor).coerceAtLeast(0f)
        val edge = ink.copy(alpha = (ALPHA * alphaFactor).coerceIn(0f, 1f))
        if (depth <= 0f || edge.alpha <= 0f) return

        drawRect(
            brush = Brush.verticalGradient(listOf(edge, Color.Transparent), 0f, depth),
            size = Size(size.width, depth),
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, edge),
                startY = size.height - depth,
                endY = size.height,
            ),
            topLeft = Offset(0f, size.height - depth),
            size = Size(size.width, depth),
        )
        drawRect(
            brush = Brush.horizontalGradient(listOf(edge, Color.Transparent), 0f, depth),
            size = Size(depth, size.height),
        )
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, edge),
                startX = size.width - depth,
                endX = size.width,
            ),
            topLeft = Offset(size.width - depth, 0f),
            size = Size(depth, size.height),
        )
    }
}
