package com.rainyday.saveableapp.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.rainyday.saveableapp.data.prefs.PreferencesRepository
import kotlinx.coroutines.launch

/** Biometric-first unlock UI with a numeric PIN fallback, shared by the app-launch and Info-section locks. */
@Composable
fun LockGateContent(
    title: String,
    subtitle: String,
    preferencesRepository: PreferencesRepository,
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authenticator = rememberBiometricAuthenticator()
    val pinIsSet by preferencesRepository.appPinIsSet.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    var showPinEntry by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (error != null) {
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (showPinEntry) {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                label = { Text("PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.padding(top = 20.dp)
            )
            Button(
                onClick = {
                    scope.launch {
                        if (preferencesRepository.verifyAppPin(pin)) {
                            onUnlocked()
                        } else {
                            error = "Incorrect PIN"
                            pin = ""
                        }
                    }
                },
                enabled = pin.length in 4..6,
                modifier = Modifier.padding(top = 12.dp)
            ) { Text("Unlock") }
            TextButton(onClick = { showPinEntry = false; error = null; pin = "" }) {
                Text("Use biometrics instead")
            }
        } else {
            Button(
                onClick = {
                    authenticator.authenticate(
                        title = title,
                        onSuccess = onUnlocked,
                        onError = { message -> error = message }
                    )
                },
                modifier = Modifier.padding(top = 20.dp)
            ) { Text("Unlock") }
            if (pinIsSet) {
                TextButton(onClick = { showPinEntry = true; error = null }) {
                    Text("Use PIN instead")
                }
            }
        }
    }
}
