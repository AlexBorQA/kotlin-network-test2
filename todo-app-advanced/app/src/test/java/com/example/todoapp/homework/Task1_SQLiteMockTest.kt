package com.example.todoapp.homework

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapp.data.database.TodoDatabase
import com.example.todoapp.data.database.dao.TodoDao
import com.example.todoapp.data.database.entity.TodoEntity
import com.example.todoapp.data.database.entity.Priority
import com.example.todoapp.data.database.entity.SyncStatus
import com.example.todoapp.data.api.TodoApiService
import com.example.todoapp.data.network.NetworkManager
import com.example.todoapp.data.repository.CachedTodoRepository
import com.example.todoapp.core.idling.IdlingResourceProvider
import com.example.todoapp.domain.model.Todo
import com.example.todoapp.domain.model.TodoPriority
import com.example.todoapp.domain.model.TodoSyncStatus
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Домашнее задание - Часть 1: Тестирование SQLite с in-memory базой данных
 * 
 * Цель: Научиться тестировать работу со списком задач используя in-memory Room DB
 * для быстрых и изолированных тестов без файловой системы.
 * 
 * ВАЖНО: В этом задании используется настоящая in-memory база данных Room,
 * что позволяет тестировать реальную логику работы с БД без использования файлов.
 */
@RunWith(AndroidJUnit4::class)
// @Ignore("Убрать после реализации тестов из ДЗ")
class Task1_SQLiteMockTest {

    private lateinit var database: TodoDatabase
    private lateinit var todoDao: TodoDao
    private lateinit var mockApiService: TodoApiService
    private lateinit var mockNetworkManager: NetworkManager
    private lateinit var mockIdlingResource: IdlingResourceProvider
    private lateinit var repository: CachedTodoRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        // Получить контекст для тестов через ApplicationProvider
        context = ApplicationProvider.getApplicationContext()
        
        // Создать in-memory базу данных Room через Room.inMemoryDatabaseBuilder()
        database = Room.inMemoryDatabaseBuilder(
            context,
            TodoDatabase::class.java
        ).build()
        
        // Получить DAO из созданной базы данных
        todoDao = database.todoDao()
        
        // Создать mock объекты для сетевых компонентов
        mockApiService = mockk(relaxed = true)
        mockNetworkManager = mockk(relaxed = true)
        mockIdlingResource = mockk(relaxed = true)
        
