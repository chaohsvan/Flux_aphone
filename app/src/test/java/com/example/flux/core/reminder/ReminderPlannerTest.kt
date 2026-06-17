package com.example.flux.core.reminder

import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.TodoEntity
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ReminderPlannerTest {
    private val originalTimeZone: TimeZone = TimeZone.getDefault()

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun eventPlan_usesFutureOccurrenceWhenFirstRecurringOccurrenceAlreadyPassed() {
        val event = event(
            startAt = "2026-04-24T09:30:00",
            recurrenceRule = "daily;interval=2",
            reminderMinutes = 10
        )
        val now = millis("2026-04-25T12:00:00")

        val plan = ReminderPlanner.eventPlan(event, nowMillis = now)

        assertNotNull(plan)
        assertEquals(millis("2026-04-26T09:20:00"), plan!!.triggerAtMillis)
    }

    @Test
    fun eventPlan_respectsRecurrenceUntilDate() {
        val event = event(
            startAt = "2026-04-24T09:30:00",
            recurrenceRule = "daily;until=2026-04-26",
            reminderMinutes = 0
        )
        val now = millis("2026-04-26T10:00:00")

        assertNull(ReminderPlanner.eventPlan(event, nowMillis = now))
    }

    @Test
    fun eventPlanAtTrigger_acceptsMatchingRecurringOccurrence() {
        val event = event(
            startAt = "2026-04-24T09:30:00",
            recurrenceRule = "weekly",
            reminderMinutes = 30
        )
        val trigger = millis("2026-05-01T09:00:00")

        val plan = ReminderPlanner.eventPlanAtTrigger(event, trigger)

        assertNotNull(plan)
        assertEquals(trigger, plan!!.triggerAtMillis)
    }

    @Test
    fun eventPlanAtTrigger_rejectsStaleAlarmAfterEventTimeChanged() {
        val event = event(
            startAt = "2026-04-24T11:30:00",
            recurrenceRule = null,
            reminderMinutes = 30
        )

        assertNull(ReminderPlanner.eventPlanAtTrigger(event, millis("2026-04-24T09:00:00")))
    }

    @Test
    fun diaryPlanAtTrigger_rejectsDeletedDiary() {
        val diary = diary(deletedAt = "2026-04-24T10:00:00")

        assertNull(ReminderPlanner.diaryPlanAtTrigger(diary, millis("2026-04-24T08:30:00")))
    }

    @Test
    fun todoPlanAtTrigger_rejectsCompletedTodo() {
        val todo = todo(status = "completed")

        assertNull(ReminderPlanner.todoPlanAtTrigger(todo, millis("2026-04-24T08:30:00")))
    }

    private fun millis(value: String): Long {
        return ReminderTimeParser.toEpochMillisOrNull(value) ?: error("Invalid test timestamp: $value")
    }

    private fun event(
        startAt: String = "2026-04-24T09:30:00",
        recurrenceRule: String? = null,
        reminderMinutes: Int? = 30,
        deletedAt: String? = null
    ): CalendarEventEntity {
        return CalendarEventEntity(
            id = "event-1",
            title = "Event",
            description = "",
            startAt = startAt,
            endAt = startAt,
            allDay = 0,
            color = null,
            locationName = null,
            reminderMinutes = reminderMinutes,
            recurrenceRule = recurrenceRule,
            createdAt = "2026-04-01T00:00:00",
            updatedAt = "2026-04-01T00:00:00",
            deletedAt = deletedAt
        )
    }

    private fun diary(deletedAt: String? = null): DiaryEntity {
        return DiaryEntity(
            id = "diary-1",
            entryDate = "2026-04-24",
            entryTime = "09:00",
            title = "Diary",
            contentMd = "Content",
            mood = null,
            weather = null,
            locationName = null,
            reminderMinutes = 30,
            createdAt = "2026-04-01T00:00:00",
            updatedAt = "2026-04-01T00:00:00",
            deletedAt = deletedAt,
            restoredAt = null,
            restoredIntoId = null
        )
    }

    private fun todo(status: String = "pending"): TodoEntity {
        return TodoEntity(
            id = "todo-1",
            projectId = null,
            title = "Todo",
            description = "",
            status = status,
            priority = "normal",
            dueAt = "2026-04-24T09:00:00",
            startAt = null,
            completedAt = null,
            reminderMinutes = 30,
            createdAt = "2026-04-01T00:00:00",
            updatedAt = "2026-04-01T00:00:00",
            deletedAt = null
        )
    }
}
