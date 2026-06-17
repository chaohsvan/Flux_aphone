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
        val plan = ReminderPlanner.diaryPlan(diary)
        if (plan == null) {
            cancelDiary(diary.id)
            return
        }
        schedule(
            requestCode = requestCode(ReminderContract.TYPE_DIARY, diary.id),
            triggerAtMillis = plan.triggerAtMillis,
            intent = ReminderReceiver.intent(
                context = context,
                type = ReminderContract.TYPE_DIARY,
                id = diary.id,
                title = plan.title,
                message = plan.message,
                triggerAtMillis = plan.triggerAtMillis
            )
        )
    }

    fun cancelDiary(id: String) {
        cancel(ReminderContract.TYPE_DIARY, id)
    }

    fun scheduleTodo(todo: TodoEntity) {
        val plan = ReminderPlanner.todoPlan(todo)
        if (plan == null) {
            cancelTodo(todo.id)
            return
        }
        schedule(
            requestCode = requestCode(ReminderContract.TYPE_TODO, todo.id),
            triggerAtMillis = plan.triggerAtMillis,
            intent = ReminderReceiver.intent(
                context = context,
                type = ReminderContract.TYPE_TODO,
                id = todo.id,
                title = plan.title,
                message = plan.message,
                triggerAtMillis = plan.triggerAtMillis
            )
        )
    }

    fun cancelTodo(id: String) {
        cancel(ReminderContract.TYPE_TODO, id)
    }

    fun scheduleEvent(event: CalendarEventEntity) {
        val plan = ReminderPlanner.eventPlan(event)
        if (plan == null) {
            cancelEvent(event.id)
            return
        }
        schedule(
            requestCode = requestCode(ReminderContract.TYPE_EVENT, event.id),
            triggerAtMillis = plan.triggerAtMillis,
            intent = ReminderReceiver.intent(
                context = context,
                type = ReminderContract.TYPE_EVENT,
                id = event.id,
                title = plan.title,
                message = plan.message,
                triggerAtMillis = plan.triggerAtMillis
            )
        )
    }

    fun cancelEvent(id: String) {
        cancel(ReminderContract.TYPE_EVENT, id)
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
}
