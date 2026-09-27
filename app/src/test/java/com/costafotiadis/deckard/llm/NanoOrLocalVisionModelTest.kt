package com.costafotiadis.deckard.llm

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NanoOrLocalVisionModelTest {

    private val nano = FakeVisionModel("nano")
    private val local = FakeVisionModel("local")
    private val model = NanoOrLocalVisionModel(nano, local)

    @Test
    fun `reads with nano when it is ready`() = runTest {
        nano.ready = true
        local.ready = true

        val reply = model.read(JPEG, PROMPT)

        assertEquals(VisionReply.Text("nano"), reply)
        assertNull(local.lastPrompt)
    }

    @Test
    fun `falls back to the local engine when nano is not ready`() = runTest {
        nano.ready = false
        local.ready = true

        assertTrue(model.isReady)
        assertEquals(VisionReply.Text("local"), model.read(JPEG, PROMPT))
        assertNull(nano.lastPrompt)
    }

    @Test
    fun `is not ready when neither is`() {
        nano.ready = false
        local.ready = false

        assertFalse(model.isReady)
    }

    @Test
    fun `does not consult the local engine while nano is ready`() {
        nano.ready = true

        model.isReady

        assertEquals(0, local.readinessAsked)
    }

    private class FakeVisionModel(private val name: String) : VisionModel {
        var readinessAsked = 0
        var lastPrompt: String? = null
        var ready = false
        override val isReady: Boolean
            get() {
                readinessAsked++
                return ready
            }

        override suspend fun read(jpeg: ByteArray, prompt: String): VisionReply {
            lastPrompt = prompt
            return VisionReply.Text(name)
        }
    }

    private companion object {
        const val PROMPT = "read this"
        val JPEG = byteArrayOf(1, 2, 3)
    }
}
