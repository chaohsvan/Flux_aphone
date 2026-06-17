package com.example.flux.core.reminder

import com.example.flux.core.database.dao.DiaryDao
import com.example.flux.core.database.dao.EventDao
import com.example.flux.core.database.dao.TodoDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRescheduler @Inject constructor(
    private val diaryDao: DiaryDao,
    private val todoDao: TodoDao,
    private val eventDao: EventDao,
    private val reminderScheduler: ReminderScheduler
) {
    suspend fun cancelAllKnown() {
        diaryDao.getAllDiariesSnapshot().forEach { diary ->
            reminderScheduler.cancelDiary(diary.id)
        }
        todoDao.getAllTodosSnapshot().forEach { todo ->
            reminderScheduler.cancelTodo(todo.id)
        }
        eventDao.getAllEventsSnapshot().forEach { event ->
            reminderScheduler.cancelEvent(event.id)
        }
    }

    fun cancelEvent(id: String) {
        reminderScheduler.cancelEvent(id)
    }

    suspend fun rescheduleAll() {
        diaryDao.getAllDiariesSnapshot()
            .forEach { diary ->
                if (ReminderPlanner.diaryPlan(diary) == null) {
                    reminderScheduler.cancelDiary(diary.id)
                } else {
                    reminderScheduler.scheduleDiary(diary)
                }
            }

        todoDao.getAllTodosSnapshot()
            .forEach { todo ->
                if (ReminderPlanner.todoPlan(todo) == null) {
                    reminderScheduler.cancelTodo(todo.id)
                } else {
                    reminderScheduler.scheduleTodo(todo)
                }
            }

        eventDao.getAllEventsSnapshot()
            .forEach { event ->
                if (ReminderPlanner.eventPlan(event) == null) {
                    reminderScheduler.cancelEvent(event.id)
                } else {
                    reminderScheduler.scheduleEvent(event)
                }
            }
    }
}
