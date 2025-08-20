package com.example.todoapp.data.api

import com.example.todoapp.data.model.TodoDto
import retrofit2.Response
import retrofit2.http.*

interface TodoApiService {
    
    @GET("todos")
    suspend fun getAllTodos(): Response<List<TodoDto>>
    
    @GET("todos/{id}")
    suspend fun getTodoById(@Path("id") id: String): Response<TodoDto>
    
    @GET("todos")
    suspend fun getUserTodos(@Query("userId") userId: Int): Response<List<TodoDto>>
    
    @POST("todos")
    suspend fun createTodo(@Body todo: TodoDto): Response<TodoDto>
    
    @PUT("todos/{id}")
    suspend fun updateTodo(
        @Path("id") id: String,
        @Body todo: TodoDto
    ): Response<TodoDto>
    
    @PATCH("todos/{id}")
    suspend fun patchTodo(
        @Path("id") id: String,
        @Body todo: TodoDto
    ): Response<TodoDto>
    
    @DELETE("todos/{id}")
    suspend fun deleteTodo(@Path("id") id: String): Response<Void>
    
    // Дополнительные эндпоинты для синхронизации
    @GET("todos/sync")
    suspend fun getUpdatedTodos(
        @Query("lastSync") lastSyncTimestamp: Long
    ): Response<List<TodoDto>>
    
    @POST("todos/batch")
    suspend fun syncTodos(@Body todos: List<TodoDto>): Response<List<TodoDto>>
}