package com.rainyday.saveableapp.ui.screens.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rainyday.saveableapp.data.local.TaskWithTags
import com.rainyday.saveableapp.data.local.TodoTaskEntity
import com.rainyday.saveableapp.ui.appContainer
import com.rainyday.saveableapp.ui.components.EmptyState
import com.rainyday.saveableapp.ui.components.parseHexColor
import com.rainyday.saveableapp.ui.components.showUndoableDelete
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListDetailScreen(listId: Long, onBack: () -> Unit) {
    val container = appContainer()
    val viewModel: TodoTaskViewModel = viewModel(
        factory = viewModelFactory { initializer { TodoTaskViewModel(listId, container.todoRepository) } }
    )
    val list by viewModel.list.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val archivedTasks by viewModel.archivedTasks.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var taskPendingEdit by remember { mutableStateOf<TaskWithTags?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showArchived by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(list?.name ?: "Tasks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Archive completed") },
                            onClick = {
                                viewModel.archiveCompleted()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("View archived (${archivedTasks.size})") },
                            onClick = {
                                showArchived = true
                                showMenu = false
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New task")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
            }
            if (tasks.isEmpty()) {
                val showCreateAction = filter == TaskFilter.ALL
                EmptyState(
                    icon = Icons.Filled.Checklist,
                    title = if (showCreateAction) "No tasks yet" else "Nothing here",
                    subtitle = "Add a task with a priority, tags, and a color to keep this list organized.",
                    actionLabel = if (showCreateAction) "New task" else null,
                    onAction = if (showCreateAction) ({ showAddSheet = true }) else null
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { it.task.id }) { item ->
                        TaskRow(
                            item = item,
                            onToggleDone = { viewModel.setDone(item.task, it) },
                            onClick = { taskPendingEdit = item }
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        TaskEditSheet(
            availableTags = tags,
            onCreateTag = viewModel::createTag,
            onDismiss = { showAddSheet = false },
            onSave = { title, notes, priority, dueDate, colorHex, tagIds ->
                viewModel.createTask(title, notes, priority, dueDate, colorHex, tagIds)
                showAddSheet = false
            }
        )
    }

    taskPendingEdit?.let { item ->
        TaskEditSheet(
            initialTitle = item.task.title,
            initialNotes = item.task.notes.orEmpty(),
            initialPriority = item.task.priority,
            initialDueDate = item.task.dueDate,
            initialColorHex = item.task.colorHex,
            initialTagIds = item.tags.map { it.id }.toSet(),
            availableTags = tags,
            onCreateTag = viewModel::createTag,
            onDismiss = { taskPendingEdit = null },
            onSave = { title, notes, priority, dueDate, colorHex, tagIds ->
                viewModel.updateTask(item.task, title, notes, priority, dueDate, colorHex, tagIds)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchivedTasksSheet(
    tasks: List<TaskWithTags>,
    onDismiss: () -> Unit,
    onRestore: (TodoTaskEntity) -> Unit,
    onDeletePermanently: (TodoTaskEntity, List<Long>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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

@Composable
private fun TaskRow(
    item: TaskWithTags,
    onToggleDone: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val task = item.task
    val accentHex = task.colorHex ?: task.priority.accentHex()
    val accent = parseHexColor(accentHex)

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
            Checkbox(checked = task.isDone, onCheckedChange = onToggleDone)
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
                        text = task.priority.label(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (task.dueDate != null) {
                        Text(
                            text = "• ${formatDate(task.dueDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item.tags.forEach { tag ->
                        SuggestionChip(
                            onClick = onClick,
                            label = { Text(tag.name, style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = parseHexColor(tag.colorHex).copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }
    }
}
