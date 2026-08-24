package com.rainyday.saveableapp.ui.screens.todo

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rainyday.saveableapp.R
import com.rainyday.saveableapp.data.local.Priority
import com.rainyday.saveableapp.data.local.RecurrenceRule
import com.rainyday.saveableapp.data.local.TagEntity
import com.rainyday.saveableapp.data.local.TaskWithTags
import com.rainyday.saveableapp.data.local.TodoListEntity
import com.rainyday.saveableapp.data.local.TodoTaskEntity
import com.rainyday.saveableapp.data.repository.TodoListSnapshot
import com.rainyday.saveableapp.ui.components.EditListDialog
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.LoadingIndicator
import com.rainyday.saveableapp.ui.components.ScreenHeader
import com.rainyday.saveableapp.ui.components.TagChip
import com.rainyday.saveableapp.ui.components.parseHexColor
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import com.rainyday.saveableapp.ui.theme.AppAlpha
import com.rainyday.saveableapp.ui.theme.Dimens
import com.rainyday.saveableapp.ui.theme.PillShape
import com.rainyday.saveableapp.ui.theme.SaveableAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(onOpenSearch: () -> Unit) {
    val viewModel: TasksViewModel = hiltViewModel()
    val lists by viewModel.lists.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val archivedTasks by viewModel.archivedTasks.collectAsState()
    val lastUsedListId by viewModel.lastUsedListId.collectAsState()
    val aiParsing by viewModel.aiParsing.collectAsState()

    TasksScreenContent(
        onOpenSearch = onOpenSearch,
        lists = lists,
        groups = groups,
        tags = tags,
        sort = sort,
        filter = filter,
        archivedTasks = archivedTasks,
        lastUsedListId = lastUsedListId,
        aiParsing = aiParsing,
        onSetFilter = viewModel::setFilter,
        onSetSort = viewModel::setSort,
        onArchiveAllCompleted = viewModel::archiveAllCompleted,
        onBulkSetDone = viewModel::bulkSetDone,
        onBulkMoveToList = viewModel::bulkMoveToList,
        onBulkDeleteWithUndo = viewModel::bulkDeleteWithUndo,
        onRestoreTasks = viewModel::restoreTasks,
        onSetDone = viewModel::setDone,
        onCreateListAndSelect = viewModel::createListAndSelect,
        onCreateTag = viewModel::createTag,
        onCreateTask = viewModel::createTask,
        onUpdateTask = viewModel::updateTask,
        onDeleteTaskWithUndo = viewModel::deleteTaskWithUndo,
        onRestoreTask = viewModel::restoreTask,
        onCreateList = viewModel::createList,
        onUpdateList = viewModel::updateList,
        onDeleteListWithUndo = viewModel::deleteListWithUndo,
        onRestoreList = viewModel::restoreList,
        onUnarchiveTask = viewModel::unarchiveTask,
        onParseTaskWithAi = viewModel::parseTaskWithAi
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksScreenContent(
    onOpenSearch: () -> Unit,
    lists: List<TodoListEntity>,
    groups: List<TaskGroup>?,
    tags: List<TagEntity>,
    sort: TaskSort,
    filter: TaskFilter,
    archivedTasks: List<TaskWithTags>,
    lastUsedListId: String?,
    aiParsing: Boolean,
    onSetFilter: (TaskFilter) -> Unit = {},
    onSetSort: (TaskSort) -> Unit = {},
    onArchiveAllCompleted: () -> Unit = {},
    onBulkSetDone: (List<TaskWithTags>, Boolean) -> Unit = { _, _ -> },
    onBulkMoveToList: (List<TaskWithTags>, String) -> Unit = { _, _ -> },
    onBulkDeleteWithUndo: suspend (List<TaskWithTags>) -> List<Pair<TodoTaskEntity, List<String>>> = { emptyList() },
    onRestoreTasks: suspend (List<Pair<TodoTaskEntity, List<String>>>) -> Unit = {},
    onSetDone: (TodoTaskEntity, Boolean, List<String>) -> Unit = { _, _, _ -> },
    onCreateListAndSelect: suspend (String) -> String = { "" },
    onCreateTag: (String, String) -> Unit = { _, _ -> },
    onCreateTask: (String, String, String?, Priority, Long?, String?, List<String>, RecurrenceRule) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onUpdateTask: (TodoTaskEntity, String, String, String?, Priority, Long?, String?, List<String>, RecurrenceRule) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onDeleteTaskWithUndo: suspend (TodoTaskEntity, List<String>) -> Pair<TodoTaskEntity, List<String>> = { task, tagIds -> task to tagIds },
    onRestoreTask: suspend (Pair<TodoTaskEntity, List<String>>) -> Unit = {},
    onCreateList: (String, String, String) -> Unit = { _, _, _ -> },
    onUpdateList: (TodoListEntity, String, String, String) -> Unit = { _, _, _, _ -> },
    onDeleteListWithUndo: suspend (TodoListEntity) -> TodoListSnapshot = { TodoListSnapshot(it, emptyList()) },
    onRestoreList: suspend (TodoListSnapshot) -> Unit = {},
    onUnarchiveTask: (TodoTaskEntity) -> Unit = {},
    onParseTaskWithAi: suspend (String) -> AiParseOutcome = { AiParseOutcome.Error("") }
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var quickAddTitle by remember { mutableStateOf("") }
    var aiDraft by remember { mutableStateOf<AiTaskDraft?>(null) }
    var taskPendingEdit by remember { mutableStateOf<Pair<String, TaskWithTags>?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var listPendingEdit by remember { mutableStateOf<TodoListEntity?>(null) }
    val collapsedListIds = remember { mutableStateMapOf<String, Boolean>() }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedTaskIds by remember { mutableStateOf(emptySet<String>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun toggleSelected(id: String) {
        selectedTaskIds = if (id in selectedTaskIds) selectedTaskIds - id else selectedTaskIds + id
        if (selectedTaskIds.isEmpty()) selectionMode = false
    }

    fun clearSelection() {
        selectionMode = false
        selectedTaskIds = emptySet()
    }

    var quickAddText by remember { mutableStateOf("") }
    val undoActionLabel = stringResource(R.string.action_undo)

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val currentGroups = groups
            when {
                currentGroups == null -> LoadingIndicator(modifier = Modifier.weight(1f))
                lists.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Checklist,
                    title = stringResource(R.string.tasks_empty_lists_title),
                    subtitle = stringResource(R.string.tasks_empty_lists_subtitle),
                    actionLabel = stringResource(R.string.action_new_list),
                    onAction = { showCreateListDialog = true },
                    modifier = Modifier.weight(1f)
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = Dimens.d16),
                        verticalArrangement = Arrangement.spacedBy(Dimens.d4)
                    ) {
                        item {
                            ScreenHeader(
                                eyebrow = stringResource(R.string.tasks_screen_eyebrow),
                                title = stringResource(R.string.tasks_screen_title),
                                subtitle = stringResource(R.string.tasks_screen_subtitle),
                                onActionClick = onOpenSearch
                            )
                        }
                        item {
                            if (selectionMode) {
                                val selectedItems = currentGroups.flatMap { it.tasks }
                                    .filter { it.task.id in selectedTaskIds }
                                val deletedTasksMessage = stringResource(R.string.tasks_deleted_count, selectedItems.size)
                                SelectionActionBar(
                                    count = selectedTaskIds.size,
                                    availableLists = lists,
                                    onMarkDone = {
                                        onBulkSetDone(selectedItems, true)
                                        clearSelection()
                                    },
                                    onMoveToList = { listId ->
                                        onBulkMoveToList(selectedItems, listId)
                                        clearSelection()
                                    },
                                    onDelete = {
                                        clearSelection()
                                        scope.launch {
                                            snackbarHostState.showUndoableDelete(
                                                message = deletedTasksMessage,
                                                actionLabel = undoActionLabel,
                                                delete = { onBulkDeleteWithUndo(selectedItems) },
                                                restore = { onRestoreTasks(it) }
                                            )
                                        }
                                    },
                                    onClose = { clearSelection() }
                                )
                                return@item
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = Dimens.d20, vertical = Dimens.d8),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.d8),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val filterLabels = mapOf(
                                    TaskFilter.ALL to stringResource(R.string.filter_all),
                                    TaskFilter.ACTIVE to stringResource(R.string.filter_active),
                                    TaskFilter.COMPLETED to stringResource(R.string.filter_completed)
                                )
                                filterLabels.forEach { (option, label) ->
                                    FilterChip(
                                        selected = filter == option,
                                        onClick = { onSetFilter(option) },
                                        label = { Text(label) }
                                    )
                                }
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.cd_sort))
                                }
                                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                    val labels = mapOf(
                                        TaskSort.DEFAULT to stringResource(R.string.sort_default),
                                        TaskSort.PRIORITY to stringResource(R.string.sort_priority),
                                        TaskSort.DUE_DATE to stringResource(R.string.sort_due_date),
                                        TaskSort.ALPHABETICAL to stringResource(R.string.sort_alphabetical)
                                    )
                                    labels.forEach { (option, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            trailingIcon = { if (sort == option) Icon(Icons.Filled.Check, contentDescription = null) },
                                            onClick = {
                                                onSetSort(option)
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                                IconButton(onClick = { showCreateListDialog = true }) {
                                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_new_list))
                                }
                                IconButton(onClick = { showOverflowMenu = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_more))
                                }
                                DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.tasks_archive_completed)) },
                                        onClick = {
                                            onArchiveAllCompleted()
                                            showOverflowMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.tasks_view_archived, archivedTasks.size)) },
                                        onClick = {
                                            showArchived = true
                                            showOverflowMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        currentGroups.forEach { group ->
                            val collapsed = collapsedListIds[group.list.id] == true
                            item(key = "header-${group.list.id}") {
                                TaskGroupHeader(
                                    group = group,
                                    collapsed = collapsed,
                                    onToggle = { collapsedListIds[group.list.id] = !collapsed },
                                    onLongClick = { listPendingEdit = group.list }
                                )
                            }
                            if (!collapsed) {
                                items(group.tasks, key = { "task-${it.task.id}" }) { item ->
                                    TaskRow(
                                        item = item,
                                        selectionMode = selectionMode,
                                        selected = item.task.id in selectedTaskIds,
                                        onToggleDone = { onSetDone(item.task, it, item.tags.map { tag -> tag.id }) },
                                        onClick = {
                                            if (selectionMode) toggleSelected(item.task.id) else taskPendingEdit = group.list.id to item
                                        },
                                        onLongClick = {
                                            selectionMode = true
                                            toggleSelected(item.task.id)
                                        },
                                        modifier = Modifier.padding(horizontal = Dimens.d20, vertical = Dimens.d4)
                                    )
                                }
                            }
                        }
                    }
                    QuickAddBar(
                        text = quickAddText,
                        onTextChange = { quickAddText = it },
                        busy = aiParsing,
                        onSubmit = {
                            if (quickAddText.isNotBlank() && lists.isNotEmpty() && !aiParsing) {
                                val input = quickAddText.trim()
                                quickAddTitle = input
                                quickAddText = ""
                                aiDraft = null
                                scope.launch {
                                    when (val outcome = onParseTaskWithAi(input)) {
                                        is AiParseOutcome.Success -> aiDraft = outcome.draft
                                        is AiParseOutcome.Error -> snackbarHostState.showSnackbar(outcome.message)
                                    }
                                    showAddSheet = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        val draft = aiDraft
        val defaultListId = draft?.listId
            ?: lastUsedListId?.takeIf { id -> lists.any { it.id == id } }
            ?: lists.firstOrNull()?.id
            ?: ""
        TaskEditSheet(
            availableLists = lists,
            initialListId = defaultListId,
            initialTitle = draft?.title ?: quickAddTitle,
            initialNotes = draft?.notes.orEmpty(),
            initialPriority = draft?.priority ?: Priority.MEDIUM,
            initialDueDate = draft?.dueDate,
            initialTagIds = draft?.tagIds?.toSet() ?: emptySet(),
            initialRecurrence = draft?.recurrence ?: RecurrenceRule.NONE,
            suggestedNewListName = draft?.suggestedNewListName,
            onCreateSuggestedList = onCreateListAndSelect,
            availableTags = tags,
            onCreateTag = onCreateTag,
            onDismiss = { showAddSheet = false; quickAddTitle = ""; aiDraft = null },
            onSave = { listId, title, notes, priority, dueDate, colorHex, tagIds, recurrence ->
                onCreateTask(listId, title, notes, priority, dueDate, colorHex, tagIds, recurrence)
                showAddSheet = false
                quickAddTitle = ""
                aiDraft = null
            }
        )
    }

    taskPendingEdit?.let { (listId, item) ->
        val deletedTaskMessage = stringResource(R.string.deleted_named_item, item.task.title)
        TaskEditSheet(
            availableLists = lists,
            initialListId = listId,
            initialTitle = item.task.title,
            initialNotes = item.task.notes.orEmpty(),
            initialPriority = item.task.priority,
            initialDueDate = item.task.dueDate,
            initialColorHex = item.task.colorHex,
            initialTagIds = item.tags.map { it.id }.toSet(),
            initialRecurrence = item.task.recurrence,
            availableTags = tags,
            onCreateTag = onCreateTag,
            onDismiss = { taskPendingEdit = null },
            onSave = { newListId, title, notes, priority, dueDate, colorHex, tagIds, recurrence ->
                onUpdateTask(item.task, newListId, title, notes, priority, dueDate, colorHex, tagIds, recurrence)
                taskPendingEdit = null
            },
            onDelete = {
                taskPendingEdit = null
                val tagIds = item.tags.map { it.id }
                scope.launch {
                    snackbarHostState.showUndoableDelete(
                        message = deletedTaskMessage,
                        actionLabel = undoActionLabel,
                        delete = { onDeleteTaskWithUndo(item.task, tagIds) },
                        restore = { onRestoreTask(it) }
                    )
                }
            }
        )
    }

    if (showCreateListDialog) {
        EditListDialog(
            title = stringResource(R.string.action_new_list),
            confirmLabel = stringResource(R.string.action_create),
            onDismiss = { showCreateListDialog = false },
            onConfirm = { result ->
                onCreateList(result.name, result.icon, result.colorHex)
                showCreateListDialog = false
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
            onDismiss = { listPendingEdit = null },
            onConfirm = { result ->
                onUpdateList(list, result.name, result.icon, result.colorHex)
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

    if (showArchived) {
        val deletedItemMessagePattern = stringResource(R.string.deleted_named_item)
        ArchivedTasksSheet(
            tasks = archivedTasks,
            onDismiss = { showArchived = false },
            onRestore = { onUnarchiveTask(it) },
            onDeletePermanently = { task, tagIds ->
                scope.launch {
                    snackbarHostState.showUndoableDelete(
                        message = deletedItemMessagePattern.format(task.title),
                        actionLabel = undoActionLabel,
                        delete = { onDeleteTaskWithUndo(task, tagIds) },
                        restore = { onRestoreTask(it) }
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskGroupHeader(
    group: TaskGroup,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.d20, vertical = Dimens.d10)
            .combinedClickable(onClick = onToggle, onLongClick = onLongClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (collapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = group.list.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = Dimens.d8)
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = Dimens.d10, vertical = Dimens.d4)
        ) {
            Text(
                text = group.tasks.size.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectionActionBar(
    count: Int,
    availableLists: List<TodoListEntity>,
    onMarkDone: () -> Unit,
    onMoveToList: (String) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    var showMoveMenu by remember { mutableStateOf(false) }
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
        IconButton(onClick = onMarkDone) {
            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.cd_mark_done))
        }
        Box {
            IconButton(onClick = { showMoveMenu = true }, enabled = availableLists.isNotEmpty()) {
                Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = stringResource(R.string.cd_move_to_list))
            }
            DropdownMenu(expanded = showMoveMenu, onDismissRequest = { showMoveMenu = false }) {
                availableLists.forEach { list ->
                    DropdownMenuItem(
                        text = { Text(list.name) },
                        onClick = {
                            showMoveMenu = false
                            onMoveToList(list.id)
                        }
                    )
                }
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_selected))
        }
    }
}

@Composable
private fun QuickAddBar(text: String, onTextChange: (String) -> Unit, busy: Boolean = false, onSubmit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Dimens.d20, end = Dimens.d20, top = Dimens.d12, bottom = Dimens.d4),
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
                        stringResource(R.string.tasks_quick_add_busy_placeholder)
                    } else {
                        stringResource(R.string.tasks_quick_add_placeholder)
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
                    contentDescription = stringResource(R.string.cd_add_task),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Dimens.d18)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchivedTasksSheet(
    tasks: List<TaskWithTags>,
    onDismiss: () -> Unit,
    onRestore: (TodoTaskEntity) -> Unit,
    onDeletePermanently: (TodoTaskEntity, List<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = Dimens.d20, vertical = Dimens.d8)) {
            Text(stringResource(R.string.tasks_archived_title), style = MaterialTheme.typography.titleMedium)
            if (tasks.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Inventory2,
                    title = stringResource(R.string.tasks_archived_empty_title),
                    subtitle = stringResource(R.string.tasks_archived_empty_subtitle),
                    modifier = Modifier.padding(vertical = Dimens.d24)
                )
            } else {
                Column(modifier = Modifier.padding(top = Dimens.d8, bottom = Dimens.d24)) {
                    tasks.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimens.d6),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.task.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onRestore(item.task) }) {
                                Icon(Icons.Filled.Restore, contentDescription = stringResource(R.string.cd_restore))
                            }
                            IconButton(onClick = { onDeletePermanently(item.task, item.tags.map { it.id }) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_permanently))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    item: TaskWithTags,
    onToggleDone: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    selectionMode: Boolean = false,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val task = item.task
    val accentHex = task.colorHex ?: task.priority.accentHex()
    val accent = parseHexColor(accentHex)

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.d0),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = AppAlpha.a12)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.d8, horizontal = Dimens.d12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = if (selectionMode) selected else task.isDone,
                onCheckedChange = if (selectionMode) { _ -> onClick() } else onToggleDone
            )
            Column(modifier = Modifier.padding(start = Dimens.d4)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.padding(top = Dimens.d4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.d6)
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.d8)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Text(
                        text = task.priority.label().uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.dueDate != null) {
                        Text(
                            text = formatDate(task.dueDate),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (task.recurrence != RecurrenceRule.NONE) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = task.recurrence.label(),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Dimens.d14)
                        )
                    }
                }
                if (item.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(top = Dimens.d6)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.d6)
                    ) {
                        item.tags.forEach { tag -> TagChip(label = "#${tag.name}", onClick = onClick) }
                    }
                }
            }
        }
    }
}

