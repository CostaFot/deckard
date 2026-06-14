package com.markedusduplicate.deckard.slop

/**
 * Fewest words worth sending to the detector. Short snippets (UI labels, nav chrome, a one-line
 * caption) can't be judged reliably and waste a Pangram round-trip, so the overlay skips detection
 * below this.
 */
const val MIN_WORDS_TO_DETECT = 50

/** Counts whitespace-separated words in [text] (0 for blank). */
fun wordCount(text: String): Int = Regex("\\S+").findAll(text).count()
