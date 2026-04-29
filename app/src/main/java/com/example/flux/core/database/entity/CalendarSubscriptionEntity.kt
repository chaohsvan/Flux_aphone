package com.example.flux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_subscription")
data class CalendarSubscriptionEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "ics_url") val icsUrl: String,
    val enabled: Int = 1,
    @ColumnInfo(name = "last_sync_time") val lastSyncTime: String? = null,
    val etag: String? = null,
    @ColumnInfo(name = "last_modified_header") val lastModifiedHeader: String? = null,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String
)
