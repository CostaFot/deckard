package com.markedusduplicate.deckard.slop

/** Domain form of one analysed segment ("window") of the judged text. */
data class DomainSlopWindow(
    val text: String,
    val label: String,
    val aiAssistanceScore: Double,
    val confidence: String,
    val startIndex: Int,
    val endIndex: Int,
    val wordCount: Int,
    val tokenLength: Int,
)
