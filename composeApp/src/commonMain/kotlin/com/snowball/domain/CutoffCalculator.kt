package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.LocalDate

data class DueRow(
    val debt: Debt,
    val effectiveDueDate: LocalDate,
    val amount: Double,
    val isPaidThisCycle: Boolean,
)

object CutoffCalculator {
    fun computeDueRows(
        cutoff: Cutoff,
        activeDebts: List<Debt>,
        paymentsByDebt: Map<Long, List<Payment>>,
    ): List<DueRow> {
        val rows = mutableListOf<DueRow>()
        for (debt in activeDebts) {
            // The 30th cutoff's window spans a month boundary (e.g. May 30 -> Jun 14),
            // so the debt's due date for this cutoff may land in either month.
            val effective = candidateDueDate(debt, cutoff) ?: continue

            // Skip cycles before the debt's first payment is due.
            if (effective < debt.firstPaymentDate) continue

            val priorEffective = priorCycleDueDate(debt, effective)
            val payments = paymentsByDebt[debt.id].orEmpty()
            val paid = payments.any { it.paidDate > priorEffective && it.paidDate <= cutoff.windowEnd }

            rows.add(
                DueRow(
                    debt = debt,
                    effectiveDueDate = effective,
                    amount = debt.monthlyAmount,
                    isPaidThisCycle = paid,
                )
            )
        }
        return rows.sortedBy { it.effectiveDueDate }
    }

    data class Summary(
        val dueTotal: Double,
        val paidTotal: Double,
        val breathingRoom: Double,
    )

    fun summarize(rows: List<DueRow>, incomePerCutoff: Double): Summary {
        val due = rows.sumOf { it.amount }
        val paid = rows.filter { it.isPaidThisCycle }.sumOf { it.amount }
        return Summary(
            dueTotal = due,
            paidTotal = paid,
            breathingRoom = incomePerCutoff - due,
        )
    }

    /**
     * The debt's due date that falls within this cutoff's window, or null if none does.
     * Because the 30th cutoff's window crosses a month boundary, we check the debt's
     * dueDay in both the window's start month and its end month.
     */
    private fun candidateDueDate(debt: Debt, cutoff: Cutoff): LocalDate? {
        val ws = cutoff.windowStart
        val we = cutoff.windowEnd
        val inStartMonth = effectiveDueDate(ws.year, ws.monthNumber, debt.dueDay, debt.useLastDayOfMonth)
        if (inStartMonth != null && inStartMonth >= ws && inStartMonth <= we) return inStartMonth
        val crossesMonth = ws.monthNumber != we.monthNumber || ws.year != we.year
        if (crossesMonth) {
            val inEndMonth = effectiveDueDate(we.year, we.monthNumber, debt.dueDay, debt.useLastDayOfMonth)
            if (inEndMonth != null && inEndMonth >= ws && inEndMonth <= we) return inEndMonth
        }
        return null
    }

    private fun priorCycleDueDate(debt: Debt, current: LocalDate): LocalDate {
        val (py, pm) = if (current.monthNumber == 1) (current.year - 1) to 12 else current.year to (current.monthNumber - 1)
        val prior = effectiveDueDate(
            year = py, month = pm,
            dueDay = debt.dueDay, useLastDay = debt.useLastDayOfMonth,
        )
        return when {
            prior == null -> debt.firstPaymentDate
            prior < debt.firstPaymentDate -> debt.firstPaymentDate
            else -> prior
        }
    }
}
