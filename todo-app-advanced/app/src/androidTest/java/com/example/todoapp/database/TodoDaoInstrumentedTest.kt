package com.example.todoapp.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapp.data.database.TodoDatabase
import com.example.todoapp.data.database.dao.TodoDao
import com.example.todoapp.data.database.entity.Priority
import com.example.todoapp.data.database.entity.SyncStatus
import com.example.todoapp.data.database.entity.TodoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class TodoDaoInstrumentedTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: TodoDatabase
    private lateinit var todoDao: TodoDao
    
    @Before
    fun setup() {
        // In-memory база данных для тестов
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TodoDatabase::class.java
        ).allowMainThreadQueries().build()
        
        todoDao = database.todoDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertTodo_returnsTodoId() = runBlocking {
        // Given
        val todo = TodoEntity(
            title = "Test Todo",
            description = "Test Description",
            priority = Priority.HIGH
        )
        
        // When
        val todoId = todoDao.insertTodo(todo)
        
        // Then
        assertTrue("Todo ID should be greater than 0", todoId > 0)
    }
    
    @Test
    fun getTodoById_returnsCorrectTodo() = runBlocking {
        // Given
        val todo = TodoEntity(title = "Test Todo", description = "Description")
        val todoId = todoDao.insertTodo(todo)
        
        // When
        val retrievedTodo = todoDao.getTodoById(todoId)
        
        // Then
        assertNotNull(retrievedTodo)
        assertEquals(todo.title, retrievedTodo?.title)
        assertEquals(todo.description, retrievedTodo?.description)
    }
    
    @Test
    fun searchTodos_findsMatchingTodos() = runBlocking {
        // Given
        todoDao.insertTodo(TodoEntity(title = "Meeting with client"))
        todoDao.insertTodo(TodoEntity(title = "Buy groceries"))
        todoDao.insertTodo(TodoEntity(title = "Client presentation"))
        
        // When
        val searchResults = todoDao.searchTodos("client").first()
        
        // Then
        assertEquals("Should find 2 todos with 'client'", 2, searchResults.size)
    }
    
    @Test
    fun getTodosByPriority_returnsCorrectTodos() = runBlocking {
        // Given
        todoDao.insertTodo(TodoEntity(title = "High Priority", priority = Priority.HIGH))
        todoDao.insertTodo(TodoEntity(title = "Normal Priority", priority = Priority.NORMAL))
        todoDao.insertTodo(TodoEntity(title = "Another High", priority = Priority.HIGH))
        
        // When
        val highPriorityTodos = todoDao.getTodosByPriority(Priority.HIGH).first()
        
        // Then
        assertEquals(2, highPriorityTodos.size)
        assertTrue(highPriorityTodos.all { it.priority == Priority.HIGH })
    }
    
    @Test
    fun markAsCompleted_updatesCompletionStatus() = runBlocking {
        // Given
        val todo = TodoEntity(title = "Test Todo", isCompleted = false)
        val todoId = todoDao.insertTodo(todo)
        
        // When
        todoDao.markAsCompleted(todoId)
        
        // Then
        val updatedTodo = todoDao.getTodoById(todoId)
        assertTrue("Todo should be completed", updatedTodo?.isCompleted == true)
    }
    
    @Test
    fun deleteCompletedTodos_removesOnlyCompletedTodos() = runBlocking {
        // Given
        todoDao.insertTodo(TodoEntity(title = "Active Todo", isCompleted = false))
        todoDao.insertTodo(TodoEntity(title = "Completed Todo 1", isCompleted = true))
        todoDao.insertTodo(TodoEntity(title = "Completed Todo 2", isCompleted = true))
        
        // When
        todoDao.deleteCompletedTodos()
        
        // Then
        val remainingTodos = todoDao.getAllTodos().first()
        assertEquals(1, remainingTodos.size)
        assertFalse(remainingTodos[0].isCompleted)
    }
    
    @Test
    fun getTodosBySyncStatus_returnsCorrectTodos() = runBlocking {
        // Given
        todoDao.insertTodo(TodoEntity(title = "Local Todo", syncStatus = SyncStatus.LOCAL))
        todoDao.insertTodo(TodoEntity(title = "Synced Todo", syncStatus = SyncStatus.SYNCED))
        todoDao.insertTodo(TodoEntity(title = "Pending Todo", syncStatus = SyncStatus.PENDING))
        
        // When
        val pendingTodos = todoDao.getTodosBySyncStatus(SyncStatus.PENDING)
        
        // Then
        assertEquals(1, pendingTodos.size)
        assertEquals("Pending Todo", pendingTodos[0].title)
        assertEquals(SyncStatus.PENDING, pendingTodos[0].syncStatus)
    }
}