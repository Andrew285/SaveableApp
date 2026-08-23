package com.rainyday.saveableapp.data.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rainyday.saveableapp.data.drive.DriveBackupRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires once a day (while auto-backup is enabled) and silently backs up to Drive if signed in. */
@AndroidEntryPoint
class AutoBackupReceiver : BroadcastReceiver() {
    @Inject lateinit var driveBackupRepository: DriveBackupRepository

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val account = driveBackupRepository.getSignedInAccount()
                if (account != null) {
                    driveBackupRepository.backup(account)
                }
            } catch (e: Exception) {
                // Best-effort background job — a failed auto-backup just tries again tomorrow.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
