package com.rainyday.saveableapp.platform

import androidx.compose.runtime.Composable

/** Gate that must be satisfied before sensitive content (the Info section) is shown. */
interface AppLockAuthenticator {
    fun authenticate(title: String, onSuccess: () -> Unit, onError: (String) -> Unit)
}

/** Android: system BiometricPrompt (weak biometric or device credential). Web: an app-level PIN. */
@Composable
expect fun rememberAppLockAuthenticator(): AppLockAuthenticator

/**
 * Mounted once near the app root. Android has nothing to render (the system prompt is an overlay);
 * web renders the PIN entry/setup dialog here when [rememberAppLockAuthenticator] requests it.
 */
@Composable
expect fun AppLockPromptHost()
