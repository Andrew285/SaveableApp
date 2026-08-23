package com.rainyday.saveableapp.data.sync

import androidx.work.WorkManager
import com.rainyday.saveableapp.data.auth.FirebaseAuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Starts/stops Firestore sync (pull listeners + the periodic push worker) as the user signs in and
 * out, from wherever that happens to be triggered (currently only the Drive-backup sign-in flow in
 * Settings, but this reacts to the actual [FirebaseAuthRepository] auth state rather than being
 * called explicitly from that screen, so it stays correct if another sign-in entry point is added).
 */
@Singleton
class SyncLifecycleController @Inject constructor(
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val syncPullRepository: SyncPullRepository,
    private val workManager: WorkManager
) {
    /** Call once, e.g. from [android.app.Application.onCreate]. */
    fun start() {
        firebaseAuthRepository.addAuthStateListener { uid ->
            if (uid != null) {
                syncPullRepository.start(uid)
                SyncWorker.schedulePeriodic(workManager)
                // Flushes anything that was written to the outbox while signed out.
                SyncWorker.triggerNow(workManager)
            } else {
                syncPullRepository.stop()
                SyncWorker.cancelPeriodic(workManager)
            }
        }
    }
}
