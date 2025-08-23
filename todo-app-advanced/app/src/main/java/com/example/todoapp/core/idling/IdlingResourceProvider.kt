package com.example.todoapp.core.idling

/**
 * Интерфейс для предоставления IdlingResource
 * В продакшн коде будет пустая реализация,
 * в тестах - реальная с CountingIdlingResource
 */
interface IdlingResourceProvider {
    fun increment(reason: String = "")
    fun decrement(reason: String = "")
    fun isIdleNow(): Boolean
}

/**
 * Продакшн реализация - ничего не делает
 */
class NoOpIdlingResourceProvider : IdlingResourceProvider {
    override fun increment(reason: String) {
        // No-op in production
    }
    
    override fun decrement(reason: String) {
        // No-op in production
    }
    
    override fun isIdleNow(): Boolean = true
}