package com.markedusduplicate.deckard.mascot

import com.markedusduplicate.deckard.slop.MIN_WORDS_TO_DETECT
import com.markedusduplicate.deckard.slop.ScreenReadFailure
import com.markedusduplicate.deckard.slop.SlopLabel

/**
 * Every word Deckard says, in one place.
 *
 * He is a weary scholar and he is tired of reading machine-written prose. Deadpan, short, never
 * pleased with itself; he reports what he found and leaves you to feel about it. The states he can
 * be in carry facts ([DeckardState], [NoVerdict], [ReadMethod]) and this is the only file that
 * turns any of them into English — the register is one thing you can read top to bottom, so it
 * can't drift a line at a time inside a data class.
 *
 * The `when`s are exhaustive over sealed types, so an unwritten line is a compile error rather than
 * a blank bubble.
 *
 * Not the name's reference. "Deckard" points at the replicant hunter, which is the joke in the post
 * and on the repo; the man himself is the wise elder. Don't cross the streams — and no Horadrim.
 */
object DeckardVoice {

    /** His one line, on the verdict that earns it and on the setup screen's masthead. */
    const val CATCHPHRASE = "Slop, my son."

    /**
     * The looking-at-your-screen line. Skimming and squinting are the honest words for the two
     * screen reads — one takes what the app already exposes, the other stares at a picture of it
     * for a few seconds — so the wait each implies is the right one.
     */
    fun thinking(how: ReadMethod): String = when (how) {
        ReadMethod.Tree -> "Skimming the screen"
        ReadMethod.Screenshot -> "Squinting at the screen"
        ReadMethod.SharedText -> "Reading what you brought me"
    }

    /** What he says when there is no verdict to give. */
    fun setback(reason: NoVerdict): String = when (reason) {
        is NoVerdict.CouldNotRead -> couldNotRead(reason.failure)
        NoVerdict.NotEnoughText -> "Too thin to judge. Bring me $MIN_WORDS_TO_DETECT words."
        NoVerdict.DetectorUnreachable -> "I sent it out for a second opinion. Nobody answered."
    }

    /**
     * His note in the margin of the report card. It never quotes a figure: the stamp above it
     * already carries the human share, and a number said twice is a number you start doubting.
     */
    fun remark(verdict: UiSlopVerdict): String = when (verdict.label) {
        SlopLabel.AI -> CATCHPHRASE
        SlopLabel.ASSISTED -> "Someone started this. Something else finished it."
        SlopLabel.HUMAN -> "A person wrote this. You don't see that often."
    }

    /** Shown by the share target, before there is an overlay to say anything in. */
    const val NOTHING_SHARED = "You've brought me nothing to read."
    const val NEEDS_OVERLAY_PERMISSION = "Let me draw over other apps first."

    private fun couldNotRead(failure: ScreenReadFailure): String = when (failure) {
        ScreenReadFailure.NoAccessibilityService ->
            "You haven't let me read the screen yet. Until you do, I'm decoration."

        ScreenReadFailure.ModelNotReady ->
            "No eyes for pictures yet. Swipe instead; that read needs no model."

        ScreenReadFailure.ScreenshotFailed -> "I reached for the screen and came back holding nothing."
        ScreenReadFailure.TranscriptionFailed -> "I stared at it a good while and made nothing of it."
        ScreenReadFailure.NoTextFound -> "Not a word on this screen. Nothing to weigh."
    }
}
