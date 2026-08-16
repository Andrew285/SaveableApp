package com.rainyday.saveableapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldValueDao {
    @Query(
        "SELECT v.* FROM list_item_field_values v " +
            "INNER JOIN simple_list_items i ON v.itemId = i.id " +
            "WHERE i.listId = :listId"
    )
    fun observeValuesForList(listId: Long): Flow<List<FieldValueEntity>>

    @Query("SELECT * FROM list_item_field_values WHERE itemId = :itemId")
    suspend fun getValuesForItem(itemId: Long): List<FieldValueEntity>

    @Insert
    suspend fun insertAll(values: List<FieldValueEntity>)

    @Query("DELETE FROM list_item_field_values WHERE itemId = :itemId")
    suspend fun deleteForItem(itemId: Long)
}
