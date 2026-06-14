package com.markedusduplicate.deckard.mascot

/**
 * UI-layer representation of a slop verdict, shown in Deckard's speech bubble. Carries the scalar
 * breakdown for now and relies on its data-class [toString] for display ("toString in the bubble
 * for now"); per-segment windows are omitted as they don't fit the bubble.
 */
data class UiSlopVerdict(
    val isAi: Boolean,
    val aiLikelihood: Double,
    val summary: String,
    val predictionShort: String,
    val headline: String,
    val prediction: String,
    val fractionAi: Double,
    val fractionAiAssisted: Double,
    val fractionHuman: Double,
    val numAiSegments: Int,
    val numAiAssistedSegments: Int,
    val numHumanSegments: Int,
    val dashboardLink: String?,
)
