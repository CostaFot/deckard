package com.markedusduplicate.deckard.slop

import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.common.result.fold
import com.markedusduplicate.deckard.mascot.UiSlopVerdict
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Judges whether the given text is AI "slop" and maps the domain verdict to its UI representation
 * ([UiSlopVerdict]) — the domain → UI seam the overlay calls (it never touches the repository
 * directly). Screen reading stays with the caller; this use case takes the already-read text.
 *
 * Gates on [MIN_WORDS_TO_DETECT]: text below the threshold returns [SlopCheck.NotEnoughText] without
 * hitting the detector (short snippets can't be judged reliably and waste the call).
 *
 * Main-safe: all work runs on `dispatcherProvider.io`, so callers may invoke it from any thread.
 */
class DetectSlopUseCase @Inject constructor(
    private val aiDetectorRepository: AiDetectorRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend operator fun invoke(text: String): SlopCheck =
        withContext(dispatcherProvider.io) {
            if (wordCount(text) < MIN_WORDS_TO_DETECT) {
                return@withContext SlopCheck.NotEnoughText
            }
            aiDetectorRepository.detect(text).fold(
                ifError = { SlopCheck.Failed },
                ifSuccess = { SlopCheck.Judged(it.toUi()) },
            )
        }
}

private fun DomainSlopVerdict.toUi(): UiSlopVerdict = UiSlopVerdict(
    isAi = isAi,
    aiLikelihood = aiLikelihood,
    summary = summary,
    predictionShort = predictionShort,
    headline = headline,
    prediction = prediction,
    fractionAi = fractionAi,
    fractionAiAssisted = fractionAiAssisted,
    fractionHuman = fractionHuman,
    numAiSegments = numAiSegments,
    numAiAssistedSegments = numAiAssistedSegments,
    numHumanSegments = numHumanSegments,
    dashboardLink = dashboardLink,
    version = version,
    wordCount = wordCount,
    analyzedText = analyzedText,
    confidence = confidence,
    dominantLabel = dominantLabel,
)
