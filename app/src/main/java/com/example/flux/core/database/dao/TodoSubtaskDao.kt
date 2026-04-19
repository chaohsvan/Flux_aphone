package com.example.flux.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flux.core.database.entity.TodoSubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoSubtaskDao {
    @Query("SELECT * FROM todo_subtasks WHERE todo_id = :todoId AND deleted_at IS NULL ORDER BY sort_order ASC")
    fun getSubtasksForTodo(todoId: String): Flow<List<TodoSubtaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: TodoSubtaskEntity)

    @Query("UPDATE todo_subtasks SET is_completed = :isCompleted, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateCompleted(id: String, isCompleted: Int, updatedAt: String)

    @Query("UPDATE todo_subtasks SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id")
    suspend fun softDeleteSubtask(id: String, deletedAt: String)
}
