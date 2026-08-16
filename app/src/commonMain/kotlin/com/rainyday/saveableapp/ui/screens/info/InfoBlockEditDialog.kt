package com.rainyday.saveableapp.ui.screens.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rainyday.saveableapp.ui.screens.todo.formatDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBlockEditDialog(
    title: String,
    initialTitle: String = "",
    initialContent: String = "",
    initialSensitive: Boolean = false,
    initialExpiryDate: Long? = null,
    templates: List<InfoBlockTemplate> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, isSensitive: Boolean, expiryDate: Long?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var blockTitle by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    var sensitive by remember { mutableStateOf(initialSensitive) }
    var expiryDate by remember { mutableStateOf(initialExpiryDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (templates.isNotEmpty()) {
                    Text(
                        text = "Start from a template",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(templates) { template ->
                            SuggestionChip(
                                onClick = {
                                    blockTitle = template.title
                                    sensitive = template.isSensitive
                                },
                                label = { Text(template.label) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = blockTitle,
                    onValueChange = { blockTitle = it },
                    label = { Text("Title (e.g. Passport number)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (templates.isNotEmpty()) 12.dp else 0.dp)
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Value") },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = sensitive, onCheckedChange = { sensitive = it })
                    Text("Mask value until tapped")
                }
                Text(
                    text = "Expiry date",
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { showDatePicker = true },
                        label = { Text(expiryDate?.let { formatDate(it) } ?: "Set expiry date") }
                    )
                    if (expiryDate != null) {
                        IconButton(onClick = { expiryDate = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear expiry date")
                        }
                    }
                }
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("Delete")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = blockTitle.isNotBlank() && content.isNotBlank(),
                onClick = { onConfirm(blockTitle.trim(), content.trim(), sensitive, expiryDate) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = expiryDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    expiryDate = state.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
