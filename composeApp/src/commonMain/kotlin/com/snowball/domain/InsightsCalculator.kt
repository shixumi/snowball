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
    /** Per-debt breakdown of what's due this cutoff, sorted by due date. */
    val rows: List<DueRow> = emptyList(),
)

data class MonthForecast(
    val year: Int,
    val month: Int,
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

    /**
     * Rolls a per-cutoff forecast up into calendar months. Income for a month scales
     * with how many of that month's paydays appear in the window (1 for a partial
     * leading month, 2 for a full month), so left-over stays accurate.
     */
    fun aggregateByMonth(
        cutoffs: List<CutoffForecast>,
        incomePerCutoff: Double,
    ): List<MonthForecast> =
        cutoffs
            .groupBy { it.cutoff.year to it.cutoff.month }
            .map { (key, group) ->
                val due = group.sumOf { it.dueTotal }
                MonthForecast(
                    year = key.first,
                    month = key.second,
                    dueTotal = due,
                    leftOver = incomePerCutoff * group.size - due,
                    isAllClear = group.all { it.isAllClear },
                )
            }
            .sortedWith(compareBy({ it.year }, { it.month }))

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

        // Insights shows cutoffs from two ahead (Home owns the current + next cutoff). But
        // the schedule must still be advanced THROUGH those two skipped cutoffs — otherwise
        // a debt whose final payment falls in one of them never rolls off and wrongly spills
        // into a later, visible cutoff. So simulate from the current cutoff and only collect
        // results once we reach displayStart.
        val displayStart = nextCutoff(today).next()
        var c = currentCutoff(today)

        while (results.size < count) {
            val stillOwed = activeScheduledDebts.filter { d ->
                (virtual[d.id]?.size ?: 0) < d.totalPayments
            }
            val rows = CutoffCalculator.computeDueRows(c, stillOwed, virtual)

            if (c.payDate >= displayStart.payDate) {
                val dueTotal = rows.sumOf { it.amount }
                results.add(
                    CutoffForecast(
                        cutoff = c,
                        dueTotal = dueTotal,
                        leftOver = incomePerCutoff - dueTotal,
                        isAllClear = rows.isEmpty(),
                        rows = rows,
                    )
                )
            }

            // Advance the schedule: assume each not-yet-paid due cycle is paid on time.
            // The isPaidThisCycle guard matters for the skipped near cutoffs, where a real
            // payment may already cover the cycle — don't add a duplicate virtual payment.
            rows.forEach { row ->
                if (!row.isPaidThisCycle) {
                    virtual.getOrPut(row.debt.id) { mutableListOf() }
                        .add(
                            Payment(
                                id = -1L,
                                debtId = row.debt.id,
                                paidDate = row.effectiveDueDate,
                                amount = row.amount,
                            )
                        )
                }
            }
            c = c.next()
        }
        return results
    }
}
