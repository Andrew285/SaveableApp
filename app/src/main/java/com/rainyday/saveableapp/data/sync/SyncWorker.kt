package com.rainyday.saveableapp.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rainyday.saveableapp.data.auth.FirebaseAuthRepository
import com.rainyday.saveableapp.data.prefs.PreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Drains the local sync outbox into Firestore. A no-op (returns success immediately) when signed out. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pushRepository: SyncPushRepository,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val preferencesRepository: PreferencesRepository
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        pushRepository.pushPending()
    }.fold(
        onSuccess = {
            // Only a meaningful "last synced" moment if we were actually signed in to check —
            // pushPending() no-ops (returns 0) when signed out, which isn't a sync confirmation.
            if (firebaseAuthRepository.currentUser != null) {
                preferencesRepository.setLastSyncAt(System.currentTimeMillis())
            }
            Result.success()
        },
        onFailure = { Result.retry() }
    )

    companion object {
        private const val UNIQUE_PERIODIC_NAME = "sync_outbox_periodic"
        private const val UNIQUE_ONE_TIME_NAME = "sync_outbox_now"

        private val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Call once (e.g. on sign-in) to keep the outbox draining in the background even if the app isn't open. */
        fun schedulePeriodic(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(networkConstraints)
                .build()
            workManager.enqueueUniquePeriodicWork(UNIQUE_PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun cancelPeriodic(workManager: WorkManager) {
            workManager.cancelUniqueWork(UNIQUE_PERIODIC_NAME)
        }

        /** Call right after a local write so changes push promptly instead of waiting for the periodic run. */
        fun triggerNow(workManager: WorkManager) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networkConstraints)
                .build()
            // KEEP, not REPLACE: pushPending() drains everything queued when it runs, so a trigger
            // that arrives while one is already pending/running needs nothing more than that existing
            // run to eventually pick up its entries too — no need to stack up redundant requests.
            workManager.enqueueUniqueWork(UNIQUE_ONE_TIME_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
