package com.markedusduplicate.deckard.slop

/**
 * Domain verdict on whether a piece of text is AI-generated ("slop"). Carries the full detection
 * breakdown for now (mapped from the API layer); the consumer-facing shape will be refined later.
 * Provider-agnostic — independent of which detection backend produced it.
 */
data class DomainSlopVerdict(
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
    val windows: List<DomainSlopWindow>,
)
