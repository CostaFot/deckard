package com.costafotiadis.deckard.shutter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * One way of saying a picture was just taken of your screen.
 *
 * A painter over a rectangle and nothing more: it is handed the size it is drawing into, where it is
 * in the run, and the one ink it is allowed. Knowing nothing else is what lets the same code run
 * full-screen over another app and miniature inside the setup screen's picker — the picker is not a
 * mock-up of the effect, it is the effect.
 *
 * The ink is always `onSurface`. This product has no accent roles — the scheme is ink/paper and
 * `primary` *is* ink — and the three [StampInks][com.costafotiadis.design.theme.StampInks] each mean
 * one specific verdict, so borrowing one here would be a naming lie. The vocabulary is a page being
 * marked up, not a glow.
 *
 * Nothing here blurs. The vision inference this is covering runs on the same GPU, and an effect that
 * stutters exactly when it is meant to reassure is a failed effect.
 */
interface ShutterPainter {
    fun DrawScope.paint(frame: ShutterFrame, ink: Color)
}

/** Where a run is: the shutter closing, the wait, and the release once there is a verdict. */
enum class ShutterPhase { Snap, Working, Release }

/**
 * One frame of a run.
 *
 * [progress] runs 0..1 through the current [phase] and is the thing to animate on during [Snap] and
 * [Release]. It is meaningless during [Working], which lasts as long as the model takes — use [loop]
 * there, which keeps advancing across every phase so nothing jumps at a boundary.
 *
 * [scale] is 1 at full size and a fraction of that in the picker's miniature. Painters multiply
 * every dp by it, so a 56dp bloom over a phone becomes a proportionate one inside a 64dp box instead
 * of swallowing it whole.
 */
data class ShutterFrame(
    val phase: ShutterPhase,
    val progress: Float,
    val loop: Float,
    val scale: Float,
)
