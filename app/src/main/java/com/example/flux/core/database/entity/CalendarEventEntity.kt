package com.example.flux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    @ColumnInfo(name = "start_at") val startAt: String,
    @ColumnInfo(name = "end_at") val endAt: String,
    @ColumnInfo(name = "all_day") val allDay: Int = 0,
    val color: String?,
    @ColumnInfo(name = "location_name") val locationName: String?,
    @ColumnInfo(name = "reminder_minutes") val reminderMinutes: Int?,
    @ColumnInfo(name = "recurrence_rule") val recurrenceRule: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: String?,
    val version: Int = 1
)
