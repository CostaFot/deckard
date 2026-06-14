package com.markedusduplicate.deckard.net.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response to Pangram's `POST /task`: the async task id to poll with `GET /task/{taskId}`. */
@Serializable
data class ApiPangramTaskCreated(
    @SerialName("task_id") val taskId: String,
)
