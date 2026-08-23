package com.rainyday.saveableapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SimpleListDao {
    @Query("SELECT * FROM simple_lists ORDER BY position ASC")
    fun observeLists(): Flow<List<SimpleListEntity>>

    @Query("SELECT * FROM simple_lists WHERE id = :id")
    fun observeById(id: String): Flow<SimpleListEntity?>

    @Query("SELECT * FROM simple_lists WHERE id = :id")
    suspend fun getById(id: String): SimpleListEntity?

    @Insert
    suspend fun insert(list: SimpleListEntity)

    @Update
    suspend fun update(list: SimpleListEntity)

    @Delete
    suspend fun delete(list: SimpleListEntity)

    @Query("DELETE FROM simple_lists WHERE id = :id")
    suspend fun deleteById(id: String)
}
