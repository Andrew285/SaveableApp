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
import androidx.compose.ui.unit.dp
import com.rainyday.saveableapp.data.local.FieldDefinitionEntity
import com.rainyday.saveableapp.data.local.FieldType
import com.rainyday.saveableapp.ui.components.parseHexColor

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
        title = { Text("Custom fields") },
        text = {
            Column {
                if (fields.isEmpty()) {
                    Text(
                        text = "Add fields like Author, Rating, or Release date to track more than just a title.",
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
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(field.colorHex))
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp)
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
                                contentDescription = "Edit ${field.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                TextButton(onClick = { showAddDialog = true }, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Add field", modifier = Modifier.padding(start = 4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )

    if (showAddDialog) {
        FieldEditDialog(
            title = "New field",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, colorHex ->
                onAddField(name, type, colorHex)
                showAddDialog = false
            }
        )
    }

    fieldPendingEdit?.let { field ->
        FieldEditDialog(
            title = "Edit field",
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

