package com.rainyday.saveableapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ListProgress(val listId: Long, val total: Int, val checked: Int)

@Dao
interface SimpleListItemDao {
    @Query("SELECT * FROM simple_list_items WHERE listId = :listId ORDER BY isChecked ASC, position ASC")
    fun observeItemsForList(listId: Long): Flow<List<SimpleListItemEntity>>

    @Query(
        "SELECT * FROM simple_list_items WHERE text LIKE '%' || :query || '%' " +
            "OR note LIKE '%' || :query || '%' ORDER BY createdAt DESC"
    )
    fun search(query: String): Flow<List<SimpleListItemEntity>>

    @Query(
        "SELECT listId, COUNT(*) as total, SUM(CASE WHEN isChecked THEN 1 ELSE 0 END) as checked " +
            "FROM simple_list_items GROUP BY listId"
    )
    fun observeProgress(): Flow<List<ListProgress>>

    @Insert
    suspend fun insert(item: SimpleListItemEntity): Long

    @Update
    suspend fun update(item: SimpleListItemEntity)

    @Update
    suspend fun updateAll(items: List<SimpleListItemEntity>)

    @Delete
    suspend fun delete(item: SimpleListItemEntity)
}
