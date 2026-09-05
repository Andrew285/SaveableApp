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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.local.FieldDefinitionEntity
import com.rainyday.saveableapp.data.local.FieldTemplate
import com.rainyday.saveableapp.data.local.FieldType
import com.rainyday.saveableapp.data.local.SimpleListEntity
import com.rainyday.saveableapp.data.repository.SimpleListSnapshot
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
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.PillShape
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme
import kotlinx.coroutines.launch

private val templateOptions = simpleListTemplates.map {
    ListTemplateOption(it.label, EditListResult(it.name, it.icon, it.colorHex, it.showCheckbox, it.fields))
}

@Composable
fun SimpleListsScreen(
    onOpenList: (String) -> Unit,
    onOpenSearch: () -> Unit
) {
    val viewModel: SimpleListsViewModel = hiltViewModel()
    val lists by viewModel.lists.collectAsState()
    val aiParsing by viewModel.aiParsing.collectAsState()
    val fieldsByListId by viewModel.fieldsByListId.collectAsState()

    SimpleListsScreenContent(
        onOpenList = onOpenList,
        onOpenSearch = onOpenSearch,
        lists = lists,
        aiParsing = aiParsing,
        fieldsByListId = fieldsByListId,
        onCreateList = viewModel::createList,
        onUpdateList = viewModel::updateList,
        onCreateListAndSelect = viewModel::createListAndSelect,
        onDeleteListWithUndo = viewModel::deleteListWithUndo,
        onRestoreList = viewModel::restoreList,
        onCreateItem = viewModel::createItem,
        onParseItemWithAi = viewModel::parseItemWithAi
    )
}

