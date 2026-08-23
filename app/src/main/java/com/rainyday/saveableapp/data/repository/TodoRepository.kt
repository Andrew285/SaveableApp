package com.rainyday.saveableapp.data.repository

import com.rainyday.saveableapp.data.local.Priority
import com.rainyday.saveableapp.data.local.RecurrenceRule
import com.rainyday.saveableapp.data.local.TagDao
import com.rainyday.saveableapp.data.local.TagEntity
import com.rainyday.saveableapp.data.local.TaskTagCrossRef
import com.rainyday.saveableapp.data.local.TaskWithTags
import com.rainyday.saveableapp.data.local.TodoListDao
import com.rainyday.saveableapp.data.local.TodoListEntity
import com.rainyday.saveableapp.data.local.TodoTaskDao
import com.rainyday.saveableapp.data.local.TodoTaskEntity
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class TodoListSnapshot(val list: TodoListEntity, val tasks: List<TaskWithTags>)

class TodoRepository(
    private val listDao: TodoListDao,
    private val taskDao: TodoTaskDao,
    private val tagDao: TagDao
) {
    fun observeLists(): Flow<List<TodoListEntity>> = listDao.observeLists()

    fun observeList(listId: Long): Flow<TodoListEntity?> = listDao.observeById(listId)

    suspend fun createList(name: String, colorHex: String, icon: String): Long =
        listDao.insert(
            TodoListEntity(
                name = name,
                colorHex = colorHex,
                icon = icon,
                createdAt = System.currentTimeMillis()
            )
        )

    suspend fun updateList(list: TodoListEntity) = listDao.update(list)

    suspend fun deleteListWithSnapshot(list: TodoListEntity): TodoListSnapshot {
        val tasks = taskDao.observeTasksForList(list.id).first()
        listDao.delete(list)
        return TodoListSnapshot(list, tasks)
    }

    suspend fun restoreList(snapshot: TodoListSnapshot) {
        listDao.insert(snapshot.list)
        snapshot.tasks.forEach { taskWithTags ->
            taskDao.insert(taskWithTags.task)
            taskWithTags.tags.forEach { tag -> tagDao.insertCrossRef(TaskTagCrossRef(taskWithTags.task.id, tag.id)) }
        }
    }

    fun observeTasks(listId: Long): Flow<List<TaskWithTags>> = taskDao.observeTasksForList(listId)

    fun observeArchivedTasks(listId: Long): Flow<List<TaskWithTags>> = taskDao.observeArchivedTasksForList(listId)

    fun observeAllTasks(): Flow<List<TaskWithTags>> = taskDao.observeAllTasks()

    fun observeAllArchivedTasks(): Flow<List<TaskWithTags>> = taskDao.observeAllArchivedTasks()

    fun searchTasks(query: String): Flow<List<TaskWithTags>> = taskDao.search(query)

    suspend fun createTask(
        listId: Long,
        title: String,
        notes: String?,
        priority: Priority,
        dueDate: Long?,
        colorHex: String?,
        tagIds: List<Long>,
        recurrence: RecurrenceRule = RecurrenceRule.NONE
    ): Long {
        val id = taskDao.insert(
            TodoTaskEntity(
                listId = listId,
                title = title,
                notes = notes,
                priority = priority,
                dueDate = dueDate,
                colorHex = colorHex,
                createdAt = System.currentTimeMillis(),
                recurrence = recurrence
            )
        )
        tagIds.forEach { tagDao.insertCrossRef(TaskTagCrossRef(id, it)) }
        return id
    }

    suspend fun updateTask(task: TodoTaskEntity, tagIds: List<Long>) {
        taskDao.update(task)
        tagDao.clearTagsForTask(task.id)
        tagIds.forEach { tagDao.insertCrossRef(TaskTagCrossRef(task.id, it)) }
    }

    /**
     * Marks [task] done/undone. If it's being completed and has a [RecurrenceRule], also spawns the
     * next occurrence (same fields and [tagIds], due date advanced by the rule) so it's ready to go —
     * returns that new occurrence so the caller can (re)schedule its reminder, or null if none spawned.
     */
    suspend fun setTaskDone(task: TodoTaskEntity, isDone: Boolean, tagIds: List<Long> = emptyList()): TodoTaskEntity? {
        taskDao.update(
            task.copy(
                isDone = isDone,
                completedAt = if (isDone) System.currentTimeMillis() else null
            )
        )
        if (isDone && task.recurrence != RecurrenceRule.NONE) {
            val nextTask = task.copy(
                id = 0,
                isDone = false,
                completedAt = null,
                isArchived = false,
                dueDate = nextOccurrence(task.dueDate, task.recurrence),
                createdAt = System.currentTimeMillis()
            )
            val nextId = taskDao.insert(nextTask)
            tagIds.forEach { tagDao.insertCrossRef(TaskTagCrossRef(nextId, it)) }
            return nextTask.copy(id = nextId)
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

    suspend fun deleteTask(task: TodoTaskEntity) = taskDao.delete(task)

    suspend fun restoreTask(task: TodoTaskEntity, tagIds: List<Long>) {
        taskDao.insert(task)
        tagIds.forEach { tagDao.insertCrossRef(TaskTagCrossRef(task.id, it)) }
    }

    suspend fun archiveCompleted(listId: Long) = taskDao.archiveCompletedForList(listId)

    suspend fun archiveAllCompleted() = taskDao.archiveAllCompleted()

    suspend fun unarchiveTask(task: TodoTaskEntity) = taskDao.update(task.copy(isArchived = false))

    fun observeTags(): Flow<List<TagEntity>> = tagDao.observeTags()

    suspend fun createTag(name: String, colorHex: String): Long =
        tagDao.insert(TagEntity(name = name, colorHex = colorHex))

    suspend fun deleteTag(tag: TagEntity) = tagDao.delete(tag)
}
