package com.example.todoapp.e2e

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.todoapp.data.api.TodoApiService
import com.example.todoapp.data.database.TodoDatabase
import com.example.todoapp.data.database.dao.TodoDao
import com.example.todoapp.data.database.entity.Priority
import com.example.todoapp.data.database.entity.SyncStatus
import com.example.todoapp.data.model.TodoDto
import com.example.todoapp.data.network.NetworkManager
import com.example.todoapp.data.repository.CachedTodoRepository
import com.example.todoapp.domain.model.Todo
import com.example.todoapp.domain.model.TodoPriority
import com.example.todoapp.background.SyncManager
import com.example.todoapp.background.SyncWorker
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.android.testing.BindValue
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.runner.RunWith
import retrofit2.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.google.gson.Gson

/**
 * End-to-End тест для проверки полного цикла синхронизации задач
 * Использует Hilt Testing API для dependency injection
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@UninstallModules(com.example.todoapp.di.AppModule::class)
class TodoSyncE2ETest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var database: TodoDatabase
    
    @Inject
    lateinit var todoDao: TodoDao
    
    @BindValue
    @JvmField
    val mockApiService: TodoApiService = mockk()
    
    @BindValue
    @JvmField
    val mockNetworkManager: NetworkManager = mockk()
    
    private lateinit var repository: CachedTodoRepository
    private lateinit var syncManager: SyncManager
    private lateinit var workManager: WorkManager
    private val gson = Gson()
    
    @Before
    fun setup() {
        hiltRule.inject()
        
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
            
        WorkManagerTestInitHelper.initializeTestWorkManager(
            ApplicationProvider.getApplicationContext(),
            config
        )
        
        workManager = WorkManager.getInstance(ApplicationProvider.getApplicationContext())
        syncManager = SyncManager(workManager)
        repository = CachedTodoRepository(todoDao, mockApiService, mockNetworkManager)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun testFullSyncCycle_CreateTodoOffline_ThenSyncOnline() = runTest {
        every { mockNetworkManager.isNetworkAvailable() } returns false
        
        val newTodo = Todo(
            id = 0,
            title = "E2E Test Task",
            description = "Created offline, should sync online",
            isCompleted = false,
            priority = TodoPriority.HIGH
        )
        
        val createdTodoId = repository.insertTodo(newTodo)
        Assert.assertTrue("Todo should be created with local ID", createdTodoId > 0)
        
        val localTodos = repository.getAllTodos().first()
        Assert.assertEquals(1, localTodos.size)
        
        val localTodo = localTodos[0]
        Assert.assertEquals("E2E Test Task", localTodo.title)
        Assert.assertEquals(createdTodoId, localTodo.id)
        
        every { mockNetworkManager.isNetworkAvailable() } returns true
        every { mockNetworkManager.observeNetworkChanges() } returns flowOf(true)
        
        val syncedTodoDto = TodoDto(
            id = "server-123",
            title = "E2E Test Task",
            description = "Created offline, should sync online",
            isCompleted = false,
            priority = "HIGH",
            remoteId = "server-123",
            localId = createdTodoId,
            updatedAt = System.currentTimeMillis()
        )
        
        coEvery { 
            mockApiService.syncTodos(any())
        } returns Response.success(listOf(syncedTodoDto))
        
        coEvery {
            mockApiService.getAllTodos()
        } returns Response.success(listOf(syncedTodoDto))
        
        val syncResult = repository.syncWithRemote()
        Assert.assertTrue("Sync should be successful", syncResult.isSuccess)
        
        val syncedTodos = repository.getAllTodos().first()
        Assert.assertEquals(1, syncedTodos.size)
        
        val syncedTodo = syncedTodos[0]
        Assert.assertEquals("E2E Test Task", syncedTodo.title)
        
        repository.updateTodo(
            syncedTodo.copy(
                isCompleted = true,
                description = "Updated in E2E test"
            )
        )
        
        val updatedTodoDto = syncedTodoDto.copy(
            isCompleted = true,
            description = "Updated in E2E test",
            updatedAt = System.currentTimeMillis()
        )
        
        coEvery { 
            mockApiService.updateTodo(any(), any())
        } returns Response.success(updatedTodoDto)
        
        val updateSyncResult = repository.syncWithRemote()
        Assert.assertTrue("Update sync should be successful", updateSyncResult.isSuccess)
        
        val finalTodos = repository.getAllTodos().first()
        Assert.assertEquals(1, finalTodos.size)
        
        val finalTodo = finalTodos[0]
        Assert.assertTrue("Todo should be completed", finalTodo.isCompleted)
        Assert.assertEquals("Updated in E2E test", finalTodo.description)
    }
    
    @Test
    fun testConflictResolution_ServerWinsWithNewerTimestamp() = runTest {
        every { mockNetworkManager.isNetworkAvailable() } returns true
        
        val localTodoEntity = com.example.todoapp.data.database.entity.TodoEntity(
            id = 1,
            title = "Conflict Test - Local",
            description = "Local version",
            isCompleted = false,
            priority = Priority.MEDIUM,
            syncStatus = SyncStatus.SYNCED,
            remoteId = "remote-1",
            updatedAt = System.currentTimeMillis() - 10000
        )
        
        todoDao.insertTodo(localTodoEntity)
        
        val serverTodoDto = TodoDto(
            id = "remote-1",
            title = "Conflict Test - Server",
            description = "Server version",
            isCompleted = true,
            priority = "HIGH",
            remoteId = "remote-1",
            updatedAt = System.currentTimeMillis()
        )
        
        coEvery {
            mockApiService.getAllTodos()
        } returns Response.success(listOf(serverTodoDto))
        
        val syncResult = repository.syncWithRemote()
        Assert.assertTrue("Sync should be successful", syncResult.isSuccess)
        
        val todos = repository.getAllTodos().first()
        Assert.assertEquals(1, todos.size)
        
        val resolvedTodo = todos[0]
        Assert.assertEquals("Conflict Test - Server", resolvedTodo.title)
        Assert.assertEquals("Server version", resolvedTodo.description)
        Assert.assertTrue("Should use server's completed status", resolvedTodo.isCompleted)
        Assert.assertEquals(TodoPriority.HIGH, resolvedTodo.priority)
    }
    
    @Test
    fun testBatchSync_MultiplePendingTodos() = runTest {
        every { mockNetworkManager.isNetworkAvailable() } returns false
        
        val todos = listOf(
            Todo(0, "Task 1", "Description 1", false, TodoPriority.HIGH),
            Todo(0, "Task 2", "Description 2", false, TodoPriority.MEDIUM),
            Todo(0, "Task 3", "Description 3", true, TodoPriority.LOW)
        )
        
        val createdIds = todos.map { repository.insertTodo(it) }
        Assert.assertEquals(3, createdIds.size)
        
        every { mockNetworkManager.isNetworkAvailable() } returns true
        
        val syncedDtos = createdIds.mapIndexed { index, localId ->
            TodoDto(
                id = "server-${index + 1}",
                title = "Task ${index + 1}",
                description = "Description ${index + 1}",
                isCompleted = index == 2,
                priority = when(index) {
                    0 -> "HIGH"
                    1 -> "MEDIUM"
                    else -> "LOW"
                },
                remoteId = "server-${index + 1}",
                localId = localId,
                updatedAt = System.currentTimeMillis()
            )
        }
        
        coEvery {
            mockApiService.syncTodos(any())
        } returns Response.success(syncedDtos)
        
        coEvery {
            mockApiService.getAllTodos()
        } returns Response.success(syncedDtos)
        
        val syncResult = repository.syncWithRemote()
        Assert.assertTrue("Batch sync should be successful", syncResult.isSuccess)
        
        val syncedTodos = repository.getAllTodos().first()
        Assert.assertEquals(3, syncedTodos.size)
        
        syncedTodos.forEach { todo ->
            Assert.assertNotNull("Each todo should have remoteId after sync", todo.remoteId)
        }
        
        coVerify(exactly = 1) { mockApiService.syncTodos(any()) }
    }
}