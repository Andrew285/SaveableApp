package com.rainyday.saveableapp.ui.screens.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainyday.saveableapp.data.local.Priority
import com.rainyday.saveableapp.data.local.TagEntity
import com.rainyday.saveableapp.data.local.TaskWithTags
import com.rainyday.saveableapp.data.local.TodoListEntity
import com.rainyday.saveableapp.data.local.TodoTaskEntity
import com.rainyday.saveableapp.data.prefs.PreferencesRepository
import com.rainyday.saveableapp.data.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskGroup(val list: TodoListEntity, val tasks: List<TaskWithTags>)

enum class TaskSort { DEFAULT, PRIORITY, DUE_DATE, ALPHABETICAL }
enum class TaskFilter { ALL, ACTIVE, COMPLETED }

class TasksViewModel(
    private val repository: TodoRepository,
    private val preferencesRepository: PreferencesRepository
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

    fun setSort(value: TaskSort) {
        _sort.value = value
    }

    fun setFilter(value: TaskFilter) {
        _filter.value = value
    }

    fun setDone(task: TodoTaskEntity, done: Boolean) {
        viewModelScope.launch { repository.setTaskDone(task, done) }
    }

    suspend fun deleteTaskWithUndo(task: TodoTaskEntity, tagIds: List<Long>): Pair<TodoTaskEntity, List<Long>> {
        repository.deleteTask(task)
        return task to tagIds
    }

    suspend fun restoreTask(snapshot: Pair<TodoTaskEntity, List<Long>>) =
        repository.restoreTask(snapshot.first, snapshot.second)

    fun createTask(
        listId: Long,
        title: String,
        notes: String?,
        priority: Priority,
        dueDate: Long?,
        colorHex: String?,
        tagIds: List<Long>
    ) {
        viewModelScope.launch {
            repository.createTask(listId, title, notes, priority, dueDate, colorHex, tagIds)
            preferencesRepository.setLastUsedTodoListId(listId)
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
        tagIds: List<Long>
    ) {
        viewModelScope.launch {
            repository.updateTask(
                task.copy(listId = listId, title = title, notes = notes, priority = priority, dueDate = dueDate, colorHex = colorHex),
                tagIds
            )
            preferencesRepository.setLastUsedTodoListId(listId)
        }
    }

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
