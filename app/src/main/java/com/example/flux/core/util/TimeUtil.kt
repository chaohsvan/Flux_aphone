package com.example.flux.core.util

import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object TimeUtil {
    fun getCurrentIsoTime(): String {
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        df.timeZone = TimeZone.getDefault()
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

    fun isValidClockTime(value: String): Boolean {
        return Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(value)
    }

    fun normalizeDateInput(value: String): String {
        val digits = value.filter(Char::isDigit).take(8)
        return buildString {
            append(digits.take(4))
            if (digits.length > 4) append("-").append(digits.drop(4).take(2))
            if (digits.length > 6) append("-").append(digits.drop(6).take(2))
        }
    }

    fun normalizeClockInput(value: String): String {
        val digits = value.filter(Char::isDigit).take(4)
        return buildString {
            append(digits.take(2))
            if (digits.length > 2) append(":").append(digits.drop(2).take(2))
        }
    }

    fun normalizeDateTimeInput(value: String): String {
        val digits = value.filter(Char::isDigit).take(12)
        val date = normalizeDateInput(digits.take(8))
        val timeDigits = digits.drop(8)
        if (timeDigits.isEmpty()) return date
        return "$date ${normalizeClockInput(timeDigits)}"
    }

    fun formatTimestampForDisplay(value: String?): String {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return ""
        parseTimestamp(raw)?.let { date ->
            return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }.format(date)
        }
        return raw.replace('T', ' ').removeSuffix("Z").take(16)
    }

    fun localDatePart(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank()) return null
        parseTimestamp(raw)?.let { date ->
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getDefault()
            }.format(date)
        }
        return raw.takeIf { it.length >= 10 }?.take(10)
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

    private fun parseTimestamp(value: String): Date? {
        val pattern = when {
            Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z").matches(value) -> "yyyy-MM-dd'T'HH:mm:ss'Z'" to TimeZone.getTimeZone("UTC")
            Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}").matches(value) -> "yyyy-MM-dd'T'HH:mm:ss" to TimeZone.getDefault()
            Regex("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}").matches(value) -> "yyyy-MM-dd HH:mm" to TimeZone.getDefault()
            else -> return null
        }
        return runCatching {
            SimpleDateFormat(pattern.first, Locale.US).apply {
                isLenient = false
                timeZone = pattern.second
            }.parse(value)
        }.getOrNull()
    }
}
