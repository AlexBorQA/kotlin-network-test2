package com.example.todoapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.example.todoapp.background.SyncManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Broadcast Receiver для реагирования на изменения состояния сети
 * Запускает синхронизацию при появлении интернет-соединения
 */
@AndroidEntryPoint
class NetworkReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var syncManager: SyncManager
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            if (isNetworkAvailable(context)) {
                // Сеть доступна - запускаем синхронизацию
                syncManager.startOneTimeSync()
            }
        }
    }
    
    @Suppress("DEPRECATION")
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) 
                as ConnectivityManager
                
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || 
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
}