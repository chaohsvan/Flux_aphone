package com.example.flux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todos",
    foreignKeys = [
        ForeignKey(
            entity = TodoProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"]
        )
    ],
    indices = [
        Index(name = "idx_todos_due", value = ["due_at", "status", "deleted_at"]),
        Index(name = "idx_todos_project", value = ["project_id", "deleted_at"]),
        Index(name = "idx_todos_status", value = ["status", "deleted_at"])
    ]
)
data class TodoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "project_id") val projectId: String?,
    val title: String,
    @ColumnInfo(defaultValue = "''") val description: String = "",
    @ColumnInfo(defaultValue = "'pending'") val status: String = "pending",
    @ColumnInfo(defaultValue = "'none'") val priority: String = "normal",
    @ColumnInfo(name = "due_at") val dueAt: String?,
    @ColumnInfo(name = "start_at") val startAt: String?,
    @ColumnInfo(name = "completed_at") val completedAt: String?,
    @ColumnInfo(name = "sort_order", defaultValue = "0") val sortOrder: Int = 0,
    @ColumnInfo(name = "is_my_day", defaultValue = "0") val isMyDay: Int = 0,
    @ColumnInfo(name = "reminder_minutes") val reminderMinutes: Int?,
    @ColumnInfo(name = "recurrence", defaultValue = "'none'") val recurrence: String = "none",
    @ColumnInfo(name = "recurrence_interval", defaultValue = "1") val recurrenceInterval: Int = 1,
    @ColumnInfo(name = "recurrence_until") val recurrenceUntil: String? = null,
    @ColumnInfo(name = "parent_todo_id") val parentTodoId: String? = null,
    @ColumnInfo(name = "is_important", defaultValue = "0") val isImportant: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
    @ColumnInfo(defaultValue = "1") val version: Int = 1
)
