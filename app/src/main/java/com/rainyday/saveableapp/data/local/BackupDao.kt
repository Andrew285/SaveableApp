package com.rainyday.saveableapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BackupDao {
    @Query("SELECT * FROM todo_lists")
    suspend fun getAllTodoLists(): List<TodoListEntity>

    @Query("SELECT * FROM todo_tasks")
    suspend fun getAllTodoTasks(): List<TodoTaskEntity>

    @Query("SELECT * FROM tags")
    suspend fun getAllTags(): List<TagEntity>

    @Query("SELECT * FROM task_tag_cross_ref")
    suspend fun getAllTaskTagCrossRefs(): List<TaskTagCrossRef>

    @Query("SELECT * FROM simple_lists")
    suspend fun getAllSimpleLists(): List<SimpleListEntity>

    @Query("SELECT * FROM simple_list_items")
    suspend fun getAllSimpleListItems(): List<SimpleListItemEntity>

    @Query("SELECT * FROM info_categories")
    suspend fun getAllInfoCategories(): List<InfoCategoryEntity>

    @Query("SELECT * FROM info_blocks")
    suspend fun getAllInfoBlocks(): List<InfoBlockEntity>

    @Query("DELETE FROM task_tag_cross_ref")
    suspend fun clearTaskTagCrossRefs()

    @Query("DELETE FROM todo_tasks")
    suspend fun clearTodoTasks()

    @Query("DELETE FROM todo_lists")
    suspend fun clearTodoLists()

    @Query("DELETE FROM tags")
    suspend fun clearTags()

    @Query("DELETE FROM simple_list_items")
    suspend fun clearSimpleListItems()

    @Query("DELETE FROM simple_lists")
    suspend fun clearSimpleLists()

    @Query("DELETE FROM info_blocks")
    suspend fun clearInfoBlocks()

    @Query("DELETE FROM info_categories")
    suspend fun clearInfoCategories()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodoLists(items: List<TodoListEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodoTasks(items: List<TodoTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(items: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskTagCrossRefs(items: List<TaskTagCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimpleLists(items: List<SimpleListEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSimpleListItems(items: List<SimpleListItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInfoCategories(items: List<InfoCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInfoBlocks(items: List<InfoBlockEntity>)
}
