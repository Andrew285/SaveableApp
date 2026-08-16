package com.rainyday.saveableapp.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainyday.saveableapp.data.prefs.PreferencesRepository
import com.rainyday.saveableapp.data.prefs.ThemeMode
import com.rainyday.saveableapp.data.repository.BackupRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val dynamicColorEnabled: StateFlow<Boolean> = preferencesRepository.dynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val infoLockEnabled: StateFlow<Boolean> = preferencesRepository.infoLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDynamicColorEnabled(enabled) }
    }

    fun setInfoLockEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setInfoLockEnabled(enabled) }
    }

    fun exportData(onResult: (content: String?) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { backupRepository.exportJson() }.getOrNull())
        }
    }

    fun importData(content: String, onDone: (success: Boolean) -> Unit) {
        viewModelScope.launch {
            onDone(runCatching { backupRepository.importJson(content) }.isSuccess)
        }
    }
}
