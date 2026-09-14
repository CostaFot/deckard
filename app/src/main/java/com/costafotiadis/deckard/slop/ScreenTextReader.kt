package com.costafotiadis.deckard.slop

/**
 * Reads the text currently on screen for the slop detector. Two strategies sit behind this seam,
 * selected via Hilt qualifiers in [com.costafotiadis.deckard.di.ScreenTextModule]:
 * [OcrScreenTextReader] (screenshot OCR — in use) and [AccessibilityScreenTextReader] (a custom
 * accessibility-tree extraction — TODO).
 */
interface ScreenTextReader {
    suspend fun read(): ScreenReadResult
}
