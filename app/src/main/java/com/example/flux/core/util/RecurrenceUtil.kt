package com.example.flux.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object RecurrenceUtil {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    private val isoDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { isLenient = false }
    private val spaceDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }

    data class Spec(
        val rule: String,
        val interval: Int = 1,
        val until: String? = null
    )

    fun parseSpec(value: String?): Spec? {
        val raw = value?.trim().orEmpty()
        if (raw.isBlank() || raw == "none") return null
        val parts = raw.split(";").map { it.trim() }.filter { it.isNotBlank() }
        val rule = parts.firstOrNull()?.takeIf { it in SUPPORTED_RULES } ?: return null
        val params = parts.drop(1).mapNotNull { part ->
            val key = part.substringBefore("=", missingDelimiterValue = "").trim()
            val paramValue = part.substringAfter("=", missingDelimiterValue = "").trim()
            if (key.isBlank() || paramValue.isBlank()) null else key to paramValue
        }.toMap()
        return Spec(
            rule = rule,
            interval = params["interval"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
            until = params["until"]?.takeIf { TimeUtil.isValidDate(it) }
        )
    }

    fun buildSpec(rule: String, interval: Int = 1, until: String? = null): String? {
        if (rule == "none") return null
        if (rule !in SUPPORTED_RULES) return null
        return buildList {
            add(rule)
            if (interval > 1) add("interval=$interval")
            until?.takeIf { TimeUtil.isValidDate(it) }?.let { add("until=$it") }
        }.joinToString(";")
    }

    fun label(value: String?): String {
        val spec = parseSpec(value) ?: return "不重复"
        val base = when (spec.rule) {
            "daily" -> "每天"
            "weekly" -> "每周"
            "monthly" -> "每月"
            "yearly" -> "每年"
            else -> spec.rule
        }
        val interval = if (spec.interval > 1) {
            when (spec.rule) {
                "daily" -> "每 ${spec.interval} 天"
                "weekly" -> "每 ${spec.interval} 周"
                "monthly" -> "每 ${spec.interval} 月"
                "yearly" -> "每 ${spec.interval} 年"
                else -> "每 ${spec.interval} 次"
            }
        } else {
            base
        }
        val until = spec.until?.let { "，截至 $it" }.orEmpty()
        return "$interval$until"
    }

    fun nextValue(value: String?, recurrence: String, interval: Int): String? {
        val spec = parseSpec(recurrence) ?: return null
        if (value.isNullOrBlank()) return null
        val parsed = parse(value) ?: return null
        val calendar = Calendar.getInstance().apply { time = parsed.date }
        val step = interval.coerceAtLeast(spec.interval).coerceAtLeast(1)
        when (spec.rule) {
            "daily" -> calendar.add(Calendar.DAY_OF_MONTH, step)
            "weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, step)
            "monthly" -> calendar.add(Calendar.MONTH, step)
            "yearly" -> calendar.add(Calendar.YEAR, step)
            else -> return null
        }
        return parsed.format.format(calendar.time)
    }

    fun nextDate(value: String?, recurrence: String, interval: Int): String? {
        return nextValue(value, recurrence, interval)?.take(10)
    }

    fun occurrenceDates(
        value: String?,
        recurrence: String?,
        interval: Int = 1,
        until: String? = null,
        rangeStart: String,
        rangeEnd: String,
        maxOccurrences: Int = 800
    ): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val startDate = value.take(10)
        if (!TimeUtil.isValidDate(startDate)) return emptyList()
        val spec = parseSpec(recurrence)
        if (spec == null) {
            return if (startDate in rangeStart..rangeEnd) listOf(startDate) else emptyList()
        }

        val result = mutableListOf<String>()
        var cursorValue: String? = value
        var count = 0
        val effectiveUntil = spec.until ?: until?.takeIf { TimeUtil.isValidDate(it) }
        val finalDate = listOfNotNull(effectiveUntil, rangeEnd).minOrNull() ?: rangeEnd
        val effectiveInterval = spec.interval.coerceAtLeast(interval).coerceAtLeast(1)
        while (!cursorValue.isNullOrBlank() && count < maxOccurrences) {
            val date = cursorValue.take(10)
            if (date > finalDate) break
            if (date in rangeStart..rangeEnd) result.add(date)
            cursorValue = nextValue(cursorValue, spec.rule, effectiveInterval)
            count += 1
        }
        return result.distinct()
    }

    private fun parse(value: String): ParsedDate? {
        return runCatching {
            when {
            Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}").matches(value) ->
                ParsedDate(isoDateTimeFormat.parse(value) ?: return null, isoDateTimeFormat)
            Regex("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}").matches(value) ->
                ParsedDate(spaceDateTimeFormat.parse(value) ?: return null, spaceDateTimeFormat)
            Regex("\\d{4}-\\d{2}-\\d{2}").matches(value) ->
                ParsedDate(dateFormat.parse(value) ?: return null, dateFormat)
            else -> null
            }
        }.getOrNull()
    }

    private data class ParsedDate(
        val date: java.util.Date,
        val format: SimpleDateFormat
    )

    private val SUPPORTED_RULES = setOf("daily", "weekly", "monthly", "yearly")
}
