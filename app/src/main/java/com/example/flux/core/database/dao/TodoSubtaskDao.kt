package com.example.flux.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.ColumnInfo
import androidx.room.Query
import com.example.flux.core.database.entity.TodoSubtaskEntity
import kotlinx.coroutines.flow.Flow

data class TodoSubtaskProgressEntity(
    @ColumnInfo(name = "todo_id") val todoId: String,
    @ColumnInfo(name = "total_count") val totalCount: Int,
    @ColumnInfo(name = "completed_count") val completedCount: Int
)

@Dao
interface TodoSubtaskDao {
    @Query("SELECT * FROM todo_subtasks WHERE todo_id = :todoId AND deleted_at IS NULL ORDER BY sort_order ASC")
    fun getSubtasksForTodo(todoId: String): Flow<List<TodoSubtaskEntity>>

    @Query("SELECT * FROM todo_subtasks WHERE todo_id = :todoId AND deleted_at IS NULL ORDER BY sort_order ASC")
    suspend fun getSubtasksSnapshotForTodo(todoId: String): List<TodoSubtaskEntity>

    @Query("""
        SELECT 
            todo_id,
            COUNT(*) AS total_count,
            SUM(CASE WHEN is_completed = 1 THEN 1 ELSE 0 END) AS completed_count
        FROM todo_subtasks
        WHERE deleted_at IS NULL
        GROUP BY todo_id
    """)
    fun getSubtaskProgress(): Flow<List<TodoSubtaskProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: TodoSubtaskEntity)

    @Query("UPDATE todo_subtasks SET is_completed = :isCompleted, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateCompleted(id: String, isCompleted: Int, updatedAt: String)

    @Query("UPDATE todo_subtasks SET sort_order = :sortOrder, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int, updatedAt: String)

    @Query("UPDATE todo_subtasks SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id")
    suspend fun softDeleteSubtask(id: String, deletedAt: String)
}
