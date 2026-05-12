package com.snowball.ui.util

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormatLongTest {

    @Test
    fun formatsSingleDigitDay() {
        assertEquals("Jan 1, 2026", formatLongDate(LocalDate(2026, 1, 1)))
    }

    @Test
    fun formatsDoubleDigitDay() {
        assertEquals("Dec 25, 2026", formatLongDate(LocalDate(2026, 12, 25)))
    }

    @Test
    fun formatsEndOfMonth() {
        assertEquals("Feb 28, 2026", formatLongDate(LocalDate(2026, 2, 28)))
    }
}
