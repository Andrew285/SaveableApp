package com.rainyday.saveableapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldDefinitionDao {
    @Query("SELECT * FROM list_field_definitions WHERE listId = :listId ORDER BY position ASC")
    fun observeFieldsForList(listId: Long): Flow<List<FieldDefinitionEntity>>

    @Query("SELECT * FROM list_field_definitions WHERE listId = :listId ORDER BY position ASC")
    suspend fun getFieldsForList(listId: Long): List<FieldDefinitionEntity>

    @Insert
    suspend fun insert(field: FieldDefinitionEntity): Long

    @Update
    suspend fun update(field: FieldDefinitionEntity)

    @Delete
    suspend fun delete(field: FieldDefinitionEntity)
}
