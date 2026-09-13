package com.markedusduplicate.deckard.mascot

import com.markedusduplicate.deckard.R
import com.markedusduplicate.deckard.slop.MIN_WORDS_TO_DETECT
import com.markedusduplicate.deckard.slop.ScreenReadFailure
import com.markedusduplicate.deckard.slop.SlopLabel
import com.markedusduplicate.textresource.TextResource

/**
 * Every word Deckard says, in one place.
 *
 * He is a weary scholar and he is tired of reading machine-written prose. Deadpan, short, never
 * pleased with itself; he reports what he found and leaves you to feel about it. The states he can
 * be in carry facts ([DeckardState], [NoVerdict], [ReadMethod]) and this is the only file that
 * decides which English any of them gets — the register is one thing you can read top to bottom, so
 * it can't drift a line at a time inside a data class.
 *
 * The words themselves live in `res/values/strings.xml` under `voice_*`, and this file names them.
 * He hands back a [TextResource] rather than a `String` because most of what he says is decided
 * away from the UI — by the overlay `Service`, by the share target — and those have no business
 * resolving copy. Whoever draws it resolves it: `asString()` in a composable, `asString(context)`
 * anywhere else.
 *
 * The `when`s are exhaustive over sealed types, so an unwritten line is a compile error rather than
 * a blank bubble.
 *
 * Not the name's reference. "Deckard" points at the replicant hunter, which is the joke in the post
 * and on the repo; the man himself is the wise elder. Don't cross the streams — and no Horadrim.
 */
object DeckardVoice {

    /** His one line, on the verdict that earns it and on the setup screen's masthead. */
    val CATCHPHRASE: TextResource = TextResource.simple(R.string.voice_catchphrase)

    /**
     * The looking-at-your-screen line. Skimming and squinting are the honest words for the two
     * screen reads — one takes what the app already exposes, the other stares at a picture of it
     * for a few seconds — so the wait each implies is the right one.
     */
    fun thinking(how: ReadMethod): TextResource = when (how) {
        ReadMethod.Tree -> TextResource.simple(R.string.voice_thinking_tree)
        ReadMethod.Screenshot -> TextResource.simple(R.string.voice_thinking_screenshot)
        ReadMethod.SharedText -> TextResource.simple(R.string.voice_thinking_shared_text)
    }

    /** What he says when there is no verdict to give. */
    fun setback(reason: NoVerdict): TextResource = when (reason) {
        is NoVerdict.CouldNotRead -> couldNotRead(reason.failure)
        NoVerdict.NotEnoughText -> TextResource.simple(R.string.voice_too_thin, MIN_WORDS_TO_DETECT)
        NoVerdict.DetectorUnreachable -> TextResource.simple(R.string.voice_detector_unreachable)
    }

    /**
     * His note in the margin of the report card. It never quotes a figure: the stamp above it
     * already carries the human share, and a number said twice is a number you start doubting.
     */
    fun remark(verdict: UiSlopVerdict): TextResource = when (verdict.label) {
        SlopLabel.AI -> CATCHPHRASE
        SlopLabel.ASSISTED -> TextResource.simple(R.string.voice_remark_assisted)
        SlopLabel.HUMAN -> TextResource.simple(R.string.voice_remark_human)
    }

    /** Shown by the share target, before there is an overlay to say anything in. */
    val NOTHING_SHARED: TextResource = TextResource.simple(R.string.voice_nothing_shared)
    val NEEDS_OVERLAY_PERMISSION: TextResource =
        TextResource.simple(R.string.voice_needs_overlay_permission)

    private fun couldNotRead(failure: ScreenReadFailure): TextResource = when (failure) {
        ScreenReadFailure.NoAccessibilityService ->
            TextResource.simple(R.string.voice_no_accessibility_service)

        ScreenReadFailure.ModelNotReady -> TextResource.simple(R.string.voice_model_not_ready)
        ScreenReadFailure.ScreenshotFailed -> TextResource.simple(R.string.voice_screenshot_failed)
        ScreenReadFailure.TranscriptionFailed ->
            TextResource.simple(R.string.voice_transcription_failed)

        ScreenReadFailure.NoTextFound -> TextResource.simple(R.string.voice_no_text_found)
    }
}
