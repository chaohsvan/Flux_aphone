package com.example.flux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Documents the app-managed diary search index shape. It is a normal SQLite
 * table rather than a virtual FTS table so it works on Android builds without
 * optional SQLite search modules.
 */
@Entity(tableName = "diary_search_index")
data class DiaryFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "diary_id")
    val diaryId: String,
    @ColumnInfo(name = "entry_date")
    val entryDate: String,
    @ColumnInfo(name = "entry_time")
    val entryTime: String?,
    val title: String,
    @ColumnInfo(name = "content_md")
    val contentMd: String,
    val mood: String?,
    val weather: String?,
    @ColumnInfo(name = "location_name")
    val locationName: String?,
    @ColumnInfo(defaultValue = "''")
    val tags: String = ""
)
