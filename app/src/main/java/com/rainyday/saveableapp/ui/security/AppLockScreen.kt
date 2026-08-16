package com.rainyday.saveableapp.ui.security

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rainyday.saveableapp.data.prefs.PreferencesRepository

@Composable
fun AppLockScreen(preferencesRepository: PreferencesRepository, onUnlocked: () -> Unit) {
    Scaffold { padding ->
        LockGateContent(
            title = "App locked",
            subtitle = "Verify it's you to open the app.",
            preferencesRepository = preferencesRepository,
            onUnlocked = onUnlocked,
            modifier = Modifier.padding(padding)
        )
    }
}
