package com.rainyday.saveableapp.data.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rainyday.saveableapp.data.prefs.PreferencesRepository
import com.rainyday.saveableapp.data.repository.TodoRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * AlarmManager alarms don't survive a reboot, so on [Intent.ACTION_BOOT_COMPLETED] this re-schedules
 * every pending task reminder and re-arms the daily auto-backup alarm if it was left enabled.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {
    @Inject lateinit var todoRepository: TodoRepository
    @Inject lateinit var taskReminderScheduler: TaskReminderScheduler
    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var autoBackupScheduler: AutoBackupScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                todoRepository.observeAllTasks().first().forEach { taskWithTags ->
                    val task = taskWithTags.task
                    if (!task.isDone && !task.isArchived && task.dueDate != null) {
                        taskReminderScheduler.schedule(task.id, task.title, task.dueDate)
                    }
                }
                if (preferencesRepository.autoBackupEnabled.first()) {
                    autoBackupScheduler.schedule()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
