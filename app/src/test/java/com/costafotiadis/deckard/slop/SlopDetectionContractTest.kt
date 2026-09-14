package com.costafotiadis.deckard.slop

import com.costafotiadis.deckard.net.model.ApiPangramDetection
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the Pangram contract against real `STAGE_SUCCESS` payloads: the API model must deserialize
 * them (with the app's `ignoreUnknownKeys` config) and the mapper must turn them into the expected
 * domain verdict.
 *
 * Both were captured from `pangram-4`, the model [AiDetectorRepository] pins, over text of known
 * authorship — one passage a person wrote, one a machine wrote. The `version` assertion is what
 * ties them to that model: a payload from Pangram's default detector reads `3.3.2`. They also carry
 * fields the app has no model field for (`is_humanized`, `humanizer_score`), so deserializing them
 * is the proof that an added key does not break a verdict.
 */
class SlopDetectionContractTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val mapper = SlopVerdictMapper()

    @Test
    fun `deserializes and maps a real human-written response`() {
        val detection = json.decodeFromString<ApiPangramDetection>(HUMAN_RESPONSE)

        assertEquals("STAGE_SUCCESS", detection.stage)
        assertEquals("4.0", detection.version)
        assertEquals("Human", detection.predictionShort)
        assertEquals(1.0, detection.fractionHuman!!, 0.0)
        assertEquals(1, detection.windows!!.size)
        assertEquals(0.03778437152504921, detection.windows!!.first().aiAssistanceScore!!, 0.0)

        val verdict = mapper.map(detection)

        assertEquals(SlopLabel.HUMAN, verdict.label)
        assertEquals(1.0, verdict.fractionHuman, 0.0)
        assertEquals("Human Written", verdict.summary)
        assertEquals(1, verdict.windows.size)
        assertEquals("High", verdict.windows.first().confidence)
        assertEquals("4.0", verdict.version)
        assertEquals(143, verdict.wordCount)
        assertEquals("High", verdict.confidence)
        assertTrue(verdict.analyzedText.startsWith("You might not believe it"))
        assertTrue(verdict.dashboardLink!!.startsWith("https://www.pangram.com/"))
    }

    @Test
    fun `deserializes and maps a real AI-generated response`() {
        val detection = json.decodeFromString<ApiPangramDetection>(AI_RESPONSE)

        val verdict = mapper.map(detection)

        assertEquals("4.0", verdict.version)
        assertEquals(SlopLabel.AI, verdict.label)
        assertEquals(0.0, verdict.fractionHuman, 0.0)
        assertEquals("AI", verdict.predictionShort)
        assertEquals(1.0, verdict.fractionAi, 0.0)
        assertEquals(133, verdict.wordCount)
        assertEquals("High", verdict.confidence)
        assertEquals("AI Generated", verdict.headline)
    }

    private companion object {
        /** Prose written by hand, judged `Human` by `pangram-4`. */
        const val HUMAN_RESPONSE = """
{"stage":"STAGE_SUCCESS","text":"You might not believe it, but most interviewers out there – whose sole job is not to be a professional interviewer – really just want to hire you. So they can be done with it, go back to their “normal” work and not have to interview more people.\nThink about it – interviews are most likely a net negative for the drive-by interviewer. There is very little to gain – they already have a job! Tickets are left behind, slack stays unanswered for hours, and they spend half their day trying not to be the reason someone posts about them on twitter.\nOn the other hand, for the interviewers who woke up on the wrong side of the bed – they will probably reject you no matter what you do. I am sorry, this business is a lot more about feelings than people are willing to admit.","version":"4.0","prediction":"We believe that this entire text is human-written.","prediction_short":"Human","fraction_ai":0.0,"fraction_ai_assisted":0.0,"fraction_human":1.0,"headline":"Human Written","num_ai_segments":0,"num_ai_assisted_segments":0,"num_human_segments":1,"windows":[{"text":"You might not believe it, but most interviewers out there – whose sole job is not to be a professional interviewer – really just want to hire you. So they can be done with it, go back to their “normal” work and not have to interview more people.\nThink about it – interviews are most likely a net negative for the drive-by interviewer. There is very little to gain – they already have a job! Tickets are left behind, slack stays unanswered for hours, and they spend half their day trying not to be the reason someone posts about them on twitter.\nOn the other hand, for the interviewers who woke up on the wrong side of the bed – they will probably reject you no matter what you do. I am sorry, this business is a lot more about feelings than people are willing to admit.","label":"Human Written","ai_assistance_score":0.03778437152504921,"confidence":"High","start_index":0,"end_index":769,"word_count":143,"token_length":167,"is_humanized":false,"humanizer_score":0.0}],"dashboard_link":"https://www.pangram.com/history/e15e758d-11b7-44df-86cd-51b6a58d2138"}
"""

        /** A LinkedIn-shaped post written by a model, judged `AI` by `pangram-4`. */
        const val AI_RESPONSE = """
{"stage":"STAGE_SUCCESS","text":"22:51\nlinkedin.com\nSearch\nHead of Growth · 2nd\n2d\nI want to share something that completely changed how I think about leadership.\nLast week a member of my team came to me with a problem. Instead of solving it for them, I asked three questions. What have you tried? What did you learn? What would you do next?\nThe result? They solved it themselves in under an hour.\nHere is the thing: great leaders do not have all the answers. They ask better questions.\nThree lessons I am taking forward:\n1. Silence is a tool, not a gap.\n2. Ownership cannot be delegated, only invited.\n3. Your job is to build thinkers, not followers.\nWhat is one question that changed how you lead?\n#Leadership #Management #GrowthMindset\nHome\nMy Network\nPost\nNotifications\nJobs","version":"4.0","prediction":"We believe that this entire text is AI.","prediction_short":"AI","fraction_ai":1.0,"fraction_ai_assisted":0.0,"fraction_human":0.0,"headline":"AI Generated","num_ai_segments":1,"num_ai_assisted_segments":0,"num_human_segments":0,"windows":[{"text":"22:51\nlinkedin.com\nSearch\nHead of Growth · 2nd\n2d\nI want to share something that completely changed how I think about leadership.\nLast week a member of my team came to me with a problem. Instead of solving it for them, I asked three questions. What have you tried? What did you learn? What would you do next?\nThe result? They solved it themselves in under an hour.\nHere is the thing: great leaders do not have all the answers. They ask better questions.\nThree lessons I am taking forward:\n1. Silence is a tool, not a gap.\n2. Ownership cannot be delegated, only invited.\n3. Your job is to build thinkers, not followers.\nWhat is one question that changed how you lead?\n#Leadership #Management #GrowthMindset\nHome\nMy Network\nPost\nNotifications\nJobs","label":"AI-Generated","ai_assistance_score":0.9999198913574219,"confidence":"High","start_index":0,"end_index":745,"word_count":133,"token_length":178,"is_humanized":false,"humanizer_score":0.01941567286849022}],"dashboard_link":"https://www.pangram.com/history/5b09df56-82ae-4629-87bb-8c3298e85a1a"}
"""
    }
}
