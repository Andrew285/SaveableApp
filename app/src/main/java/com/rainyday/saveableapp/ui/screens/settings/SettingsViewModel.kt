package com.rainyday.saveableapp.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.rainyday.saveableapp.data.drive.DriveBackupRepository
import com.rainyday.saveableapp.data.prefs.PreferencesRepository
import com.rainyday.saveableapp.data.prefs.ThemeMode
import com.rainyday.saveableapp.data.repository.BackupRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val backupRepository: BackupRepository,
    val driveBackupRepository: DriveBackupRepository
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

    val groqApiKey: StateFlow<String?> = preferencesRepository.groqApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setGroqApiKey(key: String) {
        viewModelScope.launch { preferencesRepository.setGroqApiKey(key) }
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
