package com.example.flux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_holidays")
data class CalendarHolidayOverrideEntity(
    @PrimaryKey
    @ColumnInfo(name = "day")
    val date: String,
    @ColumnInfo(name = "is_holiday", defaultValue = "1")
    val isHoliday: Int = 1,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String
)
