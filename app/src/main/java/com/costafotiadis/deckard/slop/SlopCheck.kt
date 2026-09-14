package com.costafotiadis.deckard.slop

import com.costafotiadis.deckard.mascot.UiSlopVerdict

/**
 * Outcome of [DetectSlopUseCase]: a delivered [Judged] verdict, [NotEnoughText] when the on-screen
 * text is too short to be worth sending to the detector, or [Failed] when detection errored.
 */
sealed interface SlopCheck {
    data class Judged(val verdict: UiSlopVerdict) : SlopCheck
    data object NotEnoughText : SlopCheck
    data object Failed : SlopCheck
}
