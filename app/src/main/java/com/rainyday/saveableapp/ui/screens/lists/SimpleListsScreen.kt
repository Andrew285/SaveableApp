package com.rainyday.saveableapp.ui.screens.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rainyday.saveableapp.data.local.SimpleListEntity
import com.rainyday.saveableapp.ui.appContainer
import com.rainyday.saveableapp.ui.components.EditListDialog
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.IconCatalog
import com.rainyday.saveableapp.ui.components.ListRow
import com.rainyday.saveableapp.ui.components.ListTemplateOption
import com.rainyday.saveableapp.ui.components.EditListResult
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import kotlinx.coroutines.launch

private val templateOptions = simpleListTemplates.map {
    ListTemplateOption(it.label, EditListResult(it.name, it.icon, it.colorHex, it.showCheckbox))
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SimpleListsScreen(
    onOpenList: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val container = appContainer()
    val viewModel: SimpleListsViewModel = viewModel(
        factory = viewModelFactory { initializer { SimpleListsViewModel(container.listsRepository) } }
    )
    val lists by viewModel.lists.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var listPendingEdit by remember { mutableStateOf<SimpleListEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lists") },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New list")
            }
        }
    ) { padding ->
        if (lists.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = "No lists yet",
                subtitle = "Movies to watch, books to read, favorite quotes — any simple list you want to keep.",
                actionLabel = "New list",
                onAction = { showCreateDialog = true },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(lists, key = { it.list.id }) { entry ->
                    val list = entry.list
                    ListRow(
                        title = list.name,
                        subtitle = if (list.showCheckbox && entry.total > 0) "${entry.checked}/${entry.total}" else null,
                        icon = IconCatalog.resolve(list.icon),
                        accentHex = list.colorHex,
                        onClick = { onOpenList(list.id) },
                        onLongClick = { listPendingEdit = list }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        EditListDialog(
            title = "New list",
            confirmLabel = "Create",
            showCheckboxOption = true,
            checkboxOptionLabel = "Items have a checkbox (e.g. watched, read)",
            templates = templateOptions,
            onDismiss = { showCreateDialog = false },
            onConfirm = { result ->
                viewModel.createList(result.name, result.icon, result.colorHex, result.showCheckbox)
                showCreateDialog = false
            }
        )
    }

    listPendingEdit?.let { list ->
        EditListDialog(
            title = "Edit list",
            initialName = list.name,
            initialIcon = list.icon,
            initialColorHex = list.colorHex,
            showCheckboxOption = true,
            initialShowCheckbox = list.showCheckbox,
            checkboxOptionLabel = "Items have a checkbox (e.g. watched, read)",
            onDismiss = { listPendingEdit = null },
            onConfirm = { result ->
                viewModel.updateList(list, result.name, result.icon, result.colorHex, result.showCheckbox)
                listPendingEdit = null
            },
            onDelete = {
                listPendingEdit = null
                scope.launch {
                    snackbarHostState.showUndoableDelete(
                        message = "Deleted \"${list.name}\"",
                        delete = { viewModel.deleteListWithUndo(list) },
                        restore = { viewModel.restoreList(it) }
                    )
                }
            }
        )
    }
}
