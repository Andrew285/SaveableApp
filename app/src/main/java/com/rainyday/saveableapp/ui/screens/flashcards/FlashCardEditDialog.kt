package com.rainyday.saveableapp.ui.screens.flashcards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

@Composable
fun FlashCardEditDialog(
    title: String,
    initialFront: String = "",
    initialBack: String = "",
    onDismiss: () -> Unit,
    onConfirm: (front: String, back: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var front by remember { mutableStateOf(initialFront) }
    var back by remember { mutableStateOf(initialBack) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text(stringResource(R.string.flashcards_card_front_label)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text(stringResource(R.string.flashcards_card_back_label)) },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.d12)
                )
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.padding(top = Dimens.d8)
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = front.isNotBlank() && back.isNotBlank(),
                onClick = { onConfirm(front.trim(), back.trim()) }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun FlashCardEditDialogPreview() {
    SaveableAppTheme {
        FlashCardEditDialog(
            title = "Edit card",
            initialFront = "Casa",
            initialBack = "House",
            onDismiss = {},
            onConfirm = { _, _ -> },
            onDelete = {}
        )
    }
}
