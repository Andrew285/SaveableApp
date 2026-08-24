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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.local.FieldDefinitionEntity
import com.rainyday.saveableapp.data.local.FieldType
import com.rainyday.saveableapp.data.local.SimpleListEntity
import com.rainyday.saveableapp.ui.components.LinkPreviewCard
import com.rainyday.saveableapp.ui.components.PillButtonFilled
import com.rainyday.saveableapp.ui.components.SuggestedListChip
import com.rainyday.saveableapp.ui.components.normalizeUrl
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme
import kotlinx.coroutines.launch

/**
 * Confirmation sheet shown after AI parses a quick-add item string, so the user can review (and fix)
 * the title, destination list, link, note, and any custom field values the model picked before it's
 * actually saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAddItemSheet(
    availableLists: List<SimpleListEntity>,
    fieldsByListId: Map<String, List<FieldDefinitionEntity>>,
    initialListId: String,
    initialText: String,
    initialNote: String = "",
    initialUrl: String = "",
    initialFieldValues: Map<String, String> = emptyMap(),
    suggestedNewListName: String? = null,
    onCreateSuggestedList: (suspend (String) -> String)? = null,
    onDismiss: () -> Unit,
    onSave: (listId: String, text: String, note: String?, url: String?, fieldValues: Map<String, String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var listId by remember { mutableStateOf(initialListId) }
    var text by remember { mutableStateOf(initialText) }
    var note by remember { mutableStateOf(initialNote) }
    var link by remember { mutableStateOf(initialUrl) }
    var showListMenu by remember { mutableStateOf(false) }
    val fieldValues = remember { mutableStateMapOf<String, String>().apply { putAll(initialFieldValues) } }
    var datePickerFieldId by remember { mutableStateOf<String?>(null) }
    var suggestionDismissed by remember { mutableStateOf(false) }
    val currentFields = fieldsByListId[listId].orEmpty()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = Dimens.d20)
                .padding(bottom = Dimens.d24)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = Dimens.d8)
                )
                Text(
                    text = stringResource(R.string.lists_ai_review_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.lists_item_title_label)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.d16)
            )

            SectionLabel(stringResource(R.string.lists_destination_list_eyebrow))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val selectedList = availableLists.firstOrNull { it.id == listId }
                AssistChip(
                    onClick = { showListMenu = true },
                    label = { Text(selectedList?.name ?: stringResource(R.string.lists_choose_list)) },
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

            if (suggestedNewListName != null && !suggestionDismissed && onCreateSuggestedList != null) {
                SuggestedListChip(
                    suggestedName = suggestedNewListName,
                    onCreateAndUse = {
                        scope.launch {
                            // A brand-new list has no custom fields yet, so any field values entered
                            // for the previously selected list no longer apply.
                            fieldValues.clear()
                            listId = onCreateSuggestedList(suggestedNewListName)
                            suggestionDismissed = true
                        }
                    },
                    onDismiss = { suggestionDismissed = true },
                    modifier = Modifier.padding(top = Dimens.d8)
                )
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
                label = { Text(stringResource(R.string.lists_link_label)) },
                placeholder = { Text(stringResource(R.string.lists_link_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.d16)
            )
            normalizeUrl(link)?.let { normalized ->
                LinkPreviewCard(
                    url = normalized,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.d8)
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.lists_note_label)) },
                minLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.d12)
            )

            PillButtonFilled(
                text = stringResource(R.string.lists_add_to_list_action),
                enabled = text.isNotBlank() && listId.isNotEmpty(),
                onClick = {
                    onSave(listId, text.trim(), note.trim().ifBlank { null }, normalizeUrl(link), fieldValues.toMap())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.d20)
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
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { datePickerFieldId = null }) { Text(stringResource(R.string.action_cancel)) }
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

@Preview(showBackground = true)
@Composable
private fun AiAddItemSheetPreview() {
    SaveableAppTheme {
        AiAddItemSheet(
            availableLists = listOf(
                SimpleListEntity(id = "list-1", name = "Books to Read", icon = "book", colorHex = "#1E88E5", createdAt = 0L, updatedAt = 0L),
                SimpleListEntity(id = "list-2", name = "Movies to Watch", icon = "movie", colorHex = "#6750A4", createdAt = 0L, updatedAt = 0L)
            ),
            fieldsByListId = mapOf(
                "list-1" to listOf(
                    FieldDefinitionEntity(id = "field-1", listId = "list-1", name = "Author", type = FieldType.TEXT, colorHex = "#43A047", createdAt = 0L, updatedAt = 0L)
                )
            ),
            initialListId = "list-1",
            initialText = "Dune",
            initialNote = "Recommended by Alex",
            onDismiss = {},
            onSave = { _, _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AiAddIteSheetSuggestedListNamePreview() {
    SaveableAppTheme {
        AiAddItemSheet(
            availableLists = listOf(
                SimpleListEntity(id = "list-1", name = "Books to Read", icon = "book", colorHex = "#1E88E5", createdAt = 0L, updatedAt = 0L),
                SimpleListEntity(id = "list-2", name = "Movies to Watch", icon = "movie", colorHex = "#6750A4", createdAt = 0L, updatedAt = 0L)
            ),
            fieldsByListId = mapOf(
                "list-1" to listOf(
                    FieldDefinitionEntity(id = "field-1", listId = "list-1", name = "Author", type = FieldType.TEXT, colorHex = "#43A047", createdAt = 0L, updatedAt = 0L)
                )
            ),
            initialListId = "list-1",
            initialText = "Dune",
            initialNote = "Recommended by Alex",
            suggestedNewListName = "Groceries",
            onCreateSuggestedList = { str -> "" },
            onDismiss = {},
            onSave = { _, _, _, _, _ -> }
        )
    }
}

