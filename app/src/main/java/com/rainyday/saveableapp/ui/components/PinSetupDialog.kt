package com.rainyday.saveableapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (pin: String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val pinValid = pin.length in 4..6
    val mismatch = confirmPin.isNotEmpty() && confirmPin != pin

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pin_setup_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.pin_setup_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                    label = { Text(stringResource(R.string.pin_setup_new_pin_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.d12)
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
                    label = { Text(stringResource(R.string.pin_setup_confirm_pin_label)) },
                    singleLine = true,
                    isError = mismatch,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.d8)
                )
                if (mismatch) {
                    Text(
                        text = stringResource(R.string.pin_setup_mismatch),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = Dimens.d4)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = pinValid && pin == confirmPin,
                onClick = { onConfirm(pin) }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun PinSetupDialogPreview() {
    SaveableAppTheme {
        PinSetupDialog(onDismiss = {}, onConfirm = {})
    }
}
