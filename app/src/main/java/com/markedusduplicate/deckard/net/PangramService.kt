package com.markedusduplicate.deckard.net

import com.markedusduplicate.deckard.net.model.ApiPangramDetection
import com.markedusduplicate.deckard.net.model.ApiPangramTaskCreated
import com.markedusduplicate.deckard.net.model.ApiPangramTaskRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Pangram AI-content detection API. Async: [createTask] submits the text and returns a task id;
 * [getTask] is polled until its [ApiPangramDetection.stage] is `STAGE_SUCCESS` or `STAGE_FAILED`.
 * The `x-api-key` auth header is added by an interceptor in
 * [com.markedusduplicate.deckard.di.NetworkModule], so it stays off these signatures.
 */
interface PangramService {

    @POST("task")
    suspend fun createTask(@Body request: ApiPangramTaskRequest): ApiPangramTaskCreated

    @GET("task/{taskId}")
    suspend fun getTask(@Path("taskId") taskId: String): ApiPangramDetection
}
