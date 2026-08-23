package com.rainyday.saveableapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.rainyday.saveableapp.data.scheduling.TaskReminderScheduler
import com.rainyday.saveableapp.di.AppContainer

class SaveableApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
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
