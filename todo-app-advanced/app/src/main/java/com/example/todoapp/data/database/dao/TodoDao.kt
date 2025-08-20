package com.example.todoapp.data.database.dao

import androidx.room.*
import com.example.todoapp.data.database.entity.Priority
import com.example.todoapp.data.database.entity.SyncStatus
import com.example.todoapp.data.database.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    
    @Query("SELECT * FROM todos ORDER BY priority DESC, created_at DESC")
    fun getAllTodos(): Flow<List<TodoEntity>>
    
    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoById(id: Long): TodoEntity?
    
    @Query("SELECT * FROM todos WHERE is_completed = 0 ORDER BY priority DESC, due_date ASC")
    fun getActiveTodos(): Flow<List<TodoEntity>>
    
    @Query("SELECT * FROM todos WHERE is_completed = 1 ORDER BY updated_at DESC")
    fun getCompletedTodos(): Flow<List<TodoEntity>>
    
    @Query("SELECT * FROM todos WHERE category = :category ORDER BY priority DESC, created_at DESC")
    fun getTodosByCategory(category: String): Flow<List<TodoEntity>>
    
    @Query("SELECT * FROM todos WHERE priority = :priority ORDER BY created_at DESC")
    fun getTodosByPriority(priority: Priority): Flow<List<TodoEntity>>
    
    @Query("SELECT * FROM todos WHERE sync_status = :syncStatus")
    suspend fun getTodosBySyncStatus(syncStatus: SyncStatus): List<TodoEntity>
    
    @Query("SELECT * FROM todos WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchTodos(query: String): Flow<List<TodoEntity>>
    
    @Query("SELECT * FROM todos WHERE due_date BETWEEN :startDate AND :endDate ORDER BY due_date ASC")
    fun getTodosByDateRange(startDate: Long, endDate: Long): Flow<List<TodoEntity>>
    
    @Query("SELECT COUNT(*) FROM todos WHERE is_completed = 0")
    fun getActiveCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM todos WHERE is_completed = 1")
    fun getCompletedCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM todos WHERE priority = :priority AND is_completed = 0")
    fun getCountByPriority(priority: Priority): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodos(todos: List<TodoEntity>): List<Long>
    
    @Update
    suspend fun updateTodo(todo: TodoEntity)
    
    @Query("UPDATE todos SET is_completed = :isCompleted, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateCompletionStatus(id: Long, isCompleted: Boolean, updatedAt: Long)
    
    @Query("UPDATE todos SET sync_status = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, syncStatus: SyncStatus)
    
    @Query("UPDATE todos SET remote_id = :remoteId, sync_status = :syncStatus WHERE id = :id")
    suspend fun updateRemoteId(id: Long, remoteId: String, syncStatus: SyncStatus)
    
    @Delete
    suspend fun deleteTodo(todo: TodoEntity)
    
    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodoById(id: Long)
    
    @Query("DELETE FROM todos WHERE is_completed = 1")
    suspend fun deleteCompletedTodos()
    
    @Query("DELETE FROM todos")
    suspend fun deleteAllTodos()
    
    @Transaction
    suspend fun markAsCompleted(id: Long) {
        updateCompletionStatus(id, true, System.currentTimeMillis())
    }
    
    @Transaction
    suspend fun markAsActive(id: Long) {
        updateCompletionStatus(id, false, System.currentTimeMillis())
    }
}