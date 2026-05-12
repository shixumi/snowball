package com.snowball.ui.home

import com.snowball.data.Repos
import com.snowball.domain.Cutoff
import com.snowball.domain.CutoffCalculator
import com.snowball.domain.DueRow
import com.snowball.domain.JourneyCalculator
import com.snowball.domain.JourneyStats
import com.snowball.domain.currentCutoff
import com.snowball.domain.nextCutoff
import com.snowball.domain.today
import kotlinx.datetime.LocalDate

data class HomeState(
    val cutoff: Cutoff,
    val rows: List<DueRow>,
    val summary: CutoffCalculator.Summary,
    val income: Double,
    val nextCutoff: Cutoff,
    val nextRows: List<DueRow>,
    val nextTotal: Double,
    val journey: JourneyStats?,
)

class HomeViewModel(private val repos: Repos) {

    fun load(today: LocalDate = today()): HomeState {
        val cutoff = currentCutoff(today)
        val debts = repos.debts.allActive()
        val paymentsByDebt = debts.associate { it.id to repos.payments.historyForDebt(it.id) }
        val rows = CutoffCalculator.computeDueRows(cutoff, debts, paymentsByDebt)
        val income = repos.settings.get().incomePerCutoff
        val summary = CutoffCalculator.summarize(rows, income)

        val next = nextCutoff(today)
        val nextRows = CutoffCalculator.computeDueRows(next, debts, paymentsByDebt)
        val nextTotal = nextRows.sumOf { it.amount }

        val allDebts = repos.debts.all()
        val allPayments = allDebts.flatMap { repos.payments.historyForDebt(it.id) }
        val journey = JourneyCalculator.compute(allDebts, allPayments)

        return HomeState(cutoff, rows, summary, income, next, nextRows, nextTotal, journey)
    }

    fun markPaid(row: DueRow, todayDate: LocalDate = today()) {
        repos.payments.markPaid(row.debt.id, todayDate, row.amount)
        val totalPayments = repos.payments.countForDebt(row.debt.id)
        if (totalPayments >= row.debt.totalPayments) {
            repos.debts.setArchived(row.debt.id, true)
        }
    }

    fun undoPayment(row: DueRow) {
        val history = repos.payments.historyForDebt(row.debt.id)
        val latest = history.firstOrNull() ?: return
        repos.payments.delete(latest.id)
        if (row.debt.isArchived) {
            repos.debts.setArchived(row.debt.id, false)
        }
    }
}
