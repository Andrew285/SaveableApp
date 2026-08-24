package com.rainyday.saveableapp.ui.screens.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.local.FieldDefinitionEntity
import com.rainyday.saveableapp.data.local.FieldType
import com.rainyday.saveableapp.ui.components.parseHexColor
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme

/** Lists a list's custom fields and lets the user add, edit, or delete them. */
@Composable
fun FieldsManagerDialog(
    fields: List<FieldDefinitionEntity>,
    onDismiss: () -> Unit,
    onAddField: (name: String, type: FieldType, colorHex: String) -> Unit,
    onUpdateField: (field: FieldDefinitionEntity, name: String, type: FieldType, colorHex: String) -> Unit,
    onDeleteField: (FieldDefinitionEntity) -> Unit
) {
    var fieldPendingEdit by remember { mutableStateOf<FieldDefinitionEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lists_custom_fields_title)) },
        text = {
            Column {
                if (fields.isEmpty()) {
                    Text(
                        text = stringResource(R.string.lists_custom_fields_empty_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    fields.forEach { field ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .clickable { fieldPendingEdit = field }
                                .padding(vertical = Dimens.d6),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(Dimens.d14)
                                    .clip(CircleShape)
                                    .background(parseHexColor(field.colorHex))
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = Dimens.d10)
                            ) {
                                Text(field.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = field.type.label(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = stringResource(R.string.cd_edit_named_item, field.name),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                TextButton(onClick = { showAddDialog = true }, modifier = Modifier.padding(top = Dimens.d8)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(Dimens.d18))
                    Text(stringResource(R.string.lists_add_field_action), modifier = Modifier.padding(start = Dimens.d4))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
        }
    )

    if (showAddDialog) {
        FieldEditDialog(
            title = stringResource(R.string.lists_new_field_title),
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, colorHex ->
                onAddField(name, type, colorHex)
                showAddDialog = false
            }
        )
    }

    fieldPendingEdit?.let { field ->
        FieldEditDialog(
            title = stringResource(R.string.lists_edit_field_title),
            initialName = field.name,
            initialType = field.type,
            initialColorHex = field.colorHex,
            onDismiss = { fieldPendingEdit = null },
            onConfirm = { name, type, colorHex ->
                onUpdateField(field, name, type, colorHex)
                fieldPendingEdit = null
            },
            onDelete = {
                onDeleteField(field)
                fieldPendingEdit = null
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FieldsManagerDialogPreview() {
    SaveableAppTheme {
        FieldsManagerDialog(
            fields = listOf(
                FieldDefinitionEntity(id = "field-1", listId = "list-1", name = "Author", type = FieldType.TEXT, colorHex = "#43A047", createdAt = 0L, updatedAt = 0L),
                FieldDefinitionEntity(id = "field-2", listId = "list-1", name = "Rating", type = FieldType.RATING, colorHex = "#FB8C00", createdAt = 0L, updatedAt = 0L)
            ),
            onDismiss = {},
            onAddField = { _, _, _ -> },
            onUpdateField = { _, _, _, _ -> },
            onDeleteField = {}
        )
    }
}

