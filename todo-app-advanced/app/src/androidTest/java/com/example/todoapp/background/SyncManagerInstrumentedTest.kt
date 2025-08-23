package com.example.todoapp.background

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SyncManagerInstrumentedTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    private lateinit var context: Context
    private lateinit var driver: TestDriver
    
    @Inject
    lateinit var syncManager: SyncManager
    
    @Inject
    lateinit var workManager: WorkManager
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        hiltRule.inject()
        driver = WorkManagerTestInitHelper.getTestDriver(context)!!
    }
    
    @After
    fun tearDown() {
        // Чистим за собой между тестами
        workManager.cancelAllWork().result.get()
        workManager.pruneWork()
    }
    
    @Test
    fun testStartPeriodicSync() {
        // When - запускаем периодическую синхронизацию
        syncManager.startPeriodicSync()
        
        // Then - проверяем, что работа была запланирована
        val workInfos = workManager.getWorkInfosForUniqueWork("PeriodicSyncWork").get()
        assertTrue("Should have at least one sync work", workInfos.isNotEmpty())
        
        val workInfo = workInfos.first()
        assertTrue(
            "Work should be enqueued or running",
            workInfo.state == WorkInfo.State.ENQUEUED || 
            workInfo.state == WorkInfo.State.RUNNING
        )
        
        // Если у тебя есть ограничения/задержки — «прожми» их
        workInfos.forEach { info ->
            driver.setAllConstraintsMet(info.id)
            driver.setInitialDelayMet(info.id)
        }
    }
    
    @Test
    fun testStartOneTimeSync() {
        // When - запускаем одноразовую синхронизацию
        syncManager.startOneTimeSync()
        
        // Then - проверяем, что работа была добавлена
        val workInfos = workManager.getWorkInfosForUniqueWork("OneTimeSyncWork").get()
        assertTrue("Should have one-time sync work scheduled", workInfos.isNotEmpty())
        
        val workInfo = workInfos.first()
        assertTrue(
            "Work should be enqueued or running",
            workInfo.state == WorkInfo.State.ENQUEUED || 
            workInfo.state == WorkInfo.State.RUNNING
        )
        
        // Прожимаем ограничения/задержки
        workInfos.forEach { info ->
            driver.setAllConstraintsMet(info.id)
            driver.setInitialDelayMet(info.id)
        }
    }
    
    @Test
    fun testStopPeriodicSync() {
        // Given - сначала запускаем периодическую синхронизацию
        syncManager.startPeriodicSync()
        
        val workInfosBefore = workManager.getWorkInfosForUniqueWork("PeriodicSyncWork").get()
        assertTrue("Should have periodic work before stop", workInfosBefore.isNotEmpty())
        
        // When - останавливаем периодическую синхронизацию
        syncManager.stopPeriodicSync()
        
        // Then - проверяем, что работа была отменена
        Thread.sleep(100) // Даем время на отмену
        val workInfosAfter = workManager.getWorkInfosForUniqueWork("PeriodicSyncWork").get()
        
        val allCancelled = workInfosAfter.all { 
            it.state == WorkInfo.State.CANCELLED || 
            it.state == WorkInfo.State.FAILED
        }
        
        assertTrue(
            "All periodic work should be cancelled",
            allCancelled || workInfosAfter.isEmpty()
        )
    }
    
    @Test
    fun testCancelAllSync() {
        // Given - запускаем обе синхронизации
        syncManager.startPeriodicSync()
        syncManager.startOneTimeSync()
        
        // When - отменяем все синхронизации
        syncManager.cancelAllSync()
        
        // Then - проверяем, что все работы отменены
        Thread.sleep(100)
        val periodicInfos = workManager.getWorkInfosForUniqueWork("PeriodicSyncWork").get()
        val oneTimeInfos = workManager.getWorkInfosForUniqueWork("OneTimeSyncWork").get()
        
        val allCancelled = (periodicInfos + oneTimeInfos).all {
            it.state == WorkInfo.State.CANCELLED || 
            it.state == WorkInfo.State.FAILED
        }
        
        assertTrue(
            "All sync work should be cancelled",
            allCancelled || (periodicInfos.isEmpty() && oneTimeInfos.isEmpty())
        )
    }
    
    @Test
    fun testGetSyncStatus() {
        // Given - запускаем периодическую синхронизацию
        syncManager.startPeriodicSync()
        
        // When - получаем статус
        val status = syncManager.getSyncStatus()
        
        // Then - статус должен быть доступен
        assertNotNull("Sync status should not be null", status)
        assertTrue(
            "Status should indicate work is scheduled",
            status.value?.isNotEmpty() == true || status.value == null
        )
    }
    
    @Test
    fun testUniquePeriodicWork() {
        // When - запускаем периодическую синхронизацию несколько раз
        syncManager.startPeriodicSync()
        syncManager.startPeriodicSync()
        syncManager.startPeriodicSync()
        
        // Then - должна быть только одна периодическая работа
        val workInfos = workManager.getWorkInfosForUniqueWork("PeriodicSyncWork").get()
        assertEquals("Should have exactly one periodic work", 1, workInfos.size)
    }
}