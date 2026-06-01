package com.example.flux.core.reminder

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object ReminderTimeParser {
    fun diaryTriggerSource(entryDate: String, entryTime: String?): String? {
        val date = entryDate.trim()
        if (date.isBlank()) return null
        val time = entryTime?.trim().orEmpty()
        return if (time.isBlank()) date else "$date $time"
    }

    fun toEpochMillisOrNull(value: String): Long? {
        val trimmed = value.trim()
        val spec = when {
            Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z").matches(trimmed) ->
                PatternSpec("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"))
            Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}").matches(trimmed) ->
                PatternSpec("yyyy-MM-dd'T'HH:mm:ss")
            Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}").matches(trimmed) ->
                PatternSpec("yyyy-MM-dd'T'HH:mm")
            Regex("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}").matches(trimmed) ->
                PatternSpec("yyyy-MM-dd HH:mm")
            Regex("\\d{4}-\\d{2}-\\d{2}").matches(trimmed) ->
                PatternSpec("yyyy-MM-dd", isDateOnly = true)
            else -> return null
        }

        return runCatching {
            val date = SimpleDateFormat(spec.pattern, Locale.US).apply {
                isLenient = false
                timeZone = spec.timeZone ?: TimeZone.getDefault()
            }.parse(trimmed) ?: return null
            if (!spec.isDateOnly) return date.time

            Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.getOrNull()
    }

    private data class PatternSpec(
        val pattern: String,
        val timeZone: TimeZone? = null,
        val isDateOnly: Boolean = false
    )
}
