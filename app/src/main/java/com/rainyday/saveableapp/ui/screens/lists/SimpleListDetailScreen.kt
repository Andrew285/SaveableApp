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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.export.buildListCsv
import com.rainyday.saveableapp.data.export.writeListPdf
import com.rainyday.saveableapp.data.local.FieldDefinitionEntity
import com.rainyday.saveableapp.data.local.FieldType
import com.rainyday.saveableapp.data.local.FieldValueEntity
import com.rainyday.saveableapp.data.local.SimpleListEntity
import com.rainyday.saveableapp.data.local.SimpleListItemEntity
import com.rainyday.saveableapp.data.repository.SimpleListItemSnapshot
import com.rainyday.saveableapp.ui.components.DetailHeader
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.FieldValueChip
import com.rainyday.saveableapp.ui.components.LinkPreviewCard
import com.rainyday.saveableapp.ui.components.RemoteThumbnail
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import com.rainyday.saveableapp.ui.theme.AppAlpha
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleListDetailScreen(listId: String, onBack: () -> Unit) {
    val viewModel: SimpleListItemViewModel = hiltViewModel()
    val list by viewModel.list.collectAsState()
    val items by viewModel.items.collectAsState()
    val fields by viewModel.fields.collectAsState()
    val fieldValuesByItem by viewModel.fieldValuesByItem.collectAsState()

    SimpleListDetailScreenContent(
        onBack = onBack,
        list = list,
        items = items,
        fields = fields,
        fieldValuesByItem = fieldValuesByItem,
        onMoveItem = viewModel::moveItem,
        onBulkSetChecked = viewModel::bulkSetChecked,
        onBulkDeleteWithUndo = viewModel::bulkDeleteWithUndo,
        onRestoreItems = viewModel::restoreItems,
        onSetChecked = viewModel::setChecked,
        onCreateItem = viewModel::createItem,
        onUpdateItem = viewModel::updateItem,
        onDeleteItemWithUndo = viewModel::deleteItemWithUndo,
        onRestoreItem = viewModel::restoreItem,
        onAddField = viewModel::addField,
        onUpdateField = viewModel::updateField,
        onDeleteField = viewModel::deleteField
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleListDetailScreenContent(
    onBack: () -> Unit,
    list: SimpleListEntity?,
    items: List<SimpleListItemEntity>,
    fields: List<FieldDefinitionEntity>,
    fieldValuesByItem: Map<String, List<FieldValueEntity>>,
    onMoveItem: (Int, Int) -> Unit = { _, _ -> },
    onBulkSetChecked: (List<SimpleListItemEntity>, Boolean) -> Unit = { _, _ -> },
    onBulkDeleteWithUndo: suspend (List<SimpleListItemEntity>) -> List<SimpleListItemSnapshot> = { emptyList() },
    onRestoreItems: suspend (List<SimpleListItemSnapshot>) -> Unit = {},
    onSetChecked: (SimpleListItemEntity, Boolean) -> Unit = { _, _ -> },
    onCreateItem: (String, String?, String?, Map<String, String>) -> Unit = { _, _, _, _ -> },
    onUpdateItem: (SimpleListItemEntity, String, String?, String?, Map<String, String>) -> Unit = { _, _, _, _, _ -> },
    onDeleteItemWithUndo: suspend (SimpleListItemEntity) -> SimpleListItemSnapshot = { item -> SimpleListItemSnapshot(item, emptyList()) },
    onRestoreItem: suspend (SimpleListItemSnapshot) -> Unit = {},
    onAddField: (String, FieldType, String) -> Unit = { _, _, _ -> },
    onUpdateField: (FieldDefinitionEntity, String, FieldType, String) -> Unit = { _, _, _, _ -> },
    onDeleteField: (FieldDefinitionEntity) -> Unit = {}
) {
    val context = LocalContext.current
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
    val undoActionLabel = stringResource(R.string.action_undo)

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
        onMoveItem(from.index, to.index)
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val defaultListName = stringResource(R.string.lists_default_name)
            if (selectionMode) {
                val selectedItems = items.filter { it.id in selectedItemIds }
                val deletedItemsMessage = stringResource(R.string.lists_deleted_items_count, selectedItems.size)
                ItemSelectionActionBar(
                    count = selectedItemIds.size,
                    showCheckAction = showCheckbox,
                    onCheck = { onBulkSetChecked(selectedItems, true); clearSelection() },
                    onUncheck = { onBulkSetChecked(selectedItems, false); clearSelection() },
                    onDelete = {
                        clearSelection()
                        scope.launch {
                            snackbarHostState.showUndoableDelete(
                                message = deletedItemsMessage,
                                actionLabel = undoActionLabel,
                                delete = { onBulkDeleteWithUndo(selectedItems) },
                                restore = { onRestoreItems(it) }
                            )
                        }
                    },
                    onClose = { clearSelection() }
                )
            } else {
                val shareChooserTitle = stringResource(R.string.lists_share_chooser_title)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DetailHeader(onBack = onBack, backLabel = stringResource(R.string.nav_lists), modifier = Modifier.weight(1f))
                    IconButton(onClick = { showFieldsManager = true }) {
                        Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.lists_cd_manage_fields))
                    }
                    IconButton(onClick = {
                        val text = buildString {
                            appendLine(list?.name ?: defaultListName)
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
                        context.startActivity(Intent.createChooser(intent, shareChooserTitle))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.cd_share))
                    }
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_export))
                        }
                        DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.lists_export_csv)) },
                                onClick = {
                                    showExportMenu = false
                                    csvExportLauncher.launch("${list?.name ?: defaultListName.lowercase()}.csv")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.lists_export_pdf)) },
                                onClick = {
                                    showExportMenu = false
                                    pdfExportLauncher.launch("${list?.name ?: defaultListName.lowercase()}.pdf")
                                }
                            )
                        }
                    }
                }
            }
            Text(
                text = list?.name ?: defaultListName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = Dimens.d20)
            )

            if (items.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.FiberManualRecord,
                    title = stringResource(R.string.lists_detail_empty_title),
                    subtitle = stringResource(R.string.lists_detail_empty_subtitle),
                    actionLabel = stringResource(R.string.lists_new_item_title),
                    onAction = { showAddDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(Dimens.d20, Dimens.d12, Dimens.d20, Dimens.d12),
                    verticalArrangement = Arrangement.spacedBy(Dimens.d8)
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
                                onToggleChecked = { onSetChecked(item, it) },
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
                    placeholder = stringResource(
                        R.string.lists_quick_add_placeholder,
                        list?.name?.lowercase()?.trimEnd('s') ?: stringResource(R.string.lists_quick_add_placeholder_fallback)
                    ),
                    text = quickAddText,
                    onTextChange = { quickAddText = it },
                    onSubmit = {
                        if (quickAddText.isNotBlank()) {
                            onCreateItem(quickAddText.trim(), null, null, emptyMap())
                            quickAddText = ""
                        }
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        ItemEditDialog(
            title = stringResource(R.string.lists_new_item_title),
            fields = fields,
            onDismiss = { showAddDialog = false },
            onConfirm = { text, note, url, fieldValues ->
                onCreateItem(text, note, url, fieldValues)
                showAddDialog = false
            }
        )
    }

    itemPendingEdit?.let { item ->
        val deletedItemMessage = stringResource(R.string.deleted_named_item, item.text)
        ItemEditDialog(
            title = stringResource(R.string.lists_edit_item_title),
            initialText = item.text,
            initialNote = item.note.orEmpty(),
            initialUrl = item.url.orEmpty(),
            fields = fields,
            initialFieldValues = fieldValuesByItem[item.id].orEmpty().associate { it.fieldId to it.value },
            onDismiss = { itemPendingEdit = null },
            onConfirm = { text, note, url, fieldValues ->
                onUpdateItem(item, text, note, url, fieldValues)
                itemPendingEdit = null
            },
            onDelete = {
                itemPendingEdit = null
                scope.launch {
                    snackbarHostState.showUndoableDelete(
                        message = deletedItemMessage,
                        actionLabel = undoActionLabel,
                        delete = { onDeleteItemWithUndo(item) },
                        restore = { onRestoreItem(it) }
                    )
                }
            }
        )
    }

    if (showFieldsManager) {
        FieldsManagerDialog(
            fields = fields,
            onDismiss = { showFieldsManager = false },
            onAddField = onAddField,
            onUpdateField = onUpdateField,
            onDeleteField = onDeleteField
        )
    }
}

@Composable
private fun QuickAddItemBar(placeholder: String, text: String, onTextChange: (String) -> Unit, onSubmit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.d20, vertical = Dimens.d12),
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
                .padding(start = Dimens.d8)
                .size(Dimens.d40)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onSubmit),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.cd_add_item),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(Dimens.d18)
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
            .padding(horizontal = Dimens.d12, vertical = Dimens.d8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_cancel_selection))
        }
        Text(
            text = stringResource(R.string.selection_count, count),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = Dimens.d4)
        )
        if (showCheckAction) {
            IconButton(onClick = onCheck) {
                Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.lists_cd_check_selected))
            }
            IconButton(onClick = onUncheck) {
                Icon(
                    Icons.Filled.FiberManualRecord,
                    contentDescription = stringResource(R.string.lists_cd_uncheck_selected),
                    modifier = Modifier.size(Dimens.d16)
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_selected))
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
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.d0),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.a12)
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
                .padding(vertical = Dimens.d8, horizontal = Dimens.d12),
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
                        .size(Dimens.d8)
                        .padding(start = Dimens.d14, end = Dimens.d6),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (!item.imageUrl.isNullOrBlank() && item.url.isNullOrBlank()) {
                RemoteThumbnail(
                    url = item.imageUrl,
                    modifier = Modifier
                        .padding(start = Dimens.d4)
                        .size(Dimens.d44)
                        .clip(RoundedCornerShape(Dimens.d8))
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Dimens.d4)
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
                            .padding(top = Dimens.d6)
                    )
                }
                if (fieldChips.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.d6),
                        verticalArrangement = Arrangement.spacedBy(Dimens.d4),
                        modifier = Modifier.padding(top = Dimens.d4)
                    ) {
                        fieldChips.forEach { (field, value) -> FieldValueChip(field = field, rawValue = value) }
                    }
                }
            }
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = stringResource(R.string.cd_reorder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.then(dragHandle())
            )
        }
    }
}

