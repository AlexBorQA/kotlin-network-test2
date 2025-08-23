package com.example.todoapp.data.model

import com.google.gson.annotations.SerializedName
import com.example.todoapp.domain.model.TodoPriority
import com.example.todoapp.domain.model.TodoSyncStatus

data class TodoDto(
    @SerializedName("id")
    val id: String? = null,
    
    @SerializedName("localId")
    val localId: Long? = null,
    
    @SerializedName("userId")
    val userId: Int? = null,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("completed")
    val completed: Boolean = false,
    
    @SerializedName("priority")
    val priority: String = "NORMAL",
    
    @SerializedName("category")
    val category: String? = null,
    
    @SerializedName("dueDate")
    val dueDate: Long? = null,
    
    @SerializedName("createdAt")
    val createdAt: Long,
    
    @SerializedName("updatedAt")
    val updatedAt: Long,
    
    @SerializedName("syncStatus")
    val syncStatus: String = "PENDING"
)