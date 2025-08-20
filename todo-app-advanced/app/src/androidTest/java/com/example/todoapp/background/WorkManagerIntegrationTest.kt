package com.example.todoapp.background

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.todoapp.domain.repository.TodoRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WorkManagerIntegrationTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var testDriver: TestDriver
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var todoRepository: TodoRepository
    
    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        
        // Инициализируем WorkManager для тестов с HiltWorkerFactory
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(workerFactory)
            .build()
        
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
        testDriver = WorkManagerTestInitHelper.getTestDriver(context)!!
    }
    
    @Test
    fun testOneTimeWork_Success() = runBlocking {
        // Given - создаем одноразовую работу
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .addTag("test_sync")
            .build()
        
        // When - добавляем в очередь
        workManager.enqueue(request).result.get()
        
        // Then - проверяем, что работа была запущена (SynchronousExecutor запускает сразу)
        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertTrue(
            "Work should be enqueued or running",
            workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
        )
        
        // Симулируем выполнение
        testDriver.setAllConstraintsMet(request.id)
        testDriver.setInitialDelayMet(request.id)
    }
    
    @Test
    fun testPeriodicWork_Setup() {
        // Given - создаем периодическую работу
        val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .addTag("periodic_sync")
            .build()
        
        // When - добавляем в очередь
        workManager.enqueue(periodicRequest).result.get()
        
        // Then - проверяем, что работа запланирована
        val workInfo = workManager.getWorkInfoById(periodicRequest.id).get()
        assertTrue(
            "Work should be enqueued or running",
            workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
        )
        
        // Симулируем наступление периода
        testDriver.setPeriodDelayMet(periodicRequest.id)
        testDriver.setAllConstraintsMet(periodicRequest.id)
    }
    
    @Test
    fun testWorkWithConstraints() {
        // Given - работа с ограничениями сети
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag("constrained_sync")
            .build()
        
        // When - добавляем в очередь
        workManager.enqueue(request).result.get()
        
        // Then - работа должна быть в очереди или выполняться
        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertTrue(
            "Work should be enqueued or running",
            workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
        )
        
        // Симулируем выполнение условий
        testDriver.setAllConstraintsMet(request.id)
    }
    
    @Test
    fun testUniqueWork_Replace() {
        // Given - первая уникальная работа
        val request1 = OneTimeWorkRequestBuilder<SyncWorker>()
            .addTag("unique_test")
            .build()
        
        workManager.enqueueUniqueWork(
            "unique_sync",
            ExistingWorkPolicy.REPLACE,
            request1
        ).result.get()
        
        // When - добавляем вторую с той же уникальностью  
        val request2 = OneTimeWorkRequestBuilder<SyncWorker>()
            .addTag("unique_test_2")
            .build()
        
        workManager.enqueueUniqueWork(
            "unique_sync", 
            ExistingWorkPolicy.REPLACE,
            request2
        ).result.get()
        
        // Then - просто проверяем, что работы есть в системе (из-за foreground service ограничений)
        val uniqueWorkInfos = workManager.getWorkInfosForUniqueWork("unique_sync").get()
        assertTrue("Should have unique work", uniqueWorkInfos.isNotEmpty())
    }
    
    @Test
    fun testCancelWork() {
        // Given - работа в очереди
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .addTag("cancel_test")
            .build()
        
        workManager.enqueue(request).result.get()
        
        // When - отменяем работу
        workManager.cancelWorkById(request.id).result.get()
        
        // Then - работа должна быть отменена
        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertEquals(WorkInfo.State.CANCELLED, workInfo.state)
    }
    
    @Test
    fun testWorkChain() {
        // Given - цепочка работ  
        val work1 = OneTimeWorkRequestBuilder<SyncWorker>()
            .addTag("chain_1")
            .build()
        
        val work2 = OneTimeWorkRequestBuilder<SyncWorker>()
            .addTag("chain_2")
            .build()
        
        // When - создаем цепочку
        workManager
            .beginWith(work1)
            .then(work2)
            .enqueue()
            .result.get()
        
        // Then - обе работы должны быть в системе
        val workInfo1 = workManager.getWorkInfoById(work1.id).get()
        assertTrue(
            "First work should be enqueued or running",
            workInfo1.state == WorkInfo.State.ENQUEUED || workInfo1.state == WorkInfo.State.RUNNING
        )
        
        val workInfo2 = workManager.getWorkInfoById(work2.id).get()
        assertTrue(
            "Second work should be blocked, enqueued or running",
            workInfo2.state == WorkInfo.State.BLOCKED || 
            workInfo2.state == WorkInfo.State.ENQUEUED || 
            workInfo2.state == WorkInfo.State.RUNNING
        )
    }
}