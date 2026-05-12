package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

data class JourneyStats(
    val percentCleared: Int,
    val totalMelted: Double,
    val forecastEndDate: LocalDate?,
)

object JourneyCalculator {
    fun compute(allDebts: List<Debt>, allPayments: List<Payment>): JourneyStats? {
        val totalMelted = allPayments.sumOf { it.amount }
        if (totalMelted == 0.0) return null

        val scheduled = allDebts.sumOf { it.monthlyAmount * it.totalPayments }
        val rawPct = if (scheduled > 0.0) ((totalMelted / scheduled) * 100).toInt() else 0
        val percent = rawPct.coerceIn(0, 100)

        val active = allDebts.filterNot { it.isArchived }
        val forecast = active.mapNotNull(::projectedEndDate).maxOrNull()

        return JourneyStats(percent, totalMelted, forecast)
    }

    private fun projectedEndDate(debt: Debt): LocalDate? {
        if (debt.totalPayments <= 0) return null
        val endMonth = debt.startDate.plus(debt.totalPayments - 1, DateTimeUnit.MONTH)

        // Try with the original settings first
        var result = effectiveDueDate(
            year = endMonth.year,
            month = endMonth.monthNumber,
            dueDay = debt.dueDay,
            useLastDay = debt.useLastDayOfMonth,
        )

        // If the due day doesn't exist and useLastDay is false, try clamping
        if (result == null && !debt.useLastDayOfMonth) {
            result = effectiveDueDate(
                year = endMonth.year,
                month = endMonth.monthNumber,
                dueDay = debt.dueDay,
                useLastDay = true,
            )
        }

        return result
    }
}
