package com.rainyday.saveableapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InfoCategoryDao {
    @Query("SELECT * FROM info_categories ORDER BY position ASC")
    fun observeCategories(): Flow<List<InfoCategoryEntity>>

    @Query("SELECT * FROM info_categories WHERE id = :id")
    fun observeById(id: Long): Flow<InfoCategoryEntity?>

    @Insert
    suspend fun insert(category: InfoCategoryEntity): Long

    @Update
    suspend fun update(category: InfoCategoryEntity)

    @Delete
    suspend fun delete(category: InfoCategoryEntity)
}
