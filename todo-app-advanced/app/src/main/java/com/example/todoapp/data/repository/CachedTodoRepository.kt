package com.example.todoapp.data.repository

import android.util.Log
import com.example.todoapp.core.idling.IdlingResourceProvider
import com.example.todoapp.data.api.TodoApiService
import com.example.todoapp.data.database.dao.TodoDao
import com.example.todoapp.data.database.entity.SyncStatus
import com.example.todoapp.data.mapper.TodoMapper
import com.example.todoapp.data.model.TodoDto
import com.example.todoapp.data.network.NetworkManager
import com.example.todoapp.domain.model.Todo
import com.example.todoapp.domain.model.TodoPriority
import com.example.todoapp.domain.model.TodoSyncStatus
import com.example.todoapp.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий с кэшированием и продуманной логикой синхронизации:
 * - Учёт версий для разрешения конфликтов
 * - Батчевая синхронизация
 * - Оптимистичная блокировка
 * - Стратегия "offline-first"
 * - Поддержка IdlingResource для тестирования
 */
@Singleton
class CachedTodoRepository @Inject constructor(
    private val todoDao: TodoDao,
    private val apiService: TodoApiService,
    private val networkManager: NetworkManager,
    private val idlingResource: IdlingResourceProvider
) : TodoRepository {
    
    companion object {
        private const val TAG = "CachedTodoRepository"
        private const val BATCH_SIZE = 20
    }
    
    // ========== Основные операции чтения ==========
    
    override fun getAllTodos(): Flow<List<Todo>> {
        // Всегда возвращаем из локальной БД (offline-first)
        return todoDao.getAllTodos().map { entities ->
            TodoMapper.mapEntitiesToDomain(entities)
        }
    }
    
    override suspend fun getTodoById(id: Long): Todo? {
        return todoDao.getTodoById(id)?.let { entity ->
            TodoMapper.mapEntityToDomain(entity)
        }
    }
    
    // ========== Операции записи с отложенной синхронизацией ==========
    
    override suspend fun insertTodo(todo: Todo): Long {
        val timestamp = Date()
        val localId = todoDao.insertTodo(TodoMapper.mapDomainToEntity(
            todo.copy(
                createdAt = timestamp,
                updatedAt = timestamp,
                syncStatus = TodoSyncStatus.PENDING // Всегда помечаем для синхронизации
            )
        ))
        
        // Запускаем фоновую синхронизацию если есть сеть
        if (networkManager.isNetworkAvailable()) {
            trySyncSingleTodo(localId)
        }
        
        return localId
    }
    
    override suspend fun updateTodo(todo: Todo) {
        val timestamp = Date()
        val updatedTodo = todo.copy(
            updatedAt = timestamp,
            // Если todo уже синхронизировано, помечаем для повторной синхронизации
            syncStatus = if (todo.syncStatus == TodoSyncStatus.SYNCED) {
                TodoSyncStatus.PENDING
            } else {
                todo.syncStatus
            }
        )
        
        todoDao.updateTodo(TodoMapper.mapDomainToEntity(updatedTodo))
        
        // Запускаем фоновую синхронизацию если есть сеть
        if (networkManager.isNetworkAvailable()) {
            trySyncSingleTodo(todo.id)
        }
    }
    
    override suspend fun deleteTodo(todo: Todo) {
        if (todo.remoteId != null) {
            // Если есть remote ID, помечаем для удаления на сервере
            val deletedTodo = todo.copy(
                syncStatus = TodoSyncStatus.PENDING,
                // Добавляем флаг удаления в описание (простой способ)
                description = "__DELETED__${todo.description ?: ""}"
            )
            todoDao.updateTodo(TodoMapper.mapDomainToEntity(deletedTodo))
            
            if (networkManager.isNetworkAvailable()) {
                tryDeleteRemoteTodo(todo)
            }
        } else {
            // Локальное todo - просто удаляем
            todoDao.deleteTodo(TodoMapper.mapDomainToEntity(todo))
        }
    }
    
    override suspend fun deleteTodoById(id: Long) {
        getTodoById(id)?.let { todo ->
            deleteTodo(todo)
        }
    }
    
    // ========== Специализированные запросы ==========
    
    override fun getActiveTodos(): Flow<List<Todo>> {
        return todoDao.getActiveTodos().map { entities ->
            TodoMapper.mapEntitiesToDomain(entities)
        }
    }
    
    override fun getCompletedTodos(): Flow<List<Todo>> {
        return todoDao.getCompletedTodos().map { entities ->
            TodoMapper.mapEntitiesToDomain(entities)
        }
    }
    
    override fun getTodosByCategory(category: String): Flow<List<Todo>> {
        return todoDao.getTodosByCategory(category).map { entities ->
            TodoMapper.mapEntitiesToDomain(entities)
        }
    }
    
    override fun getTodosByPriority(priority: TodoPriority): Flow<List<Todo>> {
        val entityPriority = mapPriorityToEntity(priority)
        return todoDao.getTodosByPriority(entityPriority).map { entities ->
            TodoMapper.mapEntitiesToDomain(entities)
        }
    }
    
    override fun getTodosByDateRange(startDate: Date, endDate: Date): Flow<List<Todo>> {
        return todoDao.getTodosByDateRange(startDate.time, endDate.time).map { entities ->
            TodoMapper.mapEntitiesToDomain(entities)
        }
    }
    
    override fun searchTodos(query: String): Flow<List<Todo>> {
        return todoDao.searchTodos(query).map { entities ->
            TodoMapper.mapEntitiesToDomain(entities)
        }
    }
    
    // ========== Счётчики ==========
    
    override fun getActiveCount(): Flow<Int> = todoDao.getActiveCount()
    override fun getCompletedCount(): Flow<Int> = todoDao.getCompletedCount()
    
    override fun getCountByPriority(priority: TodoPriority): Flow<Int> {
        val entityPriority = mapPriorityToEntity(priority)
        return todoDao.getCountByPriority(entityPriority)
    }
    
    // ========== Операции изменения статуса ==========
    
    override suspend fun markAsCompleted(id: Long) {
        todoDao.markAsCompleted(id)
        todoDao.updateSyncStatus(id, SyncStatus.PENDING)
        
        if (networkManager.isNetworkAvailable()) {
            trySyncSingleTodo(id)
        }
    }
    
    override suspend fun markAsActive(id: Long) {
        todoDao.markAsActive(id)
        todoDao.updateSyncStatus(id, SyncStatus.PENDING)
        
        if (networkManager.isNetworkAvailable()) {
            trySyncSingleTodo(id)
        }
    }
    
    override suspend fun toggleCompletion(id: Long) {
        val todo = getTodoById(id)
        if (todo != null) {
            if (todo.isCompleted) {
                markAsActive(id)
            } else {
                markAsCompleted(id)
            }
        }
    }
    
    // ========== Синхронизация ==========
    
    override suspend fun syncWithRemote(): Result<Unit> {
        if (!networkManager.isNetworkAvailable()) {
            return Result.failure(Exception("No internet connection"))
        }
        
        idlingResource.increment("syncWithRemote")
        return try {
            coroutineScope {
                // 1. Получаем время последней синхронизации
                val lastSyncTime = getLastSyncTime()
                
                // 2. Параллельно выполняем:
                val uploadJob = async { uploadPendingChanges() }
                val downloadJob = async { downloadRemoteChanges(lastSyncTime) }
                
                // 3. Ждём завершения обеих операций
                val uploadResult = uploadJob.await()
                val downloadResult = downloadJob.await()
                
                // 4. Обрабатываем конфликты если есть
                if (uploadResult.conflicts.isNotEmpty() || downloadResult.conflicts.isNotEmpty()) {
                    resolveConflicts(uploadResult.conflicts + downloadResult.conflicts)
                }
                
                // 5. Сохраняем время синхронизации
                saveLastSyncTime(Date())
                
                // 6. Очищаем удалённые записи
                cleanupDeletedTodos()
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.failure(e)
        } finally {
            idlingResource.decrement("syncWithRemote")
        }
    }
    
    override suspend fun getTodosBySyncStatus(syncStatus: TodoSyncStatus): List<Todo> {
        val entitySyncStatus = mapSyncStatusToEntity(syncStatus)
        val entities = todoDao.getTodosBySyncStatus(entitySyncStatus)
        return TodoMapper.mapEntitiesToDomain(entities)
    }
    
    override suspend fun updateSyncStatus(id: Long, syncStatus: TodoSyncStatus) {
        val entitySyncStatus = mapSyncStatusToEntity(syncStatus)
        todoDao.updateSyncStatus(id, entitySyncStatus)
    }
    
    override suspend fun insertTodos(todos: List<Todo>): List<Long> {
        val entities = TodoMapper.mapDomainToEntities(todos)
        return todoDao.insertTodos(entities)
    }
    
    override suspend fun deleteCompletedTodos() = todoDao.deleteCompletedTodos()
    override suspend fun deleteAllTodos() = todoDao.deleteAllTodos()
    
    override suspend fun markForSync(id: Long) {
        todoDao.updateSyncStatus(id, SyncStatus.PENDING)
    }
    
    override suspend fun getPendingSyncTodos(): List<Todo> {
        return getTodosBySyncStatus(TodoSyncStatus.PENDING)
    }
    
    // ========== Приватные методы синхронизации ==========
    
    private data class SyncResult(
        val successful: Int = 0,
        val failed: Int = 0,
        val conflicts: List<ConflictInfo> = emptyList()
    )
    
    private data class ConflictInfo(
        val localTodo: Todo,
        val remoteTodo: TodoDto,
        val resolution: ConflictResolution = ConflictResolution.KEEP_LATEST
    )
    
    private enum class ConflictResolution {
        KEEP_LOCAL,
        KEEP_REMOTE,
        KEEP_LATEST,
        MERGE
    }
    
    private suspend fun uploadPendingChanges(): SyncResult {
        val pendingTodos = getPendingSyncTodos()
        
        if (pendingTodos.isEmpty()) {
            return SyncResult()
        }
        
        idlingResource.increment("uploadPendingChanges")
        var successful = 0
        var failed = 0
        val conflicts = mutableListOf<ConflictInfo>()
        
        // Группируем по батчам для оптимизации
        pendingTodos.chunked(BATCH_SIZE).forEach { batch ->
            try {
                // Отделяем удалённые от обычных
                val (deletedTodos, regularTodos) = batch.partition { 
                    it.description?.startsWith("__DELETED__") == true
                }
                
                // Обрабатываем удаления
                deletedTodos.forEach { todo ->
                    if (todo.remoteId != null) {
                        try {
                            apiService.deleteTodo(todo.remoteId)
                            // Полностью удаляем из БД после успешного удаления на сервере
                            todoDao.deleteTodoById(todo.id)
                            successful++
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to delete remote todo ${todo.remoteId}", e)
                            failed++
                        }
                    }
                }
                
                // Обрабатываем обычные изменения
                if (regularTodos.isNotEmpty()) {
                    val dtos = regularTodos.map { TodoMapper.mapDomainToDto(it) }
                    val response = apiService.syncTodos(dtos)
                    
                    if (response.isSuccessful) {
                        response.body()?.let { syncedDtos ->
                            syncedDtos.zip(regularTodos).forEach { (dto, localTodo) ->
                                // Обновляем remote ID и статус
                                dto.id?.let { remoteId ->
                                    todoDao.updateRemoteId(localTodo.id, remoteId, SyncStatus.SYNCED)
                                }
                            }
                            successful += syncedDtos.size
                        }
                    } else {
                        failed += regularTodos.size
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Batch sync failed", e)
                failed += batch.size
            }
        }
        
        Log.d(TAG, "Upload complete: $successful successful, $failed failed")
        idlingResource.decrement("uploadPendingChanges")
        return SyncResult(successful, failed, conflicts)
    }
    
    private suspend fun downloadRemoteChanges(lastSyncTime: Date): SyncResult {
        idlingResource.increment("downloadRemoteChanges")
        return try {
            val response = apiService.getUpdatedTodos(lastSyncTime.time)
            
            if (response.isSuccessful) {
                response.body()?.let { remoteTodos ->
                    processRemoteTodos(remoteTodos)
                } ?: SyncResult()
            } else {
                SyncResult(failed = 1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            SyncResult(failed = 1)
        } finally {
            idlingResource.decrement("downloadRemoteChanges")
        }
    }
    
    private suspend fun processRemoteTodos(remoteTodos: List<TodoDto>): SyncResult {
        var successful = 0
        var failed = 0
        val conflicts = mutableListOf<ConflictInfo>()
        
        remoteTodos.forEach { remoteDto ->
            try {
                val localTodo = findTodoByRemoteId(remoteDto.id ?: "")
                
                if (localTodo == null) {
                    // Новое todo с сервера
                    val newTodo = TodoMapper.mapDtoToDomain(remoteDto).copy(
                        syncStatus = TodoSyncStatus.SYNCED
                    )
                    val entity = TodoMapper.mapDomainToEntity(newTodo.copy(id = 0))
                    todoDao.insertTodo(entity)
                    successful++
                } else {
                    // Проверяем на конфликт
                    val remoteUpdated = Date(remoteDto.updatedAt)
                    val localUpdated = localTodo.updatedAt
                    
                    when {
                        // Remote новее и локальных изменений нет
                        remoteUpdated.after(localUpdated) && localTodo.syncStatus == TodoSyncStatus.SYNCED -> {
                            val updatedTodo = TodoMapper.mapDtoToDomain(remoteDto).copy(
                                id = localTodo.id,
                                syncStatus = TodoSyncStatus.SYNCED
                            )
                            todoDao.updateTodo(TodoMapper.mapDomainToEntity(updatedTodo))
                            successful++
                        }
                        
                        // Есть конфликт - оба изменились
                        remoteUpdated.after(localUpdated) && localTodo.syncStatus == TodoSyncStatus.PENDING -> {
                            conflicts.add(ConflictInfo(localTodo, remoteDto))
                        }
                        
                        // Локальная версия новее - игнорируем remote
                        else -> {
                            Log.d(TAG, "Keeping local version for todo ${localTodo.id}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process remote todo", e)
                failed++
            }
        }
        
        Log.d(TAG, "Download complete: $successful successful, $failed failed, ${conflicts.size} conflicts")
        return SyncResult(successful, failed, conflicts)
    }
    
    private suspend fun resolveConflicts(conflicts: List<ConflictInfo>) {
        conflicts.forEach { conflict ->
            when (conflict.resolution) {
                ConflictResolution.KEEP_LATEST -> {
                    // Берём версию с более поздним updatedAt
                    val remoteUpdated = Date(conflict.remoteTodo.updatedAt)
                    if (remoteUpdated.after(conflict.localTodo.updatedAt)) {
                        // Применяем remote версию
                        val resolvedTodo = TodoMapper.mapDtoToDomain(conflict.remoteTodo).copy(
                            id = conflict.localTodo.id,
                            syncStatus = TodoSyncStatus.SYNCED
                        )
                        todoDao.updateTodo(TodoMapper.mapDomainToEntity(resolvedTodo))
                    }
                    // Иначе оставляем локальную
                }
                
                ConflictResolution.KEEP_LOCAL -> {
                    // Помечаем для повторной синхронизации
                    todoDao.updateSyncStatus(conflict.localTodo.id, SyncStatus.PENDING)
                }
                
                ConflictResolution.KEEP_REMOTE -> {
                    val resolvedTodo = TodoMapper.mapDtoToDomain(conflict.remoteTodo).copy(
                        id = conflict.localTodo.id,
                        syncStatus = TodoSyncStatus.SYNCED
                    )
                    todoDao.updateTodo(TodoMapper.mapDomainToEntity(resolvedTodo))
                }
                
                ConflictResolution.MERGE -> {
                    // Простая стратегия мержа - берём title от local, остальное от remote
                    val mergedTodo = TodoMapper.mapDtoToDomain(conflict.remoteTodo).copy(
                        id = conflict.localTodo.id,
                        title = conflict.localTodo.title,
                        syncStatus = TodoSyncStatus.PENDING
                    )
                    todoDao.updateTodo(TodoMapper.mapDomainToEntity(mergedTodo))
                }
            }
        }
        
        Log.d(TAG, "Resolved ${conflicts.size} conflicts")
    }
    
    private suspend fun trySyncSingleTodo(localId: Long) {
        try {
            val todo = getTodoById(localId) ?: return
            val dto = TodoMapper.mapDomainToDto(todo)
            
            val response = if (todo.remoteId == null) {
                apiService.createTodo(dto)
            } else {
                apiService.updateTodo(todo.remoteId, dto)
            }
            
            if (response.isSuccessful) {
                response.body()?.let { responseDto ->
                    responseDto.id?.let { remoteId ->
                        todoDao.updateRemoteId(localId, remoteId, SyncStatus.SYNCED)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync single todo", e)
        }
    }
    
    private suspend fun tryDeleteRemoteTodo(todo: Todo) {
        try {
            todo.remoteId?.let { remoteId ->
                val response = apiService.deleteTodo(remoteId)
                if (response.isSuccessful) {
                    // Удаляем локально после успешного удаления на сервере
                    todoDao.deleteTodoById(todo.id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete remote todo", e)
        }
    }
    
    private suspend fun findTodoByRemoteId(remoteId: String): Todo? {
        val allTodos = getAllTodos().first()
        return allTodos.find { it.remoteId == remoteId }
    }
    
    private suspend fun cleanupDeletedTodos() {
        // Удаляем todos помеченные как удалённые и синхронизированные
        val todos = getAllTodos().first()
        todos.filter { 
            it.description?.startsWith("__DELETED__") == true && 
            it.syncStatus == TodoSyncStatus.SYNCED 
        }.forEach { todo ->
            todoDao.deleteTodoById(todo.id)
        }
    }
    
    private suspend fun getLastSyncTime(): Date {
        // В реальном приложении это должно храниться в SharedPreferences или DataStore
        // Для примера возвращаем время 24 часа назад
        return Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
    }
    
    private suspend fun saveLastSyncTime(time: Date) {
        // В реальном приложении сохраняем в SharedPreferences или DataStore
        Log.d(TAG, "Last sync time saved: $time")
    }
    
    // ========== Маппинг ==========
    
    private fun mapPriorityToEntity(priority: TodoPriority) = when (priority) {
        TodoPriority.LOW -> com.example.todoapp.data.database.entity.Priority.LOW
        TodoPriority.NORMAL -> com.example.todoapp.data.database.entity.Priority.NORMAL
        TodoPriority.HIGH -> com.example.todoapp.data.database.entity.Priority.HIGH
        TodoPriority.URGENT -> com.example.todoapp.data.database.entity.Priority.URGENT
    }
    
    private fun mapSyncStatusToEntity(syncStatus: TodoSyncStatus) = when (syncStatus) {
        TodoSyncStatus.LOCAL -> SyncStatus.LOCAL
        TodoSyncStatus.SYNCED -> SyncStatus.SYNCED
        TodoSyncStatus.PENDING -> SyncStatus.PENDING
        TodoSyncStatus.CONFLICT -> SyncStatus.CONFLICT
    }
}