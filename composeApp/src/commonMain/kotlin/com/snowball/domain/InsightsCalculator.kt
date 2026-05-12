package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.LocalDate

data class SnapshotStats(
    val remaining: Double,
    val debtCount: Int,
    val monthlyBurden: Double,
    val monthlyIncome: Double,
    val coveragePercent: Int?,
)

data class CutoffForecast(
    val cutoff: Cutoff,
    val dueTotal: Double,
    val leftOver: Double,
    val isAllClear: Boolean,
)

object InsightsCalculator {

    fun snapshot(
        activeScheduledDebts: List<Debt>,
        paymentsByDebt: Map<Long, List<Payment>>,
        incomePerCutoff: Double,
    ): SnapshotStats {
        val remaining = activeScheduledDebts.sumOf { d ->
            val made = paymentsByDebt[d.id]?.size ?: 0
            (d.totalPayments - made).coerceAtLeast(0) * d.monthlyAmount
        }
        val monthlyBurden = activeScheduledDebts.sumOf { it.monthlyAmount }
        val monthlyIncome = incomePerCutoff * 2.0
        val coverage = if (monthlyIncome > 0.0) {
            ((monthlyBurden / monthlyIncome) * 100).toInt().coerceIn(0, 999)
        } else null
        return SnapshotStats(
            remaining = remaining,
            debtCount = activeScheduledDebts.size,
            monthlyBurden = monthlyBurden,
            monthlyIncome = monthlyIncome,
            coveragePercent = coverage,
        )
    }

    fun forecastCutoffs(
        today: LocalDate,
        activeScheduledDebts: List<Debt>,
        paymentsByDebt: Map<Long, List<Payment>>,
        incomePerCutoff: Double,
        count: Int = 12,
    ): List<CutoffForecast> {
        if (activeScheduledDebts.isEmpty()) return emptyList()

        val results = mutableListOf<CutoffForecast>()
        val virtual: MutableMap<Long, MutableList<Payment>> =
            paymentsByDebt.mapValues { it.value.toMutableList() }.toMutableMap()
        var c = nextCutoff(today).next()

        repeat(count) {
            val stillOwed = activeScheduledDebts.filter { d ->
                (virtual[d.id]?.size ?: 0) < d.totalPayments
            }
            val rows = CutoffCalculator.computeDueRows(c, stillOwed, virtual)
            val dueTotal = rows.sumOf { it.amount }
            val leftOver = incomePerCutoff - dueTotal
            results.add(
                CutoffForecast(
                    cutoff = c,
                    dueTotal = dueTotal,
                    leftOver = leftOver,
                    isAllClear = rows.isEmpty(),
                )
            )
            rows.forEach { row ->
                val list = virtual.getOrPut(row.debt.id) { mutableListOf() }
                list.add(
                    Payment(
                        id = -1L,
                        debtId = row.debt.id,
                        paidDate = row.effectiveDueDate,
                        amount = row.amount,
                    )
                )
            }
            c = c.next()
        }
        return results
    }
}
