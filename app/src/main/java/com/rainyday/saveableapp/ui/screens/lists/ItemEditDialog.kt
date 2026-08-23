package com.rainyday.saveableapp.ui.screens.lists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.rainyday.saveableapp.data.local.FieldType
import com.rainyday.saveableapp.ui.components.LinkPreviewCard
import com.rainyday.saveableapp.ui.components.StarRatingInput
import com.rainyday.saveableapp.ui.components.normalizeUrl
import com.rainyday.saveableapp.ui.components.parseHexColor
import com.rainyday.saveableapp.ui.screens.todo.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditDialog(
    title: String,
    initialText: String = "",
    initialNote: String = "",
    initialUrl: String = "",
    fields: List<FieldDefinitionEntity> = emptyList(),
    initialFieldValues: Map<String, String> = emptyMap(),
    textLabel: String = "Title",
    onDismiss: () -> Unit,
    onConfirm: (text: String, note: String?, url: String?, fieldValues: Map<String, String>) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf(initialText) }
    var note by remember { mutableStateOf(initialNote) }
    var link by remember { mutableStateOf(initialUrl) }
    val fieldValues = remember { mutableStateMapOf<String, String>().apply { putAll(initialFieldValues) } }
    var datePickerFieldId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(textLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                fields.forEach { field ->
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
                        .padding(top = 12.dp)
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
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Delete")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = {
                    onConfirm(text.trim(), note.trim().ifBlank { null }, normalizeUrl(link), fieldValues.toMap())
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

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
internal fun FieldInput(
    field: FieldDefinitionEntity,
    value: String?,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onOpenDatePicker: () -> Unit
) {
    when (field.type) {
        FieldType.TEXT -> OutlinedTextField(
            value = value ?: "",
            onValueChange = onValueChange,
            label = { Text(field.name) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
        FieldType.NUMBER -> OutlinedTextField(
            value = value ?: "",
            onValueChange = { candidate ->
                if (candidate.isEmpty() || candidate.matches(Regex("^-?\\d*\\.?\\d*$"))) onValueChange(candidate)
            },
            label = { Text(field.name) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
        FieldType.RATING -> Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(
                text = field.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            StarRatingInput(
                rating = value?.toIntOrNull() ?: 0,
                onRatingChange = { onValueChange(it.toString()) },
                color = parseHexColor(field.colorHex)
            )
        }
        FieldType.DATE -> Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(
                text = field.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dateMillis = value?.toLongOrNull()
                AssistChip(
                    onClick = onOpenDatePicker,
                    label = { Text(dateMillis?.let { formatDate(it) } ?: "Set date") }
                )
                if (dateMillis != null) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear ${field.name}")
                    }
                }
            }
        }
    }
}
