package com.costafotiadis.deckard.llm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.costafotiadis.deckard.accessibility.ScreenshotCapturer
import com.costafotiadis.logging.logDebug
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * COS-259 spike: one Gemini Nano read, from wherever it is triggered, with everything logged under
 * `nano:` so `adb logcat | grep nano:` tells the story. Fired by
 * `adb shell am broadcast -a com.costafotiadis.deckard.action.NANO_READ -p <pkg> --es where
 * activity|service|trampoline [--es file /path/on/device.jpg]`; each host registers a receiver
 * with [receiver] and acts only on its own `where`.
 */
@Singleton
class NanoSpike @Inject constructor(
    @ApplicationContext private val context: Context,
    private val screenshotCapturer: ScreenshotCapturer,
    val model: GeminiNanoVisionModel,
) {

    suspend fun run(where: String, jpegPath: String?): String {
        logDebug { "nano[$where]: status ${model.refresh()}, ready=${model.isReady}" }
        val jpeg = jpeg(jpegPath) ?: return "no jpeg".also { logDebug { "nano[$where]: no jpeg" } }
        logDebug { "nano[$where]: reading ${jpeg.size} bytes" }
        val started = System.currentTimeMillis()
        val text = model.read(jpeg, OcrPrompt.extractMainContent())
        val elapsed = System.currentTimeMillis() - started
        val outcome = text?.let { "ok in ${elapsed}ms:\n$it" } ?: "failed in ${elapsed}ms: ${model.lastError}"
        logDebug { "nano[$where]: $outcome" }
        return outcome
    }

    suspend fun jpeg(path: String?): ByteArray? =
        if (path != null) File(path).takeIf { it.exists() }?.readBytes() else screenshotCapturer.capture()

    /** Stashes a screenshot for the trampoline Activity to read once it is on top. */
    fun stash(jpeg: ByteArray): String =
        File(context.cacheDir, "nano-spike.jpg").apply { writeBytes(jpeg) }.absolutePath

    fun register(host: Context, where: Set<String>, onRead: (where: String, path: String?, untouchable: Boolean) -> Unit): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val target = intent?.getStringExtra(EXTRA_WHERE) ?: return
                if (target !in where) return
                onRead(target, intent.getStringExtra(EXTRA_FILE), intent.getBooleanExtra(EXTRA_UNTOUCHABLE, false))
            }
        }
        ContextCompat.registerReceiver(host, receiver, IntentFilter(ACTION), ContextCompat.RECEIVER_EXPORTED)
        return receiver
    }

    fun trampoline(from: Context, path: String, untouchable: Boolean): Intent =
        Intent(from, NanoTrampolineActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(EXTRA_FILE, path)
            .putExtra(EXTRA_UNTOUCHABLE, untouchable)

    companion object {
        const val ACTION = "com.costafotiadis.deckard.action.NANO_READ"
        const val EXTRA_WHERE = "where"
        const val EXTRA_FILE = "file"
        const val EXTRA_UNTOUCHABLE = "untouchable"
    }
}
