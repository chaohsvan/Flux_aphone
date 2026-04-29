package com.example.flux.core.domain.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IcsCalendarParserTest {
    private val parser = IcsCalendarParser()

    @Test
    fun parse_readsBasicTimedEvent() {
        val events = parser.parse(
            """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:event-1@example.com
            SUMMARY:Team meeting
            DTSTART:20260425T090000
            DTEND:20260425T100000
            DESCRIPTION:Weekly sync
            LOCATION:Room 1
            END:VEVENT
            END:VCALENDAR
            """.trimIndent()
        )

        assertEquals(1, events.size)
        assertEquals("event-1@example.com", events.first().uid)
        assertEquals("Team meeting", events.first().title)
        assertEquals("2026-04-25 09:00", events.first().startAt)
        assertEquals("2026-04-25 10:00", events.first().endAt)
        assertEquals("Room 1", events.first().locationName)
    }

    @Test
    fun parse_readsAllDayEvent() {
        val events = parser.parse(
            """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:event-2@example.com
            SUMMARY:Holiday
            DTSTART;VALUE=DATE:20260425
            DTEND;VALUE=DATE:20260426
            END:VEVENT
            END:VCALENDAR
            """.trimIndent()
        )

        assertEquals("2026-04-25", events.first().startAt)
        assertTrue(events.first().allDay)
    }
}
