package com.snowball.ui.insights

import com.snowball.data.Repos
import com.snowball.data.model.Category
import com.snowball.data.model.CategoryBehavior
import com.snowball.data.model.Debt
import com.snowball.domain.CutoffForecast
import com.snowball.domain.InsightsCalculator
import com.snowball.domain.SnapshotStats
import com.snowball.domain.projectedEndDate
import com.snowball.domain.today
import kotlinx.datetime.LocalDate

data class PayoffRow(
    val debt: Debt,
    val category: Category,
    val endDate: LocalDate,
    val monthlyAmount: Double,
)

data class InsightsState(
    val snapshot: SnapshotStats,
    val forecast: List<CutoffForecast>,
    val payoffTimeline: List<PayoffRow>,
)

class InsightsViewModel(private val repos: Repos) {
    fun load(today: LocalDate = today()): InsightsState {
        val scheduledCats = repos.categories.all()
            .filter { it.behavior == CategoryBehavior.SCHEDULED }
        val scheduledCatIds = scheduledCats.map { it.id }.toSet()
        val catById = scheduledCats.associateBy { it.id }
        val active = repos.debts.allActive().filter { it.categoryId in scheduledCatIds }
        val paymentsByDebt = active.associate { it.id to repos.payments.historyForDebt(it.id) }
        val income = repos.settings.get().incomePerCutoff
        val snapshot = InsightsCalculator.snapshot(active, paymentsByDebt, income)
        val forecast = InsightsCalculator.forecastCutoffs(today, active, paymentsByDebt, income, count = 12)
        val payoffTimeline = active
            .mapNotNull { debt ->
                val cat = catById[debt.categoryId] ?: return@mapNotNull null
                val endDate = projectedEndDate(debt) ?: return@mapNotNull null
                PayoffRow(
                    debt = debt,
                    category = cat,
                    endDate = endDate,
                    monthlyAmount = debt.monthlyAmount,
                )
            }
            .sortedBy { it.endDate }
        return InsightsState(snapshot, forecast, payoffTimeline)
    }
}
