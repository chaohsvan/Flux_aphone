package com.example.flux.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.flux.core.database.entity.AttachmentMetadataEntity
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.CalendarHolidayOverrideEntity
import com.example.flux.core.database.entity.CalendarSubscriptionEntity
import com.example.flux.core.database.entity.CalendarStaticHolidayEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.DiaryFtsEntity
import com.example.flux.core.database.entity.DiaryTagEntity
import com.example.flux.core.database.entity.DiaryTagLinkEntity
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.database.entity.TodoHistoryEntity
import com.example.flux.core.database.entity.TodoProjectEntity
import com.example.flux.core.database.entity.TodoSubtaskEntity

@Database(
    entities = [
        DiaryEntity::class,
        DiaryFtsEntity::class,
        DiaryTagEntity::class,
        DiaryTagLinkEntity::class,
        TodoProjectEntity::class,
        TodoEntity::class,
        TodoSubtaskEntity::class,
        TodoHistoryEntity::class,
        CalendarEventEntity::class,
        CalendarHolidayOverrideEntity::class,
        CalendarStaticHolidayEntity::class,
        CalendarSubscriptionEntity::class,
        AttachmentMetadataEntity::class
    ],
    version = 9,
    exportSchema = true
)
abstract class FluxDatabase : RoomDatabase() {
    abstract fun diaryDao(): com.example.flux.core.database.dao.DiaryDao
    abstract fun todoDao(): com.example.flux.core.database.dao.TodoDao
    abstract fun todoSubtaskDao(): com.example.flux.core.database.dao.TodoSubtaskDao
    abstract fun todoProjectDao(): com.example.flux.core.database.dao.TodoProjectDao
    abstract fun todoHistoryDao(): com.example.flux.core.database.dao.TodoHistoryDao
    abstract fun eventDao(): com.example.flux.core.database.dao.EventDao
    abstract fun holidayDao(): com.example.flux.core.database.dao.HolidayDao
    abstract fun attachmentMetadataDao(): com.example.flux.core.database.dao.AttachmentMetadataDao
    abstract fun calendarSubscriptionDao(): com.example.flux.core.database.dao.CalendarSubscriptionDao
}
