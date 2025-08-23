package com.example.todoapp.testing

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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
        // Мокаем Android Log для предотвращения RuntimeException в тестах
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<Throwable>()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0
    }
    
    @After
    open fun baseTearDown() {
        // Очищаем мокинг Android Log
        unmockkStatic(Log::class)
    }
    
    /**
     * Обертка для runTest с таймаутом
     */
    protected fun runBlockingTest(block: suspend () -> Unit) = runTest(timeout = DEFAULT_TIMEOUT) {
        block()
    }
}
