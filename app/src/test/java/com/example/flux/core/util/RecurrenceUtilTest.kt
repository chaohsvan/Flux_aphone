package com.example.flux.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecurrenceUtilTest {

    @Test
    fun parseSpec_supportsLegacyRule() {
        val spec = RecurrenceUtil.parseSpec("weekly")

        requireNotNull(spec)
        assertEquals("weekly", spec.rule)
        assertEquals(1, spec.interval)
        assertNull(spec.until)
    }

    @Test
    fun buildSpec_supportsIntervalAndUntil() {
        val spec = RecurrenceUtil.buildSpec(
            rule = "weekly",
            interval = 2,
            until = "2026-12-31"
        )

        assertEquals("weekly;interval=2;until=2026-12-31", spec)
    }

    @Test
    fun nextValue_keepsDateTimeFormat() {
        val next = RecurrenceUtil.nextValue(
            value = "2026-04-24T09:30:00",
            recurrence = "monthly;interval=2",
            interval = 1
        )

        assertEquals("2026-06-24T09:30:00", next)
    }

    @Test
    fun occurrenceDates_respectsIntervalAndUntil() {
        val dates = RecurrenceUtil.occurrenceDates(
            value = "2026-04-24",
            recurrence = "daily;interval=2;until=2026-04-30",
            rangeStart = "2026-04-24",
            rangeEnd = "2026-05-02"
        )

        assertEquals(
            listOf("2026-04-24", "2026-04-26", "2026-04-28", "2026-04-30"),
            dates
        )
    }

    @Test
    fun trimRuleBeforeDate_keepsOnlyEarlierOccurrences() {
        val rule = RecurrenceUtil.trimRuleBeforeDate(
            value = "2026-04-24T09:30:00",
            recurrence = "daily;interval=2;until=2026-05-10",
            date = "2026-04-30"
        )

        assertEquals("daily;interval=2;until=2026-04-28", rule)
    }

    @Test
    fun trimRuleBeforeDate_returnsNullWhenDeletingFirstOccurrence() {
        val rule = RecurrenceUtil.trimRuleBeforeDate(
            value = "2026-04-24T09:30:00",
            recurrence = "weekly",
            date = "2026-04-24"
        )

        assertNull(rule)
    }

    @Test
    fun label_formatsFriendlyText() {
        val label = RecurrenceUtil.label("weekly;interval=2;until=2026-12-31")

        assertEquals("每 2 周，截至 2026-12-31", label)
    }
}
