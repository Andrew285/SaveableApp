package com.rainyday.saveableapp.ui.screens.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import com.rainyday.saveableapp.data.local.FieldType
import com.rainyday.saveableapp.ui.components.ColorPickerRow
import com.rainyday.saveableapp.ui.theme.AccentColors

internal fun FieldType.label(): String = when (this) {
    FieldType.TEXT -> "Text"
    FieldType.NUMBER -> "Number"
    FieldType.RATING -> "Rating"
    FieldType.DATE -> "Date"
}

@Composable
fun FieldEditDialog(
    title: String,
    initialName: String = "",
    initialType: FieldType = FieldType.TEXT,
    initialColorHex: String = AccentColors.palette.first(),
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: FieldType, colorHex: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var type by remember { mutableStateOf(initialType) }
    var colorHex by remember { mutableStateOf(initialColorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Field name (e.g. Author)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Type",
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(option.label()) }
                        )
                    }
                }
                Text(
                    text = "Color",
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                ColorPickerRow(selectedHex = colorHex, onSelect = { colorHex = it })
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text("Delete field")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim(), type, colorHex) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
