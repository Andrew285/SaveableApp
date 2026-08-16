package com.rainyday.saveableapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashCardDeckDao {
    @Query("SELECT * FROM flashcard_decks ORDER BY position ASC")
    fun observeDecks(): Flow<List<FlashCardDeckEntity>>

    @Query("SELECT * FROM flashcard_decks WHERE id = :id")
    fun observeById(id: Long): Flow<FlashCardDeckEntity?>

    @Insert
    suspend fun insert(deck: FlashCardDeckEntity): Long

    @Update
    suspend fun update(deck: FlashCardDeckEntity)

    @Delete
    suspend fun delete(deck: FlashCardDeckEntity)
}
