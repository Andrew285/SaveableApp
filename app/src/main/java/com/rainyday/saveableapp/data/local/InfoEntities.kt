package com.rainyday.saveableapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "info_categories")
data class InfoCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val colorHex: String,
    val position: Int = 0
)

@Serializable
@Entity(
    tableName = "info_blocks",
    foreignKeys = [
        ForeignKey(
            entity = InfoCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class InfoBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val title: String,
    val content: String,
    val isSensitive: Boolean = false,
    val isFavorite: Boolean = false,
    val position: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val expiryDate: Long? = null
)
