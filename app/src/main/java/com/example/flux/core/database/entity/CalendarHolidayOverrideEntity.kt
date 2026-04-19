package com.example.flux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_holiday_overrides")
data class CalendarHolidayOverrideEntity(
    @PrimaryKey val date: String,
    @ColumnInfo(name = "is_holiday") val isHoliday: Int,
    val label: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    val version: Int = 1
)
