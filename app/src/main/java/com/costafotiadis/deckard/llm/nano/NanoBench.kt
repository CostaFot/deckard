package com.costafotiadis.deckard.llm.nano

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import com.costafotiadis.deckard.accessibility.ScreenshotCapturer
import com.costafotiadis.deckard.llm.OcrPrompt
import com.costafotiadis.logging.logDebug
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.common.StreamingCallback
import com.google.mlkit.genai.prompt.Candidate
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.GenerateContentResponse
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.ModelPreference
import com.google.mlkit.genai.prompt.SystemInstruction
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.genai.prompt.generationConfig
import com.google.mlkit.genai.prompt.modelConfig
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.TimeSource

/**
 * Debug only: reads the current screen with Gemini Nano under one chosen configuration and logs what
 * every step costs, so `scripts/deckard nano-bench` can compare the levers (image size, where the
 * prompt goes, the output cap, the fast model, warm-up) one at a time without a summon, a verdict,
 * or a Pangram call. The numbers land under this class's tag; the text comes along so a faster read
 * that drops words is caught as such. Three probes say where the time goes rather than shave it:
 * a prompt of your own ([Spec.ask]), a request with no image, and a streamed read timed to its
 * first token. A cached prompt prefix is not a lever: the API refuses one on a request with an image.
 *
 * The screenshot is the accessibility service's own JPEG re-scaled to [Spec.dim], which is the
 * same picture the real read gets minus a second encode, close enough to compare sizes.
 */
