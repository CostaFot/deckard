package com.costafotiadis.deckard.llm

import com.costafotiadis.deckard.llm.nano.GeminiNanoVisionModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The phone's own Gemini Nano when AICore has it, the LiteRT-LM engine over a pushed file
 * otherwise. Nano is asked first and the engine only when Nano is not ready, so a phone that has
 * Nano never starts loading a 3GB file it is not going to read with.
 */
@Singleton
class NanoOrLocalVisionModel internal constructor(
    private val nano: VisionModel,
    private val local: VisionModel,
) : VisionModel {

    @Inject
    constructor(nano: GeminiNanoVisionModel, local: LlmEngine) : this(nano as VisionModel, local)

    private val chosen: VisionModel
        get() = if (nano.isReady) nano else local

    override val isReady: Boolean
        get() = chosen.isReady

    override suspend fun read(jpeg: ByteArray, prompt: String): VisionReply = chosen.read(jpeg, prompt)
}
