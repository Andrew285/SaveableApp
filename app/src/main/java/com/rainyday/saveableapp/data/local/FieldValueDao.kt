package com.rainyday.saveableapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldValueDao {
    @Query(
        "SELECT v.* FROM list_item_field_values v " +
            "INNER JOIN simple_list_items i ON v.itemId = i.id " +
            "WHERE i.listId = :listId"
    )
    fun observeValuesForList(listId: String): Flow<List<FieldValueEntity>>

    @Query("SELECT * FROM list_item_field_values WHERE itemId = :itemId")
    suspend fun getValuesForItem(itemId: String): List<FieldValueEntity>

    @Query("SELECT * FROM list_item_field_values WHERE id = :id")
    suspend fun getById(id: String): FieldValueEntity?

    @Query("SELECT * FROM list_item_field_values WHERE fieldId = :fieldId")
    suspend fun getValuesForField(fieldId: String): List<FieldValueEntity>

    @Insert
    suspend fun insertAll(values: List<FieldValueEntity>)

    @Update
    suspend fun update(value: FieldValueEntity)

    @Query("DELETE FROM list_item_field_values WHERE itemId = :itemId")
    suspend fun deleteForItem(itemId: String)

    @Query("DELETE FROM list_item_field_values WHERE id = :id")
    suspend fun deleteById(id: String)
}
