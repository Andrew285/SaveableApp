package com.rainyday.saveableapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "simple_lists")
data class SimpleListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val showCheckbox: Boolean = true,
    val position: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
@Entity(
    tableName = "simple_list_items",
    foreignKeys = [
        ForeignKey(
            entity = SimpleListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class SimpleListItemEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val text: String,
    val note: String? = null,
    val url: String? = null,
    val isChecked: Boolean = false,
    val position: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
