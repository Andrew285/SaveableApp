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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.local.InfoCategoryEntity
import com.rainyday.saveableapp.data.repository.InfoCategorySnapshot
import com.rainyday.saveableapp.ui.components.DirectoryCard
import com.rainyday.saveableapp.ui.components.EditListDialog
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.IconCatalog
import com.rainyday.saveableapp.ui.components.LoadingIndicator
import com.rainyday.saveableapp.ui.components.PillButtonPrimary
import com.rainyday.saveableapp.ui.components.ScreenHeader
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme
import kotlinx.coroutines.launch

@Composable
fun InfoCategoriesScreen(
    onOpenCategory: (String) -> Unit,
    onOpenSearch: () -> Unit
) {
    val viewModel: InfoCategoriesViewModel = hiltViewModel()
    val categories by viewModel.categories.collectAsState()

    InfoCategoriesScreenContent(
        onOpenCategory = onOpenCategory,
        onOpenSearch = onOpenSearch,
        categories = categories,
        onCreateCategory = viewModel::createCategory,
        onUpdateCategory = viewModel::updateCategory,
        onDeleteCategoryWithUndo = viewModel::deleteCategoryWithUndo,
        onRestoreCategory = viewModel::restoreCategory
    )
}

@Composable
private fun InfoCategoriesScreenContent(
    onOpenCategory: (String) -> Unit,
    onOpenSearch: () -> Unit,
    categories: List<InfoCategoryUiModel>?,
    onCreateCategory: (String, String, String) -> Unit = { _, _, _ -> },
    onUpdateCategory: (InfoCategoryEntity, String, String, String) -> Unit = { _, _, _, _ -> },
    onDeleteCategoryWithUndo: suspend (InfoCategoryEntity) -> InfoCategorySnapshot = { InfoCategorySnapshot(it, emptyList()) },
    onRestoreCategory: suspend (InfoCategorySnapshot) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val undoActionLabel = stringResource(R.string.action_undo)

    var showCreateDialog by remember { mutableStateOf(false) }
    var categoryPendingEdit by remember { mutableStateOf<InfoCategoryEntity?>(null) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val currentCategories = categories
            when {
                currentCategories == null -> LoadingIndicator(modifier = Modifier.weight(1f))
                currentCategories.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Badge,
                    title = stringResource(R.string.info_empty_categories_title),
                    subtitle = stringResource(R.string.info_empty_categories_subtitle),
                    actionLabel = stringResource(R.string.info_new_category),
                    onAction = { showCreateDialog = true },
                    modifier = Modifier.weight(1f)
                )
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = Dimens.d20, vertical = Dimens.d8),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.d12),
                        verticalArrangement = Arrangement.spacedBy(Dimens.d12)
                    ) {
                        item(span = { GridItemSpan(2) }) {
                            ScreenHeader(
                                eyebrow = stringResource(R.string.info_vault_eyebrow),
                                title = stringResource(R.string.info_vault_title),
                                subtitle = stringResource(R.string.info_vault_subtitle),
                                onActionClick = onOpenSearch,
                                modifier = Modifier.padding(horizontal = Dimens.d0)
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
                        text = stringResource(R.string.info_new_category_cta),
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.d20, vertical = Dimens.d12)
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        EditListDialog(
            title = stringResource(R.string.info_new_category),
            confirmLabel = stringResource(R.string.action_create),
            initialIcon = "document",
            onDismiss = { showCreateDialog = false },
            onConfirm = { result ->
                onCreateCategory(result.name, result.icon, result.colorHex)
                showCreateDialog = false
            }
        )
    }

    categoryPendingEdit?.let { category ->
        val deletedCategoryMessage = stringResource(R.string.deleted_named_item, category.name)
        EditListDialog(
            title = stringResource(R.string.info_edit_category),
            initialName = category.name,
            initialIcon = category.icon,
            initialColorHex = category.colorHex,
            onDismiss = { categoryPendingEdit = null },
            onConfirm = { result ->
                onUpdateCategory(category, result.name, result.icon, result.colorHex)
                categoryPendingEdit = null
            },
            onDelete = {
                categoryPendingEdit = null
                scope.launch {
                    snackbarHostState.showUndoableDelete(
                        message = deletedCategoryMessage,
                        actionLabel = undoActionLabel,
                        delete = { onDeleteCategoryWithUndo(category) },
                        restore = { onRestoreCategory(it) }
                    )
                }
            }
        )
    }
}

private val previewInfoCategories = listOf(
    InfoCategoryUiModel(
        category = InfoCategoryEntity(id = "cat-1", name = "Passports", icon = "badge", colorHex = "#6750A4", position = 0, updatedAt = 0L),
        itemCount = 4
    ),
    InfoCategoryUiModel(
        category = InfoCategoryEntity(id = "cat-2", name = "Vehicle", icon = "document", colorHex = "#1E88E5", position = 1, updatedAt = 0L),
        itemCount = 2
    )
)

@Preview(showBackground = true)
@Composable
fun InfoCategoriesScreenPreview() {
    SaveableAppTheme {
        InfoCategoriesScreenContent(
            onOpenCategory = {},
            onOpenSearch = {},
            categories = previewInfoCategories
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InfoCategoriesScreenEmptyPreview() {
    SaveableAppTheme {
        InfoCategoriesScreenContent(
            onOpenCategory = {},
            onOpenSearch = {},
            categories = emptyList()
        )
    }
}
