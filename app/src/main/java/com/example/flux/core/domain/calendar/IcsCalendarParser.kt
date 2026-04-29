package com.example.flux.core.domain.calendar

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class ParsedIcsEvent(
    val uid: String,
    val title: String,
    val description: String,
    val locationName: String?,
    val startAt: String,
    val endAt: String,
    val allDay: Boolean,
    val hash: String
)

class IcsCalendarParser @Inject constructor() {
    fun parse(content: String): List<ParsedIcsEvent> {
        val lines = unfold(content)
        val events = mutableListOf<List<IcsProperty>>()
        var current = mutableListOf<IcsProperty>()
        var inEvent = false

        lines.forEach { line ->
            when (line.trim()) {
                "BEGIN:VEVENT" -> {
                    current = mutableListOf()
                    inEvent = true
                }
                "END:VEVENT" -> {
                    if (inEvent) events.add(current.toList())
                    inEvent = false
                }
                else -> {
                    if (inEvent) parseProperty(line)?.let(current::add)
                }
            }
        }

        return events.mapNotNull { properties ->
            val uid = properties.firstValue("UID")?.trim().orEmpty()
            val start = properties.firstProperty("DTSTART")?.toDateValue() ?: return@mapNotNull null
            if (uid.isBlank()) return@mapNotNull null
            val end = properties.firstProperty("DTEND")?.toDateValue()
            val title = properties.firstValue("SUMMARY")?.icsUnescape()?.ifBlank { "(No title)" } ?: "(No title)"
            val description = properties.firstValue("DESCRIPTION")?.icsUnescape().orEmpty()
            val location = properties.firstValue("LOCATION")?.icsUnescape()?.takeIf { it.isNotBlank() }
            val endAt = end?.value ?: start.value
            ParsedIcsEvent(
                uid = uid,
                title = title,
                description = description,
                locationName = location,
                startAt = start.value,
                endAt = endAt,
                allDay = start.allDay,
                hash = listOf(uid, title, description, location.orEmpty(), start.value, endAt, start.allDay.toString())
                    .joinToString("\u001F")
                    .sha256()
            )
        }
    }

    private fun unfold(content: String): List<String> {
        val result = mutableListOf<String>()
        content.replace("\r\n", "\n").replace('\r', '\n').split('\n').forEach { rawLine ->
            if ((rawLine.startsWith(" ") || rawLine.startsWith("\t")) && result.isNotEmpty()) {
                result[result.lastIndex] = result.last() + rawLine.drop(1)
            } else {
                result.add(rawLine)
            }
        }
        return result
    }

    private fun parseProperty(line: String): IcsProperty? {
        val separator = line.indexOf(':')
        if (separator <= 0) return null
        val nameAndParams = line.substring(0, separator).split(';')
        val name = nameAndParams.first().uppercase(Locale.US)
        val params = nameAndParams.drop(1).mapNotNull { part ->
            val key = part.substringBefore("=", missingDelimiterValue = "").uppercase(Locale.US)
            val value = part.substringAfter("=", missingDelimiterValue = "")
            if (key.isBlank() || value.isBlank()) null else key to value
        }.toMap()
        return IcsProperty(name, params, line.substring(separator + 1))
    }

    private fun List<IcsProperty>.firstProperty(name: String): IcsProperty? {
        return firstOrNull { it.name == name }
    }

    private fun List<IcsProperty>.firstValue(name: String): String? {
        return firstProperty(name)?.value
    }

    private fun IcsProperty.toDateValue(): IcsDateValue? {
        val isDate = params["VALUE"] == "DATE" || Regex("\\d{8}").matches(value)
        return if (isDate) {
            IcsDateValue("${value.substring(0, 4)}-${value.substring(4, 6)}-${value.substring(6, 8)}", true)
        } else {
            parseDateTime(value)?.let { IcsDateValue(it, false) }
        }
    }

    private fun parseDateTime(value: String): String? {
        val localOutput = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        return when {
            Regex("\\d{8}T\\d{6}Z").matches(value) -> {
                val parser = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = false
                }
                runCatching { localOutput.format(parser.parse(value) ?: return null) }.getOrNull()
            }
            Regex("\\d{8}T\\d{6}").matches(value) -> {
                val parser = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US).apply { isLenient = false }
                runCatching { localOutput.format(parser.parse(value) ?: return null) }.getOrNull()
            }
            else -> null
        }
    }

    private fun String.icsUnescape(): String {
        return replace("\\n", "\n")
            .replace("\\N", "\n")
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
    }

    private fun String.sha256(): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private data class IcsProperty(
        val name: String,
        val params: Map<String, String>,
        val value: String
    )

    private data class IcsDateValue(
        val value: String,
        val allDay: Boolean
    )
}
