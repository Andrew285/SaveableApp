package com.rainyday.saveableapp.data.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rainyday.saveableapp.SaveableApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires once a day (while auto-backup is enabled) and silently backs up to Drive if signed in. */
class AutoBackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val container = (context.applicationContext as SaveableApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val account = container.driveBackupRepository.getSignedInAccount()
                if (account != null) {
                    container.driveBackupRepository.backup(account)
                }
            } catch (e: Exception) {
                // Best-effort background job — a failed auto-backup just tries again tomorrow.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
