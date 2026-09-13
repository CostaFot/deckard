package com.markedusduplicate.deckard.mascot

/**
 * What the floating mascot is doing: hidden, reading the screen one of the ways he can, delivering a
 * full slop [Verdict] report, or with nothing to show for it.
 *
 * Every variant carries facts. Turning them into Deckard's own words happens once, in
 * [DeckardVoice], at the point they're drawn.
 */
sealed interface DeckardState {
    data object Hidden : DeckardState
    data class Thinking(val how: ReadMethod) : DeckardState
    data class Verdict(val verdict: UiSlopVerdict) : DeckardState
    data class Unavailable(val reason: NoVerdict) : DeckardState
}
