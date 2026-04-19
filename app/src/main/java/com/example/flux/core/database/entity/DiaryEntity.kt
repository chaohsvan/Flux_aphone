package com.example.flux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diaries",
    indices = [
        Index(name = "idx_diaries_date", value = ["entry_date", "deleted_at"]),
        Index(name = "idx_diaries_mood", value = ["mood", "deleted_at"]),
        Index(name = "idx_diaries_one_active_per_day", value = ["entry_date"], unique = true)
    ]
)
data class DiaryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "entry_date") val entryDate: String,
    @ColumnInfo(name = "entry_time") val entryTime: String?,
    val title: String,
    @ColumnInfo(name = "content_md", defaultValue = "''") val contentMd: String = "",
    val mood: String?,
    val weather: String?,
    @ColumnInfo(name = "location_name") val locationName: String?,
    @ColumnInfo(name = "is_favorite", defaultValue = "0") val isFavorite: Int = 0,
    @ColumnInfo(name = "word_count", defaultValue = "0") val wordCount: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
    @ColumnInfo(defaultValue = "1") val version: Int = 1,
    @ColumnInfo(name = "restored_at") val restoredAt: String?,
    @ColumnInfo(name = "restored_into_id") val restoredIntoId: String?
)
