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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.local.FieldType
import com.rainyday.saveableapp.ui.components.ColorPickerRow
import com.rainyday.saveableapp.ui.theme.AccentColors
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

@Composable
internal fun FieldType.label(): String = stringResource(
    when (this) {
        FieldType.TEXT -> R.string.field_type_text
        FieldType.NUMBER -> R.string.field_type_number
        FieldType.RATING -> R.string.field_type_rating
        FieldType.DATE -> R.string.field_type_date
    }
)

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
                    label = { Text(stringResource(R.string.lists_field_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.lists_field_type_label),
                    modifier = Modifier.padding(top = Dimens.d16, bottom = Dimens.d8)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.d8)) {
                    FieldType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(option.label()) }
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.lists_field_color_label),
                    modifier = Modifier.padding(top = Dimens.d16, bottom = Dimens.d8)
                )
                ColorPickerRow(selectedHex = colorHex, onSelect = { colorHex = it })
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.padding(top = Dimens.d12)
                    ) {
                        Text(stringResource(R.string.lists_delete_field_action))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim(), type, colorHex) }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun FieldEditDialogPreview() {
    SaveableAppTheme {
        FieldEditDialog(
            title = "Edit field",
            initialName = "Rating",
            initialType = FieldType.RATING,
            initialColorHex = AccentColors.palette.first(),
            onDismiss = {},
            onConfirm = { _, _, _ -> },
            onDelete = {}
        )
    }
}