private val previewDetailList = SimpleListEntity(id = "list-1", name = "Books to Read", icon = "book", colorHex = "#1E88E5", showCheckbox = true, createdAt = 0L, updatedAt = 0L)

private val previewDetailFields = listOf(
    FieldDefinitionEntity(id = "field-1", listId = "list-1", name = "Author", type = FieldType.TEXT, colorHex = "#43A047", createdAt = 0L, updatedAt = 0L),
    FieldDefinitionEntity(id = "field-2", listId = "list-1", name = "Rating", type = FieldType.RATING, colorHex = "#FB8C00", createdAt = 0L, updatedAt = 0L)
)

private val previewDetailItems = listOf(
    SimpleListItemEntity(id = "item-1", listId = "list-1", text = "Dune", note = "Recommended by Alex", isChecked = false, createdAt = 0L, updatedAt = 0L),
    SimpleListItemEntity(id = "item-2", listId = "list-1", text = "Project Hail Mary", isChecked = true, createdAt = 0L, updatedAt = 0L)
)

private val previewDetailFieldValues = mapOf(
    "item-1" to listOf(
        FieldValueEntity(id = "value-1", itemId = "item-1", fieldId = "field-1", value = "Frank Herbert", updatedAt = 0L)
    )
)

@Preview(showBackground = true)
@Composable
fun SimpleListDetailScreenPreview() {
    SaveableAppTheme {
        SimpleListDetailScreenContent(
            onBack = {},
            list = previewDetailList,
            items = previewDetailItems,
            fields = previewDetailFields,
            fieldValuesByItem = previewDetailFieldValues
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SimpleListDetailScreenEmptyPreview() {
    SaveableAppTheme {
        SimpleListDetailScreenContent(
            onBack = {},
            list = previewDetailList,
            items = emptyList(),
            fields = previewDetailFields,
            fieldValuesByItem = emptyMap()
        )
    }
}
