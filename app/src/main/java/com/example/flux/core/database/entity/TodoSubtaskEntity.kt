package com.example.flux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_subtasks")
data class TodoSubtaskEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "todo_id")
    val todoId: String,
    val title: String,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Int = 0,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: String? = null,
    val version: Int = 1
)
