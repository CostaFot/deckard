package com.markedusduplicate.deckard.net.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body of Pangram's `POST /task` — the text to analyse for AI-generated ("slop") content. */
@Serializable
data class ApiPangramTaskRequest(
    val text: String,
    @SerialName("public_dashboard_link") val publicDashboardLink: Boolean = false,
)
