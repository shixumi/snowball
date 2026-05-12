package com.snowball.ui.home

import com.snowball.data.Repos
import com.snowball.domain.Cutoff
import com.snowball.domain.CutoffCalculator
import com.snowball.domain.DueRow
import com.snowball.domain.currentCutoff
import com.snowball.domain.today
import kotlinx.datetime.LocalDate

data class HomeState(
    val cutoff: Cutoff,
    val rows: List<DueRow>,
    val summary: CutoffCalculator.Summary,
    val income: Double,
)

class HomeViewModel(private val repos: Repos) {

    fun load(today: LocalDate = today()): HomeState {
        val cutoff = currentCutoff(today)
        val debts = repos.debts.allActive()
        val paymentsByDebt = debts.associate { it.id to repos.payments.historyForDebt(it.id) }
        val rows = CutoffCalculator.computeDueRows(cutoff, debts, paymentsByDebt)
        val income = repos.settings.get().incomePerCutoff
        val summary = CutoffCalculator.summarize(rows, income)
        return HomeState(cutoff, rows, summary, income)
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
