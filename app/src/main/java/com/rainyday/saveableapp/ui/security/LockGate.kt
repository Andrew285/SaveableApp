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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.prefs.PreferencesRepository
import com.rainyday.saveableapp.ui.theme.AppAlpha
import com.rainyday.saveableapp.ui.theme.Dimens
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

    val incorrectPinMessage = stringResource(R.string.lock_gate_incorrect_pin)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.d32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(Dimens.d56),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.a60)
        )
        Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Dimens.d16))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Dimens.d4)
        )
        if (error != null) {
            Text(
                text = error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Dimens.d8)
            )
        }

        if (showPinEntry) {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                label = { Text(stringResource(R.string.lock_gate_pin_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.padding(top = Dimens.d20)
            )
            Button(
                onClick = {
                    scope.launch {
                        if (preferencesRepository.verifyAppPin(pin)) {
                            onUnlocked()
                        } else {
                            error = incorrectPinMessage
                            pin = ""
                        }
                    }
                },
                enabled = pin.length in 4..6,
                modifier = Modifier.padding(top = Dimens.d12)
            ) { Text(stringResource(R.string.lock_gate_unlock)) }
            TextButton(onClick = { showPinEntry = false; error = null; pin = "" }) {
                Text(stringResource(R.string.lock_gate_use_biometrics))
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
                modifier = Modifier.padding(top = Dimens.d20)
            ) { Text(stringResource(R.string.lock_gate_unlock)) }
            if (pinIsSet) {
                TextButton(onClick = { showPinEntry = true; error = null }) {
                    Text(stringResource(R.string.lock_gate_use_pin))
                }
            }
        }
    }
}
