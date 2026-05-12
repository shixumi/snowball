package com.snowball.ui.insights

import com.snowball.data.Repos
import com.snowball.data.model.CategoryBehavior
import com.snowball.domain.CutoffForecast
import com.snowball.domain.InsightsCalculator
import com.snowball.domain.SnapshotStats
import com.snowball.domain.today
import kotlinx.datetime.LocalDate

data class InsightsState(
    val snapshot: SnapshotStats,
    val forecast: List<CutoffForecast>,
)

class InsightsViewModel(private val repos: Repos) {
    fun load(today: LocalDate = today()): InsightsState {
        val scheduledCatIds = repos.categories.all()
            .filter { it.behavior == CategoryBehavior.SCHEDULED }
            .map { it.id }
            .toSet()
        val active = repos.debts.allActive().filter { it.categoryId in scheduledCatIds }
        val paymentsByDebt = active.associate { it.id to repos.payments.historyForDebt(it.id) }
        val income = repos.settings.get().incomePerCutoff
        val snapshot = InsightsCalculator.snapshot(active, paymentsByDebt, income)
        val forecast = InsightsCalculator.forecastCutoffs(today, active, paymentsByDebt, income, count = 12)
        return InsightsState(snapshot, forecast)
    }
}
