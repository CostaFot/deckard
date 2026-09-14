package com.costafotiadis.deckard.slop

/**
 * Reads the text currently on screen for the slop detector. Three strategies sit behind this seam,
 * selected via Hilt qualifiers in [com.costafotiadis.deckard.di.ScreenTextModule]:
 * [AccessibilityScreenTextReader] (a11y-tree extraction, driven by the swipe),
 * [OcrContentScreenTextReader] (screenshot + vision isolation, driven by the long-press) and
 * [OcrScreenTextReader] (screenshot + full transcription, the fallback).
 */
interface ScreenTextReader {

    /**
     * Read the screen's text, invoking [onScreenCaptured] the moment the reader is done with the
     * screen itself and before the slow interpretation begins. The caller must draw nothing over the
     * screen until then: on the screenshot paths that is the difference between the vision model
     * transcribing the post and transcribing Deckard's own speech bubble. A reader that fails before
     * it ever reads the screen never calls it.
     */
    suspend fun read(onScreenCaptured: () -> Unit): ScreenReadResult
}
