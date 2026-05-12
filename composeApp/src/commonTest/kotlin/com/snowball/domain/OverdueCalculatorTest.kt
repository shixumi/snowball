package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverdueCalculatorTest {

    private fun debt(
        id: Long = 1L,
        monthlyAmount: Double = 1500.0,
        totalPayments: Int = 12,
        startDate: LocalDate = LocalDate(2026, 1, 1),
        firstPaymentDate: LocalDate = startDate,
        dueDay: Int = 10,
        useLastDayOfMonth: Boolean = false,
        isArchived: Boolean = false,
    ) = Debt(
        id = id, name = "D$id", categoryId = 1L,
        monthlyAmount = monthlyAmount, totalPayments = totalPayments,
        dueDay = dueDay, useLastDayOfMonth = useLastDayOfMonth,
        startDate = startDate, firstPaymentDate = firstPaymentDate,
        isArchived = isArchived, notes = null,
    )

    @Test
    fun upToDateDebtReturnsEmpty() {
        // First payment Jan 1, dueDay 10. Today Jan 9 (before first due). Expected = 0.
        val d = debt()
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to emptyList()),
            today = LocalDate(2026, 1, 9),
        )
        assertTrue(info.isEmpty())
    }

    @Test
    fun cycleNotOverdueBeforeDueDate() {
        // First payment Jan 1, dueDay 10. Today Feb 9 (after Jan 10, before Feb 10).
        // Cycle 1 is in the past. Zero payments → 1 missed.
        val d = debt()
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to emptyList()),
            today = LocalDate(2026, 2, 9),
        )
        assertEquals(1, info.size)
        assertEquals(1, info[0].missedCycles)
        assertEquals(LocalDate(2026, 1, 10), info[0].firstMissedDueDate)
    }

    @Test
    fun multipleMissedCycles() {
        // First payment Jan 1, dueDay 10. Today Apr 9 (after Mar 10, before Apr 10).
        // Cycles 1-3 are past, 0 paid → 3 missed.
        val d = debt()
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to emptyList()),
            today = LocalDate(2026, 4, 9),
        )
        assertEquals(1, info.size)
        assertEquals(3, info[0].missedCycles)
        assertEquals(4500.0, info[0].missedAmount)
        assertEquals(LocalDate(2026, 1, 10), info[0].firstMissedDueDate)
    }

    @Test
    fun partiallyPaidDebtReportsRemainingMissed() {
        // Today Apr 9. Cycles 1-3 expected. 1 payment made → 2 missed (cycles 2 and 3).
        val d = debt()
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to listOf(Payment(1L, d.id, LocalDate(2026, 1, 10), 1500.0))),
            today = LocalDate(2026, 4, 9),
        )
        assertEquals(1, info.size)
        assertEquals(2, info[0].missedCycles)
        // First missed is the 2nd cycle (Feb 10).
        assertEquals(LocalDate(2026, 2, 10), info[0].firstMissedDueDate)
    }

    @Test
    fun archivedDebtSkipped() {
        val d = debt(isArchived = true)
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to emptyList()),
            today = LocalDate(2026, 4, 15),
        )
        assertTrue(info.isEmpty())
    }

    @Test
    fun futureStartDebtReturnsEmpty() {
        val d = debt(startDate = LocalDate(2026, 6, 1), firstPaymentDate = LocalDate(2026, 6, 1))
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to emptyList()),
            today = LocalDate(2026, 1, 1),
        )
        assertTrue(info.isEmpty())
    }

    @Test
    fun firstPaymentDateOffsetFromStartDate() {
        // User's real-world case: got loan Feb 17, started paying Mar 17.
        // 6 cycles. 2 payments recorded. Today May 13.
        // Cycle 1 Mar 17 (paid), Cycle 2 Apr 17 (paid), Cycle 3 May 17 (not yet due).
        // expected = 2, actual = 2, missed = 0. NOT overdue.
        val d = debt(
            startDate = LocalDate(2026, 2, 17),
            firstPaymentDate = LocalDate(2026, 3, 17),
            dueDay = 17,
            totalPayments = 6,
            monthlyAmount = 3566.71,
        )
        val payments = listOf(
            Payment(1L, d.id, LocalDate(2026, 3, 17), 3566.71),
            Payment(2L, d.id, LocalDate(2026, 4, 17), 3566.71),
        )
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to payments),
            today = LocalDate(2026, 5, 13),
        )
        assertTrue(info.isEmpty(), "Cycle 3 (May 17) is not yet due on May 13")
    }

    @Test
    fun firstPaymentDateCycleBecomesOverdueAfterDueDate() {
        // Same debt as above, but today May 18 — past May 17 cycle.
        // Now cycles 1, 2, 3 are expected. 2 paid → 1 missed.
        val d = debt(
            startDate = LocalDate(2026, 2, 17),
            firstPaymentDate = LocalDate(2026, 3, 17),
            dueDay = 17,
            totalPayments = 6,
            monthlyAmount = 3566.71,
        )
        val payments = listOf(
            Payment(1L, d.id, LocalDate(2026, 3, 17), 3566.71),
            Payment(2L, d.id, LocalDate(2026, 4, 17), 3566.71),
        )
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to payments),
            today = LocalDate(2026, 5, 18),
        )
        assertEquals(1, info.size)
        assertEquals(1, info[0].missedCycles)
        assertEquals(LocalDate(2026, 5, 17), info[0].firstMissedDueDate)
    }
}
