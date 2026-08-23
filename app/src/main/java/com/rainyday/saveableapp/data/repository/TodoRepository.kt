package com.rainyday.saveableapp.data.repository

import com.rainyday.saveableapp.data.local.Priority
import com.rainyday.saveableapp.data.local.RecurrenceRule
import com.rainyday.saveableapp.data.local.SyncEntityType
import com.rainyday.saveableapp.data.local.TagDao
import com.rainyday.saveableapp.data.local.TagEntity
import com.rainyday.saveableapp.data.local.TaskTagCrossRef
import com.rainyday.saveableapp.data.local.TaskWithTags
import com.rainyday.saveableapp.data.local.TodoListDao
import com.rainyday.saveableapp.data.local.TodoListEntity
import com.rainyday.saveableapp.data.local.TodoTaskDao
import com.rainyday.saveableapp.data.local.TodoTaskEntity
import com.rainyday.saveableapp.data.sync.SyncOutbox
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class TodoListSnapshot(val list: TodoListEntity, val tasks: List<TaskWithTags>)

/**
 * Task-tag membership (join rows in [TaskTagCrossRef]) isn't synced as its own Firestore collection —
 * it's embedded as a `tagIds` array on the task's own document, so every call here that changes a
 * task's tags is covered by that same task id going through the outbox as a TODO_TASK upsert; no
 * separate TASK_TAG_CROSS_REF bookkeeping is needed.
 */
