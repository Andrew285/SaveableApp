package com.rainyday.saveableapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoTaskDao {
    @Transaction
    @Query("SELECT * FROM todo_tasks WHERE listId = :listId AND isArchived = 0 ORDER BY isDone ASC, position ASC")
    fun observeTasksForList(listId: String): Flow<List<TaskWithTags>>

    @Transaction
    @Query("SELECT * FROM todo_tasks WHERE listId = :listId AND isArchived = 1 ORDER BY completedAt DESC")
    fun observeArchivedTasksForList(listId: String): Flow<List<TaskWithTags>>

    @Transaction
    @Query("SELECT * FROM todo_tasks WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun observeAllTasks(): Flow<List<TaskWithTags>>

    @Transaction
    @Query("SELECT * FROM todo_tasks WHERE isArchived = 1 ORDER BY completedAt DESC")
    fun observeAllArchivedTasks(): Flow<List<TaskWithTags>>

    @Transaction
    @Query(
        "SELECT * FROM todo_tasks WHERE isArchived = 0 AND (title LIKE '%' || :query || '%' " +
            "OR notes LIKE '%' || :query || '%') ORDER BY createdAt DESC"
    )
    fun search(query: String): Flow<List<TaskWithTags>>

    @Query("SELECT * FROM todo_tasks WHERE id = :id")
    suspend fun getById(id: String): TodoTaskEntity?

    @Transaction
    @Query("SELECT * FROM todo_tasks WHERE id = :id")
    suspend fun getByIdWithTags(id: String): TaskWithTags?

    @Insert
    suspend fun insert(task: TodoTaskEntity)

    @Update
    suspend fun update(task: TodoTaskEntity)

    @Delete
    suspend fun delete(task: TodoTaskEntity)

    @Query("DELETE FROM todo_tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE todo_tasks SET isArchived = 1 WHERE listId = :listId AND isDone = 1")
    suspend fun archiveCompletedForList(listId: String)

    @Query("UPDATE todo_tasks SET isArchived = 1 WHERE isDone = 1")
    suspend fun archiveAllCompleted()
}
