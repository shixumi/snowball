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
            val effective = effectiveDueDate(
                year = cutoff.windowStart.year,
                month = cutoff.windowStart.monthNumber,
                dueDay = debt.dueDay,
                useLastDay = debt.useLastDayOfMonth,
            ) ?: continue

            if (effective < cutoff.windowStart || effective > cutoff.windowEnd) continue
            if (debt.startDate > cutoff.payDate) continue

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

    private fun priorCycleDueDate(debt: Debt, current: LocalDate): LocalDate {
        val (py, pm) = if (current.monthNumber == 1) (current.year - 1) to 12 else current.year to (current.monthNumber - 1)
        val prior = effectiveDueDate(
            year = py, month = pm,
            dueDay = debt.dueDay, useLastDay = debt.useLastDayOfMonth,
        )
        return when {
            prior == null -> debt.startDate
            prior < debt.startDate -> debt.startDate
            else -> prior
        }
    }
}
