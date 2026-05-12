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

    /**
     * Number of cycles whose effective due date is on or before `asOf`.
     * Strict semantic: a cycle is "expected to be paid" once its due date passes.
     * Cycle dates are anchored at firstPaymentDate (NOT startDate — startDate is
     * loan origination, not the first payment).
     */
    private fun expectedPaymentsByDate(debt: Debt, asOf: LocalDate): Int {
        var count = 0
        for (n in 1..debt.totalPayments) {
            val due = nthDueDate(debt, n) ?: continue
            if (due <= asOf) count++ else break
        }
        return count
    }

    private fun nthDueDate(debt: Debt, n: Int): LocalDate? {
        if (n < 1 || n > debt.totalPayments) return null
        val month = debt.firstPaymentDate.plus(n - 1, DateTimeUnit.MONTH)
        return effectiveDueDate(
            year = month.year,
            month = month.monthNumber,
            dueDay = debt.dueDay,
            useLastDay = debt.useLastDayOfMonth,
        )
    }
}
