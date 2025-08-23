package com.example.todoapp.domain.repository

import com.example.todoapp.domain.model.Todo
import com.example.todoapp.domain.model.TodoPriority
import com.example.todoapp.domain.model.TodoSyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface TodoRepository {
    
    // Основные операции CRUD  
    fun getAllTodos(): Flow<List<Todo>>          // Flow - не suspend
    suspend fun getTodoById(id: Long): Todo?     // Одноразовое получение - suspend
    suspend fun insertTodo(todo: Todo): Long     // Операция записи - suspend
    suspend fun updateTodo(todo: Todo)           // Операция записи - suspend  
    suspend fun deleteTodo(todo: Todo)           // Операция записи - suspend
    suspend fun deleteTodoById(id: Long)         // Операция записи - suspend
    
    // Фильтрация и сортировка (все Flow - не suspend)
    fun getActiveTodos(): Flow<List<Todo>>
    fun getCompletedTodos(): Flow<List<Todo>>
    fun getTodosByCategory(category: String): Flow<List<Todo>>
    fun getTodosByPriority(priority: TodoPriority): Flow<List<Todo>>
    fun getTodosByDateRange(startDate: Date, endDate: Date): Flow<List<Todo>>
    
    // Поиск (Flow - не suspend)
    fun searchTodos(query: String): Flow<List<Todo>>
    
    // Счетчики (все Flow - не suspend)
    fun getActiveCount(): Flow<Int>
    fun getCompletedCount(): Flow<Int>
    fun getCountByPriority(priority: TodoPriority): Flow<Int>
    
    // Операции с состоянием
    suspend fun markAsCompleted(id: Long)
    suspend fun markAsActive(id: Long)
    suspend fun toggleCompletion(id: Long)
    
    // Синхронизация
    suspend fun getTodosBySyncStatus(syncStatus: TodoSyncStatus): List<Todo>
    suspend fun updateSyncStatus(id: Long, syncStatus: TodoSyncStatus)
    suspend fun syncWithRemote(): Result<Unit>
    
    // Batch операции
    suspend fun insertTodos(todos: List<Todo>): List<Long>
    suspend fun deleteCompletedTodos()
    suspend fun deleteAllTodos()
    
    // Уведомления для фоновых сервисов
    suspend fun markForSync(id: Long)
    suspend fun getPendingSyncTodos(): List<Todo>
}