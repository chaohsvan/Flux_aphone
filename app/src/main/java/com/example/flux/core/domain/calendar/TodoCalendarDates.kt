package com.example.flux.core.domain.calendar

import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.util.TimeUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val calendarDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    isLenient = false
}

fun TodoEntity.calendarOccurrenceDates(rangeStart: String, rangeEnd: String): List<String> {
    val startDate = startAt?.datePart()
    val dueDate = dueAt?.datePart() ?: return emptyList()
    return dateRange(
        start = startDate ?: rangeStart,
        end = dueDate,
        rangeStart = rangeStart,
        rangeEnd = rangeEnd
    )
}

fun TodoEntity.occursOnCalendarDate(date: String): Boolean {
    return calendarOccurrenceDates(date, date).contains(date)
}

private fun String.datePart(): String? {
    return TimeUtil.localDatePart(this)
        ?.takeIf { TimeUtil.isValidDate(it) }
}

private fun dateRange(start: String, end: String, rangeStart: String, rangeEnd: String): List<String> {
    if (start > end) return emptyList()
    val actualStart = start.coerceAtLeast(rangeStart)
    val actualEnd = end.coerceAtMost(rangeEnd)
    if (actualStart > actualEnd) return emptyList()

    val calendar = Calendar.getInstance().apply {
        time = calendarDateFormat.parse(actualStart) ?: return emptyList()
    }
    val endDate = calendarDateFormat.parse(actualEnd) ?: return emptyList()
    return buildList {
        while (!calendar.time.after(endDate)) {
            add(calendarDateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
}
