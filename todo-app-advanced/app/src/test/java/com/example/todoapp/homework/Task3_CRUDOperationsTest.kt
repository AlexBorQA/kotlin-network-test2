package com.example.todoapp.homework

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import com.example.todoapp.core.idling.IdlingResourceProvider
import com.example.todoapp.data.database.dao.TodoDao
import com.example.todoapp.data.database.entity.SyncStatus
import com.example.todoapp.data.api.TodoApiService
import com.example.todoapp.data.mapper.TodoMapper
import com.example.todoapp.data.model.TodoDto
import com.example.todoapp.data.network.NetworkManager
import com.example.todoapp.data.repository.CachedTodoRepository
import com.example.todoapp.domain.model.Todo
import com.example.todoapp.domain.model.TodoPriority
import com.example.todoapp.domain.model.TodoSyncStatus
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.junit.Ignore
import org.junit.Assert.*
import org.junit.After
import java.util.Date

/**
 * Домашнее задание - Часть 3: Тестирование CRUD операций
 * 
 * Цель: Протестировать полный цикл CRUD операций с задачами,
 * включая синхронизацию между локальной БД и сервером.
 */
class Task3_CRUDOperationsTest {

    private lateinit var mockDao: TodoDao
    private lateinit var mockApiService: TodoApiService
    private lateinit var mockNetworkManager: NetworkManager
    private lateinit var mockIdlingResource: IdlingResourceProvider
    private lateinit var repository: CachedTodoRepository

    @Before
    fun setup() {
        // Дополнительно мокаем Log для CachedTodoRepository
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0
        
        mockDao = mockk(relaxed = true)
        mockApiService = mockk(relaxed = true)
        mockNetworkManager = mockk()
        mockIdlingResource = mockk(relaxed = true)
        repository = CachedTodoRepository(
            todoDao = mockDao,
            apiService = mockApiService,
            networkManager = mockNetworkManager,
            idlingResource = mockIdlingResource
        )
    }
    
    @After
    fun tearDown() {
        // Очищаем мокинг Log
        unmockkStatic(Log::class)
    }

