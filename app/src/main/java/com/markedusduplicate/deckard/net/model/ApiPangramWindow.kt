package com.markedusduplicate.deckard.net.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One analysed segment ("window") of the input text in a Pangram detection result. All fields are
 * nullable: non-success poll responses omit them.
 */
@Serializable
data class ApiPangramWindow(
    val text: String? = null,
    val label: String? = null,
    @SerialName("ai_assistance_score") val aiAssistanceScore: Double? = null,
    val confidence: String? = null,
    @SerialName("start_index") val startIndex: Int? = null,
    @SerialName("end_index") val endIndex: Int? = null,
    @SerialName("word_count") val wordCount: Int? = null,
    @SerialName("token_length") val tokenLength: Int? = null,
)
