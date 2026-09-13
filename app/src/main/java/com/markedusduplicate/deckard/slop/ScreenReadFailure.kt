package com.markedusduplicate.deckard.slop

/**
 * Why a [ScreenTextReader] came back with nothing. The reader names the condition; it does not word
 * it — Deckard's phrasing for each of these lives in `mascot/DeckardVoice`, so the register sits in
 * one file instead of drifting across the data layer.
 */
enum class ScreenReadFailure {
    /** The accessibility service isn't running, so nothing can read the screen at all. */
    NoAccessibilityService,

    /** The on-device model hasn't finished loading (or there isn't one). OCR path only. */
    ModelNotReady,

    /** The screenshot itself failed. */
    ScreenshotFailed,

    /** The model was handed a screenshot and produced nothing. */
    TranscriptionFailed,

    /** The read worked and there was simply no text on the screen. */
    NoTextFound,
}
