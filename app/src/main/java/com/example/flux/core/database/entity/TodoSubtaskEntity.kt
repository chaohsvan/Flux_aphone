package com.example.flux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "todo_subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TodoEntity::class,
            parentColumns = ["id"],
            childColumns = ["todo_id"]
        )
    ]
)
data class TodoSubtaskEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "todo_id")
    val todoId: String,
    val title: String,
    @ColumnInfo(name = "is_completed", defaultValue = "0")
    val isCompleted: Int = 0,
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: String? = null,
    @ColumnInfo(defaultValue = "1")
    val version: Int = 1
)
