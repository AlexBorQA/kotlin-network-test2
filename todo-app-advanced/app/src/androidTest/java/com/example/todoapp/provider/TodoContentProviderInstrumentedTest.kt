package com.example.todoapp.provider

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapp.data.database.TodoDatabase
import com.example.todoapp.data.database.dao.TodoDao
import com.example.todoapp.data.database.entity.Priority
import com.example.todoapp.data.database.entity.TodoEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Инструментальные тесты для TodoContentProvider
 * Проверяют корректность операций ContentProvider в реальном Android окружении
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TodoContentProviderInstrumentedTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    
    @Inject
    lateinit var database: TodoDatabase
    
    private lateinit var todoDao: TodoDao
    
    private val authority = "com.example.todoapp.provider"
    private val baseUri = Uri.parse("content://$authority/todos")
    
    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        contentResolver = context.contentResolver
        todoDao = database.todoDao()
        
        // Очищаем базу данных перед каждым тестом
        runBlocking {
            database.clearAllTables()
        }
    }
    
    @After
    fun tearDown() {
        runBlocking {
            database.clearAllTables()
        }
    }
    
    @Test
    fun testQuery_returnsAllTodos() = runBlocking {
        // Given - добавляем тестовые данные напрямую в базу
        val todo1 = TodoEntity(
            title = "Test Task 1",
            description = "Description 1",
            isCompleted = false,
            priority = Priority.HIGH,
            category = "Work"
        )
        val todo2 = TodoEntity(
            title = "Test Task 2",
            description = "Description 2",
            isCompleted = true,
            priority = Priority.NORMAL,
            category = "Personal"
        )
        
        val id1 = todoDao.insertTodo(todo1)
        val id2 = todoDao.insertTodo(todo2)
        
        // When - запрашиваем все задачи через ContentProvider
        val cursor = contentResolver.query(baseUri, null, null, null, null)
        
        // Then - проверяем результат
        assertNotNull(cursor)
        cursor?.use {
            assertEquals(2, it.count)
            
            // Проверяем первую задачу
            it.moveToFirst()
            val titleIndex = it.getColumnIndex("title")
            val descriptionIndex = it.getColumnIndex("description")
            val isCompletedIndex = it.getColumnIndex("is_completed")
            val priorityIndex = it.getColumnIndex("priority")
            val categoryIndex = it.getColumnIndex("category")
            
            assertEquals("Test Task 1", it.getString(titleIndex))
            assertEquals("Description 1", it.getString(descriptionIndex))
            assertEquals(0, it.getInt(isCompletedIndex)) // false = 0
            assertEquals("HIGH", it.getString(priorityIndex))
            assertEquals("Work", it.getString(categoryIndex))
            
            // Проверяем вторую задачу
            it.moveToNext()
            assertEquals("Test Task 2", it.getString(titleIndex))
            assertEquals("Description 2", it.getString(descriptionIndex))
            assertEquals(1, it.getInt(isCompletedIndex)) // true = 1
            assertEquals("NORMAL", it.getString(priorityIndex))
            assertEquals("Personal", it.getString(categoryIndex))
        }
    }
    
    @Test
    fun testQuery_withTodoId_returnsSingleTodo() = runBlocking {
        // Given - добавляем тестовую задачу
        val todo = TodoEntity(
            title = "Single Task",
            description = "Single Description",
            isCompleted = false,
            priority = Priority.LOW,
            category = "Test"
        )
        val todoId = todoDao.insertTodo(todo)
        
        // When - запрашиваем задачу по ID
        val uriWithId = Uri.withAppendedPath(baseUri, todoId.toString())
        val cursor = contentResolver.query(uriWithId, null, null, null, null)
        
        // Then - проверяем результат
        assertNotNull(cursor)
        cursor?.use {
            assertEquals(1, it.count)
            it.moveToFirst()
            
            val titleIndex = it.getColumnIndex("title")
            assertEquals("Single Task", it.getString(titleIndex))
        }
    }
    
    @Test
    fun testGetType_returnsCorrectMimeTypes() {
        // Test for collection URI
        val collectionType = contentResolver.getType(baseUri)
        assertEquals("vnd.android.cursor.dir/vnd.$authority.todos", collectionType)
        
        // Test for item URI
        val itemUri = Uri.withAppendedPath(baseUri, "1")
        val itemType = contentResolver.getType(itemUri)
        assertEquals("vnd.android.cursor.item/vnd.$authority.todos", itemType)
    }
    
    @Test
    fun testInsert_createsNewTodo() = runBlocking {
        // Given - подготавливаем данные для вставки
        val values = ContentValues().apply {
            put("title", "Inserted Task")
            put("description", "Inserted Description")
            put("is_completed", false)
            put("category", "Test Category")
        }
        
        // When - вставляем через ContentProvider
        val resultUri = contentResolver.insert(baseUri, values)
        
        // Then - проверяем результат
        assertNotNull(resultUri)
        val todoId = resultUri?.lastPathSegment?.toLong()
        assertNotNull(todoId)
        
        // Проверяем, что задача действительно создана в базе
        val insertedTodo = todoDao.getTodoById(todoId!!)
        assertNotNull(insertedTodo)
        assertEquals("Inserted Task", insertedTodo?.title)
        assertEquals("Inserted Description", insertedTodo?.description)
        assertEquals(false, insertedTodo?.isCompleted)
        assertEquals("Test Category", insertedTodo?.category)
    }
    
    @Test
    fun testDelete_removesTodo() = runBlocking {
        // Given - создаем задачу для удаления
        val todo = TodoEntity(
            title = "To Delete",
            description = "Will be deleted",
            isCompleted = false,
            category = "Test"
        )
        val todoId = todoDao.insertTodo(todo)
        
        // When - удаляем через ContentProvider
        val deleteUri = Uri.withAppendedPath(baseUri, todoId.toString())
        val deletedCount = contentResolver.delete(deleteUri, null, null)
        
        // Then - проверяем результат
        assertEquals(1, deletedCount)
        
        // Проверяем, что задача действительно удалена
        val deletedTodo = todoDao.getTodoById(todoId)
        assertNull(deletedTodo)
    }
    
    @Test
    fun testUpdate_modifiesTodo() = runBlocking {
        // Given - создаем задачу для обновления
        val todo = TodoEntity(
            title = "Original Title",
            description = "Original Description",
            isCompleted = false,
            priority = Priority.LOW,
            category = "Original"
        )
        val todoId = todoDao.insertTodo(todo)
        
        // When - обновляем через ContentProvider
        val updateUri = Uri.withAppendedPath(baseUri, todoId.toString())
        val updateValues = ContentValues().apply {
            put("title", "Updated Title")
            put("is_completed", true)
            put("category", "Updated")
        }
        val updatedCount = contentResolver.update(updateUri, updateValues, null, null)
        
        // Then - проверяем результат
        assertEquals(1, updatedCount)
        
        // Проверяем, что задача действительно обновлена
        val updatedTodo = todoDao.getTodoById(todoId)
        assertNotNull(updatedTodo)
        assertEquals("Updated Title", updatedTodo?.title)
        assertEquals("Original Description", updatedTodo?.description) // Не изменялось
        assertEquals(true, updatedTodo?.isCompleted)
        assertEquals("Updated", updatedTodo?.category)
        assertEquals(Priority.LOW, updatedTodo?.priority) // Не изменялось
    }
    
    @Test
    fun testUpdate_nonExistentTodo_returnsZero() {
        // Given - несуществующий ID
        val nonExistentUri = Uri.withAppendedPath(baseUri, "9999")
        val values = ContentValues().apply {
            put("title", "Won't be updated")
        }
        
        // When - пытаемся обновить
        val updatedCount = contentResolver.update(nonExistentUri, values, null, null)
        
        // Then - должно вернуть 0
        assertEquals(0, updatedCount)
    }
    
    @Test
    fun testDelete_nonExistentTodo_returnsZero() {
        // Given - несуществующий ID
        val nonExistentUri = Uri.withAppendedPath(baseUri, "9999")
        
        // When - пытаемся удалить
        val deletedCount = contentResolver.delete(nonExistentUri, null, null)
        
        // Then - должно вернуть 0
        assertEquals(0, deletedCount)
    }
    
    @Test
    fun testQuery_emptyDatabase_returnsEmptyCursor() {
        // When - запрашиваем из пустой базы
        val cursor = contentResolver.query(baseUri, null, null, null, null)
        
        // Then - курсор должен быть пустым
        assertNotNull(cursor)
        cursor?.use {
            assertEquals(0, it.count)
        }
    }
    
    @Test
    fun testContentObserver_notifiesOnInsert() = runBlocking {
        // Given - регистрируем observer
        var notificationReceived = false
        val observer = object : android.database.ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                notificationReceived = true
            }
        }
        contentResolver.registerContentObserver(baseUri, true, observer)
        
        // When - вставляем данные
        val values = ContentValues().apply {
            put("title", "New Task")
            put("description", "Test notification")
        }
        contentResolver.insert(baseUri, values)
        
        // Then - ждем немного для получения уведомления
        Thread.sleep(100)
        assertTrue("Content observer should be notified", notificationReceived)
        
        // Cleanup
        contentResolver.unregisterContentObserver(observer)
    }
}