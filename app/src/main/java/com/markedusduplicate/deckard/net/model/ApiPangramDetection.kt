package com.markedusduplicate.deckard.net.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Result of polling Pangram's `GET /task/{taskId}`. The shape is the same across stages, so every
 * field is nullable/defaulted: in-progress responses carry only [stage], failures carry empties,
 * and the full breakdown is present once [stage] is `STAGE_SUCCESS`.
 */
@Serializable
data class ApiPangramDetection(
    val stage: String? = null,
    val text: String? = null,
    val version: String? = null,
    val headline: String? = null,
    val prediction: String? = null,
    @SerialName("prediction_short") val predictionShort: String? = null,
    @SerialName("fraction_ai") val fractionAi: Double? = null,
    @SerialName("fraction_ai_assisted") val fractionAiAssisted: Double? = null,
    @SerialName("fraction_human") val fractionHuman: Double? = null,
    @SerialName("num_ai_segments") val numAiSegments: Int? = null,
    @SerialName("num_ai_assisted_segments") val numAiAssistedSegments: Int? = null,
    @SerialName("num_human_segments") val numHumanSegments: Int? = null,
    @SerialName("dashboard_link") val dashboardLink: String? = null,
    val windows: List<ApiPangramWindow>? = null,
)