        // Создать репозиторий с реальным DAO и мок-сервисами
        repository = CachedTodoRepository(
            todoDao = todoDao,
            apiService = mockApiService,
            networkManager = mockNetworkManager,
            idlingResource = mockIdlingResource
        )
    }
    
    @After
    fun tearDown() {
        // Закрыть базу данных после каждого теста для освобождения ресурсов
        database.close()
    }

    /**
     * Тест 1: Получение всех задач из in-memory БД
     * 
     * Ожидаемый результат:
     * - Вставить 3 задачи в реальную in-memory БД через DAO
     * - Вызвать repository.getAllTodos()
     * - Проверить что все 3 задачи присутствуют в результате
     * - Проверить корректность преобразования Entity в Domain модели
     */
    @Test
    fun `test getAllTodos returns all tasks from database`() = runTest {
        // Arrange: Вставить 3 тестовые задачи в in-memory БД
        val task1 = TodoEntity(
            id = 1L,
            title = "Task 1",
            isCompleted = false,
            priority = Priority.NORMAL,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.SYNCED,
            remoteId = "remote1"
        )
        val task2 = TodoEntity(
            id = 2L,
            title = "Task 2",
            isCompleted = true,
            priority = Priority.HIGH,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.SYNCED,
            remoteId = "remote2"
        )
        val task3 = TodoEntity(
            id = 3L,
            title = "Task 3",
            isCompleted = false,
            priority = Priority.LOW,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.PENDING,
            remoteId = null
        )
        
        todoDao.insertTodo(task1)
        todoDao.insertTodo(task2)
        todoDao.insertTodo(task3)
        
        // Act: Получить все задачи через репозиторий
        val result = repository.getAllTodos().first()
        
        // Assert: Проверить что получили все 3 задачи
        assertEquals(3, result.size)
        
        // Проверить корректность преобразования Entity в Domain модели
        val domainTask1 = result.find { it.id == 1L }!!
        assertEquals("Task 1", domainTask1.title)
        assertEquals(false, domainTask1.isCompleted)
        assertEquals(TodoPriority.NORMAL, domainTask1.priority)
        assertEquals(TodoSyncStatus.SYNCED, domainTask1.syncStatus)
        
        val domainTask2 = result.find { it.id == 2L }!!
        assertEquals("Task 2", domainTask2.title)
        assertEquals(true, domainTask2.isCompleted)
        assertEquals(TodoPriority.HIGH, domainTask2.priority)
        
        val domainTask3 = result.find { it.id == 3L }!!
        assertEquals("Task 3", domainTask3.title)
        assertEquals(TodoSyncStatus.PENDING, domainTask3.syncStatus)
    }

    /**
     * Тест 2: Создание новой задачи без интернета
     * 
     * Ожидаемый результат:
     * - При отсутствии сети задача сохраняется локально
     * - Статус синхронизации = PENDING
     * - API не вызывается
     */
    @Test
    fun `test insertTodo saves locally when no network`() = runTest {
        // Arrange: Настроить отсутствие сети
        every { mockNetworkManager.isNetworkAvailable() } returns false
        
        val newTodo = Todo(
            id = 0L, // 0 для новой задачи
            title = "New offline task",
            isCompleted = false,
            priority = TodoPriority.NORMAL,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = TodoSyncStatus.SYNCED, // Будет изменен на PENDING
            remoteId = null
        )
        
        // Act: Создать задачу без сети
        repository.insertTodo(newTodo)
        
        // Assert: Проверить что задача сохранена локально
        val allTodos = repository.getAllTodos().first()
        assertEquals(1, allTodos.size)
        
        val savedTodo = allTodos.first()
        assertEquals("New offline task", savedTodo.title)
        assertEquals(false, savedTodo.isCompleted)
        assertEquals(TodoPriority.NORMAL, savedTodo.priority)
        assertEquals(TodoSyncStatus.PENDING, savedTodo.syncStatus) // Должен быть PENDING
        assertNull(savedTodo.remoteId) // Нет remoteId при offline
        
        // Проверить что API не вызывался
        coVerify(exactly = 0) { mockApiService.createTodo(any()) }
    }

    /**
     * Тест 3: Поиск задач по ключевому слову
     * 
     * Ожидаемый результат:
     * - Mock DAO возвращает задачи содержащие поисковый запрос
     * - Поиск регистронезависимый
     * - Возвращаются только подходящие задачи
     */
    @Test
    fun `test searchTodos returns filtered tasks`() = runTest {
        // Arrange: Создать тестовые задачи с разными названиями
        val task1 = TodoEntity(
            id = 1L,
            title = "Buy groceries",
            isCompleted = false,
            priority = Priority.NORMAL,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.SYNCED,
            remoteId = "remote1"
        )
        val task2 = TodoEntity(
            id = 2L,
            title = "Write KOTLIN code",
            isCompleted = false,
            priority = Priority.HIGH,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.SYNCED,
            remoteId = "remote2"
        )
        val task3 = TodoEntity(
            id = 3L,
            title = "Call mom",
            isCompleted = true,
            priority = Priority.LOW,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.SYNCED,
            remoteId = "remote3"
        )
        
        todoDao.insertTodo(task1)
        todoDao.insertTodo(task2)
        todoDao.insertTodo(task3)
        
        // Act: Поиск по ключевому слову (регистронезависимый)
        val searchResults = repository.searchTodos("kotlin").first()
        
        // Assert: Проверить что найдена только одна задача содержащая "kotlin"
        assertEquals(1, searchResults.size)
        assertEquals("Write KOTLIN code", searchResults.first().title)
        assertEquals(2L, searchResults.first().id)
        
        // Тест поиска другого слова
        val groceryResults = repository.searchTodos("Buy").first()
        assertEquals(1, groceryResults.size)
        assertEquals("Buy groceries", groceryResults.first().title)
        
        // Тест поиска несуществующего слова
        val emptyResults = repository.searchTodos("nonexistent").first()
        assertEquals(0, emptyResults.size)
    }

    /**
     * Тест 4: Обновление статуса задачи (выполнено/не выполнено)
     * 
     * Ожидаемый результат:
     * - При вызове toggleCompletion статус меняется
     * - Обновляется время изменения
     * - Статус синхронизации меняется на PENDING
     */
    @Test
    fun `test toggleCompletion changes task status`() = runTest {
        // Arrange: Создать задачу в состоянии "не выполнено"
        val originalTask = TodoEntity(
            id = 1L,
            title = "Test task",
            isCompleted = false,
            priority = Priority.NORMAL,
            createdAt = Date(),
            updatedAt = Date(System.currentTimeMillis() - 60000), // Час назад
            syncStatus = SyncStatus.SYNCED,
            remoteId = "remote1"
        )
        
        todoDao.insertTodo(originalTask)
        
        // Настроить отсутствие сети для проверки что статус станет PENDING
        every { mockNetworkManager.isNetworkAvailable() } returns false
        
        // Act: Переключить статус выполнения
        repository.toggleCompletion(1L)
        
        // Assert: Проверить что статус изменился
        val updatedTodos = repository.getAllTodos().first()
        assertEquals(1, updatedTodos.size)
        
        val updatedTodo = updatedTodos.first()
        assertEquals(true, updatedTodo.isCompleted) // Статус изменился на выполнено
        assertEquals(TodoSyncStatus.PENDING, updatedTodo.syncStatus) // Стал PENDING
        assertTrue("Время обновления должно быть позже", 
            updatedTodo.updatedAt.time > originalTask.updatedAt.time)
        
        // Проверить что API не вызывался (нет сети)
        coVerify(exactly = 0) { mockApiService.updateTodo(any(), any()) }
        
        // Act: Переключить статус обратно
        repository.toggleCompletion(1L)
        
        // Assert: Проверить что статус снова изменился
        val reUpdatedTodos = repository.getAllTodos().first()
        val reUpdatedTodo = reUpdatedTodos.first()
        assertEquals(false, reUpdatedTodo.isCompleted) // Статус снова стал "не выполнено"
    }

    /**
     * Тест 5: Получение статистики (количество активных/выполненных задач)
     * 
     * Ожидаемый результат:
     * - getActiveCount возвращает правильное количество активных задач
     * - getCompletedCount возвращает правильное количество выполненных задач
     */
    @Test
    fun `test getStatistics returns correct counts`() = runTest {
        // Arrange: Создать смешанный набор задач
        val activeTask1 = TodoEntity(
            id = 1L,
            title = "Active task 1",
            isCompleted = false,
            priority = Priority.NORMAL,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.SYNCED,
            remoteId = "remote1"
        )
        val activeTask2 = TodoEntity(
            id = 2L,
            title = "Active task 2",
            isCompleted = false,
            priority = Priority.HIGH,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.PENDING,
            remoteId = null
        )
        val completedTask1 = TodoEntity(
            id = 3L,
            title = "Completed task 1",
            isCompleted = true,
            priority = Priority.LOW,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.SYNCED,
            remoteId = "remote3"
        )
        val completedTask2 = TodoEntity(
            id = 4L,
            title = "Completed task 2",
            isCompleted = true,
            priority = Priority.NORMAL,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.SYNCED,
            remoteId = "remote4"
        )
        val completedTask3 = TodoEntity(
            id = 5L,
            title = "Completed task 3",
            isCompleted = true,
            priority = Priority.HIGH,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.PENDING,
            remoteId = null
        )
        
        // Добавить все задачи в БД
        todoDao.insertTodo(activeTask1)
        todoDao.insertTodo(activeTask2)
        todoDao.insertTodo(completedTask1)
        todoDao.insertTodo(completedTask2)
        todoDao.insertTodo(completedTask3)
        
        // Act: Получить статистику
        val activeCount = repository.getActiveCount().first()
        val completedCount = repository.getCompletedCount().first()
        
        // Assert: Проверить правильность подсчетов
        assertEquals(2, activeCount) // 2 активные задачи
        assertEquals(3, completedCount) // 3 выполненные задачи
        
        // Проверить общее количество
        val allTodos = repository.getAllTodos().first()
        assertEquals(5, allTodos.size)
        assertEquals(activeCount + completedCount, allTodos.size)
    }

    /**
     * Тест 6: Удаление выполненных задач
     * 
     * Ожидаемый результат:
     * - deleteCompletedTodos удаляет только выполненные задачи
     * - Активные задачи остаются в БД
     */
    @Test
    fun `test deleteCompletedTodos removes only completed tasks`() = runTest {
        // Arrange: Создать смешанный набор задач
        val activeTask1 = TodoEntity(
            id = 1L,
            title = "Active task 1",
            isCompleted = false,
            priority = Priority.NORMAL,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.SYNCED,
            remoteId = "remote1"
        )
        val activeTask2 = TodoEntity(
            id = 2L,
            title = "Active task 2",
            isCompleted = false,
            priority = Priority.HIGH,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.PENDING,
            remoteId = null
        )
        val completedTask1 = TodoEntity(
            id = 3L,
            title = "Completed task 1",
            isCompleted = true,
            priority = Priority.LOW,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.SYNCED,
            remoteId = "remote3"
        )
        val completedTask2 = TodoEntity(
            id = 4L,
            title = "Completed task 2",
            isCompleted = true,
            priority = Priority.NORMAL,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.SYNCED,
            remoteId = "remote4"
        )
        
        // Добавить все задачи в БД
        todoDao.insertTodo(activeTask1)
        todoDao.insertTodo(activeTask2)
        todoDao.insertTodo(completedTask1)
        todoDao.insertTodo(completedTask2)
        
        // Проверить начальное состояние
        val initialTodos = repository.getAllTodos().first()
        assertEquals(4, initialTodos.size)
        
        // Act: Удалить выполненные задачи
        repository.deleteCompletedTodos()
        
        // Assert: Проверить что остались только активные задачи
        val remainingTodos = repository.getAllTodos().first()
        assertEquals(2, remainingTodos.size)
        
        // Проверить что остались правильные задачи
        val remainingTitles = remainingTodos.map { it.title }.toSet()
        assertTrue(remainingTitles.contains("Active task 1"))
        assertTrue(remainingTitles.contains("Active task 2"))
        assertFalse(remainingTitles.contains("Completed task 1"))
        assertFalse(remainingTitles.contains("Completed task 2"))
        
        // Проверить что все оставшиеся задачи активные
        remainingTodos.forEach { todo ->
            assertEquals(false, todo.isCompleted)
        }
        
        // Проверить статистику после удаления
        val activeCount = repository.getActiveCount().first()
        val completedCount = repository.getCompletedCount().first()
        assertEquals(2, activeCount)
        assertEquals(0, completedCount)
    }

    /**
     * Тест 7: Фильтрация по приоритету
     * 
     * Ожидаемый результат:
     * - getTodosByPriority возвращает только задачи с указанным приоритетом
     */
    @Test
    fun `test getTodosByPriority returns filtered tasks`() = runTest {
        // Arrange: Создать задачи с разными приоритетами
        val highPriorityTask1 = TodoEntity(
            id = 1L,
            title = "High priority task 1",
            isCompleted = false,
            priority = Priority.HIGH,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.SYNCED,
            remoteId = "remote1"
        )
        val highPriorityTask2 = TodoEntity(
            id = 2L,
            title = "High priority task 2",
            isCompleted = true,
            priority = Priority.HIGH,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.PENDING,
            remoteId = null
        )
        val normalPriorityTask = TodoEntity(
            id = 3L,
            title = "Normal priority task",
            isCompleted = false,
            priority = Priority.NORMAL,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.SYNCED,
            remoteId = "remote3"
        )
        val lowPriorityTask = TodoEntity(
            id = 4L,
            title = "Low priority task",
            isCompleted = true,
            priority = Priority.LOW,
            createdAt = Date(),
            updatedAt = Date(),
            syncStatus = SyncStatus.SYNCED,
            remoteId = "remote4"
        )
        
        // Добавить все задачи в БД
        todoDao.insertTodo(highPriorityTask1)
        todoDao.insertTodo(highPriorityTask2)
        todoDao.insertTodo(normalPriorityTask)
        todoDao.insertTodo(lowPriorityTask)
        
        // Act & Assert: Тестировать фильтрацию по каждому приоритету
        
        // Тест HIGH приоритета
        val highPriorityTodos = repository.getTodosByPriority(TodoPriority.HIGH).first()
        assertEquals(2, highPriorityTodos.size)
        highPriorityTodos.forEach { todo ->
            assertEquals(TodoPriority.HIGH, todo.priority)
        }
        val highTitles = highPriorityTodos.map { it.title }.toSet()
        assertTrue(highTitles.contains("High priority task 1"))
        assertTrue(highTitles.contains("High priority task 2"))
        
        // Тест NORMAL приоритета
        val normalPriorityTodos = repository.getTodosByPriority(TodoPriority.NORMAL).first()
        assertEquals(1, normalPriorityTodos.size)
        assertEquals("Normal priority task", normalPriorityTodos.first().title)
        assertEquals(TodoPriority.NORMAL, normalPriorityTodos.first().priority)
        
        // Тест LOW приоритета
        val lowPriorityTodos = repository.getTodosByPriority(TodoPriority.LOW).first()
        assertEquals(1, lowPriorityTodos.size)
        assertEquals("Low priority task", lowPriorityTodos.first().title)
        assertEquals(TodoPriority.LOW, lowPriorityTodos.first().priority)
        
        // Проверить что общее количество задач правильное
        val totalHighNormalLow = highPriorityTodos.size + normalPriorityTodos.size + lowPriorityTodos.size
        val allTodos = repository.getAllTodos().first()
        assertEquals(totalHighNormalLow, allTodos.size)
    }
}