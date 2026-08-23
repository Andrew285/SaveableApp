package com.rainyday.saveableapp.ui.screens.lists

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rainyday.saveableapp.data.export.buildListCsv
import com.rainyday.saveableapp.data.export.writeListPdf
import com.rainyday.saveableapp.data.local.FieldDefinitionEntity
import com.rainyday.saveableapp.data.local.SimpleListItemEntity
import com.rainyday.saveableapp.ui.components.DetailHeader
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.FieldValueChip
import com.rainyday.saveableapp.ui.components.LinkPreviewCard
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleListDetailScreen(listId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: SimpleListItemViewModel = hiltViewModel()
    val list by viewModel.list.collectAsState()
    val items by viewModel.items.collectAsState()
    val fields by viewModel.fields.collectAsState()
    val fieldValuesByItem by viewModel.fieldValuesByItem.collectAsState()
    val fieldsById = fields.associateBy { it.id }
    val showCheckbox = list?.showCheckbox ?: true
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var itemPendingEdit by remember { mutableStateOf<SimpleListItemEntity?>(null) }
    var showFieldsManager by remember { mutableStateOf(false) }
    var quickAddText by remember { mutableStateOf("") }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedItemIds by remember { mutableStateOf(emptySet<String>()) }
    var showExportMenu by remember { mutableStateOf(false) }

    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val csv = buildListCsv(items, fields, fieldValuesByItem)
            context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
        }
    }
    val pdfExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            writeListPdf(context, uri, list?.name ?: "List", items, fields, fieldValuesByItem)
        }
    }

    fun toggleSelected(id: String) {
        selectedItemIds = if (id in selectedItemIds) selectedItemIds - id else selectedItemIds + id
        if (selectedItemIds.isEmpty()) selectionMode = false
    }

    fun clearSelection() {
        selectionMode = false
        selectedItemIds = emptySet()
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.moveItem(from.index, to.index)
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (selectionMode) {
                val selectedItems = items.filter { it.id in selectedItemIds }
                ItemSelectionActionBar(
                    count = selectedItemIds.size,
                    showCheckAction = showCheckbox,
                    onCheck = { viewModel.bulkSetChecked(selectedItems, true); clearSelection() },
                    onUncheck = { viewModel.bulkSetChecked(selectedItems, false); clearSelection() },
                    onDelete = {
                        clearSelection()
                        scope.launch {
                            snackbarHostState.showUndoableDelete(
                                message = "Deleted ${selectedItems.size} items",
                                delete = { viewModel.bulkDeleteWithUndo(selectedItems) },
                                restore = { viewModel.restoreItems(it) }
                            )
                        }
                    },
                    onClose = { clearSelection() }
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DetailHeader(onBack = onBack, backLabel = "Lists", modifier = Modifier.weight(1f))
                    IconButton(onClick = { showFieldsManager = true }) {
                        Icon(Icons.Filled.Tune, contentDescription = "Manage fields")
                    }
                    IconButton(onClick = {
                        val text = buildString {
                            appendLine(list?.name ?: "List")
                            items.forEach { item ->
                                if (showCheckbox) append(if (item.isChecked) "[x] " else "[ ] ")
                                append(item.text)
                                if (!item.note.isNullOrBlank()) append(" — ${item.note}")
                                if (!item.url.isNullOrBlank()) append(" (${item.url})")
                                appendLine()
                            }
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share list"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Export")
                        }
                        DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Export as CSV") },
                                onClick = {
                                    showExportMenu = false
                                    csvExportLauncher.launch("${list?.name ?: "list"}.csv")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as PDF") },
                                onClick = {
                                    showExportMenu = false
                                    pdfExportLauncher.launch("${list?.name ?: "list"}.pdf")
                                }
                            )
                        }
                    }
                }
            }
            Text(
                text = list?.name ?: "List",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            if (items.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.FiberManualRecord,
                    title = "Nothing here yet",
                    subtitle = "Add your first item to this list.",
                    actionLabel = "New item",
                    onAction = { showAddDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                        ReorderableItem(reorderableState, key = item.id) { _ ->
                            val chips = fieldValuesByItem[item.id].orEmpty()
                                .mapNotNull { value -> fieldsById[value.fieldId]?.let { it to value.value } }
                            ItemRow(
                                item = item,
                                showCheckbox = showCheckbox,
                                fieldChips = chips,
                                selectionMode = selectionMode,
                                selected = item.id in selectedItemIds,
                                onToggleChecked = { viewModel.setChecked(item, it) },
                                onClick = {
                                    if (selectionMode) toggleSelected(item.id) else itemPendingEdit = item
                                },
                                onLongClick = {
                                    selectionMode = true
                                    toggleSelected(item.id)
                                },
                                dragHandle = { Modifier.draggableHandle() }
                            )
                        }
                    }
                }
                QuickAddItemBar(
                    placeholder = "Add ${list?.name?.lowercase()?.trimEnd('s') ?: "an item"}...",
                    text = quickAddText,
                    onTextChange = { quickAddText = it },
                    onSubmit = {
                        if (quickAddText.isNotBlank()) {
                            viewModel.createItem(quickAddText.trim(), null, null, emptyMap())
                            quickAddText = ""
                        }
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        ItemEditDialog(
            title = "New item",
            fields = fields,
            onDismiss = { showAddDialog = false },
            onConfirm = { text, note, url, fieldValues ->
                viewModel.createItem(text, note, url, fieldValues)
                showAddDialog = false
            }
        )
    }

    itemPendingEdit?.let { item ->
        ItemEditDialog(
            title = "Edit item",
            initialText = item.text,
            initialNote = item.note.orEmpty(),
            initialUrl = item.url.orEmpty(),
            fields = fields,
            initialFieldValues = fieldValuesByItem[item.id].orEmpty().associate { it.fieldId to it.value },
            onDismiss = { itemPendingEdit = null },
            onConfirm = { text, note, url, fieldValues ->
                viewModel.updateItem(item, text, note, url, fieldValues)
                itemPendingEdit = null
            },
            onDelete = {
                itemPendingEdit = null
                scope.launch {
                    snackbarHostState.showUndoableDelete(
                        message = "Deleted \"${item.text}\"",
                        delete = { viewModel.deleteItemWithUndo(item) },
                        restore = { viewModel.restoreItem(it) }
                    )
                }
            }
        )
    }

    if (showFieldsManager) {
        FieldsManagerDialog(
            fields = fields,
            onDismiss = { showFieldsManager = false },
            onAddField = viewModel::addField,
            onUpdateField = viewModel::updateField,
            onDeleteField = viewModel::deleteField
        )
    }
}

@Composable
private fun QuickAddItemBar(placeholder: String, text: String, onTextChange: (String) -> Unit, onSubmit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            shape = com.rainyday.saveableapp.ui.theme.PillShape,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onSubmit),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Add item",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ItemSelectionActionBar(
    count: Int,
    showCheckAction: Boolean,
    onCheck: () -> Unit,
    onUncheck: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
        }
        Text(
            text = "$count selected",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        )
        if (showCheckAction) {
            IconButton(onClick = onCheck) {
                Icon(Icons.Filled.Check, contentDescription = "Check selected")
            }
            IconButton(onClick = onUncheck) {
                Icon(Icons.Filled.FiberManualRecord, contentDescription = "Uncheck selected", modifier = Modifier.size(16.dp))
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemRow(
    item: SimpleListItemEntity,
    showCheckbox: Boolean,
    fieldChips: List<Pair<FieldDefinitionEntity, String>>,
    onToggleChecked: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    selectionMode: Boolean = false,
    selected: Boolean = false,
    dragHandle: @Composable () -> Modifier
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onClick() })
            } else if (showCheckbox) {
                Checkbox(checked = item.isChecked, onCheckedChange = onToggleChecked)
            } else {
                Icon(
                    Icons.Filled.FiberManualRecord,
                    contentDescription = null,
                    modifier = Modifier
                        .size(8.dp)
                        .padding(start = 14.dp, end = 6.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (showCheckbox && item.isChecked) TextDecoration.LineThrough else null,
                    color = if (showCheckbox && item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (!item.note.isNullOrBlank()) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!item.url.isNullOrBlank()) {
                    LinkPreviewCard(
                        url = item.url,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )
                }
                if (fieldChips.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        fieldChips.forEach { (field, value) -> FieldValueChip(field = field, rawValue = value) }
                    }
                }
            }
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = "Reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.then(dragHandle())
            )
        }
    }
}
