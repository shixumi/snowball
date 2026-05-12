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
        dueDay: Int = 10,
        useLastDayOfMonth: Boolean = false,
        isArchived: Boolean = false,
    ) = Debt(
        id = id, name = "D$id", categoryId = 1L,
        monthlyAmount = monthlyAmount, totalPayments = totalPayments,
        dueDay = dueDay, useLastDayOfMonth = useLastDayOfMonth,
        startDate = startDate, isArchived = isArchived, notes = null,
    )

    @Test
    fun upToDateDebtReturnsEmpty() {
        val d = debt()
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to emptyList()),
            today = LocalDate(2026, 1, 9),
        )
        assertTrue(info.isEmpty())
    }

    @Test
    fun oneMissedCycle() {
        val d = debt()
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to emptyList()),
            today = LocalDate(2026, 2, 15),
        )
        assertEquals(1, info.size)
        assertEquals(1, info[0].missedCycles)
        assertEquals(1500.0, info[0].missedAmount)
        assertEquals(LocalDate(2026, 1, 10), info[0].firstMissedDueDate)
    }

    @Test
    fun multipleMissedCycles() {
        val d = debt()
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to emptyList()),
            today = LocalDate(2026, 4, 15),
        )
        assertEquals(1, info.size)
        assertEquals(3, info[0].missedCycles)
        assertEquals(4500.0, info[0].missedAmount)
        assertEquals(LocalDate(2026, 1, 10), info[0].firstMissedDueDate)
    }

    @Test
    fun partiallyPaidDebtReportsRemainingMissed() {
        val d = debt()
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to listOf(Payment(1L, d.id, LocalDate(2026, 1, 10), 1500.0))),
            today = LocalDate(2026, 4, 15),
        )
        assertEquals(1, info.size)
        assertEquals(2, info[0].missedCycles)
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
        val d = debt(startDate = LocalDate(2026, 6, 1))
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to emptyList()),
            today = LocalDate(2026, 1, 1),
        )
        assertTrue(info.isEmpty())
    }
}
