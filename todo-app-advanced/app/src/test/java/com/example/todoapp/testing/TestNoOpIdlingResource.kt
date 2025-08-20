package com.example.todoapp.testing

import com.example.todoapp.core.idling.IdlingResourceProvider

/**
 * Тестовая реализация IdlingResource для unit тестов.
 * В отличие от mock, позволяет проверять количество вызовов increment/decrement
 */
class TestNoOpIdlingResource : IdlingResourceProvider {
    
    var incrementCount = 0
        private set
    
    var decrementCount = 0
        private set
    
    val activeOperations: Int
        get() = incrementCount - decrementCount
    
    override fun increment(reason: String) {
        incrementCount++
    }
    
    override fun decrement(reason: String) {
        decrementCount++
    }
    
    override fun isIdleNow(): Boolean = activeOperations == 0
    
    fun reset() {
        incrementCount = 0
        decrementCount = 0
    }
}