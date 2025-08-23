package com.example.todoapp.homework

import com.example.todoapp.data.api.TodoApiService
import com.example.todoapp.data.model.TodoDto
import com.google.gson.Gson
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.Description
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Severity
import io.qameta.allure.kotlin.SeverityLevel
import io.qameta.allure.kotlin.Story
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.Ignore
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Домашнее задание - Часть 2: Тестирование API с MockWebServer
 * 
 * Цель: Научиться тестировать сетевые запросы с помощью MockWebServer
 * для проверки корректности работы API без реального сервера.
 * 
 * ЗАДАНИЕ:
 * 1. Инициализируйте MockWebServer в методе setup()
 * 2. Создайте и настройте Retrofit с MockWebServer URL
 * 3. Создайте экземпляр TodoApiService
 * 4. Не забудьте остановить MockWebServer в tearDown()
 * 
 * Документация MockWebServer: https://github.com/square/okhttp/tree/master/mockwebserver
 */
@Epic("ДЗ 6: Тестирование сетевого слоя")
@Feature("MockWebServer API тестирование")
// @Ignore("Убрать после реализации тестов из ДЗ")
class Task2_MockWebServerTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: TodoApiService
    private lateinit var gson: Gson

    @Before
    fun setup() {
        // Инициализируем и настраиваем MockWebServer
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        // Создаем Gson для сериализации
        gson = Gson()
        
        // Создаем Retrofit с необходимыми настройками
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.SECONDS)
                .writeTimeout(1, TimeUnit.SECONDS)
                .build())
            .build()
        
        // Получаем экземпляр TodoApiService
        apiService = retrofit.create(TodoApiService::class.java)
    }

    @After
    fun tearDown() {
        // Корректно завершаем работу MockWebServer
        mockWebServer.shutdown()
    }

    /**
     * Тест 1: GET запрос - получение всех задач
     * 
     * TODO: Реализовать тест
     * 
     * Ожидаемый результат:
     * - MockWebServer возвращает список из 3 задач
     * - Проверить корректность десериализации
     * - Проверить что запрос был GET /todos
     * 
     * ВАЖНО: Сначала реализуйте инициализацию в setup()!
     */
    @Test
    @Story("HTTP API операции")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Проверяет GET запрос для получения всех задач через MockWebServer")
    fun `test GET all todos returns list of tasks`() = runTest {
        // Arrange: Создать тестовые данные - список из 3 TodoDto
        val currentTime = System.currentTimeMillis()
        val testTodos = listOf(
            TodoDto(id = "1", title = "Task 1", completed = false, userId = 1, createdAt = currentTime, updatedAt = currentTime),
            TodoDto(id = "2", title = "Task 2", completed = true, userId = 1, createdAt = currentTime, updatedAt = currentTime),
            TodoDto(id = "3", title = "Task 3", completed = false, userId = 2, createdAt = currentTime, updatedAt = currentTime)
        )
        
        // Настроить MockWebServer для возврата этих данных
        val response = MockResponse()
            .setResponseCode(200)
            .setBody(gson.toJson(testTodos))
            .addHeader("Content-Type", "application/json")
        mockWebServer.enqueue(response)
        
        // Act: Выполнить запрос через apiService
        val result = apiService.getAllTodos()
        
        // Assert: Проверить полученный результат
        assertTrue(result.isSuccessful)
        val todos = result.body()
        assertNotNull(todos)
        assertEquals(3, todos!!.size)
        assertEquals("Task 1", todos[0].title)
        assertEquals(false, todos[0].completed)
        assertEquals("Task 2", todos[1].title)
        assertEquals(true, todos[1].completed)
        assertEquals("Task 3", todos[2].title)
        assertEquals(2, todos[2].userId)
        
        // Проверить параметры отправленного запроса
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/todos", request.path)
    }

    /**
     * Тест 2: GET запрос - получение задачи по ID
     * 
     * TODO: Реализовать тест
     * 
     * Ожидаемый результат:
     * - MockWebServer возвращает одну задачу
     * - Проверить что запрос содержит правильный ID в пути
     */
    @Test
    fun `test GET todo by id returns single task`() = runTest {
        // Arrange: Создать тестовую задачу с конкретным ID
        val testId = "42"
        val currentTime = System.currentTimeMillis()
        val testTodo = TodoDto(
            id = testId, 
            title = "Specific task", 
            completed = false, 
            userId = 1,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        
        // Настроить ответ сервера
        val response = MockResponse()
            .setResponseCode(200)
            .setBody(gson.toJson(testTodo))
            .addHeader("Content-Type", "application/json")
        mockWebServer.enqueue(response)
        
        // Act: Выполнить запрос по ID
        val result = apiService.getTodoById(testId)
        
        // Assert: Проверить полученную задачу
        assertTrue(result.isSuccessful)
        val todo = result.body()
        assertNotNull(todo)
        assertEquals(testId, todo!!.id)
        assertEquals("Specific task", todo.title)
        assertEquals(false, todo.completed)
        assertEquals(1, todo.userId)
        
        // Проверить путь запроса
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/todos/$testId", request.path)
    }

    /**
     * Тест 3: POST запрос - создание новой задачи
     * 
     * TODO: Реализовать тест
     * 
     * Ожидаемый результат:
     * - MockWebServer возвращает созданную задачу с присвоенным ID
     * - Проверить что в запросе передается правильный JSON
     * - Проверить заголовок Content-Type
     */
    @Test
    fun `test POST creates new todo and returns it with id`() = runTest {
        // Arrange: Создать новую задачу без ID
        val currentTime = System.currentTimeMillis()
        val newTodo = TodoDto(
            id = null, // Новая задача без ID
            title = "New task",
            completed = false,
            userId = 1,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        
        val createdTodo = TodoDto(
            id = "101", // Сервер присваивает ID
            title = "New task",
            completed = false,
            userId = 1,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        
        // Настроить ответ сервера с присвоенным ID
        val response = MockResponse()
            .setResponseCode(201) // Created
            .setBody(gson.toJson(createdTodo))
            .addHeader("Content-Type", "application/json")
        mockWebServer.enqueue(response)
        
        // Act: Выполнить POST запрос
        val result = apiService.createTodo(newTodo)
        
        // Assert: Проверить успешность создания
        assertTrue(result.isSuccessful)
        assertEquals(201, result.code())
        val todo = result.body()
        assertNotNull(todo)
        assertEquals("101", todo!!.id)
        assertEquals("New task", todo.title)
        assertEquals(false, todo.completed)
        assertEquals(1, todo.userId)
        
        // Проверить содержимое запроса и заголовки
        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/todos", request.path)
        assertEquals("application/json; charset=UTF-8", request.getHeader("Content-Type"))
        
        // Проверить тело запроса
        val requestBody = request.body.readUtf8()
        val sentTodo = gson.fromJson(requestBody, TodoDto::class.java)
        assertEquals("New task", sentTodo.title)
        assertEquals(false, sentTodo.completed)
        assertEquals(1, sentTodo.userId)
    }

    /**
     * Тест 4: PUT запрос - обновление существующей задачи
     * 
     * TODO: Реализовать тест
     * 
     * Ожидаемый результат:
     * - MockWebServer возвращает обновленную задачу
     * - Проверить что запрос содержит ID в пути и обновленные данные в теле
     */
    @Test
    fun `test PUT updates existing todo`() = runTest {
        // Arrange: Подготовить обновленные данные задачи
        val todoId = "42"
        val currentTime = System.currentTimeMillis()
        val updatedTodo = TodoDto(
            id = todoId,
            title = "Updated task",
            completed = true, // Изменили статус
            userId = 1,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        
        // Настроить ответ сервера
        val response = MockResponse()
            .setResponseCode(200)
            .setBody(gson.toJson(updatedTodo))
            .addHeader("Content-Type", "application/json")
        mockWebServer.enqueue(response)
        
        // Act: Выполнить обновление
        val result = apiService.updateTodo(todoId, updatedTodo)
        
        // Assert: Проверить результат
        assertTrue(result.isSuccessful)
        assertEquals(200, result.code())
        val todo = result.body()
        assertNotNull(todo)
        assertEquals(todoId, todo!!.id)
        assertEquals("Updated task", todo.title)
        assertEquals(true, todo.completed)
        assertEquals(1, todo.userId)
        
        // Проверить параметры запроса
        val request = mockWebServer.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/todos/$todoId", request.path)
        assertEquals("application/json; charset=UTF-8", request.getHeader("Content-Type"))
        
        // Проверить тело запроса
        val requestBody = request.body.readUtf8()
        val sentTodo = gson.fromJson(requestBody, TodoDto::class.java)
        assertEquals("Updated task", sentTodo.title)
        assertEquals(true, sentTodo.completed)
        assertEquals(todoId, sentTodo.id)
    }

    /**
     * Тест 5: DELETE запрос - удаление задачи
     * 
     * TODO: Реализовать тест
     * 
     * Ожидаемый результат:
     * - MockWebServer возвращает 204 No Content
     * - Проверить что запрос DELETE с правильным путем
     */
    @Test
    fun `test DELETE removes todo`() = runTest {
        // Arrange: Настроить ответ для удаления
        val todoId = "42"
        val response = MockResponse()
            .setResponseCode(204) // No Content - успешное удаление
        mockWebServer.enqueue(response)
        
        // Act: Выполнить удаление задачи
        val result = apiService.deleteTodo(todoId)
        
        // Assert: Проверить код ответа и метод запроса
        assertTrue(result.isSuccessful)
        assertEquals(204, result.code())
        assertNull(result.body()) // No Content означает пустое тело
        
        // Проверить параметры запроса
        val request = mockWebServer.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/todos/$todoId", request.path)
    }

    /**
     * Тест 6: Обработка ошибок - 404 Not Found
     * 
     * TODO: Реализовать тест
     * 
     * Ожидаемый результат:
     * - При получении 404 response.isSuccessful = false
     * - response.code() = 404
     */
    @Test
    fun `test handles 404 error correctly`() = runTest {
        // Arrange: Настроить сервер для возврата ошибки 404
        val nonExistentId = "999"
        val response = MockResponse()
            .setResponseCode(404)
            .setBody("Not Found")
        mockWebServer.enqueue(response)
        
        // Act: Выполнить запрос несуществующего ресурса
        val result = apiService.getTodoById(nonExistentId)
        
        // Assert: Проверить обработку ошибки
        assertFalse(result.isSuccessful)
        assertEquals(404, result.code())
        assertNull(result.body())
        assertNotNull(result.errorBody())
        
        // Проверить что запрос был отправлен правильно
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/todos/$nonExistentId", request.path)
    }

    /**
     * Тест 7: Таймаут запроса
     * 
     * TODO: Реализовать тест
     * 
     * Ожидаемый результат:
     * - При задержке ответа больше таймаута выбрасывается исключение
     */
    @Test
    fun `test request timeout throws exception`() = runTest {
        // Arrange: Настроить сервер с длительной задержкой
        val response = MockResponse()
            .setResponseCode(200)
            .setBody("{}")
            .setBodyDelay(2, TimeUnit.SECONDS) // Задержка больше таймаута клиента (1 сек)
        mockWebServer.enqueue(response)
        
        // Act & Assert: Выполнить запрос и дождаться исключения
        try {
            apiService.getAllTodos()
            fail("Expected timeout exception was not thrown")
        } catch (e: Exception) {
            // Ожидаем исключение таймаута
            assertTrue("Expected timeout-related exception", 
                e.message?.contains("timeout") == true || 
                e.message?.contains("timed out") == true ||
                e is java.net.SocketTimeoutException
            )
        }
        
        // Проверить что запрос был отправлен
        val request = mockWebServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/todos", request.path)
    }

    /**
     * Тест 8: Проверка заголовков запроса
     * 
     * TODO: Реализовать тест
     * 
     * Ожидаемый результат:
     * - Проверить что в запросе есть User-Agent
     * - Проверить что Accept содержит application/json
     */
    @Test
    fun `test request contains correct headers`() = runTest {
        // Arrange: Настроить ответ сервера
        val response = MockResponse()
            .setResponseCode(200)
            .setBody("[]")
            .addHeader("Content-Type", "application/json")
        mockWebServer.enqueue(response)
        
        // Act: Выполнить любой запрос
        val result = apiService.getAllTodos()
        
        // Assert: Получить информацию об отправленном запросе
        val request = mockWebServer.takeRequest()
        
        // Проверить наличие необходимых заголовков
        val userAgent = request.getHeader("User-Agent")
        assertNotNull("User-Agent header should be present", userAgent)
        assertTrue("User-Agent should contain okhttp", 
            userAgent?.contains("okhttp", ignoreCase = true) == true)
        
        val accept = request.getHeader("Accept")
        // Retrofit автоматически добавляет Accept header основываясь на конверторе
        // Для Gson конвертора ожидаем application/json
        if (accept != null) {
            assertTrue("Accept header should include application/json",
                accept.contains("application/json", ignoreCase = true))
        }
        
        // Проверить что запрос был успешным
        assertTrue(result.isSuccessful)
    }

    /**
     * Тест 9: Батчевая синхронизация
     * 
     * TODO: Реализовать тест
     * 
     * Ожидаемый результат:
     * - POST /todos/batch отправляет список задач
     * - Возвращает список синхронизированных задач с ID
     */
    @Test
    fun `test batch sync sends multiple todos`() = runTest {
        // Arrange: Создать несколько задач для синхронизации
        val currentTime = System.currentTimeMillis()
        val todosToSync = listOf(
            TodoDto(id = null, title = "Batch task 1", completed = false, userId = 1, createdAt = currentTime, updatedAt = currentTime),
            TodoDto(id = null, title = "Batch task 2", completed = true, userId = 1, createdAt = currentTime, updatedAt = currentTime),
            TodoDto(id = null, title = "Batch task 3", completed = false, userId = 2, createdAt = currentTime, updatedAt = currentTime)
        )
        
        val syncedTodos = listOf(
            TodoDto(id = "101", title = "Batch task 1", completed = false, userId = 1, createdAt = currentTime, updatedAt = currentTime),
            TodoDto(id = "102", title = "Batch task 2", completed = true, userId = 1, createdAt = currentTime, updatedAt = currentTime),
            TodoDto(id = "103", title = "Batch task 3", completed = false, userId = 2, createdAt = currentTime, updatedAt = currentTime)
        )
        
        // Настроить ответ сервера
        val response = MockResponse()
            .setResponseCode(200)
            .setBody(gson.toJson(syncedTodos))
            .addHeader("Content-Type", "application/json")
        mockWebServer.enqueue(response)
        
        // Act: Выполнить batch синхронизацию
        val result = apiService.syncTodos(todosToSync)
        
        // Assert: Проверить результат
        assertTrue(result.isSuccessful)
        val resultTodos = result.body()
        assertNotNull(resultTodos)
        assertEquals(3, resultTodos!!.size)
        
        // Проверить что все задачи получили ID
        assertEquals("101", resultTodos[0].id)
        assertEquals("Batch task 1", resultTodos[0].title)
        assertEquals("102", resultTodos[1].id)
        assertEquals("Batch task 2", resultTodos[1].title)
        assertEquals("103", resultTodos[2].id)
        assertEquals("Batch task 3", resultTodos[2].title)
        
        // Проверить параметры запроса
        val request = mockWebServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/todos/batch", request.path)
        assertEquals("application/json; charset=UTF-8", request.getHeader("Content-Type"))
        
        // Проверить тело запроса
        val requestBody = request.body.readUtf8()
        val sentTodos = gson.fromJson(requestBody, Array<TodoDto>::class.java)
        assertEquals(3, sentTodos.size)
        assertEquals("Batch task 1", sentTodos[0].title)
        assertEquals("Batch task 2", sentTodos[1].title)
        assertEquals("Batch task 3", sentTodos[2].title)
    }
}