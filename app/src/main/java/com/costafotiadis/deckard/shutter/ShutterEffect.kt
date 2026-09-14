package com.costafotiadis.deckard.shutter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.costafotiadis.deckard.R
import com.costafotiadis.textresource.TextResource

/**
 * The ways Deckard can say he just photographed your screen, and the one where he doesn't.
 *
 * Four of these exist at once on purpose: an effect drawn over an arbitrary app cannot be judged in
 * a preview or argued about in the abstract, so they are all built and all selectable from the
 * settings screen. They stay that way — the choice is the user's, not one winner's (COS-244 wanted
 * to cull them to one and was dropped). [None] is not a placeholder — a comparison without a
 * baseline is not a comparison.
 *
 * [key] is what gets persisted and what `scripts/deckard effect` names, so it outlives any renaming
 * of the constant. The labels are constructor arguments rather than constants because
 * `android.nonFinalResIds=true` means `R.string.*` is not a compile-time constant here.
 */
enum class ShutterEffect(
    val key: String,
    val label: TextResource,
    val painter: ShutterPainter,
) {
    None("none", TextResource.simple(R.string.settings_shutter_none), NoShutterPainter),
    CropMarks("crop_marks", TextResource.simple(R.string.settings_shutter_crop_marks), CropMarksPainter),
    Bloom("bloom", TextResource.simple(R.string.settings_shutter_bloom), EdgeBloomPainter),
    Highlight("highlight", TextResource.simple(R.string.settings_shutter_highlight), TravellingHighlightPainter),
    Stamp("stamp", TextResource.simple(R.string.settings_shutter_stamp), ScreenStampPainter),
    ;

    companion object {
        /** What a fresh install gets: the cheapest to draw, and the one most clearly about paper. */
        val Default: ShutterEffect = CropMarks

        /** The stored [key] back into an effect, falling back to [Default] on anything unrecognised. */
        fun fromKey(key: String?): ShutterEffect = entries.firstOrNull { it.key == key } ?: Default
    }
}

/** [ShutterEffect.None]: the screen says nothing, which is the thing the others have to beat. */
internal object NoShutterPainter : ShutterPainter {
    override fun DrawScope.paint(frame: ShutterFrame, ink: Color) = Unit
}
