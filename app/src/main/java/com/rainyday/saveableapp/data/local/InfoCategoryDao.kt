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
    fun observeById(id: String): Flow<InfoCategoryEntity?>

    @Query("SELECT * FROM info_categories WHERE id = :id")
    suspend fun getById(id: String): InfoCategoryEntity?

    @Insert
    suspend fun insert(category: InfoCategoryEntity)

    @Update
    suspend fun update(category: InfoCategoryEntity)

    @Delete
    suspend fun delete(category: InfoCategoryEntity)

    @Query("DELETE FROM info_categories WHERE id = :id")
    suspend fun deleteById(id: String)
}
