package com.snowball.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CutoffTest {
    @Test
    fun cutoff_15_covers_15_to_29_of_same_month() {
        val c = Cutoff(year = 2026, month = 5, payday = Payday.FIFTEENTH)
        assertEquals(LocalDate(2026, 5, 15), c.windowStart)
        assertEquals(LocalDate(2026, 5, 29), c.windowEnd)
    }

    @Test
    fun cutoff_30_covers_30_to_14_of_next_month() {
        val c = Cutoff(year = 2026, month = 5, payday = Payday.THIRTIETH)
        assertEquals(LocalDate(2026, 5, 30), c.windowStart)
        assertEquals(LocalDate(2026, 6, 14), c.windowEnd)
    }

    @Test
    fun cutoff_30_in_december_rolls_to_january() {
        val c = Cutoff(year = 2026, month = 12, payday = Payday.THIRTIETH)
        assertEquals(LocalDate(2026, 12, 30), c.windowStart)
        assertEquals(LocalDate(2027, 1, 14), c.windowEnd)
    }

    @Test
    fun cutoff_15_in_february_ends_day_before_the_last_day_payday() {
        // Feb 2026 has 28 days; the "30th" payday lands on the 28th.
        // So the 15th cutoff runs 15 -> 27.
        val c = Cutoff(year = 2026, month = 2, payday = Payday.FIFTEENTH)
        assertEquals(LocalDate(2026, 2, 15), c.windowStart)
        assertEquals(LocalDate(2026, 2, 27), c.windowEnd)
    }

    @Test
    fun cutoff_30_in_february_starts_on_the_last_day() {
        // Feb 2026 (28 days): 30th payday lands on the 28th; window 28 -> Mar 14.
        val c = Cutoff(year = 2026, month = 2, payday = Payday.THIRTIETH)
        assertEquals(LocalDate(2026, 2, 28), c.windowStart)
        assertEquals(LocalDate(2026, 3, 14), c.windowEnd)
    }

    @Test
    fun currentCutoff_when_day_in_1_to_14_returns_previous_month_30() {
        val c = currentCutoff(today = LocalDate(2026, 5, 7))
        assertEquals(2026, c.year)
        assertEquals(4, c.month)
        assertEquals(Payday.THIRTIETH, c.payday)
    }

    @Test
    fun currentCutoff_when_day_in_15_to_29_returns_same_month_15() {
        val c = currentCutoff(today = LocalDate(2026, 5, 20))
        assertEquals(2026, c.year)
        assertEquals(5, c.month)
        assertEquals(Payday.FIFTEENTH, c.payday)
    }

    @Test
    fun currentCutoff_when_day_is_30_returns_same_month_30() {
        // Day 30 belongs to the 30th cutoff, NOT the 15th. This is the core fix.
        val c = currentCutoff(today = LocalDate(2026, 5, 30))
        assertEquals(2026, c.year)
        assertEquals(5, c.month)
        assertEquals(Payday.THIRTIETH, c.payday)
    }

    @Test
    fun currentCutoff_when_day_is_31_returns_same_month_30() {
        val c = currentCutoff(today = LocalDate(2026, 5, 31))
        assertEquals(2026, c.year)
        assertEquals(5, c.month)
        assertEquals(Payday.THIRTIETH, c.payday)
    }

    @Test
    fun currentCutoff_february_28_returns_same_month_30() {
        // Feb 28 is the 30th-payday day in a 28-day month.
        val c = currentCutoff(today = LocalDate(2026, 2, 28))
        assertEquals(2026, c.year)
        assertEquals(2, c.month)
        assertEquals(Payday.THIRTIETH, c.payday)
    }

    @Test
    fun currentCutoff_february_27_returns_same_month_15() {
        val c = currentCutoff(today = LocalDate(2026, 2, 27))
        assertEquals(2026, c.year)
        assertEquals(2, c.month)
        assertEquals(Payday.FIFTEENTH, c.payday)
    }

    @Test
    fun currentCutoff_on_january_5_returns_december_30() {
        val c = currentCutoff(today = LocalDate(2026, 1, 5))
        assertEquals(2025, c.year)
        assertEquals(12, c.month)
        assertEquals(Payday.THIRTIETH, c.payday)
    }

    @Test
    fun next_cutoff_after_may_15_is_may_30() {
        val current = Cutoff(2026, 5, Payday.FIFTEENTH)
        val next = current.next()
        assertEquals(2026, next.year)
        assertEquals(5, next.month)
        assertEquals(Payday.THIRTIETH, next.payday)
    }

    @Test
    fun next_cutoff_after_may_30_is_june_15() {
        val current = Cutoff(2026, 5, Payday.THIRTIETH)
        val next = current.next()
        assertEquals(2026, next.year)
        assertEquals(6, next.month)
        assertEquals(Payday.FIFTEENTH, next.payday)
    }
}
