package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JourneyCalculatorTest {

    private fun debt(
        id: Long = 1L,
        monthlyAmount: Double = 1500.0,
        totalPayments: Int = 12,
        startDate: LocalDate = LocalDate(2026, 1, 1),
        dueDay: Int = 10,
        useLastDayOfMonth: Boolean = false,
        isArchived: Boolean = false,
    ) = Debt(
        id = id,
        name = "Debt $id",
        categoryId = 1L,
        monthlyAmount = monthlyAmount,
        totalPayments = totalPayments,
        dueDay = dueDay,
        useLastDayOfMonth = useLastDayOfMonth,
        startDate = startDate,
        isArchived = isArchived,
        notes = null,
    )

    private fun payment(id: Long, debtId: Long, amount: Double, date: LocalDate = LocalDate(2026, 1, 10)) =
        Payment(id = id, debtId = debtId, paidDate = date, amount = amount)

    @Test
    fun noDebtsNoPaymentsReturnsNull() {
        assertNull(JourneyCalculator.compute(emptyList(), emptyList()))
    }

    @Test
    fun debtsButNoPaymentsReturnsNull() {
        assertNull(JourneyCalculator.compute(listOf(debt()), emptyList()))
    }

    @Test
    fun singlePaymentComputesPercentMeltedAndForecast() {
        // Debt: ₱1500/mo × 12 = ₱18,000 scheduled, starts 2026-01-01, dueDay 10.
        // One ₱1500 payment recorded.
        // Expected: 8% cleared (1500/18000 = 0.0833 → 8), ₱1500 melted,
        // forecast = 2026-12-10 (start + 11 months at dueDay).
        val stats = JourneyCalculator.compute(
            listOf(debt()),
            listOf(payment(id = 1L, debtId = 1L, amount = 1500.0)),
        )
        assertEquals(8, stats?.percentCleared)
        assertEquals(1500.0, stats?.totalMelted)
        assertEquals(LocalDate(2026, 12, 10), stats?.forecastEndDate)
    }

    @Test
    fun allDebtsArchivedForecastIsNull() {
        // Single fully-paid debt: 12 payments × ₱1500 = ₱18,000 melted, scheduled = ₱18,000.
        val payments = (1L..12L).map { payment(id = it, debtId = 1L, amount = 1500.0) }
        val stats = JourneyCalculator.compute(
            listOf(debt(isArchived = true)),
            payments,
        )
        assertEquals(100, stats?.percentCleared)
        assertEquals(18_000.0, stats?.totalMelted)
        assertNull(stats?.forecastEndDate)
    }

    @Test
    fun forecastPicksLatestEndDateAmongActive() {
        // Two active debts:
        //   debt 1: 12 months from 2026-01-01 day 10 → ends 2026-12-10
        //   debt 2: 24 months from 2026-03-01 day 5 → ends 2028-02-05
        // Expected forecast = 2028-02-05.
        val stats = JourneyCalculator.compute(
            listOf(
                debt(id = 1L, totalPayments = 12, startDate = LocalDate(2026, 1, 1), dueDay = 10),
                debt(id = 2L, totalPayments = 24, startDate = LocalDate(2026, 3, 1), dueDay = 5),
            ),
            listOf(payment(id = 1L, debtId = 1L, amount = 1500.0)),
        )
        assertEquals(LocalDate(2028, 2, 5), stats?.forecastEndDate)
    }

    @Test
    fun archivedDebtsExcludedFromForecast() {
        // Active debt ends 2026-06-10. Archived debt would end 2028-02-05 if active.
        // Forecast should be 2026-06-10.
        val stats = JourneyCalculator.compute(
            listOf(
                debt(id = 1L, totalPayments = 6, startDate = LocalDate(2026, 1, 1), dueDay = 10),
                debt(id = 2L, totalPayments = 24, startDate = LocalDate(2026, 3, 1), dueDay = 5, isArchived = true),
            ),
            listOf(payment(id = 1L, debtId = 1L, amount = 1500.0)),
        )
        assertEquals(LocalDate(2026, 6, 10), stats?.forecastEndDate)
    }

    @Test
    fun dueDay31ClampsToShorterMonth() {
        // Debt with dueDay = 31, totalPayments = 2, startDate = 2026-01-15 (no useLastDay).
        // End month = 2026-02. February has 28 days in 2026 → forecast = 2026-02-28 (clamped).
        val stats = JourneyCalculator.compute(
            listOf(debt(totalPayments = 2, startDate = LocalDate(2026, 1, 15), dueDay = 31)),
            listOf(payment(id = 1L, debtId = 1L, amount = 1500.0)),
        )
        assertEquals(LocalDate(2026, 2, 28), stats?.forecastEndDate)
    }

    @Test
    fun useLastDayOfMonthYieldsLastDay() {
        // Debt with useLastDayOfMonth = true, totalPayments = 3, start 2026-01-31, dueDay 31.
        // End month = 2026-03. Forecast = 2026-03-31.
        val stats = JourneyCalculator.compute(
            listOf(
                debt(
                    totalPayments = 3,
                    startDate = LocalDate(2026, 1, 31),
                    dueDay = 31,
                    useLastDayOfMonth = true,
                ),
            ),
            listOf(payment(id = 1L, debtId = 1L, amount = 1500.0)),
        )
        assertEquals(LocalDate(2026, 3, 31), stats?.forecastEndDate)
    }

    @Test
    fun percentClearedBoundedAt100() {
        // Pathological: melted > scheduled (shouldn't happen with real data, but the
        // calculator should still bound the percent at 100, not return 200%).
        val stats = JourneyCalculator.compute(
            listOf(debt(monthlyAmount = 1500.0, totalPayments = 1)),  // scheduled = 1500
            listOf(payment(id = 1L, debtId = 1L, amount = 3000.0)),
        )
        assertEquals(100, stats?.percentCleared)
        assertEquals(3000.0, stats?.totalMelted)
    }

    @Test
    fun percentClearedRoundsDown() {
        // 1499/18000 = 0.0832… → 8 (rounded down).
        val stats = JourneyCalculator.compute(
            listOf(debt()),
            listOf(payment(id = 1L, debtId = 1L, amount = 1499.0)),
        )
        assertEquals(8, stats?.percentCleared)
        assertTrue((stats?.totalMelted ?: 0.0) > 0.0)
    }
}
