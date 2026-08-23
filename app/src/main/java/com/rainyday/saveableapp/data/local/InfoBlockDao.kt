package com.rainyday.saveableapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class InfoCategoryCount(val categoryId: String, val total: Int)

@Dao
interface InfoBlockDao {
    @Query("SELECT * FROM info_blocks WHERE categoryId = :categoryId ORDER BY position ASC")
    fun observeBlocksForCategory(categoryId: String): Flow<List<InfoBlockEntity>>

    @Query("SELECT * FROM info_blocks WHERE id = :id")
    suspend fun getById(id: String): InfoBlockEntity?

    @Query("SELECT categoryId, COUNT(*) as total FROM info_blocks GROUP BY categoryId")
    fun observeCounts(): Flow<List<InfoCategoryCount>>

    @Query("SELECT * FROM info_blocks WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<InfoBlockEntity>>

    @Query(
        "SELECT * FROM info_blocks WHERE title LIKE '%' || :query || '%' " +
            "OR content LIKE '%' || :query || '%' ORDER BY updatedAt DESC"
    )
    fun search(query: String): Flow<List<InfoBlockEntity>>

    @Insert
    suspend fun insert(block: InfoBlockEntity)

    @Update
    suspend fun update(block: InfoBlockEntity)

    @Delete
    suspend fun delete(block: InfoBlockEntity)

    @Query("DELETE FROM info_blocks WHERE id = :id")
    suspend fun deleteById(id: String)
}
