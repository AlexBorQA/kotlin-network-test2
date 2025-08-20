package com.example.todoapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todoapp.background.SyncManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Broadcast Receiver для реагирования на загрузку устройства
 * Автоматически запускает фоновую синхронизацию после перезагрузки
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var syncManager: SyncManager
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Запускаем периодическую синхронизацию после загрузки устройства
            syncManager.startPeriodicSync()
        }
    }
}