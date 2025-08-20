package com.example.todoapp.testing

import com.example.todoapp.core.idling.IdlingResourceProvider
import com.example.todoapp.data.api.TodoApiService
import com.example.todoapp.data.database.dao.TodoDao
import com.example.todoapp.data.network.NetworkManager
import com.example.todoapp.data.repository.CachedTodoRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs

/**
 * Factory для создания mock объектов
 * Стандартизирует настройку моков в тестах
 */
object MockFactory {
    
    /**
     * Создает mock TodoDao с базовой настройкой
     */
    fun createMockDao(): TodoDao {
        return mockk(relaxed = true)
    }
    
    /**
     * Создает mock TodoApiService с базовой настройкой
     */
    fun createMockApiService(): TodoApiService {
        return mockk(relaxed = true)
    }
    
    /**
     * Создает mock NetworkManager
     */
    fun createMockNetworkManager(isConnected: Boolean = true): NetworkManager {
        return mockk<NetworkManager>(relaxed = true).apply {
            every { isNetworkAvailable() } returns isConnected
        }
    }
    
    /**
     * Создает mock IdlingResourceProvider
     */
    fun createMockIdlingResource(): IdlingResourceProvider {
        return mockk<IdlingResourceProvider>(relaxed = true).apply {
            every { increment(any()) } just Runs
            every { decrement(any()) } just Runs
            every { isIdleNow() } returns true
        }
    }
    
    /**
     * Создает полностью настроенный mock репозиторий
     */
    fun createMockRepository(
        dao: TodoDao = createMockDao(),
        apiService: TodoApiService = createMockApiService(),
        networkManager: NetworkManager = createMockNetworkManager(),
        idlingResource: IdlingResourceProvider = createMockIdlingResource()
    ): CachedTodoRepository {
        return CachedTodoRepository(dao, apiService, networkManager, idlingResource)
    }
    
    /**
     * Создает offline репозиторий (без сети)
     */
    fun createOfflineRepository(
        dao: TodoDao = createMockDao(),
        apiService: TodoApiService = createMockApiService(),
        idlingResource: IdlingResourceProvider = createMockIdlingResource()
    ): CachedTodoRepository {
        val offlineNetworkManager = createMockNetworkManager(isConnected = false)
        return CachedTodoRepository(dao, apiService, offlineNetworkManager, idlingResource)
    }
    
    /**
     * Создает online репозиторий (с сетью)
     */
    fun createOnlineRepository(
        dao: TodoDao = createMockDao(),
        apiService: TodoApiService = createMockApiService(),
        idlingResource: IdlingResourceProvider = createMockIdlingResource()
    ): CachedTodoRepository {
        val onlineNetworkManager = createMockNetworkManager(isConnected = true)
        return CachedTodoRepository(dao, apiService, onlineNetworkManager, idlingResource)
    }
}
