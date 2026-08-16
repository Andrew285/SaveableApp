package com.rainyday.saveableapp.ui.screens.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rainyday.saveableapp.data.local.Priority
import com.rainyday.saveableapp.data.local.TagEntity
import com.rainyday.saveableapp.data.local.TaskWithTags
import com.rainyday.saveableapp.data.local.TodoListEntity
import com.rainyday.saveableapp.data.local.TodoTaskEntity
import com.rainyday.saveableapp.data.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskSort { DEFAULT, PRIORITY, DUE_DATE, ALPHABETICAL }
enum class TaskFilter { ALL, ACTIVE, COMPLETED }

class TodoTaskViewModel(
    private val listId: Long,
    private val repository: TodoRepository
) : ViewModel() {
    val list: StateFlow<TodoListEntity?> = repository.observeList(listId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _sort = MutableStateFlow(TaskSort.DEFAULT)
    val sort: StateFlow<TaskSort> = _sort

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    val filter: StateFlow<TaskFilter> = _filter

    val tasks: StateFlow<List<TaskWithTags>> =
        combine(repository.observeTasks(listId), _sort, _filter) { list, sort, filter ->
            val filtered = when (filter) {
                TaskFilter.ALL -> list
                TaskFilter.ACTIVE -> list.filter { !it.task.isDone }
                TaskFilter.COMPLETED -> list.filter { it.task.isDone }
            }
            when (sort) {
                TaskSort.DEFAULT -> filtered
                TaskSort.PRIORITY -> filtered.sortedWith(compareBy({ it.task.isDone }, { -it.task.priority.ordinal }))
                TaskSort.DUE_DATE -> filtered.sortedWith(compareBy({ it.task.isDone }, { it.task.dueDate ?: Long.MAX_VALUE }))
                TaskSort.ALPHABETICAL -> filtered.sortedWith(compareBy({ it.task.isDone }, { it.task.title.lowercase() }))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedTasks: StateFlow<List<TaskWithTags>> = repository.observeArchivedTasks(listId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tags: StateFlow<List<TagEntity>> = repository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        title: String,
        notes: String?,
        priority: Priority,
        dueDate: Long?,
        colorHex: String?,
        tagIds: List<Long>
    ) {
        viewModelScope.launch {
            repository.createTask(listId, title, notes, priority, dueDate, colorHex, tagIds)
        }
    }

    fun updateTask(
        task: TodoTaskEntity,
        title: String,
        notes: String?,
        priority: Priority,
        dueDate: Long?,
        colorHex: String?,
        tagIds: List<Long>
    ) {
        viewModelScope.launch {
            repository.updateTask(
                task.copy(title = title, notes = notes, priority = priority, dueDate = dueDate, colorHex = colorHex),
                tagIds
            )
        }
    }

    fun archiveCompleted() {
        viewModelScope.launch { repository.archiveCompleted(listId) }
    }

    fun unarchiveTask(task: TodoTaskEntity) {
        viewModelScope.launch { repository.unarchiveTask(task) }
    }

    fun createTag(name: String, colorHex: String) {
        viewModelScope.launch { repository.createTag(name, colorHex) }
    }
}
