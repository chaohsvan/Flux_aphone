package com.example.flux.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.flux.core.database.entity.TodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Query("""
        SELECT * FROM todos 
        WHERE deleted_at IS NULL 
        ORDER BY 
            CASE WHEN status = 'completed' THEN 1 ELSE 0 END ASC,
            is_important DESC,
            CASE WHEN due_at IS NULL OR due_at = '' THEN 1 ELSE 0 END ASC,
            due_at ASC,
            sort_order ASC,
            created_at DESC
    """)
    fun getActiveTodos(): Flow<List<TodoEntity>>

    @Query("""
        SELECT * FROM todos 
        WHERE deleted_at IS NULL AND (substr(due_at, 1, 10) = :date OR (due_at IS NULL AND substr(created_at, 1, 10) = :date))
        ORDER BY 
            CASE WHEN status = 'completed' THEN 1 ELSE 0 END ASC,
            is_important DESC,
            CASE WHEN due_at IS NULL OR due_at = '' THEN 1 ELSE 0 END ASC,
            due_at ASC,
            sort_order ASC
    """)
    fun getTodosByDate(date: String): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    fun getDeletedTodos(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE id = :id LIMIT 1")
    suspend fun getTodoById(id: String): TodoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity)

    @Update
    suspend fun updateTodo(todo: TodoEntity)

    @Query("UPDATE todos SET deleted_at = :timestamp WHERE id = :id")
    suspend fun softDeleteTodo(id: String, timestamp: String)

    @Query("UPDATE todos SET status = :newStatus WHERE id = :id")
    suspend fun updateTodoStatus(id: String, newStatus: String)

    @Query("UPDATE todos SET priority = :priority, is_important = :isImportant, updated_at = :updatedAt WHERE id IN (:ids)")
    suspend fun updatePriority(ids: List<String>, priority: String, isImportant: Int, updatedAt: String)

    @Query("UPDATE todos SET sort_order = :sortOrder, updated_at = :updatedAt, version = version + 1 WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int, updatedAt: String)

    @Query("UPDATE todos SET project_id = NULL, updated_at = :updatedAt, version = version + 1 WHERE project_id = :projectId")
    suspend fun clearProject(projectId: String, updatedAt: String)
}
