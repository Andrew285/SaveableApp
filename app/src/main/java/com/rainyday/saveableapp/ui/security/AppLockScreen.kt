package com.rainyday.saveableapp.ui.security

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.prefs.PreferencesRepository

@Composable
fun AppLockScreen(preferencesRepository: PreferencesRepository, onUnlocked: () -> Unit) {
    Scaffold { padding ->
        LockGateContent(
            title = stringResource(R.string.app_lock_title),
            subtitle = stringResource(R.string.app_lock_subtitle),
            preferencesRepository = preferencesRepository,
            onUnlocked = onUnlocked,
            modifier = Modifier.padding(padding)
        )
    }
}
