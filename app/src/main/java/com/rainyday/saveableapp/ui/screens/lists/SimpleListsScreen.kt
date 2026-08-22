package com.rainyday.saveableapp.ui.screens.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rainyday.saveableapp.data.local.SimpleListEntity
import com.rainyday.saveableapp.ui.appContainer
import com.rainyday.saveableapp.ui.components.DirectoryCard
import com.rainyday.saveableapp.ui.components.EditListDialog
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.IconCatalog
import com.rainyday.saveableapp.ui.components.ListTemplateOption
import com.rainyday.saveableapp.ui.components.EditListResult
import com.rainyday.saveableapp.ui.components.LoadingIndicator
import com.rainyday.saveableapp.ui.components.PillButtonPrimary
import com.rainyday.saveableapp.ui.components.ScreenHeader
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import com.rainyday.saveableapp.ui.theme.PillShape
import kotlinx.coroutines.launch

private val templateOptions = simpleListTemplates.map {
    ListTemplateOption(it.label, EditListResult(it.name, it.icon, it.colorHex, it.showCheckbox, it.fields))
}

@Composable
fun SimpleListsScreen(
    onOpenList: (Long) -> Unit,
    onOpenSearch: () -> Unit
) {
    val container = appContainer()
    val viewModel: SimpleListsViewModel = viewModel(
        factory = viewModelFactory { initializer { SimpleListsViewModel(container.listsRepository, container.groqRepository) } }
    )
    val lists by viewModel.lists.collectAsState()
    val aiParsing by viewModel.aiParsing.collectAsState()
    val fieldsByListId by viewModel.fieldsByListId.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var listPendingEdit by remember { mutableStateOf<SimpleListEntity?>(null) }
    var aiInputText by remember { mutableStateOf("") }
    var aiDraft by remember { mutableStateOf<AiListItemDraft?>(null) }
    var showAiReviewSheet by remember { mutableStateOf(false) }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val currentLists = lists
            when {
                currentLists == null -> LoadingIndicator(modifier = Modifier.weight(1f))
                currentLists.isEmpty() -> EmptyState(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "No lists yet",
                    subtitle = "Movies to watch, books to read, favorite quotes — any simple list you want to keep.",
                    actionLabel = "New list",
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
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                            ScreenHeader(
                                eyebrow = "// LISTS",
                                title = "Your Lists",
                                subtitle = "Structured collections",
                                onActionClick = onOpenSearch,
                                modifier = Modifier.padding(horizontal = 0.dp)
                            )
                        }
                        items(currentLists, key = { it.list.id }) { entry ->
                            val list = entry.list
                            DirectoryCard(
                                title = list.name,
                                itemCount = entry.total,
                                icon = IconCatalog.resolve(list.icon),
                                accentHex = list.colorHex,
                                onClick = { onOpenList(list.id) },
                                onLongClick = { listPendingEdit = list }
                            )
                        }
                    }
                    AiQuickAddBar(
                        text = aiInputText,
                        onTextChange = { aiInputText = it },
                        busy = aiParsing,
                        onSubmit = {
                            if (aiInputText.isNotBlank() && !aiParsing) {
                                val input = aiInputText.trim()
                                aiInputText = ""
                                aiDraft = null
                                scope.launch {
                                    when (val outcome = viewModel.parseItemWithAi(input)) {
                                        is AiListItemOutcome.Success -> aiDraft = outcome.draft
                                        is AiListItemOutcome.Error -> snackbarHostState.showSnackbar(outcome.message)
                                    }
                                    showAiReviewSheet = true
                                }
                            }
                        }
                    )
                    PillButtonPrimary(
                        text = "+ New List",
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
            title = "New list",
            confirmLabel = "Create",
            showCheckboxOption = true,
            checkboxOptionLabel = "Items have a checkbox (e.g. watched, read)",
            templates = templateOptions,
            onDismiss = { showCreateDialog = false },
            onConfirm = { result ->
                viewModel.createList(result.name, result.icon, result.colorHex, result.showCheckbox, result.fieldTemplates)
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

    if (showAiReviewSheet) {
        val draft = aiDraft
        val availableLists = lists.orEmpty().map { it.list }
        AiAddItemSheet(
            availableLists = availableLists,
            fieldsByListId = fieldsByListId,
            initialListId = draft?.listId ?: availableLists.firstOrNull()?.id ?: 0L,
            initialText = draft?.text ?: aiInputText,
            initialNote = draft?.note.orEmpty(),
            initialUrl = draft?.url.orEmpty(),
            initialFieldValues = draft?.fieldValues.orEmpty(),
            onDismiss = { showAiReviewSheet = false; aiDraft = null },
            onSave = { listId, text, note, url, fieldValues ->
                viewModel.createItem(listId, text, note, url, fieldValues)
                showAiReviewSheet = false
                aiDraft = null
            }
        )
    }
}

@Composable
private fun AiQuickAddBar(text: String, onTextChange: (String) -> Unit, busy: Boolean = false, onSubmit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(20.dp)
        )
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text(if (busy) "Reading your item..." else "e.g. Movie \"Odyssey\"") },
            enabled = !busy,
            singleLine = true,
            shape = PillShape,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(enabled = !busy, onClick = onSubmit),
            contentAlignment = Alignment.Center
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Add item",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
