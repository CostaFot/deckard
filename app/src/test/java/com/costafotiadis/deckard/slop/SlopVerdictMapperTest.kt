package com.costafotiadis.deckard.slop

import com.costafotiadis.deckard.net.model.ApiPangramDetection
import com.costafotiadis.deckard.net.model.ApiPangramWindow
import org.junit.Assert.assertEquals
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

        assertEquals(SlopLabel.ASSISTED, verdict.label)
        assertEquals("AI Detected", verdict.summary)
        assertEquals("Mixed", verdict.predictionShort)
        assertEquals(0.20, verdict.fractionAiAssisted, 0.0)
        assertEquals(7, verdict.numAiSegments)
        assertEquals("https://www.pangram.com/history/abc", verdict.dashboardLink)
        assertEquals(1, verdict.windows.size)
        assertEquals("AI-Generated", verdict.windows.first().label)
        assertEquals("3.0", verdict.version)
        assertEquals("the text", verdict.analyzedText)
        assertEquals(1, verdict.wordCount)
        assertEquals("High", verdict.confidence)
    }

    @Test
    fun `a mixed passage is assisted, the middle a boolean used to lose`() {
        val verdict = mapper.map(
            ApiPangramDetection(
                stage = "STAGE_SUCCESS",
                predictionShort = "Mixed",
                fractionAi = 0.12,
                fractionAiAssisted = 0.63,
                fractionHuman = 0.25,
            ),
        )

        assertEquals(SlopLabel.ASSISTED, verdict.label)
    }

    @Test
    fun `the judgement follows the detector's own call, not the largest share`() {
        val verdict = mapper.map(
            ApiPangramDetection(
                stage = "STAGE_SUCCESS",
                predictionShort = "Mixed",
                fractionAi = 0.40,
                fractionAiAssisted = 0.0,
                fractionHuman = 0.60,
            ),
        )

        assertEquals(SlopLabel.ASSISTED, verdict.label)
    }

    @Test
    fun `human prediction is not slop`() {
        val verdict =
            mapper.map(ApiPangramDetection(stage = "STAGE_SUCCESS", predictionShort = "Human"))

        assertEquals(SlopLabel.HUMAN, verdict.label)
    }

    @Test
    fun `a call we don't recognise falls back to the largest share`() {
        val verdict = mapper.map(
            ApiPangramDetection(
                stage = "STAGE_SUCCESS",
                predictionShort = "Something New",
                fractionAi = 0.70,
                fractionAiAssisted = 0.20,
                fractionHuman = 0.10,
            ),
        )

        assertEquals(SlopLabel.AI, verdict.label)
    }

    @Test
    fun `null fields fall back to safe defaults`() {
        val verdict = mapper.map(ApiPangramDetection(stage = "STAGE_SUCCESS"))

        assertEquals(SlopLabel.HUMAN, verdict.label)
        assertEquals(0.0, verdict.fractionHuman, 0.0)
        assertEquals("", verdict.summary)
        assertTrue(verdict.windows.isEmpty())
    }
}
