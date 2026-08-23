package com.rainyday.saveableapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.rainyday.saveableapp.data.scheduling.TaskReminderScheduler
import com.rainyday.saveableapp.data.sync.SyncLifecycleController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SaveableApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncLifecycleController: SyncLifecycleController

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        syncLifecycleController.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            TaskReminderScheduler.CHANNEL_ID,
            "Task reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Reminders for tasks with a due date" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