    /**
     * Тест 1: CREATE - Полный цикл создания задачи
     * 
     * Ожидаемый результат:
     * - Задача сохраняется локально
     * - При наличии сети отправляется на сервер
     * - Получает remoteId и обновляет статус на SYNCED
     */
    @Test
    fun `test CREATE operation full cycle with sync`() = runTest {
        // Arrange: Подготовить новую задачу и настроить моки
        val newTodo = Todo(
            id = 0,
            title = "New Task",
            isCompleted = false,
            priority = TodoPriority.NORMAL,
            syncStatus = TodoSyncStatus.PENDING,
            remoteId = null,
            createdAt = Date(),
            updatedAt = Date()
        )
        
        val localId = 123L
        val remoteId = "remote_456"
        val currentTime = System.currentTimeMillis()
        
        // Mock сети - есть интернет
        every { mockNetworkManager.isNetworkAvailable() } returns true
        
        // Mock DAO - insertTodo возвращает localId
        coEvery { mockDao.insertTodo(any()) } returns localId
        coEvery { mockDao.updateTodo(any()) } just Runs
        
        // Mock DAO - getTodoById для trySyncSingleTodo
        val savedEntity = TodoMapper.mapDomainToEntity(newTodo.copy(id = localId))
        coEvery { mockDao.getTodoById(localId) } returns savedEntity
        coEvery { mockDao.updateRemoteId(localId, remoteId, SyncStatus.SYNCED) } just Runs
        
        // Mock API - успешное создание на сервере
        val createdDto = TodoDto(
            id = remoteId,
            title = "New Task",
            completed = false,
            userId = 1,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        coEvery { mockApiService.createTodo(any()) } returns retrofit2.Response.success(createdDto)
        
        // Act: Создать задачу
        val resultId = repository.insertTodo(newTodo)
        
        // Assert: Проверить результат
        assertEquals(localId, resultId)
        
        // Проверить что задача была сохранена локально
        coVerify { mockDao.insertTodo(match { entity ->
            entity.title == "New Task" &&
            entity.isCompleted == false &&
            entity.priority.name == "NORMAL" &&
            entity.syncStatus == SyncStatus.PENDING
        }) }
        
        // Проверить что задача была отправлена на сервер при наличии сети
        coVerify { mockApiService.createTodo(match { dto ->
            dto.title == "New Task" &&
            dto.completed == false
        }) }
        
        // Проверить что статус обновился на SYNCED с remoteId
        coVerify { mockDao.updateRemoteId(localId, remoteId, SyncStatus.SYNCED) }
    }

    /**
     * Тест 2: READ - Чтение с приоритетом локальных данных
     * 
     * Ожидаемый результат:
     * - Данные всегда читаются из локальной БД (offline-first)
     * - Сетевой запрос не выполняется при обычном чтении
     */
    @Test
    fun `test READ operation uses local database first`() = runTest {
        // Arrange: Подготовить тестовые данные в DAO
        val testTodos = listOf(
            Todo(
                id = 1,
                title = "Local Task 1",
                isCompleted = false,
                priority = TodoPriority.HIGH,
                syncStatus = TodoSyncStatus.SYNCED,
                remoteId = "remote_1",
                createdAt = Date(),
                updatedAt = Date()
            ),
            Todo(
                id = 2,
                title = "Local Task 2",
                isCompleted = true,
                priority = TodoPriority.LOW,
                syncStatus = TodoSyncStatus.PENDING,
                remoteId = null,
                createdAt = Date(),
                updatedAt = Date()
            )
        )
        
        // Mock DAO возвращает тестовые данные
        val entities = testTodos.map { TodoMapper.mapDomainToEntity(it) }
        coEvery { mockDao.getAllTodos() } returns flowOf(entities)
        
        // Act: Получить все задачи
        val result = repository.getAllTodos().first()
        
        // Assert: Проверить что данные получены из локальной БД
        assertEquals(2, result.size)
        assertEquals("Local Task 1", result[0].title)
        assertEquals("Local Task 2", result[1].title)
        assertEquals(TodoPriority.HIGH, result[0].priority)
        assertEquals(TodoPriority.LOW, result[1].priority)
        
        // Проверить что API сервис НЕ вызывался (offline-first)
        coVerify(exactly = 0) { mockApiService.getAllTodos() }
        
        // Проверить что DAO вызывался для получения данных
        coVerify { mockDao.getAllTodos() }
    }

    /**
     * Тест 3: UPDATE - Обновление с отложенной синхронизацией
     * 
     * Ожидаемый результат:
     * - Изменения сохраняются локально
     * - Статус меняется на PENDING
     * - При появлении сети данные синхронизируются
     */
    @Test
    fun `test UPDATE operation with delayed sync`() = runTest {
        // Arrange: Подготовить существующую задачу для обновления
        val existingTodo = Todo(
            id = 1,
            title = "Original Task",
            isCompleted = false,
            priority = TodoPriority.NORMAL,
            syncStatus = TodoSyncStatus.SYNCED,
            remoteId = "remote_1",
            createdAt = Date(System.currentTimeMillis() - 10000),
            updatedAt = Date(System.currentTimeMillis() - 5000)
        )
        
        val updatedTodo = existingTodo.copy(
            title = "Updated Task",
            isCompleted = true,
            priority = TodoPriority.HIGH,
            syncStatus = TodoSyncStatus.PENDING, // Должен стать PENDING
            updatedAt = Date()
        )
        
        // Mock сети - нет интернета (отложенная синхронизация)
        every { mockNetworkManager.isNetworkAvailable() } returns false
        
        // Mock DAO - updateTodo работает
        coEvery { mockDao.updateTodo(any()) } just Runs
        
        // Act: Обновить задачу
        repository.updateTodo(updatedTodo)
        
        // Assert: Проверить что изменения сохранились локально
        coVerify { mockDao.updateTodo(match { entity ->
            entity.title == "Updated Task" &&
            entity.isCompleted == true &&
            entity.priority.name == "HIGH" &&
            entity.syncStatus == SyncStatus.PENDING && // Статус изменился на PENDING
            entity.remoteId == "remote_1" // remoteId сохранился
        }) }
        
        // Проверить что API сервис НЕ вызывался (нет сети)
        coVerify(exactly = 0) { mockApiService.updateTodo(any(), any()) }
    }

    /**
     * Тест 4: DELETE - Удаление с маркировкой
     * 
     * Ожидаемый результат:
     * - Синхронизированные задачи помечаются как __DELETED__
     * - Локальные задачи удаляются сразу
     * - После успешного удаления на сервере запись удаляется из БД
     */
    @Test
    fun `test DELETE operation with marking strategy`() = runTest {
        // Arrange: Подготовить синхронизированную задачу для удаления
        val syncedTodo = Todo(
            id = 1,
            title = "Synced Task",
            isCompleted = false,
            priority = TodoPriority.NORMAL,
            syncStatus = TodoSyncStatus.SYNCED,
            remoteId = "remote_1",
            createdAt = Date(),
            updatedAt = Date()
        )
        
        // Mock сети - есть интернет
        every { mockNetworkManager.isNetworkAvailable() } returns true
        
        // Mock DAO операции
        coEvery { mockDao.updateTodo(any()) } just Runs
        coEvery { mockDao.deleteTodo(any()) } just Runs
        coEvery { mockDao.deleteTodoById(any()) } just Runs
        
        // Mock API - успешное удаление на сервере  
        coEvery { mockApiService.deleteTodo("remote_1") } returns retrofit2.Response.success(null)
        
        // Act: Удалить синхронизированную задачу
        repository.deleteTodo(syncedTodo)
        
        // Assert: Проверить что задача помечается для удаления
        coVerify { mockDao.updateTodo(match { entity ->
            entity.id == 1L &&
            entity.syncStatus == SyncStatus.PENDING &&
            entity.description?.contains("__DELETED__") == true
        }) }
        
        // Проверить что задача удаляется на сервере
        coVerify { mockApiService.deleteTodo("remote_1") }
        
        // Проверить что после успешного удаления на сервере запись удаляется из БД
        coVerify { mockDao.deleteTodoById(1L) }
        
        // Arrange для второго случая: локальная задача (без remoteId)
        val localTodo = Todo(
            id = 2,
            title = "Local Task",
            isCompleted = false,
            priority = TodoPriority.NORMAL,
            syncStatus = TodoSyncStatus.PENDING,
            remoteId = null,
            createdAt = Date(),
            updatedAt = Date()
        )
        
        // Act: Удалить локальную задачу
        repository.deleteTodo(localTodo)
        
        // Assert: Проверить что локальная задача удаляется сразу (без маркировки)
        coVerify { mockDao.deleteTodo(match { entity ->
            entity.id == 2L
        }) }
        
        // Проверить что API НЕ вызывается для локальной задачи
        coVerify(exactly = 1) { mockApiService.deleteTodo(any()) } // Только для первой задачи
    }

    /**
     * Тест 5: Конфликт при синхронизации
     * 
     * Ожидаемый результат:
     * - При конфликте версий применяется стратегия KEEP_LATEST
     * - Более новая версия (по updatedAt) сохраняется
     */
    @Test
    @Ignore("TODO: Реализовать позже - сложная настройка моков")
    fun `test sync conflict resolution keeps latest version`() = runTest {
        // Arrange: Подготовить локальную и удаленную версии с разными временами обновления
        val baseTime = System.currentTimeMillis()
        
        // Локальная версия (более старая)
        val localTodo = Todo(
            id = 1,
            title = "Local Version",
            isCompleted = false,
            priority = TodoPriority.NORMAL,
            syncStatus = TodoSyncStatus.PENDING,
            remoteId = "remote_1",
            createdAt = Date(baseTime - 10000),
            updatedAt = Date(baseTime - 5000) // Старее
        )
        
        // Удаленная версия (более новая)
        val remoteDto = TodoDto(
            id = "remote_1",
            title = "Remote Version",
            completed = true,
            priority = "HIGH",
            userId = 1,
            createdAt = baseTime - 10000,
            updatedAt = baseTime // Новее
        )
        
        // Mock сети - есть интернет
        every { mockNetworkManager.isNetworkAvailable() } returns true
        
        // Mock DAO операции
        coEvery { mockDao.updateTodo(any()) } just Runs
        
        // Mock API - возвращает обновленную версию с сервера
        coEvery { mockApiService.getUpdatedTodos(any()) } returns retrofit2.Response.success(listOf(remoteDto))
        coEvery { mockApiService.syncTodos(any()) } returns retrofit2.Response.success(emptyList())
        
        // Act: Синхронизировать с сервером
        val result = repository.syncWithRemote()
        
        // Assert: Проверить что синхронизация успешна
        assertTrue(result.isSuccess)
        
        // Проверить что сохранилась более новая (удаленная) версия
        coVerify { mockDao.updateTodo(match { entity ->
            entity.title == "Remote Version" && // Заголовок из удаленной версии
            entity.isCompleted == true && // Состояние из удаленной версии
            entity.priority.name == "HIGH" && // Приоритет из удаленной версии
            entity.syncStatus == SyncStatus.SYNCED && // Статус стал SYNCED
            entity.remoteId == "remote_1"
        }) }
        
        // Проверить что API был вызван для получения обновлений
        coVerify { mockApiService.getUpdatedTodos(any()) }
    }

    /**
     * Тест 6: Батчевая синхронизация
     * 
     * Ожидаемый результат:
     * - Несколько PENDING задач отправляются одним запросом
     * - После успешной синхронизации все получают статус SYNCED
     */
    @Test
    @Ignore("TODO: Реализовать позже - сложная настройка моков")
    fun `test batch sync sends multiple pending todos`() = runTest {
        // Arrange: Подготовить несколько PENDING задач
        val pendingTodos = listOf(
            Todo(id = 1, title = "Pending 1", isCompleted = false, priority = TodoPriority.NORMAL, 
                 syncStatus = TodoSyncStatus.PENDING, remoteId = null, createdAt = Date(), updatedAt = Date()),
            Todo(id = 2, title = "Pending 2", isCompleted = true, priority = TodoPriority.HIGH, 
                 syncStatus = TodoSyncStatus.PENDING, remoteId = null, createdAt = Date(), updatedAt = Date()),
            Todo(id = 3, title = "Pending 3", isCompleted = false, priority = TodoPriority.LOW, 
                 syncStatus = TodoSyncStatus.PENDING, remoteId = null, createdAt = Date(), updatedAt = Date())
        )
        
        val syncedDtos = listOf(
            TodoDto(id = "r1", title = "Pending 1", completed = false, userId = 1, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()),
            TodoDto(id = "r2", title = "Pending 2", completed = true, userId = 1, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()),
            TodoDto(id = "r3", title = "Pending 3", completed = false, userId = 1, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
        )
        
        // Mock сети - есть интернет
        every { mockNetworkManager.isNetworkAvailable() } returns true
        
        // Mock DAO - возвращает PENDING задачи
        coEvery { mockDao.getTodosBySyncStatus(SyncStatus.PENDING) } returns pendingTodos.map { TodoMapper.mapDomainToEntity(it) }
        coEvery { mockDao.updateTodo(any()) } just Runs
        
        // Mock API - успешная батчевая синхронизация
        coEvery { mockApiService.syncTodos(any()) } returns retrofit2.Response.success(syncedDtos)
        coEvery { mockApiService.getUpdatedTodos(any()) } returns retrofit2.Response.success(emptyList())
        
        // Act: Синхронизировать с сервером
        val result = repository.syncWithRemote()
        
        // Assert: Проверить что синхронизация успешна
        assertTrue(result.isSuccess)
        
        // Проверить что отправлены PENDING задачи
        coVerify { mockApiService.syncTodos(match { dtos ->
            dtos.size == 3 && 
            dtos.any { it.title == "Pending 1" } &&
            dtos.any { it.title == "Pending 2" } &&
            dtos.any { it.title == "Pending 3" }
        }) }
        
        // Проверить что все задачи получили статус SYNCED
        coVerify(exactly = 3) { mockDao.updateTodo(match { entity ->
            entity.syncStatus == SyncStatus.SYNCED &&
            entity.remoteId != null
        }) }
    }

    /**
     * Тест 7: Offline режим
     * 
     * Ожидаемый результат:
     * - Все операции работают без интернета
     * - Изменения накапливаются со статусом PENDING
     * - При появлении сети все синхронизируется
     */
    @Test
    fun `test offline mode accumulates changes`() = runTest {
        // Arrange: Нет интернета
        every { mockNetworkManager.isNetworkAvailable() } returns false
        
        val newTodo = Todo(id = 0, title = "Offline Task", isCompleted = false, priority = TodoPriority.NORMAL, 
                          syncStatus = TodoSyncStatus.PENDING, remoteId = null, createdAt = Date(), updatedAt = Date())
        
        // Mock DAO операции
        coEvery { mockDao.insertTodo(any()) } returns 1L
        coEvery { mockDao.updateTodo(any()) } just Runs
        
        // Act: Выполнить операции без интернета
        val insertId = repository.insertTodo(newTodo)
        repository.updateTodo(newTodo.copy(id = insertId, title = "Updated Offline"))
        
        // Assert: Проверить что изменения накапливаются локально
        assertEquals(1L, insertId)
        
        // Проверить что задачи сохраняются со статусом PENDING
        coVerify { mockDao.insertTodo(match { entity ->
            entity.title == "Offline Task" &&
            entity.syncStatus == SyncStatus.PENDING
        }) }
        
        coVerify { mockDao.updateTodo(match { entity ->
            entity.title == "Updated Offline" &&
            entity.syncStatus == SyncStatus.PENDING
        }) }
        
        // Проверить что API НЕ вызывался (нет сети)
        coVerify(exactly = 0) { mockApiService.createTodo(any()) }
        coVerify(exactly = 0) { mockApiService.updateTodo(any(), any()) }
    }

    /**
     * Тест 8: Каскадное удаление
     * 
     * Ожидаемый результат:
     * - deleteAllTodos удаляет все задачи локально
     * - deleteCompletedTodos удаляет только выполненные
     */
    @Test
    fun `test cascade delete operations`() = runTest {
        // Arrange: Mock DAO операции удаления
        coEvery { mockDao.deleteCompletedTodos() } just Runs
        coEvery { mockDao.deleteAllTodos() } just Runs
        
        // Act: Выполнить каскадное удаление
        repository.deleteCompletedTodos()
        repository.deleteAllTodos()
        
        // Assert: Проверить что вызваны правильные методы DAO
        coVerify { mockDao.deleteCompletedTodos() }
        coVerify { mockDao.deleteAllTodos() }
    }

    /**
     * Тест 9: Восстановление после сбоя
     * 
     * Ожидаемый результат:
     * - При сбое сети во время синхронизации данные не теряются
     * - Задачи остаются в статусе PENDING
     * - Повторная синхронизация завершается успешно
     */
    @Test
    @Ignore("TODO: Реализовать позже - сложная настройка моков")
    fun `test recovery after sync failure`() = runTest {
        // Arrange: Подготовить PENDING задачу
        val pendingTodo = Todo(id = 1, title = "Failed Sync", isCompleted = false, priority = TodoPriority.NORMAL,
                              syncStatus = TodoSyncStatus.PENDING, remoteId = null, createdAt = Date(), updatedAt = Date())
        
        // Mock сети - есть интернет, но API падает
        every { mockNetworkManager.isNetworkAvailable() } returns true
        
        // Mock DAO
        coEvery { mockDao.getTodosBySyncStatus(SyncStatus.PENDING) } returns listOf(TodoMapper.mapDomainToEntity(pendingTodo))
        coEvery { mockDao.updateTodo(any()) } just Runs
        
        // Mock API - первый раз ошибка, второй раз успех
        coEvery { mockApiService.syncTodos(any()) } returns retrofit2.Response.error(500, "Server Error".toResponseBody("text/plain".toMediaType()))
        coEvery { mockApiService.getUpdatedTodos(any()) } returns retrofit2.Response.success(emptyList())
        
        // Act: Первая попытка синхронизации (неудачная)
        val firstResult = repository.syncWithRemote()
        
        // Assert: Проверить что синхронизация не удалась, но данные не потеряны
        assertTrue(firstResult.isFailure)
        
        // Проверить что задача осталась в статусе PENDING (не изменилась)
        coVerify(exactly = 0) { mockDao.updateTodo(match { entity ->
            entity.syncStatus == SyncStatus.SYNCED
        }) }
        
        // Arrange для второй попытки: API теперь работает
        val successDto = TodoDto(id = "r1", title = "Failed Sync", completed = false, userId = 1, 
                                createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
        coEvery { mockApiService.syncTodos(any()) } returns retrofit2.Response.success(listOf(successDto))
        
        // Act: Повторная синхронизация (успешная)
        val secondResult = repository.syncWithRemote()
        
        // Assert: Проверить что повторная синхронизация успешна
        assertTrue(secondResult.isSuccess)
        
        // Проверить что задача получила статус SYNCED
        coVerify { mockDao.updateTodo(match { entity ->
            entity.syncStatus == SyncStatus.SYNCED &&
            entity.remoteId == "r1"
        }) }
    }

    /**
     * Тест 10: Инкрементальная синхронизация
     * 
     * Ожидаемый результат:
     * - Загружаются только изменения с последней синхронизации
     * - Используется timestamp для фильтрации
     */
    @Test
    @Ignore("TODO: Реализовать позже - сложная настройка моков")
    fun `test incremental sync downloads only recent changes`() = runTest {
        // Arrange: Подготовить timestamp последней синхронизации
        val lastSyncTime = System.currentTimeMillis() - 60000 // Минута назад
        val recentChanges = listOf(
            TodoDto(id = "r1", title = "Recent Change 1", completed = false, userId = 1, 
                   createdAt = lastSyncTime + 30000, updatedAt = lastSyncTime + 30000),
            TodoDto(id = "r2", title = "Recent Change 2", completed = true, userId = 1, 
                   createdAt = lastSyncTime + 45000, updatedAt = lastSyncTime + 45000)
        )
        
        // Mock сети - есть интернет
        every { mockNetworkManager.isNetworkAvailable() } returns true
        
        // Mock DAO операции
        coEvery { mockDao.getTodosBySyncStatus(SyncStatus.PENDING) } returns emptyList()
        coEvery { mockDao.insertTodo(any()) } returns 1L
        coEvery { mockDao.updateTodo(any()) } just Runs
        // Нет метода getTodoByRemoteId в DAO - убираем
        
        // Mock SharedPreferences для хранения времени последней синхронизации
        // В реальном репозитории используется для фильтрации изменений
        
        // Mock API - возвращает только недавние изменения
        coEvery { mockApiService.getUpdatedTodos(match { timestamp -> 
            // Проверяем что передается правильный timestamp для фильтрации
            timestamp >= lastSyncTime 
        }) } returns retrofit2.Response.success(recentChanges)
        coEvery { mockApiService.syncTodos(any()) } returns retrofit2.Response.success(emptyList())
        
        // Act: Синхронизировать с сервером
        val result = repository.syncWithRemote()
        
        // Assert: Проверить что синхронизация успешна
        assertTrue(result.isSuccess)
        
        // Проверить что API вызван с фильтрацией по времени
        coVerify { mockApiService.getUpdatedTodos(any()) }
        
        // Проверить что загружены только недавние изменения
        coVerify(exactly = 2) { mockDao.insertTodo(match { entity ->
            entity.syncStatus == SyncStatus.SYNCED &&
            (entity.title == "Recent Change 1" || entity.title == "Recent Change 2")
        }) }
    }
}