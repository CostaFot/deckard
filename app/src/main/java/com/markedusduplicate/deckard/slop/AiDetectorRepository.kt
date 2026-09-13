package com.markedusduplicate.deckard.slop

import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.common.result.Result
import com.markedusduplicate.common.result.attempt
import com.markedusduplicate.deckard.BuildConfig
import com.markedusduplicate.deckard.net.PangramService
import com.markedusduplicate.deckard.net.model.ApiPangramDetection
import com.markedusduplicate.deckard.net.model.ApiPangramTaskRequest
import com.markedusduplicate.deckard.net.model.ApiPangramWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Boundary for AI-content ("slop") detection: callers hand it the on-screen text and get back a
 * [DomainSlopVerdict]. Maps API → domain only (no UI knowledge); the domain → UI step lives in
 * [DetectSlopUseCase].
 *
 * Backed by Pangram ([PangramService]), whose API is asynchronous: [detect] submits the text with
 * `POST /task`, polls `GET /task/{id}` until the task reaches a terminal stage, and maps a
 * successful result via [SlopVerdictMapper]. The whole flow runs on `dispatcherProvider.io` and is
 * wrapped with `attempt {}`, so any network error, failure stage, or timeout surfaces as a
 * [Result.Error] and callers degrade gracefully.
 */
@Singleton
class AiDetectorRepository @Inject constructor(
    private val pangramService: PangramService,
    private val slopVerdictMapper: SlopVerdictMapper,
    private val dispatcherProvider: DispatcherProvider,
) {
    suspend fun detect(text: String): Result<Throwable, DomainSlopVerdict> {
        mockedDetection(text)?.let { return Result.Success(slopVerdictMapper.map(it)) }
        return withContext(dispatcherProvider.io) {
            attempt {
                val taskId = pangramService
                    .createTask(ApiPangramTaskRequest(text, publicDashboardLink = true))
                    .taskId
                val detection = poll(taskId)
                when (detection.stage) {
                    STAGE_SUCCESS -> slopVerdictMapper.map(detection)
                    else -> error("Pangram detection ${detection.stage}: ${detection.headline}")
                }
            }
        }
    }

    /**
     * A canned detection, for seeing the report card without spending a Pangram call. Off unless the
     * debug build was assembled with `-PmockVerdict=ai|assisted|human|mixed`; release hard-wires the
     * field to `off`, so the switch cannot survive a shipped build.
     *
     * It returns an API payload rather than a domain verdict so the real [SlopVerdictMapper] still
     * derives the label — a mock that picked its own could disagree with what the app would do.
     */
    private fun mockedDetection(text: String): ApiPangramDetection? {
        val mock = when (BuildConfig.MOCK_VERDICT) {
            MOCK_AI -> Mock("AI", "Fully AI-Generated", ai = 1.0, assisted = 0.0, human = 0.0)
            MOCK_ASSISTED -> Mock("Mixed", "Lightly AI-Assisted", ai = 0.12, assisted = 0.63, human = 0.25)
            MOCK_HUMAN -> Mock("Human", "Mostly Human Written", ai = 0.0, assisted = 0.06, human = 0.94)
            MOCK_MIXED -> Mock("Mixed", "Mixed", ai = 0.48, assisted = 0.31, human = 0.21)
            else -> return null
        }
        return ApiPangramDetection(
            stage = STAGE_SUCCESS,
            text = text,
            version = "mock",
            headline = mock.headline,
            prediction = "A canned verdict. No detector was consulted.",
            predictionShort = mock.predictionShort,
            fractionAi = mock.ai,
            fractionAiAssisted = mock.assisted,
            fractionHuman = mock.human,
            numAiSegments = 1,
            numAiAssistedSegments = 1,
            numHumanSegments = 1,
            dashboardLink = "https://www.pangram.com/",
            windows = listOf(
                ApiPangramWindow(
                    text = text,
                    label = mock.headline,
                    confidence = "High",
                    wordCount = wordCount(text),
                ),
            ),
        )
    }

    /** One canned verdict. The wording is Pangram's own vocabulary, so the card reads as it would. */
    private data class Mock(
        val predictionShort: String,
        val headline: String,
        val ai: Double,
        val assisted: Double,
        val human: Double,
    )

    private suspend fun poll(taskId: String): ApiPangramDetection {
        repeat(MAX_POLL_ATTEMPTS) {
            val detection = pangramService.getTask(taskId)
            if (detection.stage == STAGE_SUCCESS || detection.stage == STAGE_FAILED) {
                return detection
            }
            delay(POLL_INTERVAL_MS)
        }
        error("Pangram detection timed out after ${MAX_POLL_ATTEMPTS * POLL_INTERVAL_MS} ms")
    }

    private companion object {
        const val STAGE_SUCCESS = "STAGE_SUCCESS"
        const val STAGE_FAILED = "STAGE_FAILED"
        const val POLL_INTERVAL_MS = 1500L
        const val MAX_POLL_ATTEMPTS = 40
        const val MOCK_AI = "ai"
        const val MOCK_ASSISTED = "assisted"
        const val MOCK_HUMAN = "human"
        const val MOCK_MIXED = "mixed"
    }
}
