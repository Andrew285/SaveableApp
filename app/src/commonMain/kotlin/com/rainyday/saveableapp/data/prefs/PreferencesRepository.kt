package com.rainyday.saveableapp.data.prefs

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private object Keys {
    const val THEME_MODE = "theme_mode"
    const val DYNAMIC_COLOR = "dynamic_color"
    const val INFO_LOCK_ENABLED = "info_lock_enabled"
    const val PIN_HASH = "pin_hash"
}

/**
 * Wraps a plain [Settings] instance with app-facing StateFlows instead of relying on
 * multiplatform-settings' ObservableSettings (whose support differs per platform) — every
 * setter here writes through to [settings] and pushes the new value into its StateFlow directly.
 */
class PreferencesRepository(private val settings: Settings) {
    private val _themeMode = MutableStateFlow(
        settings.getStringOrNull(Keys.THEME_MODE)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(settings.getBooleanOrNull(Keys.DYNAMIC_COLOR) ?: true)
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    private val _infoLockEnabled = MutableStateFlow(settings.getBooleanOrNull(Keys.INFO_LOCK_ENABLED) ?: true)
    val infoLockEnabled: StateFlow<Boolean> = _infoLockEnabled.asStateFlow()

    private val _pinHash = MutableStateFlow(settings.getStringOrNull(Keys.PIN_HASH))
    val pinHash: StateFlow<String?> = _pinHash.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        settings.putString(Keys.THEME_MODE, mode.name)
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        settings.putBoolean(Keys.DYNAMIC_COLOR, enabled)
        _dynamicColorEnabled.value = enabled
    }

    fun setInfoLockEnabled(enabled: Boolean) {
        settings.putBoolean(Keys.INFO_LOCK_ENABLED, enabled)
        _infoLockEnabled.value = enabled
    }

    fun setPinHash(hash: String?) {
        if (hash == null) settings.remove(Keys.PIN_HASH) else settings.putString(Keys.PIN_HASH, hash)
        _pinHash.value = hash
    }
}
