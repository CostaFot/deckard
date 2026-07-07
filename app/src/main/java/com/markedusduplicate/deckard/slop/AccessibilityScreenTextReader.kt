package com.markedusduplicate.deckard.slop

import com.markedusduplicate.deckard.accessibility.ScreenTextCapturer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ScreenTextReader] backed by the accessibility tree: pulls the visible on-screen text straight from
 * the foreground app's a11y nodes (via the accessibility service's [ScreenTextCapturer]). No model and
 * no screenshot, so it returns in milliseconds — far faster than [OcrScreenTextReader]'s per-summon
 * vision inference. The chrome-trimming (viewport clipping + WebView targeting) lives in the service's
 * capture; see `DeckardAccessibilityService.captureScreenText`.
 */
@Singleton
class AccessibilityScreenTextReader @Inject constructor(
    private val screenTextCapturer: ScreenTextCapturer,
) : ScreenTextReader {

    override suspend fun read(): ScreenReadResult {
        if (!screenTextCapturer.isAvailable) {
            return ScreenReadResult.Unavailable("Turn on the accessibility service so I can read your screen.")
        }
        val text = screenTextCapturer.capture()
        return if (text.isNullOrEmpty()) {
            ScreenReadResult.Unavailable("I didn't find any text to check.")
        } else {
            ScreenReadResult.Text(text)
        }
    }
}
