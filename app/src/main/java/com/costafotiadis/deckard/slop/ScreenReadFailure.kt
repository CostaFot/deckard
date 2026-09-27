package com.costafotiadis.deckard.slop

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

    /**
     * The phone's own model reads only for the app in front, and Deckard could not get in front:
     * the see-through Activity the read runs inside was refused or never came up.
     */
    ModelNotInFront,

    /** The phone's own model read the screen and declined to repeat what was on it. */
    ModelRefused,

    /** The phone's own model has read for Deckard as much as it will today. */
    ModelOutOfQuota,

    /** The read worked and there was simply no text on the screen. */
    NoTextFound,
}
