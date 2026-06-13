package com.snowball.ui.home

import com.snowball.data.Repos
import com.snowball.data.model.Debt
import com.snowball.domain.Cutoff
import com.snowball.domain.CutoffCalculator
import com.snowball.domain.DueRow
import com.snowball.domain.JourneyCalculator
import com.snowball.domain.JourneyStats
import com.snowball.domain.OverdueCalculator
import com.snowball.domain.OverdueInfo
import com.snowball.domain.completedDebtDueInCutoff
import com.snowball.domain.cutoffFromKey
import com.snowball.domain.nextCutoff
import com.snowball.domain.resolveEffectiveCutoff
import com.snowball.domain.storageKey
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
    val overdue: List<OverdueInfo>,
    val swipeCoachmarkSeen: Boolean,
    val isActivatedEarly: Boolean,
)

class HomeViewModel(private val repos: Repos) {

    fun load(today: LocalDate = today()): HomeState {
        val settings = repos.settings.get()
        // Honor a manually-activated ("got paid early") cutoff while it's still ahead of
        // the calendar; otherwise fall back to today's cutoff and drop the stale override.
        val override = settings.paidAheadKey.takeIf { it.isNotBlank() }?.let { cutoffFromKey(it) }
        val eff = resolveEffectiveCutoff(today, override)
        if (eff.clearOverride) repos.settings.clearPaidAhead()
        val cutoff = eff.cutoff
        val next = cutoff.next()

        val active = repos.debts.allActive()
        val allDebts = repos.debts.all()
        val countById = allDebts.associate { it.id to repos.payments.countForDebt(it.id) }

        // A debt that was fully paid (and so auto-archived) should remain visible as a
        // paid row in the single cutoff its final payment belongs to, so the cutoff's
        // totals stay correct. Computed per-cutoff: a debt completed this cutoff won't
        // match the next cutoff's window, so Up Next stays clean.
        fun debtsFor(c: Cutoff): List<Debt> =
            active + allDebts.filter { completedDebtDueInCutoff(it, countById[it.id] ?: 0, c) }

        val currentDebts = debtsFor(cutoff)
        val nextDebts = debtsFor(next)

        val displayIds = (currentDebts + nextDebts).map { it.id }.toSet()
        val paymentsByDebt = displayIds.associateWith { repos.payments.historyForDebt(it) }

        val rows = CutoffCalculator.computeDueRows(cutoff, currentDebts, paymentsByDebt)
        val income = settings.incomePerCutoff
        val summary = CutoffCalculator.summarize(rows, income)

        val nextRows = CutoffCalculator.computeDueRows(next, nextDebts, paymentsByDebt)
        val nextTotal = nextRows.sumOf { it.amount }

        val allPayments = allDebts.flatMap { repos.payments.historyForDebt(it.id) }
        val journey = JourneyCalculator.compute(allDebts, allPayments)

        // A completed debt is never overdue, so overdue runs over the active set only.
        val overdue = OverdueCalculator.computeOverdue(active, paymentsByDebt, today)
        return HomeState(
            cutoff, rows, summary, income, next, nextRows, nextTotal, journey, overdue,
            swipeCoachmarkSeen = settings.swipeCoachmarkSeen,
            isActivatedEarly = eff.isActivatedEarly,
        )
    }

    fun activateNextEarly(today: LocalDate = today()) {
        repos.settings.setPaidAhead(nextCutoff(today).storageKey())
    }

    fun undoActivateEarly() {
        repos.settings.clearPaidAhead()
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

    fun markSwipeCoachmarkSeen() {
        repos.settings.markSwipeCoachmarkSeen()
    }

    fun catchUpOverdue(info: OverdueInfo, todayDate: LocalDate = today()) {
        repeat(info.missedCycles) {
            repos.payments.markPaid(info.debt.id, todayDate, info.debt.monthlyAmount)
        }
        val totalPayments = repos.payments.countForDebt(info.debt.id)
        if (totalPayments >= info.debt.totalPayments) {
            repos.debts.setArchived(info.debt.id, true)
        }
    }
}
