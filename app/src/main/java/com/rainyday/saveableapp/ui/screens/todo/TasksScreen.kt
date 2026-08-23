package com.rainyday.saveableapp.ui.screens.todo

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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rainyday.saveableapp.data.local.TodoListEntity
import com.rainyday.saveableapp.data.local.TodoTaskEntity
import com.rainyday.saveableapp.ui.appContainer
import com.rainyday.saveableapp.ui.components.EditListDialog
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.LoadingIndicator
import com.rainyday.saveableapp.ui.components.ScreenHeader
import com.rainyday.saveableapp.ui.components.TagChip
import com.rainyday.saveableapp.ui.components.parseHexColor
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import com.rainyday.saveableapp.ui.theme.PillShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(onOpenSearch: () -> Unit) {
    val container = appContainer()
    val viewModel: TasksViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TasksViewModel(
                    container.todoRepository,
                    container.preferencesRepository,
                    container.groqRepository,
                    container.taskReminderScheduler
                )
            }
        }
    )
    val lists by viewModel.lists.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val archivedTasks by viewModel.archivedTasks.collectAsState()
    val lastUsedListId by viewModel.lastUsedListId.collectAsState()
    val aiParsing by viewModel.aiParsing.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var quickAddTitle by remember { mutableStateOf("") }
    var aiDraft by remember { mutableStateOf<AiTaskDraft?>(null) }
    var taskPendingEdit by remember { mutableStateOf<Pair<Long, com.rainyday.saveableapp.data.local.TaskWithTags>?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var listPendingEdit by remember { mutableStateOf<TodoListEntity?>(null) }
    val collapsedListIds = remember { mutableStateMapOf<Long, Boolean>() }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedTaskIds by remember { mutableStateOf(emptySet<Long>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun toggleSelected(id: Long) {
        selectedTaskIds = if (id in selectedTaskIds) selectedTaskIds - id else selectedTaskIds + id
        if (selectedTaskIds.isEmpty()) selectionMode = false
    }

    fun clearSelection() {
        selectionMode = false
        selectedTaskIds = emptySet()
    }

    var quickAddText by remember { mutableStateOf("") }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val currentGroups = groups
            when {
                currentGroups == null -> LoadingIndicator(modifier = Modifier.weight(1f))
                lists.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Checklist,
                    title = "No lists yet",
                    subtitle = "Create a list to start tracking tasks with priorities, tags, and colors.",
                    actionLabel = "New list",
                    onAction = { showCreateListDialog = true },
                    modifier = Modifier.weight(1f)
                )
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            ScreenHeader(
                                eyebrow = "// TASKS",
                                title = "Active Tasks",
                                subtitle = "Everything on your plate",
                                onActionClick = onOpenSearch
                            )
                        }
                        item {
                            if (selectionMode) {
                                val selectedItems = currentGroups.flatMap { it.tasks }
                                    .filter { it.task.id in selectedTaskIds }
                                SelectionActionBar(
                                    count = selectedTaskIds.size,
                                    availableLists = lists,
                                    onMarkDone = {
                                        viewModel.bulkSetDone(selectedItems, true)
                                        clearSelection()
                                    },
                                    onMoveToList = { listId ->
                                        viewModel.bulkMoveToList(selectedItems, listId)
                                        clearSelection()
                                    },
                                    onDelete = {
                                        clearSelection()
                                        scope.launch {
                                            snackbarHostState.showUndoableDelete(
                                                message = "Deleted ${selectedItems.size} tasks",
                                                delete = { viewModel.bulkDeleteWithUndo(selectedItems) },
                                                restore = { viewModel.restoreTasks(it) }
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
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val filterLabels = mapOf(
                                    TaskFilter.ALL to "All",
                                    TaskFilter.ACTIVE to "Active",
                                    TaskFilter.COMPLETED to "Completed"
                                )
                                filterLabels.forEach { (option, label) ->
                                    FilterChip(
                                        selected = filter == option,
                                        onClick = { viewModel.setFilter(option) },
                                        label = { Text(label) }
                                    )
                                }
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                                }
                                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                    val labels = mapOf(
                                        TaskSort.DEFAULT to "Default",
                                        TaskSort.PRIORITY to "Priority",
                                        TaskSort.DUE_DATE to "Due date",
                                        TaskSort.ALPHABETICAL to "Alphabetical"
                                    )
                                    labels.forEach { (option, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            trailingIcon = { if (sort == option) Icon(Icons.Filled.Check, contentDescription = null) },
                                            onClick = {
                                                viewModel.setSort(option)
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                                IconButton(onClick = { showCreateListDialog = true }) {
                                    Icon(Icons.Filled.Add, contentDescription = "New list")
                                }
                                IconButton(onClick = { showOverflowMenu = true }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                                }
                                DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Archive completed") },
                                        onClick = {
                                            viewModel.archiveAllCompleted()
                                            showOverflowMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("View archived (${archivedTasks.size})") },
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
                                        onToggleDone = { viewModel.setDone(item.task, it, item.tags.map { tag -> tag.id }) },
                                        onClick = {
                                            if (selectionMode) toggleSelected(item.task.id) else taskPendingEdit = group.list.id to item
                                        },
                                        onLongClick = {
                                            selectionMode = true
                                            toggleSelected(item.task.id)
                                        },
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
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
                                    when (val outcome = viewModel.parseTaskWithAi(input)) {
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
            ?: 0L
        TaskEditSheet(
            availableLists = lists,
            initialListId = defaultListId,
            initialTitle = draft?.title ?: quickAddTitle,
            initialNotes = draft?.notes.orEmpty(),
            initialPriority = draft?.priority ?: com.rainyday.saveableapp.data.local.Priority.MEDIUM,
            initialDueDate = draft?.dueDate,
            initialTagIds = draft?.tagIds?.toSet() ?: emptySet(),
            initialRecurrence = draft?.recurrence ?: com.rainyday.saveableapp.data.local.RecurrenceRule.NONE,
            suggestedNewListName = draft?.suggestedNewListName,
            onCreateSuggestedList = viewModel::createListAndSelect,
            availableTags = tags,
            onCreateTag = viewModel::createTag,
            onDismiss = { showAddSheet = false; quickAddTitle = ""; aiDraft = null },
            onSave = { listId, title, notes, priority, dueDate, colorHex, tagIds, recurrence ->
                viewModel.createTask(listId, title, notes, priority, dueDate, colorHex, tagIds, recurrence)
                showAddSheet = false
                quickAddTitle = ""
                aiDraft = null
            }
        )
    }

    taskPendingEdit?.let { (listId, item) ->
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
            onCreateTag = viewModel::createTag,
            onDismiss = { taskPendingEdit = null },
            onSave = { newListId, title, notes, priority, dueDate, colorHex, tagIds, recurrence ->
                viewModel.updateTask(item.task, newListId, title, notes, priority, dueDate, colorHex, tagIds, recurrence)
                taskPendingEdit = null
            },
            onDelete = {
                taskPendingEdit = null
                val tagIds = item.tags.map { it.id }
                scope.launch {
                    snackbarHostState.showUndoableDelete(
                        message = "Deleted \"${item.task.title}\"",
                        delete = { viewModel.deleteTaskWithUndo(item.task, tagIds) },
                        restore = { viewModel.restoreTask(it) }
                    )
                }
            }
        )
    }

    if (showCreateListDialog) {
        EditListDialog(
            title = "New list",
            confirmLabel = "Create",
            onDismiss = { showCreateListDialog = false },
            onConfirm = { result ->
                viewModel.createList(result.name, result.icon, result.colorHex)
                showCreateListDialog = false
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

    if (showArchived) {
        ArchivedTasksSheet(
            tasks = archivedTasks,
            onDismiss = { showArchived = false },
            onRestore = { viewModel.unarchiveTask(it) },
            onDeletePermanently = { task, tagIds ->
                scope.launch {
                    snackbarHostState.showUndoableDelete(
                        message = "Deleted \"${task.title}\"",
                        delete = { viewModel.deleteTaskWithUndo(task, tagIds) },
                        restore = { viewModel.restoreTask(it) }
                    )
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
            .padding(horizontal = 20.dp, vertical = 10.dp)
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
                .padding(start = 8.dp)
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 4.dp)
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
    onMoveToList: (Long) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    var showMoveMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
        }
        Text(
            text = "$count selected",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        )
        IconButton(onClick = onMarkDone) {
            Icon(Icons.Filled.Check, contentDescription = "Mark done")
        }
        Box {
            IconButton(onClick = { showMoveMenu = true }, enabled = availableLists.isNotEmpty()) {
                Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move to list")
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
            Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
        }
    }
}

@Composable
private fun QuickAddBar(text: String, onTextChange: (String) -> Unit, busy: Boolean = false, onSubmit: () -> Unit) {
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
            placeholder = { Text(if (busy) "Reading your task..." else "Type a task...") },
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
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Add task",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchivedTasksSheet(
    tasks: List<com.rainyday.saveableapp.data.local.TaskWithTags>,
    onDismiss: () -> Unit,
    onRestore: (TodoTaskEntity) -> Unit,
    onDeletePermanently: (TodoTaskEntity, List<Long>) -> Unit
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Archived tasks", style = MaterialTheme.typography.titleMedium)
            if (tasks.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.Inventory2,
                    title = "Nothing archived",
                    subtitle = "Completed tasks you archive will show up here.",
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)) {
                    tasks.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.task.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onRestore(item.task) }) {
                                Icon(Icons.Filled.Restore, contentDescription = "Restore")
                            }
                            IconButton(onClick = { onDeletePermanently(item.task, item.tags.map { it.id }) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete permanently")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TaskRow(
    item: com.rainyday.saveableapp.data.local.TaskWithTags,
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
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
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = if (selectionMode) selected else task.isDone,
                onCheckedChange = if (selectionMode) { _ -> onClick() } else onToggleDone
            )
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    color = if (task.isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
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
                    if (task.recurrence != com.rainyday.saveableapp.data.local.RecurrenceRule.NONE) {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = task.recurrence.label(),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                if (item.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item.tags.forEach { tag -> TagChip(label = "#${tag.name}", onClick = onClick) }
                    }
                }
            }
        }
    }
}
