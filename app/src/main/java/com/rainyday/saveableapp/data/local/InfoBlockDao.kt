package com.rainyday.saveableapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InfoBlockDao {
    @Query("SELECT * FROM info_blocks WHERE categoryId = :categoryId ORDER BY position ASC")
    fun observeBlocksForCategory(categoryId: Long): Flow<List<InfoBlockEntity>>

    @Query("SELECT * FROM info_blocks WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<InfoBlockEntity>>

    @Query(
        "SELECT * FROM info_blocks WHERE title LIKE '%' || :query || '%' " +
            "OR content LIKE '%' || :query || '%' ORDER BY updatedAt DESC"
    )
    fun search(query: String): Flow<List<InfoBlockEntity>>

    @Insert
    suspend fun insert(block: InfoBlockEntity): Long

    @Update
    suspend fun update(block: InfoBlockEntity)

    @Delete
    suspend fun delete(block: InfoBlockEntity)
}
