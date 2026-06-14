package com.markedusduplicate.deckard.accessibility

import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-demand window-text bridge. Only an [android.accessibilityservice.AccessibilityService] can read
 * the foreground app's accessibility tree, so the service registers its extraction implementation
 * here and the screen reader
 * ([com.markedusduplicate.deckard.slop.AccessibilityScreenTextReader]) calls [capture] to pull the
 * visible on-screen text. Reading on demand (rather than caching the last event) keeps the text fresh
 * for the moment Deckard is summoned. Returns null when the service isn't connected (accessibility
 * disabled) or nothing readable is on screen.
 */
@Singleton
class ScreenTextCapturer @Inject constructor() {

    @Volatile
    private var handler: (suspend () -> String?)? = null

    /** True while the accessibility service is connected and able to read the tree. */
    val isAvailable: Boolean get() = handler != null

    fun setHandler(handler: (suspend () -> String?)?) {
        this.handler = handler
    }

    suspend fun capture(): String? = handler?.invoke()
}
