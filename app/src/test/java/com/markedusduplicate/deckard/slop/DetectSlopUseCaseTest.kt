package com.markedusduplicate.deckard.slop

import com.markedusduplicate.common.result.Result
import com.markedusduplicate.deckard.CoroutinesTestRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DetectSlopUseCaseTest {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    private val aiDetectorRepository = mockk<AiDetectorRepository>()

    private val useCase = DetectSlopUseCase(aiDetectorRepository, coroutinesTestRule.testDispatcherProvider)

    @Test
    fun `judges text at or above the word threshold`() = runTest {
        coEvery { aiDetectorRepository.detect(any(), any()) } returns Result.Success(aiVerdict)

        val result = useCase(text(words = MIN_WORDS_TO_DETECT))

        assertTrue(result is SlopCheck.Judged)
        assertTrue((result as SlopCheck.Judged).verdict.isAi)
    }

    @Test
    fun `surfaces a detection failure`() = runTest {
        coEvery { aiDetectorRepository.detect(any(), any()) } returns Result.Error(RuntimeException("boom"))

        val result = useCase(text(words = MIN_WORDS_TO_DETECT))

        assertEquals(SlopCheck.Failed, result)
    }

    @Test
    fun `skips detection for text below the word threshold`() = runTest {
        val result = useCase(text(words = MIN_WORDS_TO_DETECT - 1))

        assertEquals(SlopCheck.NotEnoughText, result)
        coVerify(exactly = 0) { aiDetectorRepository.detect(any(), any()) }
    }

    private fun text(words: Int): String = (1..words).joinToString(" ") { "word" }

    private companion object {
        val aiVerdict = DomainSlopVerdict(
            isAi = true,
            aiLikelihood = 0.917,
            summary = "AI Detected",
            predictionShort = "AI",
            headline = "AI Detected",
            prediction = "likely AI",
            fractionAi = 0.917,
            fractionAiAssisted = 0.0,
            fractionHuman = 0.083,
            numAiSegments = 1,
            numAiAssistedSegments = 0,
            numHumanSegments = 0,
            dashboardLink = null,
            windows = emptyList(),
            version = "3.3.2",
            wordCount = 60,
            analyzedText = "word word word",
            confidence = "High",
            dominantLabel = "AI-Generated",
        )
    }
}
