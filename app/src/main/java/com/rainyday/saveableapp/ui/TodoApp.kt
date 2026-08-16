package com.rainyday.saveableapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.rainyday.saveableapp.data.prefs.ThemeMode
import com.rainyday.saveableapp.navigation.AppNavHost
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

@Composable
fun TodoApp() {
    val container = appContainer()
    val themeMode by container.preferencesRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val dynamicColor by container.preferencesRepository.dynamicColorEnabled.collectAsState(initial = true)

    SaveableAppTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
        AppNavHost()
    }
}
