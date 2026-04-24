package com.example.flux.core.util

import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object TimeUtil {
    fun getCurrentIsoTime(): String {
        val tz = TimeZone.getTimeZone("UTC")
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        df.timeZone = tz
        return df.format(Date())
    }

    fun getCurrentDate(): String {
        val tz = TimeZone.getDefault()
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        df.timeZone = tz
        return df.format(Date())
    }

    fun generateUuid(): String {
        return UUID.randomUUID().toString()
    }

    fun isValidDate(value: String): Boolean {
        if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(value)) return false
        return isValidCalendarDate(value.substring(0, 4).toInt(), value.substring(5, 7).toInt(), value.substring(8, 10).toInt())
    }

    fun isValidDateOrDateTime(value: String): Boolean {
        if (value.isBlank()) return true
        val match = Regex("(\\d{4})-(\\d{2})-(\\d{2})(?:\\s+(\\d{2}):(\\d{2}))?").matchEntire(value) ?: return false
        val year = match.groupValues[1].toInt()
        val month = match.groupValues[2].toInt()
        val day = match.groupValues[3].toInt()
        val hour = match.groupValues.getOrNull(4)?.takeIf { it.isNotBlank() }?.toInt()
        val minute = match.groupValues.getOrNull(5)?.takeIf { it.isNotBlank() }?.toInt()
        return isValidCalendarDate(year, month, day) &&
            (hour == null || hour in 0..23) &&
            (minute == null || minute in 0..59)
    }

    private fun isValidCalendarDate(year: Int, month: Int, day: Int): Boolean {
        return runCatching {
            Calendar.getInstance().apply {
                isLenient = false
                clear()
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
                time
            }
        }.isSuccess
    }
}
