package com.markedusduplicate.deckard.mascot

/** What the floating mascot is doing: hidden, thinking up a remark, speaking it, or unable to look. */
sealed interface DeckardState {
    data object Hidden : DeckardState
    data object Thinking : DeckardState
    data class Speaking(val remark: String) : DeckardState
    data class Unavailable(val reason: String) : DeckardState
}
