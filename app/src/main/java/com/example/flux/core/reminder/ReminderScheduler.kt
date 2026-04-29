package com.example.flux.core.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.TodoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

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

        val triggerAt = triggerSource.toEpochMillisOrNull()
            ?.minus(reminderMinutes * MILLIS_PER_MINUTE)
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

        val triggerAt = event.startAt.toEpochMillisOrNull()
            ?.minus(reminderMinutes * MILLIS_PER_MINUTE)
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
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
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

    private fun String.toEpochMillisOrNull(): Long? {
        val trimmed = trim()
        val pattern = when {
            Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z").matches(trimmed) -> "yyyy-MM-dd'T'HH:mm:ss'Z'"
            Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}").matches(trimmed) -> "yyyy-MM-dd'T'HH:mm:ss"
            Regex("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}").matches(trimmed) -> "yyyy-MM-dd HH:mm"
            Regex("\\d{4}-\\d{2}-\\d{2}").matches(trimmed) -> "yyyy-MM-dd"
            else -> return null
        }
        return runCatching {
            val date = SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                if (pattern.endsWith("'Z'")) {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            }.parse(trimmed) ?: return null
            if (pattern == "yyyy-MM-dd") {
                Calendar.getInstance().apply {
                    time = date
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            } else {
                date.time
            }
        }.getOrNull()
    }

    private companion object {
        const val REMINDER_TYPE_TODO = "todo"
        const val REMINDER_TYPE_EVENT = "event"
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
