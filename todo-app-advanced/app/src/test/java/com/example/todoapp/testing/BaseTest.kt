package com.example.todoapp.testing

import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

/**
 * Базовый класс для всех тестов
 * Содержит общую логику настройки и очистки
 */
abstract class BaseTest {
    
    companion object {
        const val DEFAULT_TIMEOUT = 5000L
    }
    
    @Before
    open fun baseSetup() {
        // Общая настройка для всех тестов
    }
    
    @After
    open fun baseTearDown() {
        // Общая очистка для всех тестов
    }
    
    /**
     * Обертка для runTest с таймаутом
     */
    protected fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit) = runTest(timeout = DEFAULT_TIMEOUT) {
        block()
    }
}
