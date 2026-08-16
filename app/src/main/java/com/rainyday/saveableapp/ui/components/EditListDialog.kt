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
import androidx.compose.ui.unit.dp
import com.rainyday.saveableapp.data.local.FieldTemplate
import com.rainyday.saveableapp.ui.theme.AccentColors

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
    checkboxOptionLabel: String = "Show checkbox on items",
    confirmLabel: String = "Save",
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
                        text = "Start from a template",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (templates.isNotEmpty()) 12.dp else 0.dp)
                )
                Text(
                    text = "Color",
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                ColorPickerRow(selectedHex = colorHex, onSelect = { colorHex = it })
                Text(
                    text = "Icon",
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                IconPickerRow(selectedKey = icon, accentHex = colorHex, onSelect = { icon = it })
                if (showCheckboxOption) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = showCheckbox, onCheckedChange = { showCheckbox = it })
                        Text(checkboxOptionLabel, modifier = Modifier.width(220.dp))
                    }
                }
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Delete")
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
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
