package com.example.todoapp.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    
    @ColumnInfo(name = "priority")
    val priority: Priority = Priority.NORMAL,
    
    @ColumnInfo(name = "category")
    val category: String? = null,
    
    @ColumnInfo(name = "due_date")
    val dueDate: Date? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date = Date(),
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.LOCAL,
    
    @ColumnInfo(name = "remote_id")
    val remoteId: String? = null
)

enum class Priority {
    LOW, NORMAL, HIGH, URGENT
}

enum class SyncStatus {
    LOCAL,      // Только локально
    SYNCED,     // Синхронизирована с сервером
    PENDING,    // Ожидает синхронизации
    CONFLICT    // Конфликт при синхронизации
}