@Singleton
class NanoBench @Inject constructor(
    private val screenshotCapturer: ScreenshotCapturer,
    private val stage: ForegroundStage,
) {

    /** Where the rules go: all in the user turn as today, or as a system instruction. */
    enum class PromptPlacement { USER, SYSTEM }

    class Spec(
        val dim: Int,
        val prompt: PromptPlacement,
        val maxOutputTokens: Int?,
        val preference: Int?,
        val runs: Int,
        val warm: Boolean,
        val ask: String?,
        val image: Boolean,
        val stream: Boolean,
    ) {
        override fun toString(): String =
            "dim=$dim prompt=${prompt.name.lowercase()} max=${maxOutputTokens ?: "default"} " +
                "pref=${preferenceName(preference)} runs=$runs warm=$warm image=$image stream=$stream" +
                (ask?.let { " ask=\"$it\"" } ?: "")
    }

    suspend fun run(spec: Spec) {
        logDebug { "bench: start $spec" }
        val jpeg = screenshotCapturer.capture()
        if (jpeg == null) {
            logDebug { "bench: no screenshot" }
            logDebug { "bench: done" }
            return
        }
        val bitmap = decode(jpeg, spec.dim)
        logDebug { "bench: jpeg ${jpeg.size / 1024}KB, image ${bitmap.width}x${bitmap.height}" }
        val client = client(spec.preference)
        try {
            describe(client)
            val inFront = stage.inFront { runReads(client, bitmap, spec) }
            if (inFront == null) logDebug { "bench: could not get in front" }
        } finally {
            if (spec.preference != null) client.close()
            logDebug { "bench: done" }
        }
    }

    private suspend fun runReads(client: GenerativeModel, bitmap: Bitmap, spec: Spec) {
        if (spec.warm) timed("warmup") { client.warmup() }
        val request = request(bitmap, spec)
        timed("countTokens") { client.countTokens(request) }?.let { logDebug { "bench: ${it.totalTokens} input tokens" } }
        repeat(spec.runs) { run ->
            val what = "read ${run + 1}/${spec.runs}"
            val response = timed(what) {
                if (spec.stream) streamed(client, request, what) else client.generateContent(request)
            } ?: return@repeat
            val candidate = response.candidates.firstOrNull()
            val text = candidate?.text.orEmpty()
            val words = OcrPrompt.clean(text).split(Regex("\\s+")).count { it.isNotEmpty() }
            logDebug { "bench: read ${run + 1} finished ${finishName(candidate?.finishReason)}: $words words, ${text.length} chars" }
            logDebug { "bench: text ${run + 1}: ${text.replace('\n', ' ')}" }
        }
    }

    private suspend fun describe(client: GenerativeModel) {
        val name = runCatching { client.getBaseModelName() }.getOrElse { it.describe() }
        val limit = runCatching { client.getTokenLimit() }.getOrElse { -1 }
        val system = runCatching { client.isSystemPromptAvailable() }.getOrElse { false }
        val caching = runCatching { client.isCachingFeatureAvailable() }.getOrElse { false }
        val thinking = runCatching { client.isThinkingModeAvailable() }.getOrElse { false }
        logDebug { "bench: model $name, token limit $limit, system prompt $system, caching $caching, thinking $thinking" }
    }

    /** The same read through the streaming call, logging when the first and last pieces of text arrive. */
    private suspend fun streamed(client: GenerativeModel, request: GenerateContentRequest, what: String): GenerateContentResponse {
        val mark = TimeSource.Monotonic.markNow()
        var pieces = 0
        val callback = StreamingCallback { text ->
            if (pieces++ == 0) {
                logDebug { "bench: $what first text after ${mark.elapsedNow().inWholeMilliseconds}ms: ${text.take(40)}" }
            }
        }
        return client.generateContent(request, callback).also {
            logDebug { "bench: $what streamed $pieces pieces" }
        }
    }

    private fun request(bitmap: Bitmap, spec: Spec): GenerateContentRequest {
        val rules = spec.ask ?: OcrPrompt.extractMainContent()
        val tune: GenerateContentRequest.Builder.() -> Unit = {
            temperature = TEMPERATURE
            topK = TOP_K
            enableThinking = false
            spec.maxOutputTokens?.let { maxOutputTokens = it }
        }
        val system = spec.prompt == PromptPlacement.SYSTEM
        return when {
            !spec.image && system -> generateContentRequest(SystemInstruction(rules), TextPart(SHORT_ASK), tune)
            !spec.image -> generateContentRequest(TextPart(rules), tune)
            system -> generateContentRequest(SystemInstruction(rules), ImagePart(bitmap), TextPart(SHORT_ASK), tune)
            else -> generateContentRequest(ImagePart(bitmap), TextPart(rules), tune)
        }
    }

    private suspend fun <T> timed(what: String, block: suspend () -> T): T? {
        val mark = TimeSource.Monotonic.markNow()
        return runCatching { block() }
            .onSuccess { logDebug { "bench: $what took ${mark.elapsedNow().inWholeMilliseconds}ms" } }
            .onFailure {
                if (it is CancellationException) throw it
                logDebug { "bench: $what failed after ${mark.elapsedNow().inWholeMilliseconds}ms: ${it.describe()}" }
            }
            .getOrNull()
    }

    private fun client(preference: Int?): GenerativeModel =
        if (preference == null) {
            Generation.getClient()
        } else {
            Generation.getClient(generationConfig { modelConfig = modelConfig { this.preference = preference } })
        }

    private fun decode(jpeg: ByteArray, dim: Int): Bitmap {
        val full = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        val largest = maxOf(full.width, full.height)
        if (largest <= dim) return full
        val scale = dim.toFloat() / largest
        return full.scale((full.width * scale).toInt(), (full.height * scale).toInt())
    }

    private fun Throwable.describe(): String =
        if (this is GenAiException) "error $errorCode: $message" else "${javaClass.simpleName}: $message"

    private fun finishName(reason: Int?): String = when (reason) {
        Candidate.FinishReason.STOP -> "at the end"
        Candidate.FinishReason.MAX_TOKENS -> "at the token cap"
        Candidate.FinishReason.OTHER -> "for another reason"
        else -> "with no candidate"
    }

    companion object {
        /** Named by `scripts/deckard nano-bench`. */
        const val ACTION = "com.costafotiadis.deckard.action.NANO_BENCH"

        private const val SHORT_ASK = "Copy out the body text of the main post in this screenshot, word for word."
        private const val TEMPERATURE = 0.2f
        private const val TOP_K = 10

        /** The bench's configuration, from the broadcast's extras; every one has the product's default. */
        fun spec(intent: Intent): Spec = Spec(
            dim = intent.getIntExtra("dim", 1024),
            prompt = if (intent.getStringExtra("prompt") == "system") PromptPlacement.SYSTEM else PromptPlacement.USER,
            maxOutputTokens = intent.getIntExtra("max", 0).takeIf { it > 0 },
            preference = when (intent.getStringExtra("pref")) {
                "fast" -> ModelPreference.FAST
                "full" -> ModelPreference.FULL
                else -> null
            },
            runs = intent.getIntExtra("runs", 1).coerceAtLeast(1),
            warm = intent.getBooleanExtra("warm", false),
            ask = intent.getStringExtra("ask")?.takeIf { it.isNotBlank() },
            image = intent.getBooleanExtra("image", true),
            stream = intent.getBooleanExtra("stream", false),
        )

        private fun preferenceName(preference: Int?): String = when (preference) {
            ModelPreference.FAST -> "fast"
            ModelPreference.FULL -> "full"
            else -> "default"
        }
    }
}