@Composable
private fun SimpleListsScreenContent(
    onOpenList: (String) -> Unit,
    onOpenSearch: () -> Unit,
    lists: List<SimpleListUiModel>?,
    aiParsing: Boolean,
    fieldsByListId: Map<String, List<FieldDefinitionEntity>>,
    onCreateList: (String, String, String, Boolean, List<FieldTemplate>) -> Unit = { _, _, _, _, _ -> },
    onUpdateList: (SimpleListEntity, String, String, String, Boolean) -> Unit = { _, _, _, _, _ -> },
    onCreateListAndSelect: suspend (String) -> String = { "" },
    onDeleteListWithUndo: suspend (SimpleListEntity) -> SimpleListSnapshot = { SimpleListSnapshot(it, emptyList()) },
    onRestoreList: suspend (SimpleListSnapshot) -> Unit = {},
    onCreateItem: (String, String, String?, String?, String?, Map<String, String>) -> Unit = { _, _, _, _, _, _ -> },
    onParseItemWithAi: suspend (String) -> AiListItemOutcome = { AiListItemOutcome.Error("") }
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var listPendingEdit by remember { mutableStateOf<SimpleListEntity?>(null) }
    var aiInputText by remember { mutableStateOf("") }
    var aiDraft by remember { mutableStateOf<AiListItemDraft?>(null) }
    var showAiReviewSheet by remember { mutableStateOf(false) }
    val undoActionLabel = stringResource(R.string.action_undo)

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val currentLists = lists
            when {
                currentLists == null -> LoadingIndicator(modifier = Modifier.weight(1f))
                currentLists.isEmpty() -> EmptyState(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = stringResource(R.string.lists_empty_title),
                    subtitle = stringResource(R.string.lists_empty_subtitle),
                    actionLabel = stringResource(R.string.action_new_list),
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
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                            ScreenHeader(
                                eyebrow = stringResource(R.string.lists_screen_eyebrow),
                                title = stringResource(R.string.lists_screen_title),
                                subtitle = stringResource(R.string.lists_screen_subtitle),
                                onActionClick = onOpenSearch,
                                modifier = Modifier.padding(horizontal = Dimens.d0)
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
                                    when (val outcome = onParseItemWithAi(input)) {
                                        is AiListItemOutcome.Success -> aiDraft = outcome.draft
                                        is AiListItemOutcome.Error -> snackbarHostState.showSnackbar(outcome.message)
                                    }
                                    showAiReviewSheet = true
                                }
                            }
                        }
                    )
                    PillButtonPrimary(
                        text = stringResource(R.string.lists_new_list_button),
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = Dimens.d20, end = Dimens.d20, top = Dimens.d12, bottom = Dimens.d4)
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        EditListDialog(
            title = stringResource(R.string.action_new_list),
            confirmLabel = stringResource(R.string.action_create),
            showCheckboxOption = true,
            checkboxOptionLabel = stringResource(R.string.lists_checkbox_option_label),
            templates = templateOptions,
            onDismiss = { showCreateDialog = false },
            onConfirm = { result ->
                onCreateList(result.name, result.icon, result.colorHex, result.showCheckbox, result.fieldTemplates)
                showCreateDialog = false
            }
        )
    }

    listPendingEdit?.let { list ->
        val deletedListMessage = stringResource(R.string.deleted_named_item, list.name)
        EditListDialog(
            title = stringResource(R.string.action_edit_list),
            initialName = list.name,
            initialIcon = list.icon,
            initialColorHex = list.colorHex,
            showCheckboxOption = true,
            initialShowCheckbox = list.showCheckbox,
            checkboxOptionLabel = stringResource(R.string.lists_checkbox_option_label),
            onDismiss = { listPendingEdit = null },
            onConfirm = { result ->
                onUpdateList(list, result.name, result.icon, result.colorHex, result.showCheckbox)
                listPendingEdit = null
            },
            onDelete = {
                listPendingEdit = null
                scope.launch {
                    snackbarHostState.showUndoableDelete(
                        message = deletedListMessage,
                        actionLabel = undoActionLabel,
                        delete = { onDeleteListWithUndo(list) },
                        restore = { onRestoreList(it) }
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
            initialListId = draft?.listId ?: availableLists.firstOrNull()?.id ?: "",
            initialText = draft?.text ?: aiInputText,
            initialNote = draft?.note.orEmpty(),
            initialUrl = draft?.url.orEmpty(),
            initialImageUrl = draft?.imageUrl.orEmpty(),
            initialFieldValues = draft?.fieldValues.orEmpty(),
            suggestedNewListName = draft?.suggestedNewListName,
            onCreateSuggestedList = onCreateListAndSelect,
            onDismiss = { showAiReviewSheet = false; aiDraft = null },
            onSave = { listId, text, note, url, imageUrl, fieldValues ->
                onCreateItem(listId, text, note, url, imageUrl, fieldValues)
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
            .padding(horizontal = Dimens.d20, vertical = Dimens.d12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(end = Dimens.d8)
                .size(Dimens.d20)
        )
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = {
                Text(
                    if (busy) {
                        stringResource(R.string.lists_ai_add_busy_placeholder)
                    } else {
                        stringResource(R.string.lists_ai_add_placeholder)
                    }
                )
            },
            enabled = !busy,
            singleLine = true,
            shape = PillShape,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .padding(start = Dimens.d8)
                .size(Dimens.d40)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(enabled = !busy, onClick = onSubmit),
            contentAlignment = Alignment.Center
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.d18),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = Dimens.d2
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.cd_add_item),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Dimens.d18)
                )
            }
        }
    }
}

private val previewSimpleLists = listOf(
    SimpleListUiModel(
        list = SimpleListEntity(id = "list-1", name = "Movies to Watch", icon = "movie", colorHex = "#6750A4", createdAt = 0L, updatedAt = 0L),
        checked = 3,
        total = 8
    ),
    SimpleListUiModel(
        list = SimpleListEntity(id = "list-2", name = "Books to Read", icon = "book", colorHex = "#1E88E5", createdAt = 0L, updatedAt = 0L),
        checked = 1,
        total = 5
    )
)

private val previewFieldsByListId = mapOf(
    "list-2" to listOf(
        FieldDefinitionEntity(id = "field-1", listId = "list-2", name = "Author", type = FieldType.TEXT, colorHex = "#43A047", createdAt = 0L, updatedAt = 0L)
    )
)

@Preview(showBackground = true)
@Composable
fun SimpleListsScreenPreview() {
    SaveableAppTheme {
        SimpleListsScreenContent(
            onOpenList = {},
            onOpenSearch = {},
            lists = previewSimpleLists,
            aiParsing = false,
            fieldsByListId = previewFieldsByListId
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SimpleListsScreenEmptyPreview() {
    SaveableAppTheme {
        SimpleListsScreenContent(
            onOpenList = {},
            onOpenSearch = {},
            lists = emptyList(),
            aiParsing = false,
            fieldsByListId = emptyMap()
        )
    }
}
