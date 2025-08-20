package com.example.todoapp.data.mapper

import com.example.todoapp.data.database.entity.Priority
import com.example.todoapp.data.database.entity.SyncStatus
import com.example.todoapp.data.database.entity.TodoEntity
import com.example.todoapp.data.model.TodoDto
import com.example.todoapp.domain.model.Todo
import com.example.todoapp.domain.model.TodoPriority
import com.example.todoapp.domain.model.TodoSyncStatus
import java.util.Date

object TodoMapper {
    
    fun mapEntityToDomain(entity: TodoEntity): Todo {
        return Todo(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            isCompleted = entity.isCompleted,
            priority = mapPriorityToDomain(entity.priority),
            category = entity.category,
            dueDate = entity.dueDate,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            syncStatus = mapSyncStatusToDomain(entity.syncStatus),
            remoteId = entity.remoteId
        )
    }
    
    fun mapDomainToEntity(todo: Todo): TodoEntity {
        return TodoEntity(
            id = todo.id,
            title = todo.title,
            description = todo.description,
            isCompleted = todo.isCompleted,
            priority = mapPriorityToEntity(todo.priority),
            category = todo.category,
            dueDate = todo.dueDate,
            createdAt = todo.createdAt,
            updatedAt = todo.updatedAt,
            syncStatus = mapSyncStatusToEntity(todo.syncStatus),
            remoteId = todo.remoteId
        )
    }
    
    fun mapEntitiesToDomain(entities: List<TodoEntity>): List<Todo> {
        return entities.map { mapEntityToDomain(it) }
    }
    
    fun mapDomainToEntities(todos: List<Todo>): List<TodoEntity> {
        return todos.map { mapDomainToEntity(it) }
    }
    
    private fun mapPriorityToDomain(priority: Priority): TodoPriority {
        return when (priority) {
            Priority.LOW -> TodoPriority.LOW
            Priority.NORMAL -> TodoPriority.NORMAL
            Priority.HIGH -> TodoPriority.HIGH
            Priority.URGENT -> TodoPriority.URGENT
        }
    }
    
    private fun mapPriorityToEntity(priority: TodoPriority): Priority {
        return when (priority) {
            TodoPriority.LOW -> Priority.LOW
            TodoPriority.NORMAL -> Priority.NORMAL
            TodoPriority.HIGH -> Priority.HIGH
            TodoPriority.URGENT -> Priority.URGENT
        }
    }
    
    private fun mapSyncStatusToDomain(syncStatus: SyncStatus): TodoSyncStatus {
        return when (syncStatus) {
            SyncStatus.LOCAL -> TodoSyncStatus.LOCAL
            SyncStatus.SYNCED -> TodoSyncStatus.SYNCED
            SyncStatus.PENDING -> TodoSyncStatus.PENDING
            SyncStatus.CONFLICT -> TodoSyncStatus.CONFLICT
        }
    }
    
    private fun mapSyncStatusToEntity(syncStatus: TodoSyncStatus): SyncStatus {
        return when (syncStatus) {
            TodoSyncStatus.LOCAL -> SyncStatus.LOCAL
            TodoSyncStatus.SYNCED -> SyncStatus.SYNCED
            TodoSyncStatus.PENDING -> SyncStatus.PENDING
            TodoSyncStatus.CONFLICT -> SyncStatus.CONFLICT
        }
    }
    
    // API mapping functions
    fun mapDtoToDomain(dto: TodoDto): Todo {
        return Todo(
            id = dto.localId ?: 0L,
            title = dto.title,
            description = dto.description,
            isCompleted = dto.completed,
            priority = mapStringToPriority(dto.priority),
            category = dto.category,
            dueDate = dto.dueDate?.let { Date(it) },
            createdAt = Date(dto.createdAt),
            updatedAt = Date(dto.updatedAt),
            syncStatus = mapStringToSyncStatus(dto.syncStatus),
            remoteId = dto.id
        )
    }
    
    fun mapDomainToDto(todo: Todo): TodoDto {
        return TodoDto(
            id = todo.remoteId,
            localId = todo.id,
            title = todo.title,
            description = todo.description,
            completed = todo.isCompleted,
            priority = todo.priority.name,
            category = todo.category,
            dueDate = todo.dueDate?.time,
            createdAt = todo.createdAt.time,
            updatedAt = todo.updatedAt.time,
            syncStatus = todo.syncStatus.name
        )
    }
    
    fun mapDtosToEntities(dtos: List<TodoDto>): List<TodoEntity> {
        return dtos.map { dto ->
            val todo = mapDtoToDomain(dto)
            mapDomainToEntity(todo)
        }
    }
    
    private fun mapStringToPriority(priorityString: String): TodoPriority {
        return try {
            TodoPriority.valueOf(priorityString.uppercase())
        } catch (e: IllegalArgumentException) {
            TodoPriority.NORMAL
        }
    }
    
    private fun mapStringToSyncStatus(syncStatusString: String): TodoSyncStatus {
        return try {
            TodoSyncStatus.valueOf(syncStatusString.uppercase())
        } catch (e: IllegalArgumentException) {
            TodoSyncStatus.LOCAL
        }
    }
}