package com.example.todoapp.receiver

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.todoapp.background.SyncManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BootReceiverInstrumentedTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var bootReceiver: BootReceiver
    private lateinit var mockSyncManager: SyncManager

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        
        bootReceiver = BootReceiver()
        
        mockSyncManager = mockk(relaxed = true)
        val syncManagerField = BootReceiver::class.java.getDeclaredField("syncManager")
        syncManagerField.isAccessible = true
        syncManagerField.set(bootReceiver, mockSyncManager)
    }

    @Test
    fun onReceive_startsPeriodicSync_afterBootCompleted() {
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        
        bootReceiver.onReceive(context, intent)
        
        verify { mockSyncManager.setupPeriodicSync() }
        verify(exactly = 0) { mockSyncManager.startOneTimeSync() }
    }

    @Test
    fun onReceive_ignoresNonBootActions() {
        val intent = Intent(Intent.ACTION_SCREEN_ON)
        
        bootReceiver.onReceive(context, intent)
        
        verify(exactly = 0) { mockSyncManager.setupPeriodicSync() }
        verify(exactly = 0) { mockSyncManager.startOneTimeSync() }
    }

    @Test
    fun onReceive_handlesNullAction() {
        val intent = Intent()
        
        bootReceiver.onReceive(context, intent)
        
        verify(exactly = 0) { mockSyncManager.setupPeriodicSync() }
    }

    @Test
    fun multipleBootEvents_triggerSync() {
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        
        bootReceiver.onReceive(context, intent)
        bootReceiver.onReceive(context, intent)
        bootReceiver.onReceive(context, intent)
        
        verify(atLeast = 1) { mockSyncManager.setupPeriodicSync() }
    }
}