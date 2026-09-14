package com.costafotiadis.deckard.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.core.graphics.scale
import com.costafotiadis.logging.logDebug
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * The app's eyes on the screen. It does one thing only an `AccessibilityService` can do — take a
 * screenshot of the display — and registers itself with the on-demand bridge the rest of the app
 * drives it through: [ScreenshotCapturer], which the OCR readers
 * ([com.costafotiadis.deckard.slop.OcrContentScreenTextReader] and its fallback) ask for a JPEG.
 *
 * It reads no window content and listens to no events. The user must enable it under
 * Settings → Accessibility; everything stays on-device.
 */
@AndroidEntryPoint
class DeckardAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var screenshotCapturer: ScreenshotCapturer

    override fun onServiceConnected() {
        super.onServiceConnected()
        logDebug { "accessibility service connected" }
        screenshotCapturer.setHandler(::captureScreenshot)
    }

    override fun onDestroy() {
        screenshotCapturer.setHandler(null)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

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
    }
}
