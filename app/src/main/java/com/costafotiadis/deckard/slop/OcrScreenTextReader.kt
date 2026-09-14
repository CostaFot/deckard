package com.costafotiadis.deckard.slop

import com.costafotiadis.deckard.accessibility.ScreenshotCapturer
import com.costafotiadis.deckard.llm.LlmEngine
import com.costafotiadis.deckard.llm.OcrPrompt
import com.costafotiadis.logging.logDebug
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ScreenTextReader] backed by screenshot OCR: grabs the screen (via the accessibility service's
 * [ScreenshotCapturer]) and asks the on-device multimodal model ([LlmEngine.generateWithImage]) to
 * transcribe **all** of it. A screenshot is inherently the visible viewport only, so this captures
 * just what the user can see — no off-screen feed scrollback. Slow (a vision inference per summon)
 * but accurate, and it hard-requires a loaded model.
 */
@Singleton
class OcrScreenTextReader @Inject constructor(
    private val screenshotCapturer: ScreenshotCapturer,
    private val engine: LlmEngine,
) : ScreenTextReader {

    override suspend fun read(onScreenCaptured: () -> Unit): ScreenReadResult =
        ocrRead(screenshotCapturer, engine, OcrPrompt.transcribe(), onScreenCaptured)
}

/**
 * Like [OcrScreenTextReader] but asks the model to **isolate the single main post/article** from the
 * screenshot ([OcrPrompt.extractMainContent]) rather than transcribing everything — so the slop
 * detector judges the content, not the surrounding chrome, with no per-app extractor needed. Driven
 * by the long-press on the edge tab.
 */
@Singleton
class OcrContentScreenTextReader @Inject constructor(
    private val screenshotCapturer: ScreenshotCapturer,
    private val engine: LlmEngine,
) : ScreenTextReader {

    override suspend fun read(onScreenCaptured: () -> Unit): ScreenReadResult =
        ocrRead(screenshotCapturer, engine, OcrPrompt.extractMainContent(), onScreenCaptured)
}

/**
 * Shared OCR read: screenshot → [prompt] → cleaned text, naming the condition on each failure.
 * [onScreenCaptured] fires the instant the shutter closes and before the vision inference, so the
 * caller can draw over a screen it no longer owns — anything drawn earlier is in the JPEG.
 */
private suspend fun ocrRead(
    screenshotCapturer: ScreenshotCapturer,
    engine: LlmEngine,
    prompt: String,
    onScreenCaptured: () -> Unit,
): ScreenReadResult {
    if (!screenshotCapturer.isAvailable) {
        return ScreenReadResult.Unavailable(ScreenReadFailure.NoAccessibilityService)
    }
    if (engine.engineOrNull() == null) {
        return ScreenReadResult.Unavailable(ScreenReadFailure.ModelNotReady)
    }
    val jpeg = screenshotCapturer.capture()
        ?: return ScreenReadResult.Unavailable(ScreenReadFailure.ScreenshotFailed)
    onScreenCaptured()
    val raw = engine.generateWithImage(jpeg, prompt)
        ?: return ScreenReadResult.Unavailable(ScreenReadFailure.TranscriptionFailed)
    logDebug { "ocr raw: $raw" }
    val text = OcrPrompt.clean(raw)
    return if (text.isEmpty()) {
        ScreenReadResult.Unavailable(ScreenReadFailure.NoTextFound)
    } else {
        ScreenReadResult.Text(text)
    }
}
