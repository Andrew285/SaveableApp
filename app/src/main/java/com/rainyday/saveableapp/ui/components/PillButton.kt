package com.rainyday.saveableapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rainyday.saveableapp.ui.theme.PillShape

/** Outlined mint pill — primary but non-destructive actions ("Add List", "Save Task"). */
@Composable
fun PillButtonPrimary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier = modifier
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Outlined red pill — destructive actions ("Delete Task"). */
@Composable
fun PillButtonDanger(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier = modifier
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Solid mint pill — the one prominent call-to-action on a screen. */
@Composable
fun PillButtonFilled(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier = modifier
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