private val previewTodoLists = listOf(
    TodoListEntity(id = "list-1", name = "Personal", colorHex = "#6750A4", icon = "checklist", position = 0, createdAt = 0L, updatedAt = 0L),
    TodoListEntity(id = "list-2", name = "Work", colorHex = "#1E88E5", icon = "work", position = 1, createdAt = 0L, updatedAt = 0L)
)

private val previewTags = listOf(
    TagEntity(id = "tag-1", name = "urgent", colorHex = "#E53935", updatedAt = 0L),
    TagEntity(id = "tag-2", name = "home", colorHex = "#43A047", updatedAt = 0L)
)

private val previewTaskGroups = listOf(
    TaskGroup(
        list = previewTodoLists[0],
        tasks = listOf(
            TaskWithTags(
                task = TodoTaskEntity(
                    id = "task-1",
                    listId = "list-1",
                    title = "Buy groceries",
                    notes = "Milk, eggs, bread",
                    priority = Priority.MEDIUM,
                    dueDate = System.currentTimeMillis(),
                    createdAt = 0L,
                    updatedAt = 0L
                ),
                tags = listOf(previewTags[1])
            ),
            TaskWithTags(
                task = TodoTaskEntity(
                    id = "task-2",
                    listId = "list-1",
                    title = "Call the dentist",
                    isDone = true,
                    priority = Priority.LOW,
                    createdAt = 0L,
                    updatedAt = 0L
                ),
                tags = emptyList()
            )
        )
    ),
    TaskGroup(
        list = previewTodoLists[1],
        tasks = listOf(
            TaskWithTags(
                task = TodoTaskEntity(
                    id = "task-3",
                    listId = "list-2",
                    title = "Finish quarterly report",
                    priority = Priority.URGENT,
                    recurrence = RecurrenceRule.WEEKLY,
                    createdAt = 0L,
                    updatedAt = 0L
                ),
                tags = listOf(previewTags[0])
            )
        )
    )
)

@Preview(showBackground = true)
@Composable
fun TasksScreenPreview() {
    SaveableAppTheme {
        TasksScreenContent(
            onOpenSearch = {},
            lists = previewTodoLists,
            groups = previewTaskGroups,
            tags = previewTags,
            sort = TaskSort.DEFAULT,
            filter = TaskFilter.ALL,
            archivedTasks = emptyList(),
            lastUsedListId = previewTodoLists.first().id,
            aiParsing = false
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TasksScreenEmptyPreview() {
    SaveableAppTheme {
        TasksScreenContent(
            onOpenSearch = {},
            lists = emptyList(),
            groups = null,
            tags = emptyList(),
            sort = TaskSort.DEFAULT,
            filter = TaskFilter.ALL,
            archivedTasks = emptyList(),
            lastUsedListId = null,
            aiParsing = false
        )
    }
}