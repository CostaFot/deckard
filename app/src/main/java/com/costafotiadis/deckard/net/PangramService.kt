package com.costafotiadis.deckard.net

import com.costafotiadis.deckard.net.model.ApiPangramDetection
import com.costafotiadis.deckard.net.model.ApiPangramModels
import com.costafotiadis.deckard.net.model.ApiPangramTaskCreated
import com.costafotiadis.deckard.net.model.ApiPangramTaskRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Pangram AI-content detection API. Async: [createTask] submits the text and returns a task id;
 * [getTask] is polled until its [ApiPangramDetection.stage] is `STAGE_SUCCESS` or `STAGE_FAILED`.
 * [getModels] lists the detectors the key may ask for, so a key that cannot run the model we pin
 * says so before any text (and any money) is spent.
 * The `x-api-key` auth header is added by an interceptor in
 * [com.costafotiadis.deckard.di.NetworkModule], so it stays off these signatures.
 */
interface PangramService {

    @GET("models")
    suspend fun getModels(): ApiPangramModels

    @POST("task")
    suspend fun createTask(@Body request: ApiPangramTaskRequest): ApiPangramTaskCreated

    @GET("task/{taskId}")
    suspend fun getTask(@Path("taskId") taskId: String): ApiPangramDetection
}
