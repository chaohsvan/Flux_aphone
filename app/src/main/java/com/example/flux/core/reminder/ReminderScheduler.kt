package com.example.flux.core.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.DiaryEntity
import com.example.flux.core.database.entity.TodoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleDiary(diary: DiaryEntity) {
        val reminderMinutes = diary.reminderMinutes
        val triggerSource = ReminderTimeParser.diaryTriggerSource(diary.entryDate, diary.entryTime)
        if (reminderMinutes == null || triggerSource.isNullOrBlank() || diary.deletedAt != null) {
            cancelDiary(diary.id)
            return
        }

        val triggerAt = ReminderTimeParser.toEpochMillisOrNull(triggerSource)
            ?.minus(reminderMinutes.toLong() * MILLIS_PER_MINUTE)
            ?: run {
                cancelDiary(diary.id)
                return
            }
        val message = diary.contentMd
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.take(120)
            ?: "日记提醒"
        schedule(
            requestCode = requestCode(REMINDER_TYPE_DIARY, diary.id),
            triggerAtMillis = triggerAt,
            intent = ReminderReceiver.intent(
                context = context,
                type = REMINDER_TYPE_DIARY,
                id = diary.id,
                title = diary.title.ifBlank { "日记提醒" },
                message = message
            )
        )
    }

    fun cancelDiary(id: String) {
        cancel(REMINDER_TYPE_DIARY, id)
    }

    fun scheduleTodo(todo: TodoEntity) {
        val reminderMinutes = todo.reminderMinutes
        val triggerSource = todo.dueAt ?: todo.startAt
        if (
            reminderMinutes == null ||
            triggerSource.isNullOrBlank() ||
            todo.deletedAt != null ||
            todo.status == "completed"
        ) {
            cancelTodo(todo.id)
            return
        }

        val triggerAt = ReminderTimeParser.toEpochMillisOrNull(triggerSource)
            ?.minus(reminderMinutes.toLong() * MILLIS_PER_MINUTE)
            ?: run {
                cancelTodo(todo.id)
                return
            }
        schedule(
            requestCode = requestCode(REMINDER_TYPE_TODO, todo.id),
            triggerAtMillis = triggerAt,
            intent = ReminderReceiver.intent(
                context = context,
                type = REMINDER_TYPE_TODO,
                id = todo.id,
                title = todo.title,
                message = todo.description.ifBlank { "待办提醒" }
            )
        )
    }

    fun cancelTodo(id: String) {
        cancel(REMINDER_TYPE_TODO, id)
    }

    fun scheduleEvent(event: CalendarEventEntity) {
        val reminderMinutes = event.reminderMinutes
        if (reminderMinutes == null || event.deletedAt != null) {
            cancelEvent(event.id)
            return
        }

        val triggerAt = ReminderTimeParser.toEpochMillisOrNull(event.startAt)
            ?.minus(reminderMinutes.toLong() * MILLIS_PER_MINUTE)
            ?: run {
                cancelEvent(event.id)
                return
            }
        schedule(
            requestCode = requestCode(REMINDER_TYPE_EVENT, event.id),
            triggerAtMillis = triggerAt,
            intent = ReminderReceiver.intent(
                context = context,
                type = REMINDER_TYPE_EVENT,
                id = event.id,
                title = event.title,
                message = event.description.ifBlank { "日历事件提醒" }
            )
        )
    }

    fun cancelEvent(id: String) {
        cancel(REMINDER_TYPE_EVENT, id)
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun schedule(requestCode: Int, triggerAtMillis: Long, intent: Intent) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (triggerAtMillis <= System.currentTimeMillis()) {
            alarmManager.cancel(pendingIntent)
            return
        }
        if (canScheduleExactAlarm()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun cancel(type: String, id: String) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(type, id),
            ReminderReceiver.intent(context, type, id, "", ""),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun requestCode(type: String, id: String): Int {
        return "$type:$id".hashCode()
    }

    private fun canScheduleExactAlarm(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private companion object {
        const val REMINDER_TYPE_DIARY = "diary"
        const val REMINDER_TYPE_TODO = "todo"
        const val REMINDER_TYPE_EVENT = "event"
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
