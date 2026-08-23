package com.rainyday.saveableapp.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.rainyday.saveableapp.data.auth.FirebaseAuthRepository
import com.rainyday.saveableapp.data.drive.DriveBackupRepository
import com.rainyday.saveableapp.data.prefs.PreferencesRepository
import com.rainyday.saveableapp.data.prefs.ThemeMode
import com.rainyday.saveableapp.data.repository.BackupRepository
import com.rainyday.saveableapp.data.scheduling.AutoBackupScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val backupRepository: BackupRepository,
    val driveBackupRepository: DriveBackupRepository,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val autoBackupScheduler: AutoBackupScheduler
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val dynamicColorEnabled: StateFlow<Boolean> = preferencesRepository.dynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val infoLockEnabled: StateFlow<Boolean> = preferencesRepository.infoLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val appLockEnabled: StateFlow<Boolean> = preferencesRepository.appLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val appPinIsSet: StateFlow<Boolean> = preferencesRepository.appPinIsSet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val driveLastBackupAt: StateFlow<Long?> = preferencesRepository.driveLastBackupAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val autoBackupEnabled: StateFlow<Boolean> = preferencesRepository.autoBackupEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lastSyncAt: StateFlow<Long?> = preferencesRepository.lastSyncAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Links a Google sign-in (already used for Drive backup) to a Firebase Auth session, so the
     * AI-parsing Cloud Function can identify this user and enforce their call quota. Safe to call
     * every time the user (re)signs in with Google.
     */
    fun signInToFirebase(account: GoogleSignInAccount) {
        viewModelScope.launch { firebaseAuthRepository.signInWithGoogleAccount(account) }
    }

    /** Toggles the daily background Drive backup and (de)schedules the alarm that drives it. */
    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoBackupEnabled(enabled) }
        if (enabled) autoBackupScheduler.schedule() else autoBackupScheduler.cancel()
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDynamicColorEnabled(enabled) }
    }

    fun setInfoLockEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setInfoLockEnabled(enabled) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAppLockEnabled(enabled) }
    }

    fun setAppPin(pin: String) {
        viewModelScope.launch { preferencesRepository.setAppPin(pin) }
    }

    fun clearAppPin() {
        viewModelScope.launch { preferencesRepository.clearAppPin() }
    }

    fun exportData(context: Context, uri: Uri, onDone: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            val success = runCatching { backupRepository.exportToUri(context, uri) }.isSuccess
            onDone(success)
        }
    }

    fun importData(context: Context, uri: Uri, onDone: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            val success = runCatching { backupRepository.importFromUri(context, uri) }.isSuccess
            onDone(success)
        }
    }

    fun backupToDrive(account: GoogleSignInAccount, onDone: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            val result = driveBackupRepository.backup(account)
            onDone(result.isSuccess, result.exceptionOrNull()?.message)
        }
    }

    fun restoreFromDrive(account: GoogleSignInAccount, onDone: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            val result = driveBackupRepository.restore(account)
            onDone(result.isSuccess, result.exceptionOrNull()?.message)
        }
    }
}
