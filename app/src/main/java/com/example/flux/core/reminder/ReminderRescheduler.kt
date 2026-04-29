package com.example.flux.core.reminder

import com.example.flux.core.database.dao.EventDao
import com.example.flux.core.database.dao.TodoDao
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class ReminderRescheduler @Inject constructor(
    private val todoDao: TodoDao,
    private val eventDao: EventDao,
    private val reminderScheduler: ReminderScheduler
) {
    suspend fun rescheduleAll() {
        todoDao.getActiveTodos()
            .first()
            .forEach(reminderScheduler::scheduleTodo)

        eventDao.getActiveEvents()
            .first()
            .forEach(reminderScheduler::scheduleEvent)
    }
}
