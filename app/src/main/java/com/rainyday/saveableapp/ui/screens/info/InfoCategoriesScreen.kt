package com.rainyday.saveableapp.ui.screens.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rainyday.saveableapp.data.local.InfoCategoryEntity
import com.rainyday.saveableapp.ui.components.DirectoryCard
import com.rainyday.saveableapp.ui.components.EditListDialog
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.IconCatalog
import com.rainyday.saveableapp.ui.components.LoadingIndicator
import com.rainyday.saveableapp.ui.components.PillButtonPrimary
import com.rainyday.saveableapp.ui.components.ScreenHeader
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import kotlinx.coroutines.launch

@Composable
fun InfoCategoriesScreen(
    onOpenCategory: (String) -> Unit,
    onOpenSearch: () -> Unit
) {
    val viewModel: InfoCategoriesViewModel = hiltViewModel()
    val categories by viewModel.categories.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var categoryPendingEdit by remember { mutableStateOf<InfoCategoryEntity?>(null) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val currentCategories = categories
            when {
                currentCategories == null -> LoadingIndicator(modifier = Modifier.weight(1f))
                currentCategories.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Badge,
                    title = "No categories yet",
                    subtitle = "Keep sizes, IDs, and important details organized and available at a glance.",
                    actionLabel = "New category",
                    onAction = { showCreateDialog = true },
                    modifier = Modifier.weight(1f)
                )
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(span = { GridItemSpan(2) }) {
                            ScreenHeader(
                                eyebrow = "// VAULT",
                                title = "The Vault",
                                subtitle = "Locked away, always at hand",
                                onActionClick = onOpenSearch,
                                modifier = Modifier.padding(horizontal = 0.dp)
                            )
                        }
                        items(currentCategories, key = { it.category.id }) { entry ->
                            val category = entry.category
                            DirectoryCard(
                                title = category.name,
                                itemCount = entry.itemCount,
                                icon = IconCatalog.resolve(category.icon),
                                accentHex = category.colorHex,
                                onClick = { onOpenCategory(category.id) },
                                onLongClick = { categoryPendingEdit = category }
                            )
                        }
                    }
                    PillButtonPrimary(
                        text = "+ New Category",
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
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
