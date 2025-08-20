package com.example.todoapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.background.SyncManager
import com.example.todoapp.core.idling.IdlingResourceProvider
import com.example.todoapp.data.network.NetworkManager
import com.example.todoapp.domain.model.Todo
import com.example.todoapp.domain.model.TodoPriority
import com.example.todoapp.domain.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoRepository: TodoRepository,
    private val networkManager: NetworkManager,
    private val syncManager: SyncManager,
    private val idlingResource: IdlingResourceProvider
) : ViewModel() {
    
    // UI состояние
    private val _uiState = MutableStateFlow(TodoUiState())
    val uiState: StateFlow<TodoUiState> = _uiState.asStateFlow()
    
    // Todos
    val todos = todoRepository.getAllTodos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Состояние сети
    val isNetworkAvailable = networkManager.observeNetworkChanges()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = networkManager.isNetworkAvailable()
        )
    
    // Статистика
    val activeCount = todoRepository.getActiveCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    val completedCount = todoRepository.getCompletedCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    init {
        // Запускаем периодическую синхронизацию
        syncManager.startPeriodicSync()
        
        // Следим за состоянием сети и запускаем синхронизацию при подключении
        viewModelScope.launch {
            isNetworkAvailable.collect { isConnected ->
                if (isConnected && _uiState.value.hasPendingChanges) {
                    syncTodos()
                }
            }
        }
        
        // Проверяем наличие несинхронизированных изменений
        viewModelScope.launch {
            todoRepository.getPendingSyncTodos().let { pendingTodos ->
                _uiState.update { it.copy(hasPendingChanges = pendingTodos.isNotEmpty()) }
            }
        }
    }
    
    fun addTodo(
        title: String,
        description: String? = null,
        priority: TodoPriority = TodoPriority.NORMAL,
        category: String? = null,
        dueDate: Date? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val todo = Todo(
                title = title,
                description = description,
                priority = priority,
                category = category,
                dueDate = dueDate
            )
            
            try {
                todoRepository.insertTodo(todo)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        message = "Todo added successfully"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to add todo: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun updateTodo(todo: Todo) {
        viewModelScope.launch {
            try {
                todoRepository.updateTodo(todo)
                _uiState.update { 
                    it.copy(message = "Todo updated")
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to update todo: ${e.message}")
                }
            }
        }
    }
    
    fun toggleTodoCompletion(todoId: Long) {
        viewModelScope.launch {
            try {
                todoRepository.toggleCompletion(todoId)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to toggle todo: ${e.message}")
                }
            }
        }
    }
    
    fun deleteTodo(todo: Todo) {
        viewModelScope.launch {
            try {
                todoRepository.deleteTodo(todo)
                _uiState.update { 
                    it.copy(message = "Todo deleted")
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to delete todo: ${e.message}")
                }
            }
        }
    }
    
    fun syncTodos() {
        viewModelScope.launch {
            idlingResource.increment("syncTodos")
            _uiState.update { it.copy(isSyncing = true) }
            
            val result = todoRepository.syncWithRemote()
            
            if (result.isSuccess) {
                _uiState.update { 
                    it.copy(
                        isSyncing = false,
                        hasPendingChanges = false,
                        message = "Sync completed successfully",
                        lastSyncTime = Date()
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isSyncing = false,
                        error = "Sync failed: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
            idlingResource.decrement("syncTodos")
        }
    }
    
    fun forceSyncNow() {
        syncManager.startOneTimeSync()
        _uiState.update { 
            it.copy(message = "Sync started in background")
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Можно остановить периодическую синхронизацию если нужно
        // syncManager.stopPeriodicSync()
    }
}

data class TodoUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val hasPendingChanges: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val lastSyncTime: Date? = null
)