class TodoRepository(
    private val listDao: TodoListDao,
    private val taskDao: TodoTaskDao,
    private val tagDao: TagDao,
    private val syncOutbox: SyncOutbox
) {
    fun observeLists(): Flow<List<TodoListEntity>> = listDao.observeLists()

    fun observeList(listId: String): Flow<TodoListEntity?> = listDao.observeById(listId)

    suspend fun createList(name: String, colorHex: String, icon: String): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        listDao.insert(
            TodoListEntity(
                id = id,
                name = name,
                colorHex = colorHex,
                icon = icon,
                createdAt = now,
                updatedAt = now
            )
        )
        syncOutbox.upsert(SyncEntityType.TODO_LIST, id)
        return id
    }

    suspend fun updateList(list: TodoListEntity) {
        listDao.update(list.copy(updatedAt = System.currentTimeMillis()))
        syncOutbox.upsert(SyncEntityType.TODO_LIST, list.id)
    }

    /** Room cascades the local delete of [list]'s tasks; enqueue their ids too or they orphan in Firestore. */
    suspend fun deleteListWithSnapshot(list: TodoListEntity): TodoListSnapshot {
        val tasks = taskDao.observeTasksForList(list.id).first()
        listDao.delete(list)
        syncOutbox.deleteAll(SyncEntityType.TODO_TASK, tasks.map { it.task.id })
        syncOutbox.delete(SyncEntityType.TODO_LIST, list.id)
        return TodoListSnapshot(list, tasks)
    }

    suspend fun restoreList(snapshot: TodoListSnapshot) {
        listDao.insert(snapshot.list)
        snapshot.tasks.forEach { taskWithTags ->
            taskDao.insert(taskWithTags.task)
            taskWithTags.tags.forEach { tag -> tagDao.insertCrossRef(TaskTagCrossRef(taskWithTags.task.id, tag.id)) }
        }
        syncOutbox.upsert(SyncEntityType.TODO_LIST, snapshot.list.id)
        syncOutbox.upsertAll(SyncEntityType.TODO_TASK, snapshot.tasks.map { it.task.id })
    }

    fun observeTasks(listId: String): Flow<List<TaskWithTags>> = taskDao.observeTasksForList(listId)

    fun observeArchivedTasks(listId: String): Flow<List<TaskWithTags>> = taskDao.observeArchivedTasksForList(listId)

    fun observeAllTasks(): Flow<List<TaskWithTags>> = taskDao.observeAllTasks()

    fun observeAllArchivedTasks(): Flow<List<TaskWithTags>> = taskDao.observeAllArchivedTasks()

    fun searchTasks(query: String): Flow<List<TaskWithTags>> = taskDao.search(query)

    suspend fun createTask(
        listId: String,
        title: String,
        notes: String?,
        priority: Priority,
        dueDate: Long?,
        colorHex: String?,
        tagIds: List<String>,
        recurrence: RecurrenceRule = RecurrenceRule.NONE
    ): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        taskDao.insert(
            TodoTaskEntity(
                id = id,
                listId = listId,
                title = title,
                notes = notes,
                priority = priority,
                dueDate = dueDate,
                colorHex = colorHex,
                createdAt = now,
                updatedAt = now,
                recurrence = recurrence
            )
        )
        tagIds.forEach { tagDao.insertCrossRef(TaskTagCrossRef(id, it)) }
        syncOutbox.upsert(SyncEntityType.TODO_TASK, id)
        return id
    }

    suspend fun updateTask(task: TodoTaskEntity, tagIds: List<String>) {
        taskDao.update(task.copy(updatedAt = System.currentTimeMillis()))
        tagDao.clearTagsForTask(task.id)
        tagIds.forEach { tagDao.insertCrossRef(TaskTagCrossRef(task.id, it)) }
        syncOutbox.upsert(SyncEntityType.TODO_TASK, task.id)
    }

    /**
     * Marks [task] done/undone. If it's being completed and has a [RecurrenceRule], also spawns the
     * next occurrence (same fields and [tagIds], due date advanced by the rule) so it's ready to go —
     * returns that new occurrence so the caller can (re)schedule its reminder, or null if none spawned.
     */
    suspend fun setTaskDone(task: TodoTaskEntity, isDone: Boolean, tagIds: List<String> = emptyList()): TodoTaskEntity? {
        val now = System.currentTimeMillis()
        taskDao.update(
            task.copy(
                isDone = isDone,
                completedAt = if (isDone) now else null,
                updatedAt = now
            )
        )
        syncOutbox.upsert(SyncEntityType.TODO_TASK, task.id)
        if (isDone && task.recurrence != RecurrenceRule.NONE) {
            val nextId = UUID.randomUUID().toString()
            val nextTask = task.copy(
                id = nextId,
                isDone = false,
                completedAt = null,
                isArchived = false,
                dueDate = nextOccurrence(task.dueDate, task.recurrence),
                createdAt = now,
                updatedAt = now
            )
            taskDao.insert(nextTask)
            tagIds.forEach { tagDao.insertCrossRef(TaskTagCrossRef(nextId, it)) }
            syncOutbox.upsert(SyncEntityType.TODO_TASK, nextId)
            return nextTask
        }
        return null
    }

    /** Advances [baseMillis] (or now, if there was no due date) by one [rule] step. */
    private fun nextOccurrence(baseMillis: Long?, rule: RecurrenceRule): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = baseMillis ?: System.currentTimeMillis() }
        when (rule) {
            RecurrenceRule.DAILY -> calendar.add(Calendar.DAY_OF_MONTH, 1)
            RecurrenceRule.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RecurrenceRule.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RecurrenceRule.YEARLY -> calendar.add(Calendar.YEAR, 1)
            RecurrenceRule.NONE -> Unit
        }
        return calendar.timeInMillis
    }

    suspend fun deleteTask(task: TodoTaskEntity) {
        taskDao.delete(task)
        syncOutbox.delete(SyncEntityType.TODO_TASK, task.id)
    }

    suspend fun restoreTask(task: TodoTaskEntity, tagIds: List<String>) {
        taskDao.insert(task)
        tagIds.forEach { tagDao.insertCrossRef(TaskTagCrossRef(task.id, it)) }
        syncOutbox.upsert(SyncEntityType.TODO_TASK, task.id)
    }

    suspend fun archiveCompleted(listId: String) {
        val ids = taskDao.observeTasksForList(listId).first().filter { it.task.isDone }.map { it.task.id }
        taskDao.archiveCompletedForList(listId)
        syncOutbox.upsertAll(SyncEntityType.TODO_TASK, ids)
    }

    suspend fun archiveAllCompleted() {
        val ids = taskDao.observeAllTasks().first().filter { it.task.isDone }.map { it.task.id }
        taskDao.archiveAllCompleted()
        syncOutbox.upsertAll(SyncEntityType.TODO_TASK, ids)
    }

    suspend fun unarchiveTask(task: TodoTaskEntity) {
        taskDao.update(task.copy(isArchived = false, updatedAt = System.currentTimeMillis()))
        syncOutbox.upsert(SyncEntityType.TODO_TASK, task.id)
    }

    fun observeTags(): Flow<List<TagEntity>> = tagDao.observeTags()

    suspend fun createTag(name: String, colorHex: String): String {
        val id = UUID.randomUUID().toString()
        tagDao.insert(TagEntity(id = id, name = name, colorHex = colorHex, updatedAt = System.currentTimeMillis()))
        syncOutbox.upsert(SyncEntityType.TAG, id)
        return id
    }

    suspend fun deleteTag(tag: TagEntity) {
        tagDao.delete(tag)
        syncOutbox.delete(SyncEntityType.TAG, tag.id)
    }
}
