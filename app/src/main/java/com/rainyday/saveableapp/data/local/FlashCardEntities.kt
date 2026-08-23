package com.rainyday.saveableapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "flashcard_decks")
data class FlashCardDeckEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val position: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = FlashCardDeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deckId")]
)
data class FlashCardEntity(
    @PrimaryKey val id: String,
    val deckId: String,
    val front: String,
    val back: String,
    val position: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    // Spaced-repetition scheduling state (simplified SM-2). New/never-reviewed cards default to
    // dueAt = 0, i.e. already due, so they show up in a study session right away.
    val intervalDays: Int = 0,
    val easeFactor: Double = 2.5,
    val repetitions: Int = 0,
    val dueAt: Long = 0
)
