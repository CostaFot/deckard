package com.costafotiadis.deckard.llm

import com.costafotiadis.common.di.ApplicationCoroutineScope
import com.costafotiadis.logging.logDebug
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * COS-259 spike: the screen read through Gemini Nano, served by AICore over the ML Kit Prompt API.
 * The model is the system's, so there is nothing to push; [isReady] is the last status AICore
 * reported, refreshed by a warm-up on the application scope like [LlmEngine]'s.
 */
@Singleton
class GeminiNanoVisionModel @Inject constructor(
    @ApplicationCoroutineScope private val scope: CoroutineScope,
) : VisionModel {

    private val client: GenerativeModel by lazy { Generation.getClient() }

    @Volatile
    private var status: Int = FeatureStatus.UNAVAILABLE

    @Volatile
    var lastError: String? = null
        private set

    private val warmUp: Unit by lazy {
        scope.launch { refresh() }
        Unit
    }

    override val isReady: Boolean
        get() {
            warmUp
            return status == FeatureStatus.AVAILABLE
        }

    suspend fun refresh(): String {
        status = runCatching { client.checkStatus() }
            .onFailure { logDebug { "nano: checkStatus failed: ${it.describe()}" } }
            .getOrDefault(FeatureStatus.UNAVAILABLE)
        logDebug { "nano: status ${statusName(status)}" }
        if (status == FeatureStatus.DOWNLOADABLE) {
            client.download().collect { logDebug { "nano: download $it" } }
            status = runCatching { client.checkStatus() }.getOrDefault(FeatureStatus.UNAVAILABLE)
            logDebug { "nano: status after download ${statusName(status)}" }
        }
        if (status == FeatureStatus.AVAILABLE) {
            runCatching { client.warmup() }
                .onFailure { logDebug { "nano: warmup failed: ${it.describe()}" } }
            runCatching { client.getBaseModelName() }
                .onSuccess { logDebug { "nano: base model $it" } }
        }
        return statusName(status)
    }

    override suspend fun read(jpeg: ByteArray, prompt: String): String? {
        lastError = null
        return runCatching {
            client.generateContent(
                generateContentRequest(ImagePart(jpeg), TextPart(prompt)) {
                    temperature = 0.2f
                    topK = 10
                },
            ).candidates.firstOrNull()?.text
        }.getOrElse {
            lastError = it.describe()
            logDebug { "nano: inference failed: $lastError" }
            null
        }
    }

    private fun Throwable.describe(): String =
        if (this is GenAiException) {
            "${errorName(errorCode)} ($errorCode): $message"
        } else {
            "${javaClass.simpleName}: $message"
        }

    private fun statusName(status: Int): String = when (status) {
        FeatureStatus.AVAILABLE -> "AVAILABLE"
        FeatureStatus.DOWNLOADABLE -> "DOWNLOADABLE"
        FeatureStatus.DOWNLOADING -> "DOWNLOADING"
        else -> "UNAVAILABLE"
    }

    private fun errorName(code: Int): String = when (code) {
        GenAiException.ErrorCode.BACKGROUND_USE_BLOCKED -> "BACKGROUND_USE_BLOCKED"
        GenAiException.ErrorCode.NOT_AVAILABLE -> "NOT_AVAILABLE"
        GenAiException.ErrorCode.BUSY -> "BUSY"
        GenAiException.ErrorCode.REQUEST_PROCESSING_ERROR -> "REQUEST_PROCESSING_ERROR"
        GenAiException.ErrorCode.RESPONSE_PROCESSING_ERROR -> "RESPONSE_PROCESSING_ERROR"
        GenAiException.ErrorCode.RESPONSE_GENERATION_ERROR -> "RESPONSE_GENERATION_ERROR"
        GenAiException.ErrorCode.REQUEST_TOO_LARGE -> "REQUEST_TOO_LARGE"
        GenAiException.ErrorCode.INVALID_INPUT_IMAGE -> "INVALID_INPUT_IMAGE"
        GenAiException.ErrorCode.NOT_SUPPORTED -> "NOT_SUPPORTED"
        GenAiException.ErrorCode.AICORE_INCOMPATIBLE -> "AICORE_INCOMPATIBLE"
        GenAiException.ErrorCode.NEEDS_SYSTEM_UPDATE -> "NEEDS_SYSTEM_UPDATE"
        GenAiException.ErrorCode.PER_APP_BATTERY_USE_QUOTA_EXCEEDED -> "PER_APP_BATTERY_USE_QUOTA_EXCEEDED"
        else -> "UNKNOWN"
    }
}
