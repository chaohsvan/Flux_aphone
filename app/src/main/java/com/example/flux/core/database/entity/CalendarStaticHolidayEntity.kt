package com.example.flux.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_static_holidays")
data class CalendarStaticHolidayEntity(
    @PrimaryKey
    @ColumnInfo(name = "day")
    val date: String,
    @ColumnInfo(name = "is_holiday", defaultValue = "1")
    val isHoliday: Int = 1,
    val name: String?,
    @ColumnInfo(defaultValue = "'chinese-days'")
    val source: String = "chinese-days",
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
