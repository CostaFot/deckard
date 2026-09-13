package com.markedusduplicate.deckard.net.model

import kotlinx.serialization.Serializable

/** Response to Pangram's `GET /models`: the detectors this API key is allowed to ask for. */
@Serializable
data class ApiPangramModels(
    val models: List<String> = emptyList(),
)
