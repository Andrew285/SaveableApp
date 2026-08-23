package com.rainyday.saveableapp.data.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

private const val REQUEST_CODE = 9001

/** Schedules (or cancels) a once-a-day inexact repeating alarm that triggers [AutoBackupReceiver]. */
class AutoBackupScheduler(private val context: Context) {
    private val alarmManager get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule() {
        val pendingIntent = pendingIntent()
        val firstTrigger = System.currentTimeMillis() + AlarmManager.INTERVAL_DAY
        runCatching {
            alarmManager.setInexactRepeating(AlarmManager.RTC, firstTrigger, AlarmManager.INTERVAL_DAY, pendingIntent)
        }
    }

    fun cancel() {
        alarmManager.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, AutoBackupReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
