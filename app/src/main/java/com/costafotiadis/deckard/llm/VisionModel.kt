package com.costafotiadis.deckard.llm

/**
 * The on-device vision model as the screen read sees it: whether it can answer right now, and what
 * it says about a screenshot. This is all the OCR readers ever needed, so it is all they get — the
 * runtime behind it (LiteRT-LM today, something the system already ships tomorrow) is a detail of
 * `di/`, and the read stays testable with a fake.
 */
interface VisionModel {

    /**
     * True once the model can take a [read]. Non-blocking: a model still loading answers false, and
     * the read reports [com.costafotiadis.deckard.slop.ScreenReadFailure.ModelNotReady] rather than
     * waiting for it.
     */
    val isReady: Boolean

    /**
     * Sends [jpeg] (image bytes) plus [prompt] to the model and returns the raw reply, or null if
     * the model isn't ready or inference fails. Main-safe.
     */
    suspend fun read(jpeg: ByteArray, prompt: String): String?
}
