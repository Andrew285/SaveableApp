package com.rainyday.saveableapp.ui.screens.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rainyday.saveableapp.data.local.Priority
import com.rainyday.saveableapp.data.local.TagEntity
import com.rainyday.saveableapp.ui.components.ColorPickerRow
import com.rainyday.saveableapp.ui.components.parseHexColor
import com.rainyday.saveableapp.ui.theme.AccentColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditSheet(
    initialTitle: String = "",
    initialNotes: String = "",
    initialPriority: Priority = Priority.MEDIUM,
    initialDueDate: Long? = null,
    initialColorHex: String? = null,
    initialTagIds: Set<Long> = emptySet(),
    availableTags: List<TagEntity>,
    onCreateTag: (name: String, colorHex: String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (title: String, notes: String?, priority: Priority, dueDate: Long?, colorHex: String?, tagIds: List<Long>) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(initialTitle) }
    var notes by remember { mutableStateOf(initialNotes) }
    var priority by remember { mutableStateOf(initialPriority) }
    var dueDate by remember { mutableStateOf(initialDueDate) }
    var colorHex by remember { mutableStateOf(initialColorHex) }
    var selectedTagIds by remember { mutableStateOf(initialTagIds) }
    var showDatePicker by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            SectionLabel("Priority")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.label()) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = parseHexColor(p.accentHex()).copy(alpha = 0.25f)
                        )
                    )
                }
            }

            SectionLabel("Due date")
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(dueDate?.let { formatDate(it) } ?: "Set date") }
                )
                if (dueDate != null) {
                    IconButton(onClick = { dueDate = null }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear date")
                    }
                }
            }

            SectionLabel("Highlight color")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AssistChip(
                    onClick = { colorHex = null },
                    label = { Text(if (colorHex == null) "Auto ✓" else "Auto") }
                )
                ColorPickerRow(selectedHex = colorHex.orEmpty(), onSelect = { colorHex = it })
            }

            SectionLabel("Tags")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availableTags.forEach { tag ->
                    val selected = tag.id in selectedTagIds
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedTagIds = if (selected) selectedTagIds - tag.id else selectedTagIds + tag.id
                        },
                        label = { Text(tag.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = parseHexColor(tag.colorHex).copy(alpha = 0.25f)
                        )
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("New tag") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            onCreateTag(newTagName.trim(), AccentColors.palette.random())
                            newTagName = ""
                        }
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add tag")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    enabled = title.isNotBlank(),
                    onClick = {
                        onSave(title.trim(), notes.trim().ifBlank { null }, priority, dueDate, colorHex, selectedTagIds.toList())
                    }
                ) {
                    Text("Save")
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDate = state.selectedDateMillis
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

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
