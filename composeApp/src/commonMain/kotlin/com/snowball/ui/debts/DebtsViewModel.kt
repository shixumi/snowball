package com.snowball.ui.debts

import com.snowball.data.Repos
import com.snowball.data.model.Category
import com.snowball.data.model.CategoryBehavior
import com.snowball.data.model.Debt
import kotlinx.datetime.LocalDate

data class DebtRow(
    val debt: Debt,
    val paymentsMade: Int,
    val clearedDate: LocalDate?,
    val totalPaidAmount: Double,
)

data class DebtsState(
    val categories: List<Category>,
    val scheduledByCategory: Map<Long, List<DebtRow>>,
    val miscRows: List<DebtRow>,
    val showArchived: Boolean,
)

class DebtsViewModel(private val repos: Repos) {
    var showArchived: Boolean = false
        private set

    fun load(): DebtsState {
        val cats = repos.categories.all()
        val schedCatIds = cats.filter { it.behavior == CategoryBehavior.SCHEDULED }.map { it.id }.toSet()
        val miscCatIds = cats.filter { it.behavior == CategoryBehavior.LEDGER }.map { it.id }.toSet()

        // Scheduled rows respect the archived toggle.
        val scheduledDebts = if (showArchived) {
            repos.debts.all().filter { it.isArchived && it.categoryId in schedCatIds }
        } else {
            repos.debts.allActive().filter { it.categoryId in schedCatIds }
        }
        val scheduledByCategory = scheduledDebts
            .groupBy { it.categoryId }
            .mapValues { (_, debts) -> debts.map { d -> rowFor(d) } }

        // MISC rows are always shown in the Active view; suppressed in Archived view.
        val miscRows = if (showArchived) emptyList() else {
            repos.debts.all()
                .filter { it.categoryId in miscCatIds }
                .map { d -> rowFor(d) }
        }

        return DebtsState(cats, scheduledByCategory, miscRows, showArchived)
    }

    fun toggleArchive() { showArchived = !showArchived }

    fun delete(id: Long) { repos.debts.delete(id) }

    private fun rowFor(d: Debt): DebtRow {
        val payments = repos.payments.historyForDebt(d.id)
        val cleared = if (d.isArchived) payments.maxByOrNull { it.paidDate }?.paidDate else null
        val totalPaid = payments.sumOf { it.amount }
        return DebtRow(d, payments.size, cleared, totalPaid)
    }
}
