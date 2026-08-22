package com.rainyday.saveableapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class DeckCardCount(val deckId: Long, val count: Int)

@Dao
interface FlashCardDao {
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY position ASC")
    fun observeCardsForDeck(deckId: Long): Flow<List<FlashCardEntity>>

    @Query("SELECT deckId, COUNT(*) as count FROM flashcards GROUP BY deckId")
    fun observeCounts(): Flow<List<DeckCardCount>>

    @Query("SELECT deckId, COUNT(*) as count FROM flashcards WHERE dueAt <= :now GROUP BY deckId")
    fun observeDueCounts(now: Long): Flow<List<DeckCardCount>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND dueAt <= :now ORDER BY dueAt ASC")
    suspend fun getDueCards(deckId: Long, now: Long): List<FlashCardEntity>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY position ASC")
    suspend fun getAllCards(deckId: Long): List<FlashCardEntity>

    @Insert
    suspend fun insert(card: FlashCardEntity): Long

    @Update
    suspend fun update(card: FlashCardEntity)

    @Update
    suspend fun updateAll(cards: List<FlashCardEntity>)

    @Delete
    suspend fun delete(card: FlashCardEntity)
}
