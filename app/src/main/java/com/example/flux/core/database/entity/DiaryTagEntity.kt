package com.example.flux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "diary_tags")
data class DiaryTagEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
    @ColumnInfo(defaultValue = "1") val version: Int = 1
)

@Entity(
    tableName = "diary_tag_links",
    primaryKeys = ["diary_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntity::class,
            parentColumns = ["id"],
            childColumns = ["diary_id"]
        ),
        ForeignKey(
            entity = DiaryTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"]
        )
    ],
    indices = [
        Index(name = "idx_diary_tag_links_tag", value = ["tag_id", "deleted_at"])
    ]
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
