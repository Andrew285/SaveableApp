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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.ui.screens.todo.formatDate
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

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
                        text = stringResource(R.string.info_start_from_template),
                        modifier = Modifier.padding(bottom = Dimens.d8)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.d8)) {
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
                    label = { Text(stringResource(R.string.info_block_title_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (templates.isNotEmpty()) Dimens.d12 else Dimens.d0)
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(R.string.info_block_value_label)) },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.d12)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.d12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = sensitive, onCheckedChange = { sensitive = it })
                    Text(stringResource(R.string.info_mask_value_label))
                }
                Text(
                    text = stringResource(R.string.info_expiry_date_label),
                    modifier = Modifier.padding(top = Dimens.d12, bottom = Dimens.d8)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { showDatePicker = true },
                        label = { Text(expiryDate?.let { formatDate(it) } ?: stringResource(R.string.info_set_expiry_date)) }
                    )
                    if (expiryDate != null) {
                        IconButton(onClick = { expiryDate = null }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_clear_expiry_date))
                        }
                    }
                }
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.padding(top = Dimens.d4)
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = blockTitle.isNotBlank() && content.isNotBlank(),
                onClick = { onConfirm(blockTitle.trim(), content.trim(), sensitive, expiryDate) }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
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

@Preview(showBackground = true)
@Composable
private fun InfoBlockEditDialogPreview() {
    SaveableAppTheme {
        InfoBlockEditDialog(
            title = "New entry",
            templates = infoBlockTemplates(),
            onDismiss = {},
            onConfirm = { _, _, _, _ -> },
            onDelete = {}
        )
    }
}
