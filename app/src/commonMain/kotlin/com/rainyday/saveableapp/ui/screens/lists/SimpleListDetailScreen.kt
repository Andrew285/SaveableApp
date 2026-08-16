package com.rainyday.saveableapp.ui.screens.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rainyday.saveableapp.data.local.SimpleListItemEntity
import com.rainyday.saveableapp.platform.rememberShareText
import com.rainyday.saveableapp.ui.appContainer
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleListDetailScreen(listId: Long, onBack: () -> Unit) {
    val container = appContainer()
    val shareText = rememberShareText()
    val viewModel: SimpleListItemViewModel = viewModel(
        factory = viewModelFactory { initializer { SimpleListItemViewModel(listId, container.listsRepository) } }
    )
    val list by viewModel.list.collectAsState()
    val items by viewModel.items.collectAsState()
    val showCheckbox = list?.showCheckbox ?: true
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var itemPendingEdit by remember { mutableStateOf<SimpleListItemEntity?>(null) }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.moveItem(from.index, to.index)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(list?.name ?: "List") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val text = buildString {
                            appendLine(list?.name ?: "List")
                            items.forEach { item ->
                                if (showCheckbox) append(if (item.isChecked) "[x] " else "[ ] ")
                                append(item.text)
                                if (!item.note.isNullOrBlank()) append(" — ${item.note}")
                                appendLine()
                            }
                        }
                        shareText.share(text)
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New item")
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = "Nothing here yet",
                subtitle = "Add your first item to this list.",
                actionLabel = "New item",
                onAction = { showAddDialog = true },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(items, key = { _, item -> item.id }) { _, item ->
                    ReorderableItem(reorderableState, key = item.id) { _ ->
                        ItemRow(
                            item = item,
                            showCheckbox = showCheckbox,
                            onToggleChecked = { viewModel.setChecked(item, it) },
                            onClick = { itemPendingEdit = item },
                            dragHandle = { Modifier.draggableHandle() }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ItemEditDialog(
            title = "New item",
            onDismiss = { showAddDialog = false },
            onConfirm = { text, note ->
                viewModel.createItem(text, note)
                showAddDialog = false
            }
        )
    }

    itemPendingEdit?.let { item ->
        ItemEditDialog(
            title = "Edit item",
            initialText = item.text,
            initialNote = item.note.orEmpty(),
            onDismiss = { itemPendingEdit = null },
            onConfirm = { text, note ->
                viewModel.updateItem(item, text, note)
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
}

@Composable
private fun ItemRow(
    item: SimpleListItemEntity,
    showCheckbox: Boolean,
    onToggleChecked: (Boolean) -> Unit,
    onClick: () -> Unit,
    dragHandle: @Composable () -> Modifier
) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showCheckbox) {
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
