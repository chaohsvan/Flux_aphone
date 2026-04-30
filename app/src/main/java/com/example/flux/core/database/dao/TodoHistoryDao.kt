package com.example.flux.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flux.core.database.entity.TodoHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoHistoryDao {
    @Query("SELECT * FROM todo_history WHERE todo_id = :todoId ORDER BY created_at DESC")
    fun getHistoryForTodo(todoId: String): Flow<List<TodoHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: TodoHistoryEntity)

    @Query("DELETE FROM todo_history WHERE todo_id = :todoId")
    suspend fun permanentlyDeleteHistoryForTodo(todoId: String)
}
