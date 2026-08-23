package com.rainyday.saveableapp.ui.screens.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainyday.saveableapp.data.ai.OpenRouterRepository
import com.rainyday.saveableapp.data.local.Priority
import com.rainyday.saveableapp.data.local.RecurrenceRule
import com.rainyday.saveableapp.data.local.TagEntity
import com.rainyday.saveableapp.data.local.TaskWithTags
import com.rainyday.saveableapp.data.local.TodoListEntity
import com.rainyday.saveableapp.data.local.TodoTaskEntity
import com.rainyday.saveableapp.data.prefs.PreferencesRepository
import com.rainyday.saveableapp.data.repository.TodoRepository
import com.rainyday.saveableapp.data.scheduling.TaskReminderScheduler
import com.rainyday.saveableapp.ui.components.IconCatalog
import com.rainyday.saveableapp.ui.theme.AccentColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskGroup(val list: TodoListEntity, val tasks: List<TaskWithTags>)

enum class TaskSort { DEFAULT, PRIORITY, DUE_DATE, ALPHABETICAL }
enum class TaskFilter { ALL, ACTIVE, COMPLETED }

/** Task fields resolved from AI parsing, ready to prefill the add-task sheet. */
data class AiTaskDraft(
    val listId: Long,
    val title: String,
    val notes: String?,
    val priority: Priority,
    val dueDate: Long?,
    val tagIds: List<Long>,
    val recurrence: RecurrenceRule,
    /** A brand-new list name the model suggested, if it didn't match any existing list — null otherwise. */
    val suggestedNewListName: String?
)

sealed interface AiParseOutcome {
    data class Success(val draft: AiTaskDraft) : AiParseOutcome
    data class Error(val message: String) : AiParseOutcome
}

private const val UNCATEGORIZED_LIST_NAME = "Uncategorized"
private const val UNCATEGORIZED_LIST_COLOR_HEX = "#9E9E9E"
private const val UNCATEGORIZED_LIST_ICON_KEY = "checklist"

