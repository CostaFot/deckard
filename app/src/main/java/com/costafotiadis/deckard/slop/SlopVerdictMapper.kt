package com.costafotiadis.deckard.slop

import com.costafotiadis.deckard.net.model.ApiPangramDetection
import com.costafotiadis.deckard.net.model.ApiPangramWindow
import javax.inject.Inject

/**
 * Maps a Pangram [ApiPangramDetection] (API layer) to a [DomainSlopVerdict] (domain layer). The two
 * are near-1:1 apart from the judgement itself, which is derived — see [labelOf].
 */
class SlopVerdictMapper @Inject constructor() {

    fun map(detection: ApiPangramDetection): DomainSlopVerdict {
        val windows = detection.windows.orEmpty()
        return DomainSlopVerdict(
            label = labelOf(detection),
            summary = detection.headline.orEmpty(),
            predictionShort = detection.predictionShort.orEmpty(),
            headline = detection.headline.orEmpty(),
            prediction = detection.prediction.orEmpty(),
            fractionAi = detection.fractionAi ?: 0.0,
            fractionAiAssisted = detection.fractionAiAssisted ?: 0.0,
            fractionHuman = detection.fractionHuman ?: 0.0,
            numAiSegments = detection.numAiSegments ?: 0,
            numAiAssistedSegments = detection.numAiAssistedSegments ?: 0,
            numHumanSegments = detection.numHumanSegments ?: 0,
            dashboardLink = detection.dashboardLink,
            windows = windows.map(::mapWindow),
            version = detection.version.orEmpty(),
            wordCount = windows.sumOf { it.wordCount ?: 0 },
            analyzedText = detection.text.orEmpty(),
            confidence = windows.maxByOrNull { it.wordCount ?: 0 }?.confidence.orEmpty(),
        )
    }

    /**
     * The judgement, three ways. Pangram's own overall call is the answer — `Human`, `Mixed` or
     * `AI` — and `Mixed` is the middle that a boolean used to throw away, stamping a half-written
     * passage as if a machine had written all of it.
     *
     * A response missing that call is read from the fractions instead, largest share winning and
     * ties going to the graver reading. With no evidence at all it is nobody's fault: human.
     */
    private fun labelOf(detection: ApiPangramDetection): SlopLabel = when (detection.predictionShort) {
        AI -> SlopLabel.AI
        MIXED -> SlopLabel.ASSISTED
        HUMAN -> SlopLabel.HUMAN
        else -> dominantFraction(detection)
    }

    private fun dominantFraction(detection: ApiPangramDetection): SlopLabel = listOf(
        SlopLabel.AI to (detection.fractionAi ?: 0.0),
        SlopLabel.ASSISTED to (detection.fractionAiAssisted ?: 0.0),
        SlopLabel.HUMAN to (detection.fractionHuman ?: 0.0),
    ).maxBy { it.second }.takeIf { it.second > 0.0 }?.first ?: SlopLabel.HUMAN

    private fun mapWindow(window: ApiPangramWindow): DomainSlopWindow = DomainSlopWindow(
        text = window.text.orEmpty(),
        label = window.label.orEmpty(),
        aiAssistanceScore = window.aiAssistanceScore ?: 0.0,
        confidence = window.confidence.orEmpty(),
        startIndex = window.startIndex ?: 0,
        endIndex = window.endIndex ?: 0,
        wordCount = window.wordCount ?: 0,
        tokenLength = window.tokenLength ?: 0,
    )

    private companion object {
        const val AI = "AI"
        const val MIXED = "Mixed"
        const val HUMAN = "Human"
    }
}
