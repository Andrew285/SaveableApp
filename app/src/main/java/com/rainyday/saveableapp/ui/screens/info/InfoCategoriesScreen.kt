package com.rainyday.saveableapp.ui.screens.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
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
import com.rainyday.saveableapp.data.local.InfoCategoryEntity
import com.rainyday.saveableapp.ui.appContainer
import com.rainyday.saveableapp.ui.components.EditListDialog
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.IconCatalog
import com.rainyday.saveableapp.ui.components.ListRow
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun InfoCategoriesScreen(
    onOpenCategory: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val container = appContainer()
    val viewModel: InfoCategoriesViewModel = viewModel(
        factory = viewModelFactory { initializer { InfoCategoriesViewModel(container.infoRepository) } }
    )
    val categories by viewModel.categories.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var categoryPendingEdit by remember { mutableStateOf<InfoCategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Info") },
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
                Icon(Icons.Filled.Add, contentDescription = "New category")
            }
        }
    ) { padding ->
        if (categories.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Badge,
                title = "No categories yet",
                subtitle = "Keep sizes, IDs, and important details organized and available at a glance.",
                actionLabel = "New category",
                onAction = { showCreateDialog = true },
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    ListRow(
                        title = category.name,
                        subtitle = null,
                        icon = IconCatalog.resolve(category.icon),
                        accentHex = category.colorHex,
                        onClick = { onOpenCategory(category.id) },
                        onLongClick = { categoryPendingEdit = category }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        EditListDialog(
            title = "New category",
            confirmLabel = "Create",
            initialIcon = "document",
            onDismiss = { showCreateDialog = false },
            onConfirm = { result ->
                viewModel.createCategory(result.name, result.icon, result.colorHex)
                showCreateDialog = false
            }
        )
    }

    categoryPendingEdit?.let { category ->
        EditListDialog(
            title = "Edit category",
            initialName = category.name,
            initialIcon = category.icon,
            initialColorHex = category.colorHex,
            onDismiss = { categoryPendingEdit = null },
            onConfirm = { result ->
                viewModel.updateCategory(category, result.name, result.icon, result.colorHex)
                categoryPendingEdit = null
            },
            onDelete = {
                categoryPendingEdit = null
                scope.launch {
                    snackbarHostState.showUndoableDelete(
                        message = "Deleted \"${category.name}\"",
                        delete = { viewModel.deleteCategoryWithUndo(category) },
                        restore = { viewModel.restoreCategory(it) }
                    )
                }
            }
        )
    }
}
