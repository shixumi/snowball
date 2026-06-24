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

    @Test
    fun ordinalDayBasicSuffixes() {
        assertEquals("1st", ordinalDay(LocalDate(2026, 5, 1)))
        assertEquals("2nd", ordinalDay(LocalDate(2026, 5, 2)))
        assertEquals("3rd", ordinalDay(LocalDate(2026, 5, 3)))
        assertEquals("4th", ordinalDay(LocalDate(2026, 5, 4)))
        assertEquals("21st", ordinalDay(LocalDate(2026, 5, 21)))
        assertEquals("22nd", ordinalDay(LocalDate(2026, 5, 22)))
        assertEquals("23rd", ordinalDay(LocalDate(2026, 5, 23)))
        assertEquals("30th", ordinalDay(LocalDate(2026, 5, 30)))
    }

    @Test
    fun ordinalDayTeensAreAlwaysTh() {
        assertEquals("11th", ordinalDay(LocalDate(2026, 5, 11)))
        assertEquals("12th", ordinalDay(LocalDate(2026, 5, 12)))
        assertEquals("13th", ordinalDay(LocalDate(2026, 5, 13)))
    }
}
