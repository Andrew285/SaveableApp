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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.local.Priority
import com.rainyday.saveableapp.data.local.RecurrenceRule
import com.rainyday.saveableapp.data.local.TagEntity
import com.rainyday.saveableapp.data.local.TodoListEntity
import com.rainyday.saveableapp.ui.components.ColorPickerRow
import com.rainyday.saveableapp.ui.components.PillButtonDanger
import com.rainyday.saveableapp.ui.components.PillButtonFilled
import com.rainyday.saveableapp.ui.components.SegmentedPillRow
import com.rainyday.saveableapp.ui.components.SuggestedListChip
import com.rainyday.saveableapp.ui.components.TagChip
import com.rainyday.saveableapp.ui.components.parseHexColor
import com.rainyday.saveableapp.ui.theme.AccentColors
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditSheet(
    availableLists: List<TodoListEntity>,
    initialListId: String,
    initialTitle: String = "",
    initialNotes: String = "",
    initialPriority: Priority = Priority.MEDIUM,
    initialDueDate: Long? = null,
    initialColorHex: String? = null,
    initialTagIds: Set<String> = emptySet(),
    initialRecurrence: RecurrenceRule = RecurrenceRule.NONE,
    suggestedNewListName: String? = null,
    onCreateSuggestedList: (suspend (String) -> String)? = null,
    availableTags: List<TagEntity>,
    onCreateTag: (name: String, colorHex: String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        listId: String,
        title: String,
        notes: String?,
        priority: Priority,
        dueDate: Long?,
        colorHex: String?,
        tagIds: List<String>,
        recurrence: RecurrenceRule
    ) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var listId by remember { mutableStateOf(initialListId) }
    var title by remember { mutableStateOf(initialTitle) }
    var notes by remember { mutableStateOf(initialNotes) }
    var priority by remember { mutableStateOf(initialPriority) }
    var dueDate by remember { mutableStateOf(initialDueDate) }
    var colorHex by remember { mutableStateOf(initialColorHex) }
    var selectedTagIds by remember { mutableStateOf(initialTagIds) }
    var recurrence by remember { mutableStateOf(initialRecurrence) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showListMenu by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    var suggestionDismissed by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = Dimens.d20)
                .padding(bottom = Dimens.d24)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.task_edit_title_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.task_edit_notes_label)) },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.d12)
            )

            if (availableLists.isNotEmpty()) {
                SectionLabel(stringResource(R.string.task_edit_destination_list_eyebrow))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val selectedList = availableLists.firstOrNull { it.id == listId }
                    AssistChip(
                        onClick = { showListMenu = true },
                        label = { Text(selectedList?.name ?: stringResource(R.string.task_edit_choose_list)) },
                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) }
                    )
                    DropdownMenu(expanded = showListMenu, onDismissRequest = { showListMenu = false }) {
                        availableLists.forEach { list ->
                            DropdownMenuItem(
                                text = { Text(list.name) },
                                onClick = {
                                    listId = list.id
                                    showListMenu = false
                                }
                            )
                        }
                    }
                }
                if (suggestedNewListName != null && !suggestionDismissed && onCreateSuggestedList != null) {
                    SuggestedListChip(
                        suggestedName = suggestedNewListName,
                        onCreateAndUse = {
                            scope.launch {
                                listId = onCreateSuggestedList(suggestedNewListName)
                                suggestionDismissed = true
                            }
                        },
                        onDismiss = { suggestionDismissed = true },
                        modifier = Modifier.padding(top = Dimens.d8)
                    )
                }
            }

            SectionLabel(stringResource(R.string.task_edit_priority_eyebrow))
            SegmentedPillRow(
                options = Priority.entries,
                selected = priority,
                onSelect = { priority = it },
                label = { it.label() },
                accentColor = { parseHexColor(it.accentHex()) }
            )

            SectionLabel(stringResource(R.string.task_edit_repeats_eyebrow))
            SegmentedPillRow(
                options = RecurrenceRule.entries,
                selected = recurrence,
                onSelect = { recurrence = it },
                label = { it.label() }
            )

            SectionLabel(stringResource(R.string.task_edit_deadline_eyebrow))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { showDatePicker = true },
                    label = { Text(dueDate?.let { formatDate(it) } ?: stringResource(R.string.task_edit_set_date)) }
                )
                if (dueDate != null) {
                    IconButton(onClick = { dueDate = null }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_date))
                    }
                }
            }

            SectionLabel(stringResource(R.string.task_edit_highlight_color_eyebrow))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.d12)) {
                AssistChip(
                    onClick = { colorHex = null },
                    label = {
                        Text(
                            if (colorHex == null) {
                                stringResource(R.string.task_edit_auto_color_selected)
                            } else {
                                stringResource(R.string.task_edit_auto_color)
                            }
                        )
                    }
                )
                ColorPickerRow(selectedHex = colorHex.orEmpty(), onSelect = { colorHex = it })
            }

            SectionLabel(stringResource(R.string.task_edit_tags_eyebrow))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.d8)) {
                availableTags.forEach { tag ->
                    val selected = tag.id in selectedTagIds
                    TagChip(
                        label = tag.name,
                        selected = selected,
                        onClick = {
                            selectedTagIds = if (selected) selectedTagIds - tag.id else selectedTagIds + tag.id
                        }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.d8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text(stringResource(R.string.task_edit_new_tag_label)) },
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
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_tag))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.d20),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDelete != null) {
                    PillButtonDanger(
                        text = stringResource(R.string.task_edit_delete_task),
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.padding(start = Dimens.d12))
                }
                PillButtonFilled(
                    text = stringResource(R.string.action_save),
                    enabled = title.isNotBlank() && listId.isNotEmpty(),
                    onClick = {
                        onSave(
                            listId,
                            title.trim(),
                            notes.trim().ifBlank { null },
                            priority,
                            dueDate,
                            colorHex,
                            selectedTagIds.toList(),
                            recurrence
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
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
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
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
        modifier = Modifier.padding(top = Dimens.d16, bottom = Dimens.d8)
    )
}

fun formatDate(millis: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))

@Preview(showBackground = true)
@Composable
private fun TaskEditSheetPreview() {
    SaveableAppTheme {
        TaskEditSheet(
            availableLists = listOf(
                TodoListEntity(id = "list-1", name = "Personal", colorHex = "#6750A4", icon = "checklist", position = 0, createdAt = 0L, updatedAt = 0L),
                TodoListEntity(id = "list-2", name = "Work", colorHex = "#1E88E5", icon = "work", position = 1, createdAt = 0L, updatedAt = 0L)
            ),
            initialListId = "list-1",
            initialTitle = "Finish quarterly report",
            initialNotes = "Include the Q3 revenue breakdown",
            initialPriority = Priority.HIGH,
            initialDueDate = System.currentTimeMillis(),
            initialTagIds = setOf("tag-1"),
            initialRecurrence = RecurrenceRule.NONE,
            availableTags = listOf(
                TagEntity(id = "tag-1", name = "urgent", colorHex = "#E53935", updatedAt = 0L),
                TagEntity(id = "tag-2", name = "home", colorHex = "#43A047", updatedAt = 0L)
            ),
            onCreateTag = { _, _ -> },
            onDismiss = {},
            onSave = { _, _, _, _, _, _, _, _ -> },
            onDelete = {}
        )
    }
}
