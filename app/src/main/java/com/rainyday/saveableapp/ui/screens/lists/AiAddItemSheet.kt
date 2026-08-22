package com.rainyday.saveableapp.ui.screens.lists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rainyday.saveableapp.data.local.FieldDefinitionEntity
import com.rainyday.saveableapp.data.local.SimpleListEntity
import com.rainyday.saveableapp.ui.components.LinkPreviewCard
import com.rainyday.saveableapp.ui.components.PillButtonFilled
import com.rainyday.saveableapp.ui.components.normalizeUrl

/**
 * Confirmation sheet shown after AI parses a quick-add item string, so the user can review (and fix)
 * the title, destination list, link, note, and any custom field values the model picked before it's
 * actually saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAddItemSheet(
    availableLists: List<SimpleListEntity>,
    fieldsByListId: Map<Long, List<FieldDefinitionEntity>>,
    initialListId: Long,
    initialText: String,
    initialNote: String = "",
    initialUrl: String = "",
    initialFieldValues: Map<Long, String> = emptyMap(),
    onDismiss: () -> Unit,
    onSave: (listId: Long, text: String, note: String?, url: String?, fieldValues: Map<Long, String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var listId by remember { mutableStateOf(initialListId) }
    var text by remember { mutableStateOf(initialText) }
    var note by remember { mutableStateOf(initialNote) }
    var link by remember { mutableStateOf(initialUrl) }
    var showListMenu by remember { mutableStateOf(false) }
    val fieldValues = remember { mutableStateMapOf<Long, String>().apply { putAll(initialFieldValues) } }
    var datePickerFieldId by remember { mutableStateOf<Long?>(null) }
    val currentFields = fieldsByListId[listId].orEmpty()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Review before adding",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )

            SectionLabel("// DESTINATION LIST")
            Row(verticalAlignment = Alignment.CenterVertically) {
                val selectedList = availableLists.firstOrNull { it.id == listId }
                AssistChip(
                    onClick = { showListMenu = true },
                    label = { Text(selectedList?.name ?: "Choose list") },
                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) }
                )
                DropdownMenu(expanded = showListMenu, onDismissRequest = { showListMenu = false }) {
                    availableLists.forEach { list ->
                        DropdownMenuItem(
                            text = { Text(list.name) },
                            onClick = {
                                // Field values belong to the previous list's fields — drop any that
                                // don't exist on the newly chosen list instead of silently carrying them over.
                                val newFieldIds = fieldsByListId[list.id].orEmpty().map { it.id }.toSet()
                                fieldValues.keys.retainAll(newFieldIds)
                                listId = list.id
                                showListMenu = false
                            }
                        )
                    }
                }
            }

            currentFields.forEach { field ->
                FieldInput(
                    field = field,
                    value = fieldValues[field.id],
                    onValueChange = { fieldValues[field.id] = it },
                    onClear = { fieldValues.remove(field.id) },
                    onOpenDatePicker = { datePickerFieldId = field.id }
                )
            }

            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("Link (optional)") },
                placeholder = { Text("example.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
            normalizeUrl(link)?.let { normalized ->
                LinkPreviewCard(
                    url = normalized,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            PillButtonFilled(
                text = "Add to list",
                enabled = text.isNotBlank() && listId != 0L,
                onClick = {
                    onSave(listId, text.trim(), note.trim().ifBlank { null }, normalizeUrl(link), fieldValues.toMap())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
            )
        }
    }

    datePickerFieldId?.let { fieldId ->
        val state = rememberDatePickerState(initialSelectedDateMillis = fieldValues[fieldId]?.toLongOrNull())
        DatePickerDialog(
            onDismissRequest = { datePickerFieldId = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { fieldValues[fieldId] = it.toString() }
                    datePickerFieldId = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { datePickerFieldId = null }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}
