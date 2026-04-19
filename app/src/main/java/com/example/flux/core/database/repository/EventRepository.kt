package com.example.flux.core.database.repository

import com.example.flux.core.database.dao.EventDao
import com.example.flux.core.database.entity.CalendarEventEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class EventRepository @Inject constructor(
    private val eventDao: EventDao
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
    }

    suspend fun softDeleteEvent(id: String) {
        val timestamp = com.example.flux.core.util.TimeUtil.getCurrentIsoTime()
        eventDao.softDeleteEvent(id, timestamp)
    }

    suspend fun restoreEvent(id: String) {
        val timestamp = com.example.flux.core.util.TimeUtil.getCurrentIsoTime()
        eventDao.restoreEvent(id, timestamp)
    }
}
