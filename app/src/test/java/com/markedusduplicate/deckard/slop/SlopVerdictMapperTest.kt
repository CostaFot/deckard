package com.markedusduplicate.deckard.slop

import com.markedusduplicate.deckard.net.model.ApiPangramDetection
import com.markedusduplicate.deckard.net.model.ApiPangramWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlopVerdictMapperTest {

    private val mapper = SlopVerdictMapper()

    @Test
    fun `maps a success detection to the domain verdict`() {
        val detection = ApiPangramDetection(
            stage = "STAGE_SUCCESS",
            text = "the text",
            version = "3.0",
            headline = "AI Detected",
            prediction = "We are confident this document contains AI-generated content.",
            predictionShort = "Mixed",
            fractionAi = 0.70,
            fractionAiAssisted = 0.20,
            fractionHuman = 0.10,
            numAiSegments = 7,
            numAiAssistedSegments = 2,
            numHumanSegments = 1,
            dashboardLink = "https://www.pangram.com/history/abc",
            windows = listOf(
                ApiPangramWindow(
                    text = "seg",
                    label = "AI-Generated",
                    aiAssistanceScore = 0.9,
                    confidence = "High",
                    startIndex = 0,
                    endIndex = 3,
                    wordCount = 1,
                    tokenLength = 1,
                ),
            ),
        )

        val verdict = mapper.map(detection)

        assertTrue(verdict.isAi)
        assertEquals(0.70, verdict.aiLikelihood, 0.0)
        assertEquals("AI Detected", verdict.summary)
        assertEquals("Mixed", verdict.predictionShort)
        assertEquals(0.20, verdict.fractionAiAssisted, 0.0)
        assertEquals(7, verdict.numAiSegments)
        assertEquals("https://www.pangram.com/history/abc", verdict.dashboardLink)
        assertEquals(1, verdict.windows.size)
        assertEquals("AI-Generated", verdict.windows.first().label)
    }

    @Test
    fun `human prediction is not slop`() {
        val verdict =
            mapper.map(ApiPangramDetection(stage = "STAGE_SUCCESS", predictionShort = "Human"))

        assertFalse(verdict.isAi)
    }

    @Test
    fun `null fields fall back to safe defaults`() {
        val verdict = mapper.map(ApiPangramDetection(stage = "STAGE_SUCCESS"))

        assertEquals(0.0, verdict.aiLikelihood, 0.0)
        assertEquals("", verdict.summary)
        assertTrue(verdict.windows.isEmpty())
    }
}
