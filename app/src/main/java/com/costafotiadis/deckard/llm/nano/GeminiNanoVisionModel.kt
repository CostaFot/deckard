package com.costafotiadis.deckard.llm.nano

import com.costafotiadis.common.di.ApplicationCoroutineScope
import com.costafotiadis.deckard.llm.VisionFailure
import com.costafotiadis.deckard.llm.VisionModel
import com.costafotiadis.deckard.llm.VisionReply
import com.costafotiadis.logging.logDebug
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Candidate
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The screen read through the phone's own Gemini Nano, served by AICore over the ML Kit Prompt API.
 * The model is the system's, so there is nothing to push and nothing to load: [isReady] is the last
 * status AICore reported, and a model it reports as downloadable is asked for.
 *
 * Two things about AICore shape this class. It serves only the app in front, so every [read] runs
 * inside [ForegroundStage.inFront]; and its download flow says nothing while Private Compute
 * Services fetches the model in the background, so readiness is polled — a status check on
 * construction, and one more on every [isReady] — rather than listened for.
 *
 * Status checks are allowed from the background; warm-up is not, and a warm-up right before the
 * read it would warm is no warm-up at all, so there is none.
 */
@Singleton
class GeminiNanoVisionModel internal constructor(
    private val client: GenerativeModel,
    private val stage: ForegroundStage,
    private val scope: CoroutineScope,
    private val request: (jpeg: ByteArray, prompt: String) -> GenerateContentRequest,
) : VisionModel {

    @Inject
    constructor(
        stage: ForegroundStage,
        @ApplicationCoroutineScope scope: CoroutineScope,
    ) : this(Generation.getClient(), stage, scope, ::screenshotRequest)

    @Volatile
    private var status: Int = FeatureStatus.UNAVAILABLE

    private val refreshing = AtomicBoolean(false)

    init {
        refresh()
    }

    override val isReady: Boolean
        get() {
            refresh()
            return status == FeatureStatus.AVAILABLE
        }

    override suspend fun read(jpeg: ByteArray, prompt: String): VisionReply {
        if (status != FeatureStatus.AVAILABLE) return VisionReply.Failed(VisionFailure.NotReady)
        return stage.inFront { generate(jpeg, prompt) }
            ?: VisionReply.Failed(VisionFailure.NotInFront)
    }

    /** Ask AICore where the model stands, one ask at a time, and ask for it if it is only downloadable. */
    private fun refresh() {
        if (!refreshing.compareAndSet(false, true)) return
        scope.launch {
            try {
                status = checkStatus()
                if (status == FeatureStatus.DOWNLOADABLE) {
                    logDebug { "nano: downloadable, asking for it" }
                    runCatching { client.download().collect { logDebug { "nano: download $it" } } }
                        .onFailure {
                            if (it is CancellationException) throw it
                            logDebug { "nano: download failed: ${it.describe()}" }
                        }
                    status = checkStatus()
                }
                logDebug { "nano: ${statusName(status)}" }
            } finally {
                refreshing.set(false)
            }
        }
    }

    private suspend fun checkStatus(): Int = runCatching { client.checkStatus() }
        .onFailure {
            if (it is CancellationException) throw it
            logDebug { "nano: checkStatus failed: ${it.describe()}" }
        }
        .getOrDefault(FeatureStatus.UNAVAILABLE)

    private suspend fun generate(jpeg: ByteArray, prompt: String): VisionReply = runCatching {
        val candidate = client.generateContent(request(jpeg, prompt)).candidates.firstOrNull()
        if (candidate?.finishReason == Candidate.FinishReason.OTHER) {
            logDebug { "nano: reply finished for a reason other than the end of the text" }
        }
        VisionReply.Text(candidate?.text.orEmpty())
    }.getOrElse {
        if (it is CancellationException) throw it
        logDebug { "nano: inference failed: ${it.describe()}" }
        VisionReply.Failed(it.asFailure())
    }

    private fun Throwable.asFailure(): VisionFailure {
        if (this !is GenAiException) return VisionFailure.Failed
        return when (errorCode) {
            GenAiException.ErrorCode.BACKGROUND_USE_BLOCKED -> VisionFailure.NotInFront
            GenAiException.ErrorCode.RESPONSE_PROCESSING_ERROR -> VisionFailure.Refused
            GenAiException.ErrorCode.PER_APP_BATTERY_USE_QUOTA_EXCEEDED -> VisionFailure.OutOfQuota
            GenAiException.ErrorCode.NOT_AVAILABLE,
            GenAiException.ErrorCode.NOT_SUPPORTED,
            GenAiException.ErrorCode.AICORE_INCOMPATIBLE,
            GenAiException.ErrorCode.NEEDS_SYSTEM_UPDATE,
            GenAiException.ErrorCode.NOT_ENOUGH_DISK_SPACE,
            -> {
                status = FeatureStatus.UNAVAILABLE
                VisionFailure.NotReady
            }

            else -> VisionFailure.Failed
        }
    }

    private fun Throwable.describe(): String =
        if (this is GenAiException) "error $errorCode: $message" else "${javaClass.simpleName}: $message"

    private fun statusName(status: Int): String = when (status) {
        FeatureStatus.AVAILABLE -> "available"
        FeatureStatus.DOWNLOADABLE -> "downloadable"
        FeatureStatus.DOWNLOADING -> "downloading"
        else -> "unavailable"
    }
}

/** Low, for a task that wants the text back as it is; the Prompt API guide's own figure. */
private const val TEMPERATURE = 0.2f
private const val TOP_K = 10

private fun screenshotRequest(jpeg: ByteArray, prompt: String): GenerateContentRequest =
    generateContentRequest(ImagePart(jpeg), TextPart(prompt)) {
        temperature = TEMPERATURE
        topK = TOP_K
    }
