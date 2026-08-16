package com.rainyday.saveableapp.ui.screens.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
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
import com.rainyday.saveableapp.data.local.TodoListEntity
import com.rainyday.saveableapp.ui.appContainer
import com.rainyday.saveableapp.ui.components.EditListDialog
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.IconCatalog
import com.rainyday.saveableapp.ui.components.ListRow
import com.rainyday.saveableapp.ui.components.LoadingIndicator
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TodoListsScreen(
    onOpenList: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val container = appContainer()
    val viewModel: TodoListsViewModel = viewModel(
        factory = viewModelFactory { initializer { TodoListsViewModel(container.todoRepository) } }
    )
    val lists by viewModel.lists.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var listPendingEdit by remember { mutableStateOf<TodoListEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("To-Do Lists") },
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
        val currentLists = lists
        when {
            currentLists == null -> LoadingIndicator(modifier = Modifier.padding(padding))
            currentLists.isEmpty() -> EmptyState(
                icon = Icons.Filled.Checklist,
                title = "No lists yet",
                subtitle = "Create a list to start tracking tasks with priorities, tags, and colors.",
                actionLabel = "New list",
                onAction = { showCreateDialog = true },
                modifier = Modifier.padding(padding)
            )
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentLists, key = { it.id }) { list ->
                        ListRow(
                            title = list.name,
                            subtitle = null,
                            icon = IconCatalog.resolve(list.icon),
                            accentHex = list.colorHex,
                            onClick = { onOpenList(list.id) },
                            onLongClick = { listPendingEdit = list }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        EditListDialog(
            title = "New list",
            confirmLabel = "Create",
            onDismiss = { showCreateDialog = false },
            onConfirm = { result ->
                viewModel.createList(result.name, result.icon, result.colorHex)
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
            onDismiss = { listPendingEdit = null },
            onConfirm = { result ->
                viewModel.updateList(list, result.name, result.icon, result.colorHex)
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
