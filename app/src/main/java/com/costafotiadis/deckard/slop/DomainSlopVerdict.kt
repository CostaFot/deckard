package com.costafotiadis.deckard.slop

/**
 * Domain verdict on whether a piece of text is AI-generated ("slop"). Carries the full detection
 * breakdown for now (mapped from the API layer); the consumer-facing shape will be refined later.
 * Provider-agnostic — independent of which detection backend produced it.
 *
 * [label] is the judgement: three-way, because the detector's is. What the verdict *reads* as is
 * [headline] (the detector's own phrase) over [fractionHuman], the one share that means the same
 * thing under every label.
 */
data class DomainSlopVerdict(
    val label: SlopLabel,
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
    val version: String,
    val wordCount: Int,
    val analyzedText: String,
    val confidence: String,
)
