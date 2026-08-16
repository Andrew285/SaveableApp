package com.rainyday.saveableapp.platform

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.rainyday.saveableapp.ui.LocalAppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class PinRequest(val title: String, val onSuccess: () -> Unit, val onError: (String) -> Unit)

private var activeRequest by mutableStateOf<PinRequest?>(null)

/**
 * Not cryptographic — this PIN only gates casual on-screen viewing in a browser tab, at roughly
 * the same trust level as the "device credential" fallback the Android biometric prompt already allows.
 */
private fun simpleHash(value: String): String {
    var hash = 0L
    for (c in value) hash = hash * 31 + c.code
    return hash.toString()
}

private class WebPinAuthenticator : AppLockAuthenticator {
    override fun authenticate(title: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        activeRequest = PinRequest(title, onSuccess, onError)
    }
}

@Composable
actual fun rememberAppLockAuthenticator(): AppLockAuthenticator = remember { WebPinAuthenticator() }

@Composable
actual fun AppLockPromptHost() {
    val request = activeRequest ?: return
    val container = LocalAppContainer.current
    var storedHash by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(request) {
        storedHash = container.preferencesRepository.pinHash.first()
        loaded = true
    }
    if (!loaded) return

    val isSetup = storedHash == null
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = {
            activeRequest = null
            request.onError("Cancelled")
        },
        title = { Text(if (isSetup) "Set a PIN for ${request.title}" else request.title) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8); error = null },
                    label = { Text(if (isSetup) "New PIN" else "PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
                if (isSetup) {
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it.filter(Char::isDigit).take(8); error = null },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (isSetup) {
                    when {
                        pin.length < 4 -> error = "PIN must be at least 4 digits"
                        pin != confirmPin -> error = "PINs don't match"
                        else -> scope.launch {
                            container.preferencesRepository.setPinHash(simpleHash(pin))
                            activeRequest = null
                            request.onSuccess()
                        }
                    }
                } else if (simpleHash(pin) == storedHash) {
                    activeRequest = null
                    request.onSuccess()
                } else {
                    error = "Incorrect PIN"
                    pin = ""
                }
            }) { Text(if (isSetup) "Set PIN" else "Unlock") }
        },
        dismissButton = {
            TextButton(onClick = {
                activeRequest = null
                request.onError("Cancelled")
            }) { Text("Cancel") }
        }
    )
}
