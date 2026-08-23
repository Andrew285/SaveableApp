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
    fun observeFieldsForList(listId: String): Flow<List<FieldDefinitionEntity>>

    @Query("SELECT * FROM list_field_definitions WHERE id = :id")
    suspend fun getById(id: String): FieldDefinitionEntity?

    @Query("SELECT * FROM list_field_definitions WHERE listId = :listId ORDER BY position ASC")
    suspend fun getFieldsForList(listId: String): List<FieldDefinitionEntity>

    @Query("SELECT * FROM list_field_definitions ORDER BY listId ASC, position ASC")
    fun observeAllFields(): Flow<List<FieldDefinitionEntity>>

    @Insert
    suspend fun insert(field: FieldDefinitionEntity)

    @Update
    suspend fun update(field: FieldDefinitionEntity)

    @Delete
    suspend fun delete(field: FieldDefinitionEntity)

    @Query("DELETE FROM list_field_definitions WHERE id = :id")
    suspend fun deleteById(id: String)
}
