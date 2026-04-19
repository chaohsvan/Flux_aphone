package com.example.flux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "project_id") val projectId: String?,
    val title: String,
    val description: String = "",
    val status: String = "pending",
    val priority: String = "normal",
    @ColumnInfo(name = "due_at") val dueAt: String?,
    @ColumnInfo(name = "start_at") val startAt: String?,
    @ColumnInfo(name = "completed_at") val completedAt: String?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "is_important") val isImportant: Int = 0,
    @ColumnInfo(name = "reminder_minutes") val reminderMinutes: Int?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
    val version: Int = 1
)
