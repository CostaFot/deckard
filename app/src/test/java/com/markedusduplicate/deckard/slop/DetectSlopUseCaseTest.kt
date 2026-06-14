package com.markedusduplicate.deckard.slop

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
    fun `judges text at or above the word threshold`() = runTest {
        val result = useCase()(text(words = MIN_WORDS_TO_DETECT))

        assertTrue(result is SlopCheck.Judged)
        assertTrue((result as SlopCheck.Judged).verdict.isAi)
    }

    @Test
    fun `skips detection for text below the word threshold`() = runTest {
        val result = useCase()(text(words = MIN_WORDS_TO_DETECT - 1))

        assertEquals(SlopCheck.NotEnoughText, result)
    }

    private fun text(words: Int): String = (1..words).joinToString(" ") { "word" }

    private fun useCase(): DetectSlopUseCase {
        val repository = AiDetectorRepository(
            pangramService = FakePangramService(),
            slopVerdictMapper = SlopVerdictMapper(),
            dispatcherProvider = coroutinesTestRule.testDispatcherProvider,
        )
        return DetectSlopUseCase(repository, coroutinesTestRule.testDispatcherProvider)
    }

    private class FakePangramService : PangramService {
        override suspend fun createTask(request: ApiPangramTaskRequest): ApiPangramTaskCreated =
            ApiPangramTaskCreated(taskId = "task-1")

        override suspend fun getTask(taskId: String): ApiPangramDetection =
            ApiPangramDetection(stage = "STAGE_SUCCESS")
    }
}
