package com.markedusduplicate.deckard.mascot

/**
 * What the floating mascot is doing: hidden, thinking up a remark, speaking plain text, delivering a
 * full slop [Verdict] report, or unable to look.
 */
sealed interface DeckardState {
    data object Hidden : DeckardState
    data object Thinking : DeckardState
    data class Speaking(val remark: String) : DeckardState
    data class Verdict(val verdict: UiSlopVerdict) : DeckardState
    data class Unavailable(val reason: String) : DeckardState
}
