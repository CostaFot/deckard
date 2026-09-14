package com.costafotiadis.deckard.slop

import com.costafotiadis.common.result.Result
import com.costafotiadis.deckard.CoroutinesTestRule
import com.costafotiadis.deckard.net.PangramService
import com.costafotiadis.deckard.net.model.ApiPangramDetection
import com.costafotiadis.deckard.net.model.ApiPangramModels
import com.costafotiadis.deckard.net.model.ApiPangramTaskCreated
import com.costafotiadis.deckard.net.model.ApiPangramTaskRequest
import com.costafotiadis.deckard.net.model.ApiPangramWindow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Covers the half of [AiDetectorRepository] that decides *which* detector judges the text. Leaving
 * `model` off the request is not an error Pangram reports — it just answers with its default, older
 * model — so nothing but a test keeps the app and the site on the same one.
 */
class AiDetectorRepositoryTest {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    private val pangramService = mockk<PangramService>()

    private val repository = AiDetectorRepository(
        pangramService,
        SlopVerdictMapper(),
        coroutinesTestRule.testDispatcherProvider,
    )

    @Test
    fun `asks for the pinned model rather than Pangram's default`() = runTest {
        givenKeyWith("default", "pangram-4")
        val request = slot<ApiPangramTaskRequest>()
        coEvery { pangramService.createTask(capture(request)) } returns ApiPangramTaskCreated(TASK_ID)
        coEvery { pangramService.getTask(TASK_ID) } returns SUCCESS

        val result = repository.detect("a passage to judge")

        assertTrue(result is Result.Success)
        assertEquals("pangram-4", request.captured.model)
        assertTrue(request.captured.publicDashboardLink)
    }

    @Test
    fun `fails without spending a call when the key cannot run the model`() = runTest {
        givenKeyWith("default")

        val result = repository.detect("a passage to judge")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).value.message!!.contains("pangram-4"))
        coVerify(exactly = 0) { pangramService.createTask(any()) }
    }

    @Test
    fun `asks which models the key has once, not once per verdict`() = runTest {
        givenKeyWith("default", "pangram-4")
        coEvery { pangramService.createTask(any()) } returns ApiPangramTaskCreated(TASK_ID)
        coEvery { pangramService.getTask(TASK_ID) } returns SUCCESS

        repository.detect("the first passage")
        repository.detect("the second passage")

        coVerify(exactly = 1) { pangramService.getModels() }
        coVerify(exactly = 2) { pangramService.createTask(any()) }
    }

    private fun givenKeyWith(vararg models: String) {
        coEvery { pangramService.getModels() } returns ApiPangramModels(models.toList())
    }

    private companion object {
        const val TASK_ID = "task-1"

        val SUCCESS = ApiPangramDetection(
            stage = "STAGE_SUCCESS",
            text = "a passage to judge",
            version = "4.0",
            headline = "Human Written",
            predictionShort = "Human",
            fractionAi = 0.0,
            fractionAiAssisted = 0.0,
            fractionHuman = 1.0,
            windows = listOf(
                ApiPangramWindow(label = "Human Written", confidence = "High", wordCount = 4),
            ),
        )
    }
}
