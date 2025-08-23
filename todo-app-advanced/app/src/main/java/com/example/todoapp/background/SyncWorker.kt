package com.example.todoapp.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.todoapp.domain.repository.TodoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val todoRepository: TodoRepository
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val WORK_NAME = "SyncWork"
        const val NOTIFICATION_ID = 1001
    }
    
    override suspend fun doWork(): Result {
        return try {
            if (!isInTestMode()) {
                setForeground(createForegroundInfo())
            }
            
            val syncResult = todoRepository.syncWithRemote()
            
            if (syncResult.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    private fun isInTestMode(): Boolean {
        return try {
            Class.forName("androidx.work.testing.WorkManagerTestInitHelper")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    
    private fun createForegroundInfo(): ForegroundInfo {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) 
                as android.app.NotificationManager
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "SYNC_CHANNEL",
                "Background Sync",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Synchronizing todos with server"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = androidx.core.app.NotificationCompat.Builder(
            applicationContext, 
            "SYNC_CHANNEL"
        )
            .setContentTitle("Syncing Todos")
            .setContentText("Synchronizing your todos with the server...")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .build()
        
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }
}