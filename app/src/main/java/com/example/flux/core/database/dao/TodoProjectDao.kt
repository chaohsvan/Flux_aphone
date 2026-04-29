package com.example.flux.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flux.core.database.entity.TodoProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoProjectDao {
    @Query("SELECT * FROM todo_projects WHERE deleted_at IS NULL ORDER BY sort_order ASC, created_at ASC")
    fun getActiveProjects(): Flow<List<TodoProjectEntity>>

    @Query("SELECT * FROM todo_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): TodoProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: TodoProjectEntity)

    @Query("UPDATE todo_projects SET deleted_at = :deletedAt, updated_at = :deletedAt, version = version + 1 WHERE id = :id")
    suspend fun softDeleteProject(id: String, deletedAt: String)

    @Query("UPDATE todo_projects SET name = :name, updated_at = :updatedAt, version = version + 1 WHERE id = :id AND deleted_at IS NULL")
    suspend fun renameProject(id: String, name: String, updatedAt: String)
}
