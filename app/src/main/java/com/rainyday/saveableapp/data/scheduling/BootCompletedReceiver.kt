package com.rainyday.saveableapp.data.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rainyday.saveableapp.SaveableApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * AlarmManager alarms don't survive a reboot, so on [Intent.ACTION_BOOT_COMPLETED] this re-schedules
 * every pending task reminder and re-arms the daily auto-backup alarm if it was left enabled.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        val container = (context.applicationContext as SaveableApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.todoRepository.observeAllTasks().first().forEach { taskWithTags ->
                    val task = taskWithTags.task
                    if (!task.isDone && !task.isArchived && task.dueDate != null) {
                        container.taskReminderScheduler.schedule(task.id, task.title, task.dueDate)
                    }
                }
                if (container.preferencesRepository.autoBackupEnabled.first()) {
                    container.autoBackupScheduler.schedule()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
