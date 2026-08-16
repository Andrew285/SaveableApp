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

    @Query("SELECT * FROM flashcard_decks")
    suspend fun getAllFlashCardDecks(): List<FlashCardDeckEntity>

    @Query("SELECT * FROM flashcards")
    suspend fun getAllFlashCards(): List<FlashCardEntity>

    @Query("SELECT * FROM list_field_definitions")
    suspend fun getAllFieldDefinitions(): List<FieldDefinitionEntity>

    @Query("SELECT * FROM list_item_field_values")
    suspend fun getAllFieldValues(): List<FieldValueEntity>

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

    @Query("DELETE FROM flashcards")
    suspend fun clearFlashCards()

    @Query("DELETE FROM flashcard_decks")
    suspend fun clearFlashCardDecks()

    @Query("DELETE FROM list_item_field_values")
    suspend fun clearFieldValues()

    @Query("DELETE FROM list_field_definitions")
    suspend fun clearFieldDefinitions()

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashCardDecks(items: List<FlashCardDeckEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashCards(items: List<FlashCardEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFieldDefinitions(items: List<FieldDefinitionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFieldValues(items: List<FieldValueEntity>)
}
