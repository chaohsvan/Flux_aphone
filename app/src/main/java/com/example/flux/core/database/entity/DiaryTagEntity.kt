package com.example.flux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_tags")
data class DiaryTagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
    val version: Int = 1
)

@Entity(
    tableName = "diary_tag_links",
    primaryKeys = ["diary_id", "tag_id"]
)
data class DiaryTagLinkEntity(
    @ColumnInfo(name = "diary_id") val diaryId: String,
    @ColumnInfo(name = "tag_id") val tagId: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?
)

data class DiaryTagSummary(
    @ColumnInfo(name = "diary_id") val diaryId: String,
    @ColumnInfo(name = "tag_name") val tagName: String
)
