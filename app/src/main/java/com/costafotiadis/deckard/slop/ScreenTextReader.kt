package com.costafotiadis.deckard.slop

/**
 * Reads the text currently on screen for the slop detector. Two strategies sit behind this seam,
 * selected via Hilt qualifiers in [com.costafotiadis.deckard.di.ScreenTextModule]:
 * [OcrContentScreenTextReader] (screenshot + vision isolation, the one the long-press drives) and
 * [OcrScreenTextReader] (screenshot + full transcription, the fallback). Both photograph the
 * screen; the accessibility-tree reader that once sat beside them lives at the `a11y-reader` tag.
 */
interface ScreenTextReader {

    /**
     * Read the screen's text, invoking [onScreenCaptured] the moment the reader is done with the
     * screen itself and before the slow interpretation begins. The caller must draw nothing over the
     * screen until then: that is the difference between the vision model
     * transcribing the post and transcribing Deckard's own speech bubble. A reader that fails before
     * it ever reads the screen never calls it.
     *
     * Called on the main thread: what the caller does with it puts windows on the screen.
     */
    suspend fun read(onScreenCaptured: () -> Unit): ScreenReadResult
}
