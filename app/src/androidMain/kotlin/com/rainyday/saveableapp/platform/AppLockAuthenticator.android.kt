package com.rainyday.saveableapp.platform

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val ALLOWED_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL

private class BiometricAppLockAuthenticator(private val activity: FragmentActivity) : AppLockAuthenticator {
    override fun authenticate(title: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            }
        )
        prompt.authenticate(promptInfo)
    }
}

@Composable
actual fun rememberAppLockAuthenticator(): AppLockAuthenticator {
    val context = LocalContext.current
    return remember { BiometricAppLockAuthenticator(context as FragmentActivity) }
}

@Composable
actual fun AppLockPromptHost() {
    // No-op: BiometricPrompt renders as a system overlay, not composed UI.
}
