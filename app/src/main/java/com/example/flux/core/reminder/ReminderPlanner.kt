package com.example.flux.core.reminder

import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.util.RecurrenceUtil

data class ReminderPlan(
    val triggerAtMillis: Long,
    val title: String,
    val message: String
)

object ReminderPlanner {
    fun diaryPlan(diary: DiaryEntity, nowMillis: Long = System.currentTimeMillis()): ReminderPlan? {
        val plan = diaryPlanIgnoringCurrentTime(diary) ?: return null
        return plan.takeIf { it.triggerAtMillis > nowMillis }
    }

    fun diaryPlanAtTrigger(diary: DiaryEntity, triggerAtMillis: Long): ReminderPlan? {
        val plan = diaryPlanIgnoringCurrentTime(diary) ?: return null
        return plan.takeIf { it.triggerAtMillis == triggerAtMillis }
    }

    private fun diaryPlanIgnoringCurrentTime(diary: DiaryEntity): ReminderPlan? {
        val reminderMinutes = diary.reminderMinutes ?: return null
        val triggerSource = ReminderTimeParser.diaryTriggerSource(diary.entryDate, diary.entryTime) ?: return null
        if (triggerSource.isBlank() || diary.deletedAt != null) return null

        val triggerAt = ReminderTimeParser.toEpochMillisOrNull(triggerSource)
            ?.minus(reminderMinutes.toLong() * MILLIS_PER_MINUTE)
            ?: return null
        val message = diary.contentMd
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.take(120)
            ?: "日记提醒"
        return ReminderPlan(
            triggerAtMillis = triggerAt,
            title = diary.title.ifBlank { "日记提醒" },
            message = message
        )
    }

    fun todoPlan(todo: TodoEntity, nowMillis: Long = System.currentTimeMillis()): ReminderPlan? {
        val plan = todoPlanIgnoringCurrentTime(todo) ?: return null
        return plan.takeIf { it.triggerAtMillis > nowMillis }
    }

    fun todoPlanAtTrigger(todo: TodoEntity, triggerAtMillis: Long): ReminderPlan? {
        val plan = todoPlanIgnoringCurrentTime(todo) ?: return null
        return plan.takeIf { it.triggerAtMillis == triggerAtMillis }
    }

    private fun todoPlanIgnoringCurrentTime(todo: TodoEntity): ReminderPlan? {
        val reminderMinutes = todo.reminderMinutes ?: return null
        val triggerSource = todo.dueAt ?: todo.startAt
        if (
            triggerSource.isNullOrBlank() ||
            todo.deletedAt != null ||
            todo.status == "completed"
        ) {
            return null
        }

        val triggerAt = ReminderTimeParser.toEpochMillisOrNull(triggerSource)
            ?.minus(reminderMinutes.toLong() * MILLIS_PER_MINUTE)
            ?: return null
        return ReminderPlan(
            triggerAtMillis = triggerAt,
            title = todo.title,
            message = todo.description.ifBlank { "待办提醒" }
        )
    }

    fun eventPlan(event: CalendarEventEntity, nowMillis: Long = System.currentTimeMillis()): ReminderPlan? {
        val reminderMinutes = event.reminderMinutes ?: return null
        if (event.deletedAt != null) return null

        val nextStartSource = nextEventStartSource(event, reminderMinutes, nowMillis) ?: return null
        val triggerAt = ReminderTimeParser.toEpochMillisOrNull(nextStartSource)
            ?.minus(reminderMinutes.toLong() * MILLIS_PER_MINUTE)
            ?.takeIf { it > nowMillis }
            ?: return null
        return ReminderPlan(
            triggerAtMillis = triggerAt,
            title = event.title,
            message = event.description.ifBlank { "日历事件提醒" }
        )
    }

    fun eventPlanAtTrigger(event: CalendarEventEntity, triggerAtMillis: Long): ReminderPlan? {
        val reminderMinutes = event.reminderMinutes ?: return null
        if (event.deletedAt != null) return null

        val eventStartSource = eventStartSourceForTrigger(event, reminderMinutes, triggerAtMillis) ?: return null
        val eventTriggerAt = ReminderTimeParser.toEpochMillisOrNull(eventStartSource)
            ?.minus(reminderMinutes.toLong() * MILLIS_PER_MINUTE)
            ?: return null
        if (eventTriggerAt != triggerAtMillis) return null
        return ReminderPlan(
            triggerAtMillis = eventTriggerAt,
            title = event.title,
            message = event.description.ifBlank { "日历事件提醒" }
        )
    }

    private fun nextEventStartSource(
        event: CalendarEventEntity,
        reminderMinutes: Int,
        nowMillis: Long
    ): String? {
        val spec = RecurrenceUtil.parseSpec(event.recurrenceRule)
        if (spec == null) return event.startAt

        var cursorValue: String? = event.startAt
        var count = 0
        while (!cursorValue.isNullOrBlank() && count < MAX_RECURRENCE_SCAN_COUNT) {
            if (isPastRecurrenceUntil(cursorValue, spec.until)) break
            val triggerAt = ReminderTimeParser.toEpochMillisOrNull(cursorValue)
                ?.minus(reminderMinutes.toLong() * MILLIS_PER_MINUTE)
            if (triggerAt != null && triggerAt > nowMillis) return cursorValue
            cursorValue = RecurrenceUtil.nextValue(cursorValue, event.recurrenceRule.orEmpty(), 1)
            count += 1
        }
        return null
    }

    private fun eventStartSourceForTrigger(
        event: CalendarEventEntity,
        reminderMinutes: Int,
        triggerAtMillis: Long
    ): String? {
        val targetStartMillis = triggerAtMillis + reminderMinutes.toLong() * MILLIS_PER_MINUTE
        val spec = RecurrenceUtil.parseSpec(event.recurrenceRule)
        if (spec == null) {
            return event.startAt.takeIf {
                ReminderTimeParser.toEpochMillisOrNull(it) == targetStartMillis
            }
        }

        var cursorValue: String? = event.startAt
        var count = 0
        while (!cursorValue.isNullOrBlank() && count < MAX_RECURRENCE_SCAN_COUNT) {
            if (isPastRecurrenceUntil(cursorValue, spec.until)) break
            val startMillis = ReminderTimeParser.toEpochMillisOrNull(cursorValue)
            when {
                startMillis == targetStartMillis -> return cursorValue
                startMillis != null && startMillis > targetStartMillis -> return null
            }
            cursorValue = RecurrenceUtil.nextValue(cursorValue, event.recurrenceRule.orEmpty(), 1)
            count += 1
        }
        return null
    }

    private fun isPastRecurrenceUntil(value: String, until: String?): Boolean {
        return until != null && value.take(10) > until
    }

    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MAX_RECURRENCE_SCAN_COUNT = 800
}
