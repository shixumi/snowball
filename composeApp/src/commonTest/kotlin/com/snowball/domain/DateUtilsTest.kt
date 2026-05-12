package com.snowball.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DateUtilsTest {
    @Test
    fun lastDayOfMonth_returns_31_for_january() {
        assertEquals(31, lastDayOfMonth(2026, 1))
    }

    @Test
    fun lastDayOfMonth_returns_28_for_february_2026() {
        assertEquals(28, lastDayOfMonth(2026, 2))
    }

    @Test
    fun lastDayOfMonth_returns_29_for_february_2028() {
        assertEquals(29, lastDayOfMonth(2028, 2))
    }

    @Test
    fun lastDayOfMonth_returns_30_for_april() {
        assertEquals(30, lastDayOfMonth(2026, 4))
    }

    @Test
    fun effectiveDueDate_clamps_to_last_day() {
        val d = effectiveDueDate(year = 2026, month = 2, dueDay = 30, useLastDay = true)
        assertEquals(LocalDate(2026, 2, 28), d)
    }

    @Test
    fun effectiveDueDate_uses_literal_day_when_present() {
        val d = effectiveDueDate(year = 2026, month = 5, dueDay = 17, useLastDay = false)
        assertEquals(LocalDate(2026, 5, 17), d)
    }

    @Test
    fun effectiveDueDate_returns_null_when_day_invalid_and_not_floating() {
        val d = effectiveDueDate(year = 2026, month = 2, dueDay = 31, useLastDay = false)
        assertEquals(null, d)
    }
}
