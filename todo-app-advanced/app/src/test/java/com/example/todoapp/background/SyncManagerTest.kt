package com.example.todoapp.background

import androidx.lifecycle.LiveData
import androidx.work.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.mockk.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * Тесты для SyncManager
 * Проверяют корректность планирования и управления задачами синхронизации
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class SyncManagerTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    private lateinit var workManager: WorkManager
    private lateinit var syncManager: SyncManager
    
    @Before
    fun setup() {
        hiltRule.inject()
        workManager = mockk(relaxed = true)
        syncManager = SyncManager(workManager)
    }
    
    @Test
    fun `startPeriodicSync schedules periodic work with correct parameters`() {
        // Arrange
        val workRequestSlot = slot<PeriodicWorkRequest>()
        every { 
            workManager.enqueueUniquePeriodicWork(
                any(),
                any(),
                capture(workRequestSlot)
            )
        } returns mockk()
        
        // Act
        syncManager.startPeriodicSync()
        
        // Assert
        verify {
            workManager.enqueueUniquePeriodicWork(
                "PeriodicSyncWork",
                ExistingPeriodicWorkPolicy.KEEP,
                any<PeriodicWorkRequest>()
            )
        }
        
        val capturedRequest = workRequestSlot.captured
        assert(capturedRequest.tags.contains("sync_work"))
        assert(capturedRequest.workSpec.intervalDuration == TimeUnit.MINUTES.toMillis(15))
    }
    
    @Test
    fun `startPeriodicSync sets network constraint`() {
        // Arrange
        val workRequestSlot = slot<PeriodicWorkRequest>()
        every { 
            workManager.enqueueUniquePeriodicWork(
                any(),
                any(),
                capture(workRequestSlot)
            )
        } returns mockk()
        
        // Act
        syncManager.startPeriodicSync()
        
        // Assert
        val capturedRequest = workRequestSlot.captured
        assert(capturedRequest.workSpec.constraints.requiredNetworkType == NetworkType.CONNECTED)
        assert(capturedRequest.workSpec.constraints.requiresBatteryNotLow())
    }
    
    @Test
    fun `startPeriodicSync sets backoff policy`() {
        // Arrange
        val workRequestSlot = slot<PeriodicWorkRequest>()
        every { 
            workManager.enqueueUniquePeriodicWork(
                any(),
                any(),
                capture(workRequestSlot)
            )
        } returns mockk()
        
        // Act
        syncManager.startPeriodicSync()
        
        // Assert
        val capturedRequest = workRequestSlot.captured
        assert(capturedRequest.workSpec.backoffPolicy == BackoffPolicy.LINEAR)
        assert(capturedRequest.workSpec.backoffDelayDuration == TimeUnit.MINUTES.toMillis(15))
    }
    
    @Test
    fun `stopPeriodicSync cancels periodic work`() {
        // Act
        syncManager.stopPeriodicSync()
        
        // Assert
        verify { workManager.cancelUniqueWork("PeriodicSyncWork") }
    }
    
    @Test
    fun `startOneTimeSync schedules one-time work with network constraint`() {
        // Arrange
        val workRequestSlot = slot<OneTimeWorkRequest>()
        every { 
            workManager.enqueueUniqueWork(
                any(),
                any(),
                capture(workRequestSlot)
            )
        } returns mockk()
        
        // Act
        syncManager.startOneTimeSync()
        
        // Assert
        verify {
            workManager.enqueueUniqueWork(
                "OneTimeSyncWork",
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>()
            )
        }
        
        val capturedRequest = workRequestSlot.captured
        assert(capturedRequest.tags.contains("sync_work"))
        assert(capturedRequest.workSpec.constraints.requiredNetworkType == NetworkType.CONNECTED)
    }
    
    @Test
    fun `startOneTimeSync replaces existing work`() {
        // Act
        syncManager.startOneTimeSync()
        
        // Assert
        verify {
            workManager.enqueueUniqueWork(
                "OneTimeSyncWork",
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>()
            )
        }
    }
    
    @Test
    fun `getSyncStatus returns LiveData of work info`() {
        // Arrange
        val liveData = mockk<LiveData<List<WorkInfo>>>()
        every { workManager.getWorkInfosForUniqueWorkLiveData("PeriodicSyncWork") } returns liveData
        
        // Act
        val result = syncManager.getSyncStatus()
        
        // Assert
        assert(result == liveData)
        verify { workManager.getWorkInfosForUniqueWorkLiveData("PeriodicSyncWork") }
    }
    
    @Test
    fun `cancelAllSync cancels both periodic and one-time work`() {
        // Act
        syncManager.cancelAllSync()
        
        // Assert
        verify { 
            workManager.cancelUniqueWork("PeriodicSyncWork")
            workManager.cancelUniqueWork("OneTimeSyncWork")
        }
    }
    
    @Test
    fun `multiple calls to startPeriodicSync keep existing work`() {
        // Act
        syncManager.startPeriodicSync()
        syncManager.startPeriodicSync()
        
        // Assert
        verify(exactly = 2) {
            workManager.enqueueUniquePeriodicWork(
                "PeriodicSyncWork",
                ExistingPeriodicWorkPolicy.KEEP, // Не заменяет существующую
                any<PeriodicWorkRequest>()
            )
        }
    }
    
    @Test
    fun `multiple calls to startOneTimeSync replace existing work`() {
        // Act
        syncManager.startOneTimeSync()
        syncManager.startOneTimeSync()
        
        // Assert
        verify(exactly = 2) {
            workManager.enqueueUniqueWork(
                "OneTimeSyncWork",
                ExistingWorkPolicy.REPLACE, // Заменяет существующую
                any<OneTimeWorkRequest>()
            )
        }
    }
}