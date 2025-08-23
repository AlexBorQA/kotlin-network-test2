package com.example.todoapp.domain.model

import java.util.Date

data class Todo(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean = false,
    val priority: TodoPriority = TodoPriority.NORMAL,
    val category: String? = null,
    val dueDate: Date? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val syncStatus: TodoSyncStatus = TodoSyncStatus.LOCAL,
    val remoteId: String? = null
) {
    val isOverdue: Boolean
        get() = dueDate?.let { it.before(Date()) && !isCompleted } ?: false
    
    val isPendingSync: Boolean
        get() = syncStatus == TodoSyncStatus.PENDING
    
    val isHighPriority: Boolean
        get() = priority == TodoPriority.HIGH || priority == TodoPriority.URGENT
}

enum class TodoPriority(val displayName: String, val value: Int) {
    LOW("Low", 1),
    NORMAL("Normal", 2), 
    HIGH("High", 3),
    URGENT("Urgent", 4)
}

enum class TodoSyncStatus(val displayName: String) {
    LOCAL("Local only"),
    SYNCED("Synced"),
    PENDING("Pending sync"),
    CONFLICT("Sync conflict")
}