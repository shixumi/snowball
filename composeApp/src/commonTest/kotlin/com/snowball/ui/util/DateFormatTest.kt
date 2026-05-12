package com.snowball.ui.util

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormatTest {

    @Test
    fun januaryFormatsCorrectly() {
        assertEquals("Jan 2026", formatMonthYear(LocalDate(2026, 1, 1)))
    }

    @Test
    fun augustFormatsCorrectly() {
        assertEquals("Aug 2027", formatMonthYear(LocalDate(2027, 8, 15)))
    }

    @Test
    fun decemberFormatsCorrectly() {
        assertEquals("Dec 2025", formatMonthYear(LocalDate(2025, 12, 31)))
    }

    @Test
    fun dayOfMonthIgnored() {
        // Day shouldn't affect the output.
        assertEquals(
            formatMonthYear(LocalDate(2026, 5, 1)),
            formatMonthYear(LocalDate(2026, 5, 31)),
        )
    }
}
