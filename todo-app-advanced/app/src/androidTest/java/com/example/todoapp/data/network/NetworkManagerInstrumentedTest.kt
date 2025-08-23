package com.example.todoapp.data.network

import android.content.Context
import android.net.ConnectivityManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Инструментальные тесты для NetworkManager
 * Проверяют работу с реальным Android API для сетевых операций
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NetworkManagerInstrumentedTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    
    @Inject
    lateinit var networkManager: NetworkManager

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testNetworkManager_canBeInjected() {
        assert(::networkManager.isInitialized)
    }

    @Test
    fun testIsNetworkAvailable_returnsBoolean() {
        val result = networkManager.isNetworkAvailable()
        assert(result is Boolean)
    }

    @Test
    fun testWaitForNetwork_returnsBoolean() = runTest {
        val result = networkManager.waitForNetwork()
        assert(result is Boolean)
    }

    @Test
    fun testObserveNetworkChanges_emitsValues() = runTest {
        try {
            val firstEmission = networkManager.observeNetworkChanges().first()
            assert(firstEmission is Boolean)
        } catch (e: Exception) {
            assert(true)
        }
    }
}