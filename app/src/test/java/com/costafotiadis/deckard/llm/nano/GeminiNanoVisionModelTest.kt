package com.costafotiadis.deckard.llm.nano

import com.costafotiadis.deckard.llm.VisionFailure
import com.costafotiadis.deckard.llm.VisionReply
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Candidate
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.GenerateContentResponse
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GeminiNanoVisionModelTest {

    private val client = mockk<GenerativeModel>()

    /** A stage whose Activity always arrives, so the read runs; the model never sees the difference. */
    private lateinit var stage: ForegroundStage

    init {
        stage = ForegroundStage { token -> stage.arrived(token) }
        coEvery { client.checkStatus() } returns FeatureStatus.AVAILABLE
        every { client.download() } returns emptyFlow()
    }

    private fun TestScope.model(stage: ForegroundStage = this@GeminiNanoVisionModelTest.stage) =
        GeminiNanoVisionModel(
            client = client,
            stage = stage,
            scope = TestScope(UnconfinedTestDispatcher(testScheduler)),
            request = { _, prompt -> generateContentRequest(TextPart(prompt)) {} },
        )

    private fun replying(text: String, finishReason: Int = Candidate.FinishReason.STOP) {
        val candidate = mockk<Candidate> {
            every { this@mockk.text } returns text
            every { this@mockk.finishReason } returns finishReason
        }
        val response = mockk<GenerateContentResponse> { every { candidates } returns listOf(candidate) }
        coEvery { client.generateContent(any<GenerateContentRequest>()) } returns response
    }

    private fun failing(errorCode: Int) {
        coEvery { client.generateContent(any<GenerateContentRequest>()) } throws
            GenAiException("no", null, errorCode)
    }

    @Test
    fun `is ready once AICore says the model is available`() = runTest {
        assertTrue(model().isReady)
    }

    @Test
    fun `is not ready while the model is unavailable`() = runTest {
        coEvery { client.checkStatus() } returns FeatureStatus.UNAVAILABLE

        assertFalse(model().isReady)
    }

    @Test
    fun `asks for a downloadable model and checks again`() = runTest {
        coEvery { client.checkStatus() } returnsMany listOf(FeatureStatus.DOWNLOADABLE, FeatureStatus.AVAILABLE)

        val model = model()

        coVerify(exactly = 1) { client.download() }
        assertTrue(model.isReady)
    }

    @Test
    fun `reads with the phone in front`() = runTest {
        replying("the post")

        assertEquals(VisionReply.Text("the post"), model().read(JPEG, PROMPT))
    }

    @Test
    fun `hands the client the prompt`() = runTest {
        replying("the post")

        model().read(JPEG, PROMPT)

        coVerify { client.generateContent(match<GenerateContentRequest> { it.text?.textString == PROMPT }) }
    }

    @Test
    fun `will not read while the model is not available`() = runTest {
        coEvery { client.checkStatus() } returns FeatureStatus.DOWNLOADING
        replying("the post")

        assertEquals(VisionReply.Failed(VisionFailure.NotReady), model().read(JPEG, PROMPT))
        coVerify(exactly = 0) { client.generateContent(any<GenerateContentRequest>()) }
    }

    @Test
    fun `reports not being in front when nothing comes forward`() = runTest {
        replying("the post")
        val model = model(stage = ForegroundStage {})

        val read = async { model.read(JPEG, PROMPT) }
        advanceTimeBy(ForegroundStage.STEP_FORWARD_MILLIS + 1)

        assertEquals(VisionReply.Failed(VisionFailure.NotInFront), read.await())
        coVerify(exactly = 0) { client.generateContent(any<GenerateContentRequest>()) }
    }

    @Test
    fun `names AICore's refusals`() = runTest {
        val expected = mapOf(
            GenAiException.ErrorCode.BACKGROUND_USE_BLOCKED to VisionFailure.NotInFront,
            GenAiException.ErrorCode.RESPONSE_PROCESSING_ERROR to VisionFailure.Refused,
            GenAiException.ErrorCode.PER_APP_BATTERY_USE_QUOTA_EXCEEDED to VisionFailure.OutOfQuota,
            GenAiException.ErrorCode.NOT_AVAILABLE to VisionFailure.NotReady,
            GenAiException.ErrorCode.NEEDS_SYSTEM_UPDATE to VisionFailure.NotReady,
            GenAiException.ErrorCode.BUSY to VisionFailure.Failed,
            GenAiException.ErrorCode.REQUEST_TOO_LARGE to VisionFailure.Failed,
        )

        for ((code, failure) in expected) {
            failing(code)
            assertEquals("error $code", VisionReply.Failed(failure), model().read(JPEG, PROMPT))
        }
    }

    @Test
    fun `a model AICore says is gone is not ready afterwards`() = runTest {
        failing(GenAiException.ErrorCode.NOT_SUPPORTED)
        val model = model()

        model.read(JPEG, PROMPT)
        coEvery { client.checkStatus() } returns FeatureStatus.UNAVAILABLE

        assertFalse(model.isReady)
    }

    @Test
    fun `any other failure is just a failure`() = runTest {
        coEvery { client.generateContent(any<GenerateContentRequest>()) } throws IllegalStateException("boom")

        assertEquals(VisionReply.Failed(VisionFailure.Failed), model().read(JPEG, PROMPT))
    }

    @Test
    fun `an empty reply is empty text, not a failure`() = runTest {
        replying("", finishReason = Candidate.FinishReason.OTHER)

        assertEquals(VisionReply.Text(""), model().read(JPEG, PROMPT))
    }

    private companion object {
        const val PROMPT = "read this"
        val JPEG = byteArrayOf(1, 2, 3)
    }
}
