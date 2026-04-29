package com.example.flux.core.database.repository

import com.example.flux.core.database.dao.EventDao
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.reminder.ReminderScheduler
import com.example.flux.core.util.RecurrenceUtil
import com.example.flux.core.util.TimeUtil
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class EventRepository @Inject constructor(
    private val eventDao: EventDao,
    private val reminderScheduler: ReminderScheduler
) {
    fun getActiveEvents(): Flow<List<CalendarEventEntity>> {
        return eventDao.getActiveEvents()
    }

    fun getEventsByDate(date: String): Flow<List<CalendarEventEntity>> {
        return eventDao.getEventsByDate(date)
    }

    fun getDeletedEvents(): Flow<List<CalendarEventEntity>> {
        return eventDao.getDeletedEvents()
    }

    suspend fun getEventById(id: String): CalendarEventEntity? {
        return eventDao.getEventById(id)
    }

    suspend fun saveEvent(event: CalendarEventEntity) {
        eventDao.insertEvent(event)
        reminderScheduler.scheduleEvent(event)
    }

    suspend fun softDeleteEvent(id: String) {
        val timestamp = TimeUtil.getCurrentIsoTime()
        eventDao.softDeleteEvent(id, timestamp)
        reminderScheduler.cancelEvent(id)
    }

    suspend fun softDeleteEventFromDate(id: String, date: String) {
        val event = eventDao.getEventById(id) ?: return
        val recurrenceRule = event.recurrenceRule
        if (recurrenceRule.isNullOrBlank()) {
            softDeleteEvent(id)
            return
        }

        val trimmedRule = RecurrenceUtil.trimRuleBeforeDate(
            value = event.startAt,
            recurrence = recurrenceRule,
            date = date
        )
        if (trimmedRule == null) {
            softDeleteEvent(id)
        } else if (trimmedRule != recurrenceRule) {
            saveEvent(
                event.copy(
                    recurrenceRule = trimmedRule,
                    updatedAt = TimeUtil.getCurrentIsoTime(),
                    version = event.version + 1
                )
            )
        }
    }

    suspend fun restoreEvent(id: String) {
        val timestamp = TimeUtil.getCurrentIsoTime()
        eventDao.restoreEvent(id, timestamp)
    }
}
