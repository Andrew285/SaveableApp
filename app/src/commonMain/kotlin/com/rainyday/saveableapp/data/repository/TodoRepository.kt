package com.rainyday.saveableapp.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.rainyday.saveableapp.data.local.Priority
import com.rainyday.saveableapp.data.local.TagEntity
import com.rainyday.saveableapp.data.local.TaskWithTags
import com.rainyday.saveableapp.data.local.TodoListEntity
import com.rainyday.saveableapp.data.local.TodoTaskEntity
import com.rainyday.saveableapp.db.AppDatabase
import com.rainyday.saveableapp.db.Tags
import com.rainyday.saveableapp.db.Todo_lists
import com.rainyday.saveableapp.db.Todo_tasks
import com.rainyday.saveableapp.platform.nowMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class TodoListSnapshot(val list: TodoListEntity, val tasks: List<TaskWithTags>)

internal fun Todo_lists.toEntity() = TodoListEntity(
    id = id, name = name, colorHex = colorHex, icon = icon, position = position.toInt(), createdAt = createdAt
)

internal fun Todo_tasks.toEntity() = TodoTaskEntity(
    id = id, listId = listId, title = title, notes = notes, isDone = isDone, priority = priority,
    dueDate = dueDate, colorHex = colorHex, position = position.toInt(), createdAt = createdAt,
    completedAt = completedAt, isArchived = isArchived
)

internal fun Tags.toEntity() = TagEntity(id = id, name = name, colorHex = colorHex)

class TodoRepository(private val db: AppDatabase) {
    private val listQueries = db.todoListQueries
    private val taskQueries = db.todoTaskQueries
    private val tagQueries = db.tagQueries
    private val crossRefQueries = db.taskTagCrossRefQueries

    private fun tagsByTaskId(): Flow<Map<Long, List<TagEntity>>> =
        combine(
            crossRefQueries.selectAll().asFlow().mapToList(Dispatchers.Default),
            tagQueries.selectAll().asFlow().mapToList(Dispatchers.Default)
        ) { crossRefs, tags ->
            val tagsById = tags.associateBy { it.id }
            crossRefs.groupBy({ it.taskId }) { tagsById[it.tagId] }
                .mapValues { (_, tags) -> tags.filterNotNull().map { it.toEntity() } }
        }

    private fun List<Todo_tasks>.withTags(tagsByTaskId: Map<Long, List<TagEntity>>): List<TaskWithTags> =
        map { TaskWithTags(it.toEntity(), tagsByTaskId[it.id].orEmpty()) }

