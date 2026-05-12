package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

data class OverdueInfo(
    val debt: Debt,
    val missedCycles: Int,
    val missedAmount: Double,
    val firstMissedDueDate: LocalDate,
)

object OverdueCalculator {
    fun computeOverdue(
        debts: List<Debt>,
        paymentsByDebt: Map<Long, List<Payment>>,
        today: LocalDate,
    ): List<OverdueInfo> = debts.mapNotNull { debt ->
        if (debt.isArchived) return@mapNotNull null
        val expectedSoFar = expectedPaymentsByDate(debt, today)
        val actual = paymentsByDebt[debt.id]?.size ?: 0
        val missed = expectedSoFar - actual
        if (missed <= 0) return@mapNotNull null

        val firstMissed = nthDueDate(debt, actual + 1) ?: return@mapNotNull null
        val cappedMissed = missed.coerceAtMost(debt.totalPayments - actual)
        OverdueInfo(
            debt = debt,
            missedCycles = cappedMissed,
            missedAmount = cappedMissed * debt.monthlyAmount,
            firstMissedDueDate = firstMissed,
        )
    }

    private fun expectedPaymentsByDate(debt: Debt, asOf: LocalDate): Int {
        // A cycle is "expected by now" only after its due date PLUS one full month of grace
        // has passed. This matches user mental model: a payment 26 days past due is not yet
        // "overdue" — it gets caught up in the next payday cutoff. Only when a full cycle
        // has elapsed without payment do we flag the debt.
        var count = 0
        for (n in 1..debt.totalPayments) {
            val due = nthDueDate(debt, n) ?: continue
            val overdueAt = due.plus(1, DateTimeUnit.MONTH)
            if (overdueAt <= asOf) count++ else break
        }
        return count
    }

    private fun nthDueDate(debt: Debt, n: Int): LocalDate? {
        if (n < 1 || n > debt.totalPayments) return null
        val month = debt.startDate.plus(n - 1, DateTimeUnit.MONTH)
        return effectiveDueDate(
            year = month.year,
            month = month.monthNumber,
            dueDay = debt.dueDay,
            useLastDay = debt.useLastDayOfMonth,
        )
    }
}
