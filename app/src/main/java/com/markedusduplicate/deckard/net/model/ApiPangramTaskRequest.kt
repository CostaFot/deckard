package com.markedusduplicate.deckard.net.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Body of Pangram's `POST /task` — the text to analyse for AI-generated ("slop") content.
 *
 * [model] names the detector to run. It is always sent: omitting it takes whatever Pangram's
 * default happens to be, which is a different (older) detector and would have Deckard disagreeing
 * with the site over the same passage.
 */
@Serializable
data class ApiPangramTaskRequest(
    val text: String,
    val model: String,
    @SerialName("public_dashboard_link") val publicDashboardLink: Boolean = false,
)
