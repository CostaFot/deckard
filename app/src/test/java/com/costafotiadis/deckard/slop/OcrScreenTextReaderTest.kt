package com.costafotiadis.deckard.slop

import com.costafotiadis.deckard.accessibility.ScreenshotCapturer
import com.costafotiadis.deckard.llm.OcrPrompt
import com.costafotiadis.deckard.llm.VisionModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OcrScreenTextReaderTest {

    private val screenshotCapturer = ScreenshotCapturer()
    private val model = FakeVisionModel()

    @Test
    fun `reports no accessibility service before anything else`() = runTest {
        model.isReady = false

        val result = ocrRead(screenshotCapturer, model, PROMPT) {}

        assertEquals(ScreenReadResult.Unavailable(ScreenReadFailure.NoAccessibilityService), result)
    }

    @Test
    fun `reports the model not ready without taking a screenshot`() = runTest {
        var captures = 0
        screenshotCapturer.setHandler {
            captures++
            JPEG
        }
        model.isReady = false

        val result = ocrRead(screenshotCapturer, model, PROMPT) {}

        assertEquals(ScreenReadResult.Unavailable(ScreenReadFailure.ModelNotReady), result)
        assertEquals(0, captures)
    }

    @Test
    fun `reports a failed screenshot without firing the shutter or the model`() = runTest {
        screenshotCapturer.setHandler { null }
        var shutterFired = false

        val result = ocrRead(screenshotCapturer, model, PROMPT) { shutterFired = true }

        assertEquals(ScreenReadResult.Unavailable(ScreenReadFailure.ScreenshotFailed), result)
        assertEquals(false, shutterFired)
        assertNull(model.lastPrompt)
    }

    @Test
    fun `fires the shutter after the screenshot and before the model answers`() = runTest {
        screenshotCapturer.setHandler { JPEG }
        val order = mutableListOf<String>()
        model.onRead = { order += "read" }

        ocrRead(screenshotCapturer, model, PROMPT) { order += "shutter" }

        assertEquals(listOf("shutter", "read"), order)
    }

    @Test
    fun `hands the model the screenshot and the prompt`() = runTest {
        screenshotCapturer.setHandler { JPEG }

        ocrRead(screenshotCapturer, model, PROMPT) {}

        assertEquals(JPEG, model.lastJpeg)
        assertEquals(PROMPT, model.lastPrompt)
    }

    @Test
    fun `reports a model that produced nothing`() = runTest {
        screenshotCapturer.setHandler { JPEG }
        model.reply = null

        val result = ocrRead(screenshotCapturer, model, PROMPT) {}

        assertEquals(ScreenReadResult.Unavailable(ScreenReadFailure.TranscriptionFailed), result)
    }

    @Test
    fun `reports no text when the cleaned reply is empty`() = runTest {
        screenshotCapturer.setHandler { JPEG }
        model.reply = "  \"\"  "

        val result = ocrRead(screenshotCapturer, model, PROMPT) {}

        assertEquals(ScreenReadResult.Unavailable(ScreenReadFailure.NoTextFound), result)
    }

    @Test
    fun `returns the cleaned text`() = runTest {
        screenshotCapturer.setHandler { JPEG }
        model.reply = "  \"Hello world\" "

        val result = ocrRead(screenshotCapturer, model, PROMPT) {}

        assertEquals(ScreenReadResult.Text("Hello world"), result)
    }

    @Test
    fun `the content reader asks the model to isolate the main post`() = runTest {
        screenshotCapturer.setHandler { JPEG }

        OcrContentScreenTextReader(screenshotCapturer, model).read {}

        assertEquals(OcrPrompt.extractMainContent(), model.lastPrompt)
    }

    @Test
    fun `the transcribing reader asks the model for everything`() = runTest {
        screenshotCapturer.setHandler { JPEG }

        OcrScreenTextReader(screenshotCapturer, model).read {}

        assertEquals(OcrPrompt.transcribe(), model.lastPrompt)
    }

    private class FakeVisionModel : VisionModel {
        override var isReady: Boolean = true
        var reply: String? = "some text"
        var lastJpeg: ByteArray? = null
        var lastPrompt: String? = null
        var onRead: () -> Unit = {}

        override suspend fun read(jpeg: ByteArray, prompt: String): String? {
            lastJpeg = jpeg
            lastPrompt = prompt
            onRead()
            return reply
        }
    }

    private companion object {
        const val PROMPT = "read this"
        val JPEG = byteArrayOf(1, 2, 3)
    }
}
