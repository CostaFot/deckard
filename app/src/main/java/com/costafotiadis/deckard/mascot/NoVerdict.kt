package com.costafotiadis.deckard.mascot

import com.costafotiadis.deckard.slop.ScreenReadFailure

/**
 * Why Deckard has no verdict to give: he couldn't read the screen, there wasn't enough on it to be
 * worth judging, or the detector didn't answer.
 *
 * A fact, not a sentence — what he says about each of these is [DeckardVoice]'s business.
 */
sealed interface NoVerdict {
    data class CouldNotRead(val failure: ScreenReadFailure) : NoVerdict
    data object NotEnoughText : NoVerdict
    data object DetectorUnreachable : NoVerdict
}

/**
 * How the text in front of Deckard got to him. The two differ in what they cost and what they can
 * fail at — the screenshot read takes seconds and needs the on-device model, shared text involves
 * no screen read at all — so he doesn't announce them the same way.
 */
enum class ReadMethod {
    Screenshot,
    SharedText,
}
