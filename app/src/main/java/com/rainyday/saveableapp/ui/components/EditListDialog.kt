package com.rainyday.saveableapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.local.FieldTemplate
import com.rainyday.saveableapp.ui.theme.AccentColors
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

data class EditListResult(
    val name: String,
    val icon: String,
    val colorHex: String,
    val showCheckbox: Boolean,
    val fieldTemplates: List<FieldTemplate> = emptyList()
)

data class ListTemplateOption(
    val label: String,
    val result: EditListResult
)

@Composable
fun EditListDialog(
    title: String,
    initialName: String = "",
    initialIcon: String = IconCatalog.defaultKey,
    initialColorHex: String = AccentColors.palette.first(),
    showCheckboxOption: Boolean = false,
    initialShowCheckbox: Boolean = true,
    checkboxOptionLabel: String = stringResource(R.string.edit_list_dialog_show_checkbox),
    confirmLabel: String = stringResource(R.string.action_save),
    templates: List<ListTemplateOption> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (EditListResult) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var icon by remember { mutableStateOf(initialIcon) }
    var colorHex by remember { mutableStateOf(initialColorHex) }
    var showCheckbox by remember { mutableStateOf(initialShowCheckbox) }
    var fieldTemplates by remember { mutableStateOf(emptyList<FieldTemplate>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (templates.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.edit_list_dialog_start_from_template),
                        modifier = Modifier.padding(bottom = Dimens.d8)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.d8)) {
                        items(templates) { template ->
                            SuggestionChip(
                                onClick = {
                                    name = template.result.name
                                    icon = template.result.icon
                                    colorHex = template.result.colorHex
                                    showCheckbox = template.result.showCheckbox
                                    fieldTemplates = template.result.fieldTemplates
                                },
                                label = { Text(template.label) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.edit_list_dialog_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (templates.isNotEmpty()) Dimens.d12 else Dimens.d0)
                )
                Text(
                    text = stringResource(R.string.edit_list_dialog_color_label),
                    modifier = Modifier.padding(top = Dimens.d16, bottom = Dimens.d8)
                )
                ColorPickerRow(selectedHex = colorHex, onSelect = { colorHex = it })
                Text(
                    text = stringResource(R.string.edit_list_dialog_icon_label),
                    modifier = Modifier.padding(top = Dimens.d16, bottom = Dimens.d8)
                )
                IconPickerRow(selectedKey = icon, accentHex = colorHex, onSelect = { icon = it })
                if (showCheckboxOption) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.d12),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = showCheckbox, onCheckedChange = { showCheckbox = it })
                        Text(checkboxOptionLabel, modifier = Modifier.width(Dimens.d220))
                    }
                }
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.padding(top = Dimens.d8)
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onConfirm(EditListResult(name.trim(), icon, colorHex, showCheckbox, fieldTemplates))
                }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun EditListDialogPreview() {
    SaveableAppTheme {
        EditListDialog(
            title = "Edit list",
            initialName = "Groceries",
            initialColorHex = AccentColors.palette.first(),
            showCheckboxOption = true,
            onDismiss = {},
            onConfirm = {},
            onDelete = {}
        )
    }
}
