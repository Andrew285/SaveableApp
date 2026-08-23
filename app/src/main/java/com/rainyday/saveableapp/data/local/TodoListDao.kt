package com.rainyday.saveableapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoListDao {
    @Query("SELECT * FROM todo_lists ORDER BY position ASC")
    fun observeLists(): Flow<List<TodoListEntity>>

    @Query("SELECT * FROM todo_lists WHERE id = :id")
    suspend fun getById(id: String): TodoListEntity?

    @Query("SELECT * FROM todo_lists WHERE id = :id")
    fun observeById(id: String): Flow<TodoListEntity?>

    @Insert
    suspend fun insert(list: TodoListEntity)

    @Update
    suspend fun update(list: TodoListEntity)

    @Delete
    suspend fun delete(list: TodoListEntity)

    @Query("DELETE FROM todo_lists WHERE id = :id")
    suspend fun deleteById(id: String)
}
