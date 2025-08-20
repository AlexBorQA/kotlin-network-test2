package com.example.todoapp.testing

import com.example.todoapp.data.api.TodoApiService
import com.example.todoapp.data.model.TodoDto
import com.google.gson.Gson
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Базовый класс для тестов с MockWebServer
 * Содержит общую настройку MockWebServer и Retrofit
 */
abstract class BaseMockWebServerTest : BaseTest() {
    
    protected lateinit var mockWebServer: MockWebServer
    protected lateinit var apiService: TodoApiService
    protected lateinit var gson: Gson
    
    @Before
    override fun baseSetup() {
        super.baseSetup()
        
        // Инициализируем MockWebServer
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        // Создаем Gson для сериализации
        gson = Gson()
        
        // Создаем Retrofit с MockWebServer URL
        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/").toString())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            
        // Получаем API сервис
        apiService = retrofit.create(TodoApiService::class.java)
    }
    
    @After
    override fun baseTearDown() {
        mockWebServer.shutdown()
        super.baseTearDown()
    }
    
    /**
     * Добавляет в очередь список TodoDto как успешный ответ
     */
    protected fun enqueueTodos(todos: List<TodoDto>) {
        mockWebServer.enqueueSuccess(gson.toJson(todos))
    }
    
    /**
     * Добавляет в очередь одну TodoDto как успешный ответ
     */
    protected fun enqueueTodo(todo: TodoDto) {
        mockWebServer.enqueueSuccess(gson.toJson(todo))
    }
    
    /**
     * Добавляет в очередь TodoDto как ответ Created (201)
     */
    protected fun enqueueCreatedTodo(todo: TodoDto) {
        mockWebServer.enqueueCreated(gson.toJson(todo))
    }
    
    /**
     * Добавляет в очередь пустой успешный ответ (для DELETE)
     */
    protected fun enqueueEmptySuccess() {
        mockWebServer.enqueue(okhttp3.mockwebserver.MockResponse()
            .setResponseCode(204))
    }
    
    /**
     * Добавляет в очередь ошибку 404
     */
    protected fun enqueue404() {
        mockWebServer.enqueueError(404, "Not Found")
    }
    
    /**
     * Добавляет в очередь ошибку 500
     */
    protected fun enqueue500() {
        mockWebServer.enqueueError(500, "Internal Server Error")
    }
    
    /**
     * Добавляет в очередь таймаут
     */
    protected fun enqueueTimeout(delayMs: Long = 1000) {
        mockWebServer.enqueueWithDelay("{}", delayMs)
    }
    
    /**
     * Проверяет последний запрос
     */
    protected fun verifyLastRequest(expectedMethod: String, expectedPath: String) {
        val request = mockWebServer.takeRequest()
        assertEquals("HTTP method should match", expectedMethod, request.method)
        assertEquals("Request path should match", expectedPath, request.path)
    }
    
    /**
     * Проверяет последний запрос с телом
     */
    protected fun verifyLastRequestWithBody(
        expectedMethod: String, 
        expectedPath: String, 
        expectedBodyContains: String
    ) {
        val request = mockWebServer.takeRequest()
        assertEquals("HTTP method should match", expectedMethod, request.method)
        assertEquals("Request path should match", expectedPath, request.path)
        assertTrue("Request body should contain '$expectedBodyContains'", 
            request.body.readUtf8().contains(expectedBodyContains))
    }
    
    /**
     * Проверяет заголовки последнего запроса
     */
    protected fun verifyLastRequestHeaders(expectedHeaders: Map<String, String>) {
        val request = mockWebServer.takeRequest()
        expectedHeaders.forEach { (name, value) ->
            assertEquals("Header $name should match", value, request.getHeader(name))
        }
    }
    
    /**
     * Проверяет что запросов больше не было
     */
    protected fun verifyNoMoreRequests() {
        assertEquals("Should be no more requests", 0, mockWebServer.requestCount - getProcessedRequestCount())
    }
    
    private var processedRequestCount = 0
    
    /**
     * Отмечает что запрос был обработан (для verifyNoMoreRequests)
     */
    protected fun markRequestProcessed() {
        processedRequestCount++
    }
    
    private fun getProcessedRequestCount(): Int = processedRequestCount
}
