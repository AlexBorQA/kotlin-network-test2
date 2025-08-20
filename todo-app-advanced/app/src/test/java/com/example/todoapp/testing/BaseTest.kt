package com.example.todoapp.testing

import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Before

/**
 * Базовый класс для всех тестов
 * Содержит общую логику настройки и очистки
 */
abstract class BaseTest {
    
    companion object {
        val DEFAULT_TIMEOUT = 5.seconds
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
    protected fun runBlockingTest(block: suspend () -> Unit) = runTest(timeout = DEFAULT_TIMEOUT) {
        block()
    }
}
