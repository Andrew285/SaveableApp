package com.rainyday.saveableapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class FieldType { TEXT, NUMBER, RATING, DATE }

/** A field the user has defined for a list, e.g. "Author" (TEXT) or "Rating" (RATING). */
@Serializable
@Entity(
    tableName = "list_field_definitions",
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
data class FieldDefinitionEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val name: String,
    val type: FieldType,
    val colorHex: String,
    val position: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)

/** One item's value for one field, stored as raw text and interpreted per the field's [FieldType]. */
@Serializable
@Entity(
    tableName = "list_item_field_values",
    foreignKeys = [
        ForeignKey(
            entity = SimpleListItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FieldDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["fieldId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId"), Index("fieldId")]
)
data class FieldValueEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val fieldId: String,
    val value: String,
    val updatedAt: Long
)

/** A field to pre-create when a list is created from a template. Not a Room entity. */
data class FieldTemplate(val name: String, val type: FieldType, val colorHex: String)