    fun observeLists(): Flow<List<TodoListEntity>> =
        listQueries.selectAll().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toEntity() } }

    fun observeList(listId: Long): Flow<TodoListEntity?> =
        listQueries.selectById(listId).asFlow().mapToOneOrNull(Dispatchers.Default).map { it?.toEntity() }

    suspend fun createList(name: String, colorHex: String, icon: String): Long {
        listQueries.insertNew(name = name, colorHex = colorHex, icon = icon, position = 0, createdAt = nowMillis())
        return listQueries.lastInsertRowId().executeAsOne()
    }

    suspend fun updateList(list: TodoListEntity) {
        listQueries.update(
            name = list.name, colorHex = list.colorHex, icon = list.icon,
            position = list.position.toLong(), createdAt = list.createdAt, id = list.id
        )
    }

    suspend fun deleteListWithSnapshot(list: TodoListEntity): TodoListSnapshot {
        val tasks = observeTasks(list.id).first()
        listQueries.deleteById(list.id)
        return TodoListSnapshot(list, tasks)
    }

    suspend fun restoreList(snapshot: TodoListSnapshot) {
        listQueries.insertOrReplace(
            id = snapshot.list.id, name = snapshot.list.name, colorHex = snapshot.list.colorHex,
            icon = snapshot.list.icon, position = snapshot.list.position.toLong(), createdAt = snapshot.list.createdAt
        )
        snapshot.tasks.forEach { taskWithTags ->
            insertTaskOrReplace(taskWithTags.task)
            taskWithTags.tags.forEach { tag -> crossRefQueries.insertOrIgnore(taskWithTags.task.id, tag.id) }
        }
    }

    fun observeTasks(listId: Long): Flow<List<TaskWithTags>> =
        combine(
            taskQueries.selectForList(listId).asFlow().mapToList(Dispatchers.Default),
            tagsByTaskId()
        ) { tasks, tagsByTaskId -> tasks.withTags(tagsByTaskId) }

    fun observeArchivedTasks(listId: Long): Flow<List<TaskWithTags>> =
        combine(
            taskQueries.selectArchivedForList(listId).asFlow().mapToList(Dispatchers.Default),
            tagsByTaskId()
        ) { tasks, tagsByTaskId -> tasks.withTags(tagsByTaskId) }

    fun observeAllTasks(): Flow<List<TaskWithTags>> =
        combine(
            taskQueries.selectAllTasks().asFlow().mapToList(Dispatchers.Default),
            tagsByTaskId()
        ) { tasks, tagsByTaskId -> tasks.withTags(tagsByTaskId) }

    fun searchTasks(query: String): Flow<List<TaskWithTags>> =
        combine(
            taskQueries.search(query).asFlow().mapToList(Dispatchers.Default),
            tagsByTaskId()
        ) { tasks, tagsByTaskId -> tasks.withTags(tagsByTaskId) }

    suspend fun createTask(
        listId: Long,
        title: String,
        notes: String?,
        priority: Priority,
        dueDate: Long?,
        colorHex: String?,
        tagIds: List<Long>
    ): Long {
        taskQueries.insertNew(
            listId = listId, title = title, notes = notes, isDone = false, priority = priority,
            dueDate = dueDate, colorHex = colorHex, position = 0, createdAt = nowMillis(),
            completedAt = null, isArchived = false
        )
        val id = taskQueries.lastInsertRowId().executeAsOne()
        tagIds.forEach { crossRefQueries.insertOrIgnore(id, it) }
        return id
    }

    private fun insertTaskOrReplace(task: TodoTaskEntity) {
        taskQueries.insertOrReplace(
            id = task.id, listId = task.listId, title = task.title, notes = task.notes, isDone = task.isDone,
            priority = task.priority, dueDate = task.dueDate, colorHex = task.colorHex,
            position = task.position.toLong(), createdAt = task.createdAt, completedAt = task.completedAt,
            isArchived = task.isArchived
        )
    }

    private fun updateTaskRow(task: TodoTaskEntity) {
        taskQueries.update(
            listId = task.listId, title = task.title, notes = task.notes, isDone = task.isDone,
            priority = task.priority, dueDate = task.dueDate, colorHex = task.colorHex,
            position = task.position.toLong(), createdAt = task.createdAt, completedAt = task.completedAt,
            isArchived = task.isArchived, id = task.id
        )
    }

    suspend fun updateTask(task: TodoTaskEntity, tagIds: List<Long>) {
        updateTaskRow(task)
        crossRefQueries.deleteForTask(task.id)
        tagIds.forEach { crossRefQueries.insertOrIgnore(task.id, it) }
    }

    suspend fun setTaskDone(task: TodoTaskEntity, isDone: Boolean) {
        updateTaskRow(task.copy(isDone = isDone, completedAt = if (isDone) nowMillis() else null))
    }

    suspend fun deleteTask(task: TodoTaskEntity) = taskQueries.deleteById(task.id)

    suspend fun restoreTask(task: TodoTaskEntity, tagIds: List<Long>) {
        insertTaskOrReplace(task)
        tagIds.forEach { crossRefQueries.insertOrIgnore(task.id, it) }
    }

    suspend fun archiveCompleted(listId: Long) = taskQueries.archiveCompletedForList(listId)

    suspend fun unarchiveTask(task: TodoTaskEntity) = updateTaskRow(task.copy(isArchived = false))

    fun observeTags(): Flow<List<TagEntity>> =
        tagQueries.selectAll().asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map { it.toEntity() } }

    suspend fun createTag(name: String, colorHex: String): Long {
        tagQueries.insertNewOrIgnore(name, colorHex)
        return tagQueries.lastInsertRowId().executeAsOne()
    }

    suspend fun deleteTag(tag: TagEntity) = tagQueries.deleteById(tag.id)
}
