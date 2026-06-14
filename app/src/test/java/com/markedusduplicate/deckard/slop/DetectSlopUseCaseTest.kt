package com.markedusduplicate.deckard.slop

import com.markedusduplicate.common.result.Result
import com.markedusduplicate.deckard.CoroutinesTestRule
import com.markedusduplicate.deckard.net.PangramService
import com.markedusduplicate.deckard.net.model.ApiPangramDetection
import com.markedusduplicate.deckard.net.model.ApiPangramTaskCreated
import com.markedusduplicate.deckard.net.model.ApiPangramTaskRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DetectSlopUseCaseTest {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Test
    fun `maps a successful detection through to the UI verdict`() = runTest {
        val useCase = useCaseFor(
            ApiPangramDetection(
                stage = "STAGE_SUCCESS",
                headline = "AI Detected",
                predictionShort = "AI",
                fractionAi = 0.917,
            ),
        )

        val result = useCase("some text")

        assertTrue(result is Result.Success)
        val ui = (result as Result.Success).value
        assertTrue(ui.isAi)
        assertEquals(0.917, ui.aiLikelihood, 0.0)
        assertEquals("AI Detected", ui.summary)
    }

    @Test
    fun `a failed task surfaces as an error`() = runTest {
        val useCase = useCaseFor(
            ApiPangramDetection(stage = "STAGE_FAILED", headline = "bad input"),
        )

        assertTrue(useCase("some text") is Result.Error)
    }

    private fun useCaseFor(detection: ApiPangramDetection): DetectSlopUseCase {
        val repository = AiDetectorRepository(
            pangramService = FakePangramService(detection),
            slopVerdictMapper = SlopVerdictMapper(),
            dispatcherProvider = coroutinesTestRule.testDispatcherProvider,
        )
        return DetectSlopUseCase(repository, coroutinesTestRule.testDispatcherProvider)
    }

    private class FakePangramService(
        private val detection: ApiPangramDetection,
    ) : PangramService {
        override suspend fun createTask(request: ApiPangramTaskRequest): ApiPangramTaskCreated =
            ApiPangramTaskCreated(taskId = "task-1")

        override suspend fun getTask(taskId: String): ApiPangramDetection = detection
    }
}
