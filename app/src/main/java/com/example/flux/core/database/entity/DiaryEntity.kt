package com.example.flux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diaries")
data class DiaryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "entry_date") val entryDate: String,
    @ColumnInfo(name = "entry_time") val entryTime: String?,
    val title: String,
    @ColumnInfo(name = "content_md") val contentMd: String = "",
    val mood: String?,
    val weather: String?,
    @ColumnInfo(name = "location_name") val locationName: String?,
    @ColumnInfo(name = "is_favorite") val isFavorite: Int = 0,
    @ColumnInfo(name = "word_count") val wordCount: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
    val version: Int = 1,
    @ColumnInfo(name = "restored_at") val restoredAt: String?,
    @ColumnInfo(name = "restored_into_id") val restoredIntoId: String?
)
