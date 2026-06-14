package com.markedusduplicate.deckard.slop

import com.markedusduplicate.deckard.net.model.ApiPangramDetection
import com.markedusduplicate.deckard.net.model.ApiPangramWindow
import javax.inject.Inject

/**
 * Maps a Pangram [ApiPangramDetection] (API layer) to a [DomainSlopVerdict] (domain layer). The two
 * are near-1:1 for now; the `isAi`/`aiLikelihood` derivations are provisional (refined later).
 */
class SlopVerdictMapper @Inject constructor() {

    fun map(detection: ApiPangramDetection): DomainSlopVerdict {
        val predictionShort = detection.predictionShort.orEmpty()
        return DomainSlopVerdict(
            isAi = predictionShort != HUMAN,
            aiLikelihood = detection.fractionAi ?: 0.0,
            summary = detection.headline.orEmpty(),
            predictionShort = predictionShort,
            headline = detection.headline.orEmpty(),
            prediction = detection.prediction.orEmpty(),
            fractionAi = detection.fractionAi ?: 0.0,
            fractionAiAssisted = detection.fractionAiAssisted ?: 0.0,
            fractionHuman = detection.fractionHuman ?: 0.0,
            numAiSegments = detection.numAiSegments ?: 0,
            numAiAssistedSegments = detection.numAiAssistedSegments ?: 0,
            numHumanSegments = detection.numHumanSegments ?: 0,
            dashboardLink = detection.dashboardLink,
            windows = detection.windows.orEmpty().map(::mapWindow),
        )
    }

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
        const val HUMAN = "Human"
    }
}
