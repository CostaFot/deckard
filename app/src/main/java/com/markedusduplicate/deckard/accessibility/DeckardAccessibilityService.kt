package com.markedusduplicate.deckard.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.core.graphics.scale
import com.markedusduplicate.common.FlagProvider
import com.markedusduplicate.common.coroutine.DispatcherProvider
import com.markedusduplicate.deckard.accessibility.extract.ScreenContentExtractors
import com.markedusduplicate.deckard.accessibility.tree.ScreenNode
import com.markedusduplicate.deckard.accessibility.tree.ScreenNodeSnapshot
import com.markedusduplicate.deckard.accessibility.tree.toDebugString
import com.markedusduplicate.logging.logDebug
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * The app's eyes on the screen. It does two things, both of which only an `AccessibilityService` can
 * do, so it registers itself with the on-demand bridges the rest of the app drives it through:
 *
 * 1. **Window text** — snapshots the foreground app's accessibility tree on demand and hands it to
 *    the right per-app [ScreenContentExtractors] extractor; the result feeds [ScreenTextCapturer]
 *    (the in-use screen reader, [com.markedusduplicate.deckard.slop.AccessibilityScreenTextReader],
 *    pulls it when Deckard is summoned).
 * 2. **Screenshots** — registers [ScreenshotCapturer]'s handler for the fallback OCR reader
 *    ([com.markedusduplicate.deckard.slop.OcrScreenTextReader], which asks for a JPEG of the display).
 *
 * The user must enable this service under Settings → Accessibility; everything stays on-device.
 */
@AndroidEntryPoint
class DeckardAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Inject
    lateinit var screenshotCapturer: ScreenshotCapturer

    @Inject
    lateinit var screenTextCapturer: ScreenTextCapturer

    @Inject
    lateinit var screenContentExtractors: ScreenContentExtractors

    @Inject
    lateinit var flagProvider: FlagProvider

    override fun onServiceConnected() {
        super.onServiceConnected()
        logDebug { "accessibility service connected" }
        screenshotCapturer.setHandler(::captureScreenshot)
        screenTextCapturer.setHandler(::captureScreenText)
    }

    override fun onDestroy() {
        screenshotCapturer.setHandler(null)
        screenTextCapturer.setHandler(null)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    /**
     * Snapshot the foreground app's accessibility tree and let the per-app extractor pull the content
     * worth checking. On debug builds the full tree is written to a file (see [dumpTree]) so we can
     * design and tune each app's extractor against real captures.
     */
    private suspend fun captureScreenText(): String? = withContext(dispatcherProvider.io) {
        val root = rootInActiveWindow ?: return@withContext null
        val snapshot = ScreenNodeSnapshot.from(root) ?: return@withContext null
        val packageName = root.packageName?.toString().orEmpty()

        val text = screenContentExtractors.extract(packageName, snapshot)
        logDebug { "screen ($packageName): $text" }
        if (flagProvider.isDebugEnabled) dumpTree(packageName, snapshot, text)
        text
    }

    /**
     * Debug only: write the active-window tree (what the extractor sees) plus every window's tree to
     * a file we can `adb pull` — logcat is encrypted on some devices, and a file is the exact snapshot
     * the extractor received. Dumping all windows reveals content (e.g. a comment sheet) that lives
     * outside `rootInActiveWindow`.
     */
    private fun dumpTree(packageName: String, activeTree: ScreenNode, extracted: String?) {
        runCatching {
            val dir = getExternalFilesDir(null) ?: return
            val content = buildString {
                append("package: ").append(packageName).append('\n')
                append("--- extracted ---\n").append(extracted ?: "(null)").append("\n\n")
                append("=== active window (what the extractor sees) ===\n")
                append(activeTree.toDebugString()).append('\n')
                append("=== all windows ===\n")
                windows.forEach { window ->
                    val bounds = Rect().also { window.getBoundsInScreen(it) }
                    append("\n[window type=").append(window.type)
                        .append(" active=").append(window.isActive)
                        .append(" focused=").append(window.isFocused)
                        .append(" layer=").append(window.layer)
                        .append(" bounds=").append(bounds.toShortString())
                        .append("]\n")
                    val root = window.root
                    append(root?.let { ScreenNodeSnapshot.from(it)?.toDebugString() } ?: "(no tree)\n")
                }
            }
            File(dir, TREE_DUMP_FILE).writeText(content)
            logDebug { "tree dump written to ${File(dir, TREE_DUMP_FILE).absolutePath}" }
        }
    }

    /** Capture the current display as a downscaled JPEG (null on failure). */
    private suspend fun captureScreenshot(): ByteArray? = withContext(Dispatchers.IO) {
        val result = awaitScreenshot() ?: return@withContext null
        return@withContext encodeJpeg(result)
    }

    private suspend fun awaitScreenshot(): ScreenshotResult? =
        suspendCancellableCoroutine { continuation ->
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                Dispatchers.IO.asExecutor(),
                object : TakeScreenshotCallback {
                    override fun onSuccess(result: ScreenshotResult) {
                        if (continuation.isActive) continuation.resume(result)
                    }

                    override fun onFailure(errorCode: Int) {
                        logDebug { "takeScreenshot failed: $errorCode" }
                        if (continuation.isActive) continuation.resume(null)
                    }
                },
            )
        }

    private fun encodeJpeg(result: ScreenshotResult): ByteArray? {
        val buffer = result.hardwareBuffer
        return buffer.use { buffer ->
            val hardware = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace) ?: return null
            val software = hardware.copy(Bitmap.Config.ARGB_8888, false)
            val scaled = downscale(software, MAX_SCREENSHOT_DIM)
            ByteArrayOutputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                out.toByteArray()
            }
        }
    }

    private fun downscale(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largest
        return bitmap.scale((bitmap.width * scale).toInt(), (bitmap.height * scale).toInt())
    }

    private companion object {
        const val MAX_SCREENSHOT_DIM = 1024
        const val JPEG_QUALITY = 85
        const val TREE_DUMP_FILE = "deckard_tree.txt"
    }
}
