package com.example.todoapp.testing

import com.example.todoapp.data.database.entity.Priority
import com.example.todoapp.data.database.entity.SyncStatus
import com.example.todoapp.data.database.entity.TodoEntity
import com.example.todoapp.data.model.TodoDto
import com.example.todoapp.domain.model.Todo
import com.example.todoapp.domain.model.TodoPriority
import com.example.todoapp.domain.model.TodoSyncStatus
import java.util.Date

/**
 * Factory для создания тестовых данных
 * Убирает дублирование кода в тестах
 */
object TestDataFactory {
    
    /**
     * Создает TodoEntity для тестов БД
     */
    fun createTodoEntity(
        id: Long = 1L,
        title: String = "Test Task",
        isCompleted: Boolean = false,
        priority: Priority = Priority.NORMAL,
        syncStatus: SyncStatus = SyncStatus.SYNCED,
        remoteId: String? = "remote_$id"
    ): TodoEntity = TodoEntity(
        id = id,
        title = title,
        description = "Test Description $id",
        isCompleted = isCompleted,
        priority = priority,
        category = "Test",
        createdAt = Date(),
        updatedAt = Date(),
        syncStatus = syncStatus,
        remoteId = remoteId
    )
    
    /**
     * Создает Todo для тестов репозитория
     */
    fun createTodo(
        id: Long = 1L,
        title: String = "Test Task",
        isCompleted: Boolean = false,
        priority: TodoPriority = TodoPriority.NORMAL,
        syncStatus: TodoSyncStatus = TodoSyncStatus.LOCAL,
        remoteId: String? = null
    ): Todo = Todo(
        id = id,
        title = title,
        description = "Test Description $id",
        isCompleted = isCompleted,
        priority = priority,
        category = "Test",
        createdAt = Date(),
        updatedAt = Date(),
        syncStatus = syncStatus,
        remoteId = remoteId
    )
    
    /**
     * Создает TodoDto для тестов API
     */
    fun createTodoDto(
        id: String? = "1",
        title: String = "Test Task",
        completed: Boolean = false,
        priority: String = "NORMAL"
    ): TodoDto = TodoDto(
        id = id,
        localId = id?.toLongOrNull(),
        userId = 1,
        title = title,
        description = "Test Description",
        completed = completed,
        priority = priority,
        category = "Test",
        dueDate = System.currentTimeMillis() + 86400000, // +1 день
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        syncStatus = "PENDING"
    )
    
    /**
     * Builder для создания сложных TodoEntity
     */
    class TodoEntityBuilder {
        private var id: Long = 1L
        private var title: String = "Test Task"
        private var isCompleted: Boolean = false
        private var priority: Priority = Priority.NORMAL
        private var syncStatus: SyncStatus = SyncStatus.SYNCED
        private var remoteId: String? = null
        private var category: String = "Test"
        
        fun withId(id: Long) = apply { this.id = id }
        fun withTitle(title: String) = apply { this.title = title }
        fun completed() = apply { this.isCompleted = true }
        fun withPriority(priority: Priority) = apply { this.priority = priority }
        fun withSyncStatus(status: SyncStatus) = apply { this.syncStatus = status }
        fun withRemoteId(remoteId: String?) = apply { this.remoteId = remoteId }
        fun withCategory(category: String) = apply { this.category = category }
        
        fun build(): TodoEntity = TodoEntity(
            id = id,
            title = title,
            description = "Test Description $id",
            isCompleted = isCompleted,
            priority = priority,
            category = category,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = syncStatus,
            remoteId = remoteId
        )
    }
    
    /**
     * Создает Builder для TodoEntity
     */
    fun todoEntity(): TodoEntityBuilder = TodoEntityBuilder()
    
    /**
     * Создает список тестовых TodoEntity
     */
    fun createTodoEntityList(count: Int = 3): List<TodoEntity> {
        return (1..count).map { i ->
            createTodoEntity(
                id = i.toLong(),
                title = "Task $i",
                isCompleted = i % 2 == 0, // четные - выполненные
                priority = when (i % 3) {
                    0 -> Priority.LOW
                    1 -> Priority.NORMAL
                    else -> Priority.HIGH
                }
            )
        }
    }
    
    /**
     * Создает список тестовых TodoDto
     */
    fun createTodoDtoList(count: Int = 3): List<TodoDto> {
        return (1..count).map { i ->
            createTodoDto(
                id = i.toString(),
                title = "Task $i",
                completed = i % 2 == 0,
                priority = when (i % 3) {
                    0 -> "LOW"
                    1 -> "NORMAL"
                    else -> "HIGH"
                }
            )
        }
    }
}
