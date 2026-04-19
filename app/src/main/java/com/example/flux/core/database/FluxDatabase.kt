package com.example.flux.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.CalendarHolidayOverrideEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.DiaryTagEntity
import com.example.flux.core.database.entity.DiaryTagLinkEntity
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoSubtaskEntity

@Database(
    entities = [
        DiaryEntity::class,
        DiaryTagEntity::class,
        DiaryTagLinkEntity::class,
        TodoEntity::class,
        TodoSubtaskEntity::class,
        CalendarEventEntity::class,
        CalendarHolidayOverrideEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class FluxDatabase : RoomDatabase() {
    abstract fun diaryDao(): com.example.flux.core.database.dao.DiaryDao
    abstract fun todoDao(): com.example.flux.core.database.dao.TodoDao
    abstract fun todoSubtaskDao(): com.example.flux.core.database.dao.TodoSubtaskDao
    abstract fun eventDao(): com.example.flux.core.database.dao.EventDao
    abstract fun holidayDao(): com.example.flux.core.database.dao.HolidayDao
}
