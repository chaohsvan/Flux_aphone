package com.example.flux.feature.calendar.presentation.component

import androidx.compose.ui.graphics.Color
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.domain.calendar.DailyAggregation
import com.example.flux.core.util.RecurrenceUtil
import com.example.flux.feature.calendar.presentation.CalendarMonth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun CalendarEventEntity.label(): String {
    val time = if (allDay == 1) {
        "全天"
    } else if (startAt.length >= 16) {
        startAt.substring(11, 16)
    } else {
        ""
    }
    val place = locationName?.takeIf { it.isNotBlank() }?.let { " @ $it" }.orEmpty()
    return "- $title${if (time.isBlank()) "" else " ($time)"}$place"
}

fun CalendarEventEntity.timelineTime(): String {
    return if (startAt.length >= 16) startAt.substring(11, 16) else "--:--"
}

fun CalendarEventEntity.eventSubtitle(): String {
    val parts = buildList {
        if (allDay == 1) {
            add("全天")
        } else {
            val start = startAt.takeIf { it.length >= 16 }?.substring(11, 16)
            val end = endAt.takeIf { it.length >= 16 }?.substring(11, 16)
            if (start != null && end != null) add("$start-$end")
        }
        locationName?.takeIf { it.isNotBlank() }?.let { add(it) }
        reminderMinutes?.let { add("提前 $it 分钟提醒") }
        recurrenceRule?.takeIf { it.isNotBlank() }?.let { add(it.recurrenceLabel()) }
    }
    return parts.joinToString(" / ")
}

fun CalendarEventEntity.safeColor(): Color {
    return runCatching {
        Color(android.graphics.Color.parseColor(color ?: "#4A90E2"))
    }.getOrDefault(TimelineFallbackColor)
}

fun TodoEntity.timelineTime(): String {
    val raw = dueAt ?: startAt
    return raw?.takeIf { it.length >= 16 }?.substring(11, 16) ?: "--:--"
}

fun TodoEntity.todoSubtitle(): String {
    val parts = buildList {
        startAt?.takeIf { it.isNotBlank() }?.let { add("开始 $it") }
        dueAt?.takeIf { it.isNotBlank() }?.let { add("截止 $it") }
        reminderMinutes?.let { add("提前 $it 分钟提醒") }
        recurrence.takeIf { it != "none" }?.let { add(it.recurrenceLabel()) }
        if (status == "completed") add("已完成")
    }
    return parts.joinToString(" / ")
}

fun String.sortableTime(): String {
    return if (Regex("\\d{2}:\\d{2}").matches(this)) this else "99:99"
}

fun String.recurrenceLabel(): String {
    return RecurrenceUtil.label(this)
}

val TimelineFallbackColor = Color(0xFF4A90E2)

fun calendarWeekdayLabels(weekStartDay: Int): List<String> {
    return if (weekStartDay == Calendar.MONDAY) {
        listOf("一", "二", "三", "四", "五", "六", "日")
    } else {
        listOf("日", "一", "二", "三", "四", "五", "六")
    }
}

fun String.weekDates(weekStartDay: Int = Calendar.SUNDAY): List<String> {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val baseDate = runCatching { formatter.parse(this) }.getOrNull() ?: return listOf(this)
    val normalizedWeekStart = if (weekStartDay == Calendar.MONDAY) Calendar.MONDAY else Calendar.SUNDAY
    val calendar = Calendar.getInstance().apply {
        time = baseDate
        firstDayOfWeek = normalizedWeekStart
    }
    val dayOffset = (calendar.get(Calendar.DAY_OF_WEEK) - calendar.firstDayOfWeek + 7) % 7
    calendar.add(Calendar.DAY_OF_MONTH, -dayOffset)
    return List(7) {
        formatter.format(calendar.time).also {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
}

fun String.weekRangeLabel(weekStartDay: Int = Calendar.SUNDAY): String {
    val dates = weekDates(weekStartDay)
    val start = dates.firstOrNull().orEmpty()
    val end = dates.lastOrNull().orEmpty()
    return if (start.isBlank() || end.isBlank()) this else "$start | $end"
}

fun CalendarMonth.quarterLabel(): String {
    val firstQuarterMonth = ((month - 1) / 3) * 3 + 1
    val lastQuarterMonth = firstQuarterMonth + 2
    return "${year}年${firstQuarterMonth}月-${lastQuarterMonth}月"
}

fun String.isWeekendDate(): Boolean {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val date = runCatching { formatter.parse(this) }.getOrNull() ?: return false
    val calendar = Calendar.getInstance().apply { time = date }
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
}

fun CalendarMonth.quarterMonths(): List<CalendarMonth> {
    val firstQuarterMonth = ((month - 1) / 3) * 3 + 1
    return List(3) { offset -> CalendarMonth(year, firstQuarterMonth + offset) }
}

fun DailyAggregation?.weekSummary(showEvents: Boolean): String {
    if (this == null) return "无安排"
    val parts = buildList {
        if (showEvents && eventColors.isNotEmpty()) add("${eventColors.size} 个事件")
        if (pendingTodosCount > 0) add("$pendingTodosCount 个待办")
        if (completedTodosCount > 0) add("$completedTodosCount 个已完成")
        if (hasDiary) add("有日记")
        if (deletedCount > 0) add("$deletedCount 条已删")
    }
    return parts.ifEmpty { listOf("无安排") }.joinToString(" | ")
}
