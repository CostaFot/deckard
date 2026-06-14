package com.markedusduplicate.deckard.net

import com.markedusduplicate.deckard.net.model.ApiTodo
import retrofit2.http.GET
import retrofit2.http.Path

interface JsonPlaceHolderService {

    @GET("/todos/{id}")
    suspend fun getTodo(@Path(value = "id") todoId: Int): ApiTodo
}
