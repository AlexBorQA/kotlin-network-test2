package com.example.todoapp.testing

import androidx.test.espresso.IdlingResource
import androidx.test.espresso.idling.CountingIdlingResource
import com.example.todoapp.core.idling.IdlingResourceProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Тестовая реализация IdlingResourceProvider для интеграционных тестов
 */
@Singleton
class TestIdlingResourceProvider @Inject constructor() : IdlingResourceProvider {
    
    private val countingIdlingResource = CountingIdlingResource("TodoAppIdlingResource")
    
    override fun increment(reason: String) {
        countingIdlingResource.increment()
        println("IdlingResource incremented: $reason")
    }
    
    override fun decrement(reason: String) {
        if (!countingIdlingResource.isIdleNow) {
            countingIdlingResource.decrement()
            println("IdlingResource decremented: $reason")
        }
    }
    
    override fun isIdleNow(): Boolean = countingIdlingResource.isIdleNow
    
    /**
     * Возвращает IdlingResource для регистрации в Espresso
     */
    fun getIdlingResource(): IdlingResource = countingIdlingResource
}