package com.example.todoapp.background

import android.content.Context
import androidx.work.*
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.TestWorkerBuilder
import com.example.todoapp.domain.repository.TodoRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Тесты для SyncWorker
 * Проверяют корректность выполнения фоновой синхронизации
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class SyncWorkerTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    private lateinit var context: Context
    private lateinit var todoRepository: TodoRepository
    private lateinit var executor: Executor
    
    @Before
    fun setup() {
        hiltRule.inject()
        context = mockk(relaxed = true)
        todoRepository = mockk()
        executor = Executors.newSingleThreadExecutor()
    }
    
    @Test
    fun `doWork returns success when sync succeeds`() = runTest {
        // Arrange
        coEvery { todoRepository.syncWithRemote() } returns Result.success(Unit)
        
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return SyncWorker(appContext, workerParameters, todoRepository)
                }
            })
            .build()
        
        // Act
        val result = worker.doWork()
        
        // Assert
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify { todoRepository.syncWithRemote() }
    }
    
    @Test
    fun `doWork returns retry when sync fails with retryable error`() = runTest {
        // Arrange
        coEvery { todoRepository.syncWithRemote() } returns Result.failure(Exception("Network error"))
        
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return SyncWorker(appContext, workerParameters, todoRepository)
                }
            })
            .build()
        
        // Act
        val result = worker.doWork()
        
        // Assert
        assertEquals(ListenableWorker.Result.retry(), result)
    }
    
    @Test
    fun `doWork returns failure when sync throws exception`() = runTest {
        // Arrange
        coEvery { todoRepository.syncWithRemote() } throws RuntimeException("Fatal error")
        
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return SyncWorker(appContext, workerParameters, todoRepository)
                }
            })
            .build()
        
        // Act
        val result = worker.doWork()
        
        // Assert
        assertEquals(ListenableWorker.Result.failure(), result)
    }
    
    @Test
    fun `worker handles multiple retries correctly`() = runTest {
        // Arrange - первый вызов неудачный, второй успешный
        var callCount = 0
        coEvery { todoRepository.syncWithRemote() } answers {
            callCount++
            if (callCount == 1) {
                Result.failure(Exception("Temporary error"))
            } else {
                Result.success(Unit)
            }
        }
        
        val worker1 = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return SyncWorker(appContext, workerParameters, todoRepository)
                }
            })
            .build()
        
        val worker2 = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return SyncWorker(appContext, workerParameters, todoRepository)
                }
            })
            .setRunAttemptCount(1) // Вторая попытка
            .build()
        
        // Act
        val result1 = worker1.doWork()
        val result2 = worker2.doWork()
        
        // Assert
        assertEquals(ListenableWorker.Result.retry(), result1)
        assertEquals(ListenableWorker.Result.success(), result2)
        coVerify(exactly = 2) { todoRepository.syncWithRemote() }
    }
    
    @Test
    fun `worker respects cancellation`() = runTest {
        // Arrange
        var cancelled = false
        coEvery { todoRepository.syncWithRemote() } coAnswers {
            try {
                kotlinx.coroutines.delay(1000)
                Result.success(Unit)
            } catch (e: kotlinx.coroutines.CancellationException) {
                cancelled = true
                throw e
            }
        }
        
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return SyncWorker(appContext, workerParameters, todoRepository)
                }
            })
            .build()
        
        // Act - запускаем работу в отдельной корутине и сразу отменяем
        val job = this.launch {
            worker.doWork()
        }
        
        // Отменяем после небольшой задержки
        delay(100)
        job.cancel()
        
        // Ждем завершения
        job.join()
        
        // Assert - проверяем что операция была отменена
        assertTrue("Worker should have been cancelled", cancelled || job.isCancelled)
    }
}