package com.example.todoapp.provider

import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.example.todoapp.data.database.TodoDatabase
import com.example.todoapp.data.database.entity.TodoEntity
import kotlinx.coroutines.runBlocking

/**
 * Content Provider для предоставления доступа к данным задач другим приложениям
 * Позволяет внешним приложениям читать и изменять задачи
 */
class TodoContentProvider : ContentProvider() {
    
    companion object {
        const val AUTHORITY = "com.example.todoapp.provider"
        const val TODOS_TABLE = "todos"
        
        const val TODOS_PATH = "todos"
        const val TODO_ID_PATH = "todos/#"
        
        const val TODOS = 1
        const val TODO_ID = 2
        
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$TODOS_PATH")
        
        val PROJECTION = arrayOf(
            "id",
            "title", 
            "description",
            "is_completed",
            "priority",
            "category",
            "due_date",
            "created_at"
        )
    }
    
    private lateinit var database: TodoDatabase
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(AUTHORITY, TODOS_PATH, TODOS)
        addURI(AUTHORITY, TODO_ID_PATH, TODO_ID)
    }
    
    override fun onCreate(): Boolean {
        database = TodoDatabase.getDatabase(context!!)
        return true
    }
    
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        return when (uriMatcher.match(uri)) {
            TODOS -> {
                runBlocking {
                    val todos = database.todoDao().getAllTodos()
                    createCursorFromTodos(emptyList())
                }
            }
            TODO_ID -> {
                val todoId = uri.lastPathSegment?.toLongOrNull()
                if (todoId != null) {
                    runBlocking {
                        val todo = database.todoDao().getTodoById(todoId)
                        if (todo != null) {
                            createCursorFromTodos(listOf(todo))
                        } else {
                            createCursorFromTodos(emptyList())
                        }
                    }
                } else {
                    null
                }
            }
            else -> null
        }
    }
    
    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            TODOS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.$TODOS_TABLE"
            TODO_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.$TODOS_TABLE"
            else -> null
        }
    }
    
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return when (uriMatcher.match(uri)) {
            TODOS -> {
                values?.let { cv ->
                    val todo = TodoEntity(
                        title = cv.getAsString("title") ?: "",
                        description = cv.getAsString("description"),
                        isCompleted = cv.getAsBoolean("is_completed") ?: false,
                        category = cv.getAsString("category")
                    )
                    
                    runBlocking {
                        val newId = database.todoDao().insertTodo(todo)
                        context?.contentResolver?.notifyChange(uri, null)
                        Uri.withAppendedPath(CONTENT_URI, newId.toString())
                    }
                }
            }
            else -> null
        }
    }
    
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return when (uriMatcher.match(uri)) {
            TODO_ID -> {
                val todoId = uri.lastPathSegment?.toLongOrNull()
                if (todoId != null) {
                    runBlocking {
                        database.todoDao().deleteTodoById(todoId)
                        context?.contentResolver?.notifyChange(uri, null)
                        1
                    }
                } else {
                    0
                }
            }
            else -> 0
        }
    }
    
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        return when (uriMatcher.match(uri)) {
            TODO_ID -> {
                val todoId = uri.lastPathSegment?.toLongOrNull()
                if (todoId != null && values != null) {
                    runBlocking {
                        val existingTodo = database.todoDao().getTodoById(todoId)
                        if (existingTodo != null) {
                            val updatedTodo = existingTodo.copy(
                                title = values.getAsString("title") ?: existingTodo.title,
                                description = values.getAsString("description") ?: existingTodo.description,
                                isCompleted = values.getAsBoolean("is_completed") ?: existingTodo.isCompleted,
                                category = values.getAsString("category") ?: existingTodo.category
                            )
                            database.todoDao().updateTodo(updatedTodo)
                            context?.contentResolver?.notifyChange(uri, null)
                            1
                        } else {
                            0
                        }
                    }
                } else {
                    0
                }
            }
            else -> 0
        }
    }
    
    private fun createCursorFromTodos(todos: List<TodoEntity>): MatrixCursor {
        val cursor = MatrixCursor(PROJECTION)
        todos.forEach { todo ->
            cursor.addRow(arrayOf(
                todo.id,
                todo.title,
                todo.description,
                if (todo.isCompleted) 1 else 0,
                todo.priority.name,
                todo.category,
                todo.dueDate?.time,
                todo.createdAt.time
            ))
        }
        return cursor
    }
}