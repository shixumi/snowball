package com.snowball.ui.debts

import com.snowball.data.Repos
import com.snowball.data.model.Category
import com.snowball.data.model.Debt

data class DebtsState(
    val categories: List<Category>,
    val debtsByCategory: Map<Long, List<Debt>>,
    val showArchived: Boolean,
)

class DebtsViewModel(private val repos: Repos) {
    var showArchived: Boolean = false
        private set

    fun load(): DebtsState {
        val cats = repos.categories.all()
        val all = if (showArchived) repos.debts.all().filter { it.isArchived } else repos.debts.allActive()
        val grouped = all.groupBy { it.categoryId }
        return DebtsState(cats, grouped, showArchived)
    }

    fun toggleArchive() { showArchived = !showArchived }

    fun delete(id: Long) { repos.debts.delete(id) }
}
