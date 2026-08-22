package com.rainyday.saveableapp.data.repository

import com.rainyday.saveableapp.data.local.Priority
import com.rainyday.saveableapp.data.local.TagDao
import com.rainyday.saveableapp.data.local.TagEntity
import com.rainyday.saveableapp.data.local.TaskTagCrossRef
import com.rainyday.saveableapp.data.local.TaskWithTags
import com.rainyday.saveableapp.data.local.TodoListDao
import com.rainyday.saveableapp.data.local.TodoListEntity
import com.rainyday.saveableapp.data.local.TodoTaskDao
import com.rainyday.saveableapp.data.local.TodoTaskEntity
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
        tagIds: List<Long>
    ): Long {
        val id = taskDao.insert(
            TodoTaskEntity(
                listId = listId,
                title = title,
                notes = notes,
                priority = priority,
                dueDate = dueDate,
                colorHex = colorHex,
                createdAt = System.currentTimeMillis()
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

    suspend fun setTaskDone(task: TodoTaskEntity, isDone: Boolean) {
        taskDao.update(
            task.copy(
                isDone = isDone,
                completedAt = if (isDone) System.currentTimeMillis() else null
            )
        )
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
