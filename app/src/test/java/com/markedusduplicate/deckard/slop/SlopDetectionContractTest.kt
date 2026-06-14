package com.markedusduplicate.deckard.slop

import com.markedusduplicate.deckard.net.model.ApiPangramDetection
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the Pangram contract against a real `STAGE_SUCCESS` payload: the API model must deserialize
 * it (with the app's `ignoreUnknownKeys` config) and the mapper must turn it into the expected
 * domain verdict.
 */
class SlopDetectionContractTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val mapper = SlopVerdictMapper()

    @Test
    fun `deserializes and maps a real human-written response`() {
        val detection = json.decodeFromString<ApiPangramDetection>(REAL_RESPONSE)

        assertEquals("STAGE_SUCCESS", detection.stage)
        assertEquals("3.3.2", detection.version)
        assertEquals("Human", detection.predictionShort)
        assertEquals(1.0, detection.fractionHuman!!, 0.0)
        assertEquals(1, detection.windows!!.size)
        assertEquals(0.012982946820557117, detection.windows!!.first().aiAssistanceScore!!, 0.0)

        val verdict = mapper.map(detection)

        assertFalse(verdict.isAi)
        assertEquals(0.0, verdict.aiLikelihood, 0.0)
        assertEquals("Human Written", verdict.summary)
        assertEquals(1, verdict.windows.size)
        assertEquals("High", verdict.windows.first().confidence)
        assertEquals("3.3.2", verdict.version)
        assertEquals(69, verdict.wordCount)
        assertEquals("High", verdict.confidence)
        assertEquals("Human Written", verdict.dominantLabel)
        assertTrue(verdict.analyzedText.startsWith("22:42"))
    }

    @Test
    fun `deserializes and maps a real AI-generated response`() {
        val detection = json.decodeFromString<ApiPangramDetection>(AI_RESPONSE)

        val verdict = mapper.map(detection)

        assertTrue(verdict.isAi)
        assertEquals("AI", verdict.predictionShort)
        assertEquals(1.0, verdict.fractionAi, 0.0)
        assertEquals(137, verdict.wordCount)
        assertEquals("High", verdict.confidence)
        assertEquals("AI-Generated", verdict.dominantLabel)
        assertEquals("AI Generated", verdict.headline)
    }

    private companion object {
        const val REAL_RESPONSE = """
{"stage":"STAGE_SUCCESS","text":"22:42\nlinkedin.com\nSearch\nTom Colvin commented on this\nDean Cowley · 2nd\nTechnical Lead\n23h · Edited\nParticleEmitter for Compose Multiplatform is a seriously impressive particle effects library built by Piotr Prus 😋\nHere's my attempt at rendering fireworks on desktop and iOS, which I built during Piotr's workshop at the GDG London Google I/O\nExtended event. Check out his library on GitHub: https://Ink.in/eWXSAvgr\n#ComposeMultiplatform #KotlinMultiplatform #Kotlin#GDGLondon #GoogleIOExtended #GoogleIO","version":"3.3.2","prediction":"We believe that this document is fully human-written","prediction_short":"Human","fraction_ai":0.0,"fraction_ai_assisted":0.0,"fraction_human":1.0,"headline":"Human Written","num_ai_segments":0,"num_ai_assisted_segments":0,"num_human_segments":1,"windows":[{"text":"22:42\nlinkedin.com\nSearch\nTom Colvin commented on this\nDean Cowley · 2nd\nTechnical Lead\n23h · Edited\nParticleEmitter for Compose Multiplatform is a seriously impressive particle effects library built by Piotr Prus 😋\nHere's my attempt at rendering fireworks on desktop and iOS, which I built during Piotr's workshop at the GDG London Google I/O\nExtended event. Check out his library on GitHub: https://Ink.in/eWXSAvgr\n#ComposeMultiplatform #KotlinMultiplatform #Kotlin#GDGLondon #GoogleIOExtended #GoogleIO","label":"Human Written","ai_assistance_score":0.012982946820557117,"confidence":"High","start_index":0,"end_index":505,"word_count":69,"token_length":137}]}
"""

        const val AI_RESPONSE = """
{"stage":"STAGE_SUCCESS","text":"22:47\nlinkedin.com\nSearch\nAssociate Professor at Uni...\n4d\nAfter the conclusion of Posidonia 2026, we continue to reflect on a highly productive and rewarding experience for the Department of Maritime Studies-University of Piraeus\nWhat I enjoyed most was meeting young people who are considering a career in shipping.\nUntil the next Posidonia!\n#Posidonia2026 #MaritimeStudies\nHome\nMy Network\nPost\nNotifications\nJobs","version":"3.3.2","prediction":"We believe that this document is fully AI-generated","prediction_short":"AI","fraction_ai":1.0,"fraction_ai_assisted":0.0,"fraction_human":0.0,"headline":"AI Generated","num_ai_segments":1,"num_ai_assisted_segments":0,"num_human_segments":0,"windows":[{"text":"After the conclusion of Posidonia 2026...","label":"AI-Generated","ai_assistance_score":0.9613637924194336,"confidence":"High","start_index":0,"end_index":945,"word_count":137,"token_length":193}]}
"""
    }
}
