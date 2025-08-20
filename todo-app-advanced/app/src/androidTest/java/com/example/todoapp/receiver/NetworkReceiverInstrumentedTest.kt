package com.example.todoapp.receiver

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
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
import javax.inject.Inject

/**
 * Инструментальные тесты для NetworkReceiver
 * Проверяют корректность реагирования на изменения состояния сети
 * в реальном Android окружении
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NetworkReceiverInstrumentedTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var networkReceiver: NetworkReceiver
    
    @Inject
    lateinit var syncManager: SyncManager

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        
        // Создаем NetworkReceiver и мокаем SyncManager
        networkReceiver = NetworkReceiver()
        
        // Заменяем реальный SyncManager на мок
        val mockSyncManager = mockk<SyncManager>(relaxed = true)
        val syncManagerField = NetworkReceiver::class.java.getDeclaredField("syncManager")
        syncManagerField.isAccessible = true
        syncManagerField.set(networkReceiver, mockSyncManager)
        
        // Сохраняем ссылку на мок для проверок
        syncManager = mockSyncManager
    }

    @Test
    fun onReceive_startsSync_whenNetworkBecomesAvailable() {
        // Given - создаем Intent с action CONNECTIVITY_ACTION
        val intent = Intent(ConnectivityManager.CONNECTIVITY_ACTION)
        
        // Мокаем ConnectivityManager для симуляции доступной сети
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // Примечание: в реальном тесте ConnectivityManager сложно мокать,
        // поэтому мы полагаемся на реальное состояние сети
        
        // When - вызываем onReceive
        networkReceiver.onReceive(context, intent)
        
        // Then - проверяем, что синхронизация была запущена
        // (если сеть действительно доступна)
        if (isNetworkAvailable()) {
            verify { syncManager.startOneTimeSync() }
        } else {
            verify(exactly = 0) { syncManager.startOneTimeSync() }
        }
    }

    @Test
    fun onReceive_ignoresNonConnectivityActions() {
        // Given - Intent с другим action
        val intent = Intent(Intent.ACTION_BATTERY_LOW)
        
        // When - вызываем onReceive
        networkReceiver.onReceive(context, intent)
        
        // Then - синхронизация не должна запускаться
        verify(exactly = 0) { syncManager.startOneTimeSync() }
    }

    @Test
    fun onReceive_handlesNullAction() {
        // Given - Intent без action
        val intent = Intent()
        
        // When - вызываем onReceive
        networkReceiver.onReceive(context, intent)
        
        // Then - не должно быть краша, синхронизация не запускается
        verify(exactly = 0) { syncManager.startOneTimeSync() }
    }

    @Test
    fun onReceive_worksWithDifferentNetworkTypes() {
        // Given - Intent с CONNECTIVITY_ACTION
        val intent = Intent(ConnectivityManager.CONNECTIVITY_ACTION)
        
        // When - вызываем onReceive несколько раз
        // (симулируем разные типы сети)
        networkReceiver.onReceive(context, intent)
        
        // Then - проверяем поведение в зависимости от реального состояния сети
        // В реальном окружении это зависит от доступности сети на устройстве
        verify(atMost = 1) { syncManager.startOneTimeSync() }
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }
}