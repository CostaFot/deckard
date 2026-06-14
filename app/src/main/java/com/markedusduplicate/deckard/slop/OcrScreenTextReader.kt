package com.markedusduplicate.deckard.slop

import com.markedusduplicate.deckard.accessibility.ScreenshotCapturer
import com.markedusduplicate.deckard.suggestion.llm.LlmEngine
import com.markedusduplicate.deckard.suggestion.llm.OcrPrompt
import com.markedusduplicate.logging.logDebug
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ScreenTextReader] backed by screenshot OCR: grabs the screen (via the accessibility service's
 * [ScreenshotCapturer]) and asks the on-device multimodal model ([LlmEngine.generateWithImage]) to
 * transcribe **all** of it. A screenshot is inherently the visible viewport only, so this captures
 * just what the user can see — no off-screen feed scrollback. Slower than reading the tree (a vision
 * inference per summon) but accurate, and it hard-requires a loaded model.
 */
@Singleton
class OcrScreenTextReader @Inject constructor(
    private val screenshotCapturer: ScreenshotCapturer,
    private val engine: LlmEngine,
) : ScreenTextReader {

    override suspend fun read(): ScreenReadResult =
        ocrRead(screenshotCapturer, engine, OcrPrompt.transcribe())
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

    override suspend fun read(): ScreenReadResult =
        ocrRead(screenshotCapturer, engine, OcrPrompt.extractMainContent())
}

/** Shared OCR read: screenshot → [prompt] → cleaned text, mapping each failure to a user message. */
private suspend fun ocrRead(
    screenshotCapturer: ScreenshotCapturer,
    engine: LlmEngine,
    prompt: String,
): ScreenReadResult {
    if (!screenshotCapturer.isAvailable) {
        return ScreenReadResult.Unavailable("Turn on the accessibility service so I can read your screen.")
    }
    if (engine.engineOrNull() == null) {
        return ScreenReadResult.Unavailable("My brain isn't loaded yet (no model). Give me a sec.")
    }
    val jpeg = screenshotCapturer.capture()
        ?: return ScreenReadResult.Unavailable("I couldn't grab the screen, awkward.")
    val raw = engine.generateWithImage(jpeg, prompt)
        ?: return ScreenReadResult.Unavailable("I couldn't read the screen. Try again.")
    logDebug { "ocr raw: $raw" }
    val text = OcrPrompt.clean(raw)
    return if (text.isEmpty()) {
        ScreenReadResult.Unavailable("I didn't find any text to check.")
    } else {
        ScreenReadResult.Text(text)
    }
}
