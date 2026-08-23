package com.rainyday.saveableapp.data.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

private const val REMINDER_HOUR = 9

/**
 * Schedules (or cancels) a single inexact alarm per task so a notification fires around [REMINDER_HOUR]
 * on the task's due date. Uses [AlarmManager.setAndAllowWhileIdle] — no special "exact alarms" permission
 * needed, at the cost of the OS being allowed to delay delivery by a few minutes under Doze.
 */
class TaskReminderScheduler(private val context: Context) {
    private val alarmManager get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Schedules a reminder for [dueDate], or cancels any existing one if [dueDate] is null or already past. */
    fun schedule(taskId: Long, title: String, dueDate: Long?) {
        if (dueDate == null) {
            cancel(taskId)
            return
        }
        val triggerAt = reminderTimeMillis(dueDate)
        if (triggerAt <= System.currentTimeMillis()) {
            cancel(taskId)
            return
        }
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching { alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent) }
    }

    fun cancel(taskId: Long) {
        val intent = Intent(context, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
        const val CHANNEL_ID = "task_reminders"

        /** [REMINDER_HOUR]:00 local time on the day of [dueDateMillis]. */
        fun reminderTimeMillis(dueDateMillis: Long): Long {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = dueDateMillis
                set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis
        }
    }
}
