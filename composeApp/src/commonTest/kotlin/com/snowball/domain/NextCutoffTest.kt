package com.snowball.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class NextCutoffTest {

    @Test
    fun todayInFirstHalfReturnsSecondHalfOfSameMonth() {
        // May 5 is in current cutoff = April 30 payday (covers Apr 30 - May 14).
        // Next is May 15 payday (covers May 15-29).
        val next = nextCutoff(LocalDate(2026, 5, 5))
        assertEquals(2026, next.year)
        assertEquals(5, next.month)
        assertEquals(Payday.FIFTEENTH, next.payday)
        assertEquals(LocalDate(2026, 5, 15), next.windowStart)
        assertEquals(LocalDate(2026, 5, 29), next.windowEnd)
    }

    @Test
    fun todayInSecondHalfReturnsThirtiethOfSameMonth() {
        // May 20 is in current cutoff = May 15 payday (covers May 15-29).
        // Next is May 30 payday (covers May 30 - June 14).
        val next = nextCutoff(LocalDate(2026, 5, 20))
        assertEquals(2026, next.year)
        assertEquals(5, next.month)
        assertEquals(Payday.THIRTIETH, next.payday)
        assertEquals(LocalDate(2026, 5, 30), next.windowStart)
        assertEquals(LocalDate(2026, 6, 14), next.windowEnd)
    }

    @Test
    fun todayOnDay14ReturnsFifteenthSameMonth() {
        // May 14 still belongs to current cutoff (April 30 payday, Apr 30 - May 14).
        // Next is May 15 payday (May 15-29).
        val next = nextCutoff(LocalDate(2026, 5, 14))
        assertEquals(Payday.FIFTEENTH, next.payday)
        assertEquals(LocalDate(2026, 5, 15), next.windowStart)
    }

    @Test
    fun todayOnDay15ReturnsThirtiethSameMonth() {
        // May 15 is in current cutoff = May 15 payday.
        // Next is May 30 payday (May 30 - June 14).
        val next = nextCutoff(LocalDate(2026, 5, 15))
        assertEquals(Payday.THIRTIETH, next.payday)
        assertEquals(LocalDate(2026, 5, 30), next.windowStart)
        assertEquals(LocalDate(2026, 6, 14), next.windowEnd)
    }

    @Test
    fun todayOnDay31ReturnsFifteenthNextMonth() {
        // May 31 belongs to current cutoff = May 30 payday (May 30 - June 14).
        // Next is June 15 payday (June 15-29).
        val next = nextCutoff(LocalDate(2026, 5, 31))
        assertEquals(Payday.FIFTEENTH, next.payday)
        assertEquals(2026, next.year)
        assertEquals(6, next.month)
        assertEquals(LocalDate(2026, 6, 15), next.windowStart)
        assertEquals(LocalDate(2026, 6, 29), next.windowEnd)
    }

    @Test
    fun yearBoundary() {
        // Dec 25 is in current cutoff = Dec 15 payday (Dec 15-29).
        // Next is Dec 30 payday (Dec 30 - Jan 14 of next year).
        val next = nextCutoff(LocalDate(2026, 12, 25))
        assertEquals(Payday.THIRTIETH, next.payday)
        assertEquals(2026, next.year)
        assertEquals(12, next.month)
        assertEquals(LocalDate(2026, 12, 30), next.windowStart)
        assertEquals(LocalDate(2027, 1, 14), next.windowEnd)
    }
}
