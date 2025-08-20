package com.example.todoapp.integration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapp.MainActivity
import com.example.todoapp.data.api.TodoApiService
import com.example.todoapp.data.model.CreateTodoRequest
import com.example.todoapp.data.model.TodoDto
import com.example.todoapp.testing.TestIdlingResourceProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import com.google.gson.Gson

/**
 * Интеграционный тест для проверки синхронизации с использованием IdlingResource
 * 
 * Демонстрирует:
 * - Автоматическое ожидание завершения сетевых операций
 * - Использование MockWebServer для имитации сервера
 * - Проверку UI после синхронизации
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TodoSyncWithIdlingTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var idlingResourceProvider: TestIdlingResourceProvider
    
    @Inject
    lateinit var apiService: TodoApiService

    private lateinit var mockWebServer: MockWebServer
    private val gson = Gson()

    @Before
    fun setup() {
        hiltRule.inject()
        
        // Регистрируем IdlingResource
        IdlingRegistry.getInstance().register(idlingResourceProvider.getIdlingResource())
        
        // Настраиваем MockWebServer
        mockWebServer = MockWebServer()
        mockWebServer.start(8080)
    }

    @After
    fun tearDown() {
        // Отменяем регистрацию IdlingResource
        IdlingRegistry.getInstance().unregister(idlingResourceProvider.getIdlingResource())
        
        mockWebServer.shutdown()
    }

    @Test
    fun syncButton_triggersSync_andUpdatesUI() {
        // Given - подготавливаем ответ сервера
        val mockTodos = listOf(
            TodoDto(
                id = 1,
                userId = 1,
                title = "Test Todo from Server",
                completed = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            TodoDto(
                id = 2,
                userId = 1,
                title = "Another Server Todo",
                completed = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(mockTodos))
        )
        
        // When - нажимаем кнопку синхронизации
        composeTestRule.onNodeWithContentDescription("Sync")
            .performClick()
        
        // Then - благодаря IdlingResource тест автоматически ждёт завершения синхронизации
        // и проверяет, что данные появились в UI
        composeTestRule.onNodeWithText("Test Todo from Server")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Another Server Todo")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        // Проверяем, что индикатор синхронизации исчез
        composeTestRule.onAllNodesWithContentDescription("Syncing")
            .assertCountEquals(0)
    }

    @Test
    fun createTodo_withNetworkAvailable_syncImmediately() = runTest {
        // Given - сервер готов принять новый todo
        val expectedRequest = CreateTodoRequest(
            title = "New Todo for Sync Test",
            completed = false,
            userId = 1
        )
        
        val serverResponse = TodoDto(
            id = 100,
            userId = 1,
            title = "New Todo for Sync Test",
            completed = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setBody(gson.toJson(serverResponse))
        )
        
        // When - создаем новый todo через UI
        composeTestRule.onNodeWithContentDescription("Add Todo")
            .performClick()
        
        composeTestRule.onNodeWithText("Title")
            .performTextInput("New Todo for Sync Test")
        
        composeTestRule.onNodeWithText("Add")
            .performClick()
        
        // Then - IdlingResource гарантирует, что тест ждёт завершения операции
        // Проверяем, что todo появился в списке
        composeTestRule.onNodeWithText("New Todo for Sync Test")
            .assertIsDisplayed()
        
        // Проверяем, что запрос был отправлен на сервер
        val recordedRequest = mockWebServer.takeRequest()
        assert(recordedRequest.path?.contains("/todos") == true)
        assert(recordedRequest.method == "POST")
    }

    @Test
    fun networkError_showsErrorMessage_withIdlingResource() {
        // Given - сервер возвращает ошибку
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )
        
        // When - пытаемся синхронизировать
        composeTestRule.onNodeWithContentDescription("Sync")
            .performClick()
        
        // Then - IdlingResource ждёт завершения операции, включая обработку ошибки
        // Проверяем, что отображается сообщение об ошибке
        composeTestRule.onNodeWithText("Sync failed", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun toggleTodoCompletion_triggersSync_withIdlingResource() {
        // Given - есть todo в списке
        val mockTodos = listOf(
            TodoDto(
                id = 1,
                userId = 1,
                title = "Todo to Toggle",
                completed = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        
        // Первый запрос - получение списка
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(mockTodos))
        )
        
        // Второй запрос - обновление todo
        val updatedTodo = mockTodos[0].copy(completed = true)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(updatedTodo))
        )
        
        // Синхронизируем для получения начального списка
        composeTestRule.onNodeWithContentDescription("Sync")
            .performClick()
        
        // When - переключаем состояние todo
        composeTestRule.onNodeWithText("Todo to Toggle")
            .performClick()
        
        composeTestRule.onNodeWithContentDescription("Toggle completion")
            .performClick()
        
        // Then - IdlingResource ждёт завершения синхронизации
        // Проверяем, что UI обновился
        composeTestRule.onNodeWithText("Todo to Toggle")
            .assertExists() // Todo всё ещё в списке
        
        // Можно добавить проверку визуального состояния (зачёркнут/не зачёркнут)
    }

    @Test
    fun batchSync_multipleOperations_handledWithIdlingResource() {
        // Given - несколько todos для синхронизации
        val mockTodos = (1..10).map { index ->
            TodoDto(
                id = index.toLong(),
                userId = 1,
                title = "Batch Todo $index",
                completed = index % 2 == 0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(gson.toJson(mockTodos))
        )
        
        // When - запускаем синхронизацию
        composeTestRule.onNodeWithContentDescription("Sync")
            .performClick()
        
        // Then - IdlingResource гарантирует, что все операции завершились
        // Проверяем, что все todos отображаются
        mockTodos.forEach { todo ->
            composeTestRule.onNodeWithText(todo.title)
                .assertExists()
        }
        
        // Проверяем, что нет индикатора загрузки
        composeTestRule.onAllNodesWithContentDescription("Loading")
            .assertCountEquals(0)
    }
}