package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CutoffCalculatorTest {
    private fun debt(
        id: Long = 1,
        dueDay: Int,
        useLastDay: Boolean = false,
        monthly: Double = 1000.0,
        total: Int = 12,
        start: LocalDate = LocalDate(2026, 1, 1),
        firstPayment: LocalDate = start,
        archived: Boolean = false,
    ) = Debt(
        id = id, name = "d$id", categoryId = 1, monthlyAmount = monthly,
        totalPayments = total, dueDay = dueDay, useLastDayOfMonth = useLastDay,
        startDate = start, firstPaymentDate = firstPayment,
        isArchived = archived, notes = null,
    )

    @Test
    fun debt_due_day_17_in_may15_cutoff_appears() {
        val c = Cutoff(2026, 5, Payday.FIFTEENTH)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(debt(dueDay = 17)),
            paymentsByDebt = emptyMap(),
        )
        assertEquals(1, rows.size)
    }

    @Test
    fun debt_due_day_10_in_may30_cutoff_appears() {
        val c = Cutoff(2026, 5, Payday.THIRTIETH)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(debt(dueDay = 10)),
            paymentsByDebt = emptyMap(),
        )
        assertEquals(1, rows.size)
    }

    @Test
    fun debt_due_day_30_belongs_to_30th_cutoff_not_15th() {
        // The reported bug: a bill due on the 30th was being filed under the 15th
        // cutoff. With the corrected boundary, day 30 belongs to the 30th cutoff.
        val d = debt(dueDay = 30)

        val fifteenth = CutoffCalculator.computeDueRows(
            cutoff = Cutoff(2026, 5, Payday.FIFTEENTH),
            activeDebts = listOf(d),
            paymentsByDebt = emptyMap(),
        )
        assertEquals(0, fifteenth.size, "Day-30 debt must NOT appear in the 15th cutoff")

        val thirtieth = CutoffCalculator.computeDueRows(
            cutoff = Cutoff(2026, 5, Payday.THIRTIETH),
            activeDebts = listOf(d),
            paymentsByDebt = emptyMap(),
        )
        assertEquals(1, thirtieth.size, "Day-30 debt must appear in the 30th cutoff")
        assertEquals(LocalDate(2026, 5, 30), thirtieth.first().effectiveDueDate)
    }

    @Test
    fun debt_due_day_5_in_30th_cutoff_resolves_to_next_month() {
        // The 30th cutoff spans a month boundary (May 30 -> Jun 14). A bill due on
        // the 5th lands on June 5, inside that window.
        val d = debt(dueDay = 5)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = Cutoff(2026, 5, Payday.THIRTIETH),
            activeDebts = listOf(d),
            paymentsByDebt = emptyMap(),
        )
        assertEquals(1, rows.size)
        assertEquals(LocalDate(2026, 6, 5), rows.first().effectiveDueDate)
    }

    @Test
    fun debt_due_day_outside_window_excluded() {
        val c = Cutoff(2026, 5, Payday.FIFTEENTH)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(debt(dueDay = 5)),
            paymentsByDebt = emptyMap(),
        )
        assertEquals(0, rows.size)
    }

    @Test
    fun debt_starting_after_cutoff_excluded() {
        val c = Cutoff(2026, 5, Payday.FIFTEENTH)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(debt(dueDay = 17, start = LocalDate(2026, 6, 1))),
            paymentsByDebt = emptyMap(),
        )
        assertEquals(0, rows.size)
    }

    @Test
    fun archived_debt_excluded() {
        val c = Cutoff(2026, 5, Payday.FIFTEENTH)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(debt(dueDay = 17, archived = true)).filter { !it.isArchived },
            paymentsByDebt = emptyMap(),
        )
        assertEquals(0, rows.size)
    }

    @Test
    fun debt_paid_this_cycle_marked_as_paid() {
        val c = Cutoff(2026, 5, Payday.FIFTEENTH)
        val d = debt(dueDay = 17)
        val paid = Payment(id = 1, debtId = d.id, paidDate = LocalDate(2026, 5, 17), amount = 1000.0)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to listOf(paid)),
        )
        assertEquals(1, rows.size)
        assertTrue(rows.first().isPaidThisCycle)
    }

    @Test
    fun debt_paid_in_prior_cycle_remains_owed() {
        val c = Cutoff(2026, 5, Payday.FIFTEENTH)
        val d = debt(dueDay = 17)
        val priorPaid = Payment(id = 1, debtId = d.id, paidDate = LocalDate(2026, 4, 17), amount = 1000.0)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to listOf(priorPaid)),
        )
        assertEquals(1, rows.size)
        assertEquals(false, rows.first().isPaidThisCycle)
    }

    @Test
    fun totals_sum_monthly_amount() {
        val c = Cutoff(2026, 5, Payday.FIFTEENTH)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(
                debt(id = 1, dueDay = 17, monthly = 3000.0),
                debt(id = 2, dueDay = 19, monthly = 500.0),
            ),
            paymentsByDebt = emptyMap(),
        )
        val total = rows.sumOf { it.amount }
        assertEquals(3500.0, total)
    }

    @Test
    fun summary_total_uses_all_owed_paid_or_not() {
        val c = Cutoff(2026, 5, Payday.FIFTEENTH)
        val d1 = debt(id = 1, dueDay = 17, monthly = 3000.0)
        val d2 = debt(id = 2, dueDay = 19, monthly = 500.0)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(d1, d2),
            paymentsByDebt = mapOf(
                d1.id to listOf(Payment(1, d1.id, LocalDate(2026, 5, 17), 3000.0))
            ),
        )
        val summary = CutoffCalculator.summarize(rows = rows, incomePerCutoff = 25000.0)
        assertEquals(3500.0, summary.dueTotal)
        assertEquals(21500.0, summary.breathingRoom)
        assertEquals(3000.0, summary.paidTotal)
    }
}
