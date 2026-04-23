package com.example.flux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todo_history",
    foreignKeys = [
        ForeignKey(
            entity = TodoEntity::class,
            parentColumns = ["id"],
            childColumns = ["todo_id"]
        )
    ],
    indices = [
        Index(name = "idx_todo_history_todo", value = ["todo_id", "created_at"])
    ]
)
data class TodoHistoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "todo_id") val todoId: String,
    val action: String,
    val summary: String,
    @ColumnInfo(name = "payload_json", defaultValue = "'{}'") val payloadJson: String = "{}",
    @ColumnInfo(name = "created_at") val createdAt: String
)