class TasksViewModel(
    private val repository: TodoRepository,
    private val preferencesRepository: PreferencesRepository,
    private val openRouterRepository: OpenRouterRepository,
    private val reminderScheduler: TaskReminderScheduler
) : ViewModel() {
    val lists: StateFlow<List<TodoListEntity>> = repository.observeLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sort = MutableStateFlow(TaskSort.DEFAULT)
    val sort: StateFlow<TaskSort> = _sort

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    val filter: StateFlow<TaskFilter> = _filter

    val groups: StateFlow<List<TaskGroup>?> =
        combine(repository.observeLists(), repository.observeAllTasks(), _sort, _filter) { lists, tasks, sort, filter ->
            val filtered = when (filter) {
                TaskFilter.ALL -> tasks
                TaskFilter.ACTIVE -> tasks.filter { !it.task.isDone }
                TaskFilter.COMPLETED -> tasks.filter { it.task.isDone }
            }
            val byList = filtered.groupBy { it.task.listId }
            lists
                .sortedBy { it.position }
                .map { list ->
                    val listTasks = byList[list.id].orEmpty()
                    val sorted = when (sort) {
                        TaskSort.DEFAULT -> listTasks.sortedBy { it.task.position }
                        TaskSort.PRIORITY -> listTasks.sortedWith(compareBy({ it.task.isDone }, { -it.task.priority.ordinal }))
                        TaskSort.DUE_DATE -> listTasks.sortedWith(compareBy({ it.task.isDone }, { it.task.dueDate ?: Long.MAX_VALUE }))
                        TaskSort.ALPHABETICAL -> listTasks.sortedWith(compareBy({ it.task.isDone }, { it.task.title.lowercase() }))
                    }
                    TaskGroup(list, sorted)
                }
                .filter { it.tasks.isNotEmpty() || filter == TaskFilter.ALL }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val archivedTasks: StateFlow<List<TaskWithTags>> = repository.observeAllArchivedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = repository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastUsedListId: StateFlow<Long?> = preferencesRepository.lastUsedTodoListId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _aiParsing = MutableStateFlow(false)
    val aiParsing: StateFlow<Boolean> = _aiParsing

    /** Sends [input] to the AI parser to extract title, notes, priority, due date, list, and tags. */
    suspend fun parseTaskWithAi(input: String): AiParseOutcome {
        _aiParsing.value = true
        return try {
            val result = openRouterRepository.parseTask(
                input = input,
                existingListNames = lists.value.map { it.name },
                existingTagNames = tags.value.map { it.name }
            )
            result.fold(
                onSuccess = { parsed ->
                    val listId = resolveListId(parsed.listName)
                    val tagIds = parsed.tagNames.mapNotNull { resolveTagId(it) }
                    val suggestedNewListName = parsed.suggestedNewListName
                        ?.takeIf { name -> lists.value.none { it.name.equals(name, ignoreCase = true) } }
                    AiParseOutcome.Success(
                        AiTaskDraft(
                            listId = listId,
                            title = parsed.title,
                            notes = parsed.notes,
                            priority = parsed.priority ?: Priority.MEDIUM,
                            dueDate = parsed.dueDate,
                            tagIds = tagIds,
                            recurrence = parsed.recurrence,
                            suggestedNewListName = suggestedNewListName
                        )
                    )
                },
                onFailure = { e -> AiParseOutcome.Error(e.message ?: "AI parsing failed") }
            )
        } finally {
            _aiParsing.value = false
        }
    }

    /** Matches [name] against existing lists case-insensitively; falls back to (creating) Uncategorized. */
    private suspend fun resolveListId(name: String?): Long {
        val current = lists.value
        val trimmed = name?.trim().orEmpty()
        if (trimmed.isNotEmpty()) {
            current.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.let { return it.id }
        }
        current.firstOrNull { it.name.equals(UNCATEGORIZED_LIST_NAME, ignoreCase = true) }?.let { return it.id }
        return repository.createList(UNCATEGORIZED_LIST_NAME, UNCATEGORIZED_LIST_COLOR_HEX, UNCATEGORIZED_LIST_ICON_KEY)
    }

    /** Matches [name] against existing tags case-insensitively, creating a new tag if needed. */
    private suspend fun resolveTagId(name: String): Long? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        tags.value.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.let { return it.id }
        return repository.createTag(trimmed, AccentColors.palette.random())
    }

    fun setSort(value: TaskSort) {
        _sort.value = value
    }

    fun setFilter(value: TaskFilter) {
        _filter.value = value
    }

    fun setDone(task: TodoTaskEntity, done: Boolean, tagIds: List<Long> = emptyList()) {
        viewModelScope.launch {
            val spawned = repository.setTaskDone(task, done, tagIds)
            if (done) {
                reminderScheduler.cancel(task.id)
                spawned?.let { reminderScheduler.schedule(it.id, it.title, it.dueDate) }
            } else {
                reminderScheduler.schedule(task.id, task.title, task.dueDate)
            }
        }
    }

    suspend fun deleteTaskWithUndo(task: TodoTaskEntity, tagIds: List<Long>): Pair<TodoTaskEntity, List<Long>> {
        repository.deleteTask(task)
        reminderScheduler.cancel(task.id)
        return task to tagIds
    }

    suspend fun restoreTask(snapshot: Pair<TodoTaskEntity, List<Long>>) {
        repository.restoreTask(snapshot.first, snapshot.second)
        reminderScheduler.schedule(snapshot.first.id, snapshot.first.title, snapshot.first.dueDate)
    }

    /** Deletes all of [items] and returns a snapshot [restoreTasks] can use to undo it. */
    suspend fun bulkDeleteWithUndo(items: List<TaskWithTags>): List<Pair<TodoTaskEntity, List<Long>>> {
        val snapshot = items.map { it.task to it.tags.map { tag -> tag.id } }
        items.forEach {
            repository.deleteTask(it.task)
            reminderScheduler.cancel(it.task.id)
        }
        return snapshot
    }

    suspend fun restoreTasks(snapshot: List<Pair<TodoTaskEntity, List<Long>>>) {
        snapshot.forEach {
            repository.restoreTask(it.first, it.second)
            reminderScheduler.schedule(it.first.id, it.first.title, it.first.dueDate)
        }
    }

    /** Marks every task in [items] done/undone (recurring ones still spawn their next occurrence). */
    fun bulkSetDone(items: List<TaskWithTags>, done: Boolean) {
        viewModelScope.launch {
            items.forEach { item ->
                val spawned = repository.setTaskDone(item.task, done, item.tags.map { tag -> tag.id })
                if (done) {
                    reminderScheduler.cancel(item.task.id)
                    spawned?.let { reminderScheduler.schedule(it.id, it.title, it.dueDate) }
                } else {
                    reminderScheduler.schedule(item.task.id, item.task.title, item.task.dueDate)
                }
            }
        }
    }

    fun bulkMoveToList(items: List<TaskWithTags>, listId: Long) {
        viewModelScope.launch {
            items.forEach { repository.updateTask(it.task.copy(listId = listId), it.tags.map { tag -> tag.id }) }
        }
    }

    fun createTask(
        listId: Long,
        title: String,
        notes: String?,
        priority: Priority,
        dueDate: Long?,
        colorHex: String?,
        tagIds: List<Long>,
        recurrence: RecurrenceRule = RecurrenceRule.NONE
    ) {
        viewModelScope.launch {
            val id = repository.createTask(listId, title, notes, priority, dueDate, colorHex, tagIds, recurrence)
            preferencesRepository.setLastUsedTodoListId(listId)
            reminderScheduler.schedule(id, title, dueDate)
        }
    }

    fun updateTask(
        task: TodoTaskEntity,
        listId: Long,
        title: String,
        notes: String?,
        priority: Priority,
        dueDate: Long?,
        colorHex: String?,
        tagIds: List<Long>,
        recurrence: RecurrenceRule = RecurrenceRule.NONE
    ) {
        viewModelScope.launch {
            repository.updateTask(
                task.copy(
                    listId = listId,
                    title = title,
                    notes = notes,
                    priority = priority,
                    dueDate = dueDate,
                    colorHex = colorHex,
                    recurrence = recurrence
                ),
                tagIds
            )
            preferencesRepository.setLastUsedTodoListId(listId)
            reminderScheduler.schedule(task.id, title, dueDate)
        }
    }

    /** Creates a new list (e.g. from an AI suggestion) and returns its id so the caller can select it. */
    suspend fun createListAndSelect(name: String): Long =
        repository.createList(name, AccentColors.palette.random(), IconCatalog.defaultKey)

    fun archiveAllCompleted() {
        viewModelScope.launch { repository.archiveAllCompleted() }
    }

    fun unarchiveTask(task: TodoTaskEntity) {
        viewModelScope.launch { repository.unarchiveTask(task) }
    }

    fun createTag(name: String, colorHex: String) {
        viewModelScope.launch { repository.createTag(name, colorHex) }
    }

    fun createList(name: String, icon: String, colorHex: String) {
        viewModelScope.launch { repository.createList(name, colorHex, icon) }
    }

    fun updateList(list: TodoListEntity, name: String, icon: String, colorHex: String) {
        viewModelScope.launch { repository.updateList(list.copy(name = name, icon = icon, colorHex = colorHex)) }
    }

    suspend fun deleteListWithUndo(list: TodoListEntity) = repository.deleteListWithSnapshot(list)

    suspend fun restoreList(snapshot: com.rainyday.saveableapp.data.repository.TodoListSnapshot) =
        repository.restoreList(snapshot)
}
