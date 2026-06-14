package com.markedusduplicate.deckard.slop

import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.common.result.Result
import com.markedusduplicate.common.result.attempt
import com.markedusduplicate.deckard.net.PangramService
import com.markedusduplicate.deckard.net.model.ApiPangramDetection
import com.markedusduplicate.deckard.net.model.ApiPangramTaskRequest
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
    suspend fun detect(text: String): Result<Throwable, DomainSlopVerdict> =
        withContext(dispatcherProvider.io) {
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
    }
}
