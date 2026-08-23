package com.rainyday.saveableapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rainyday.saveableapp.data.prefs.ThemeMode
import com.rainyday.saveableapp.navigation.AppNavHost
import com.rainyday.saveableapp.ui.security.AppLockScreen
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Composable
fun TodoApp() {
    val preferencesRepository = rememberAppEntryPoint().preferencesRepository()
    val themeMode by preferencesRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val dynamicColor by preferencesRepository.dynamicColorEnabled.collectAsState(initial = true)

    // Read the persisted value synchronously so the very first frame already reflects reality —
    // no blank frame, and no risk of briefly rendering the wrong (default) lock state.
    val appLockEnabled by preferencesRepository.appLockEnabled.collectAsState(
        initial = remember { runBlocking { preferencesRepository.appLockEnabled.first() } }
    )

    var unlocked by remember { mutableStateOf(!appLockEnabled) }

    SaveableAppTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
        if (!unlocked) {
            AppLockScreen(
                preferencesRepository = preferencesRepository,
                onUnlocked = { unlocked = true }
            )
        } else {
            AppNavHost()
        }
    }
}
