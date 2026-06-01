package com.example.flux.core.reminder

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReminderTimeParserTest {
    @Test
    fun diaryTriggerSource_usesDateWhenTimeMissing() {
        assertEquals("2026-04-25", ReminderTimeParser.diaryTriggerSource("2026-04-25", null))
    }

    @Test
    fun diaryTriggerSource_joinsDateAndTime() {
        assertEquals("2026-04-25 09:30", ReminderTimeParser.diaryTriggerSource("2026-04-25", "09:30"))
    }

    @Test
    fun toEpochMillisOrNull_supportsDateOnly() {
        assertNotNull(ReminderTimeParser.toEpochMillisOrNull("2026-04-25"))
    }

    @Test
    fun toEpochMillisOrNull_supportsUtcIsoWithSeconds() {
        val expected = utcMillis("2026-04-25T01:30:45Z")
        assertEquals(expected, ReminderTimeParser.toEpochMillisOrNull("2026-04-25T01:30:45Z"))
    }

    @Test
    fun toEpochMillisOrNull_supportsLocalIsoWithoutSeconds() {
        assertNotNull(ReminderTimeParser.toEpochMillisOrNull("2026-04-25T09:30"))
    }

    @Test
    fun toEpochMillisOrNull_supportsLocalDateTimeWithSpace() {
        assertNotNull(ReminderTimeParser.toEpochMillisOrNull("2026-04-25 09:30"))
    }

    @Test
    fun toEpochMillisOrNull_rejectsBlank() {
        assertNull(ReminderTimeParser.toEpochMillisOrNull("   "))
    }

    @Test
    fun toEpochMillisOrNull_rejectsInvalidDate() {
        assertNull(ReminderTimeParser.toEpochMillisOrNull("2026-02-30"))
    }

    private fun utcMillis(value: String): Long {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(value)!!.time
    }
}
