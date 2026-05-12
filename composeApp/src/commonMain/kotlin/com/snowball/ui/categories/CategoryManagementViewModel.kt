package com.snowball.ui.categories

import com.snowball.data.Repos
import com.snowball.data.model.Category
import com.snowball.data.model.CategoryBehavior

data class CategoryManagementState(
    val categories: List<Category>,
    val debtCounts: Map<Long, Int>,
)

class CategoryManagementViewModel(private val repos: Repos) {

    fun load(): CategoryManagementState {
        val cats = repos.categories.all()
        val allDebts = repos.debts.all()
        val counts = cats.associate { c -> c.id to allDebts.count { it.categoryId == c.id } }
        return CategoryManagementState(cats, counts)
    }

    fun rename(id: Long, newName: String) {
        if (newName.isBlank()) return
        repos.categories.rename(id, newName.trim())
    }

    fun setIcon(id: Long, iconKey: String) {
        repos.categories.setIcon(id, iconKey)
    }

    fun create(name: String, iconKey: String) {
        if (name.isBlank()) return
        repos.categories.add(name.trim(), CategoryBehavior.SCHEDULED, iconKey)
    }

    fun reassignAndDelete(sourceId: Long, targetId: Long) {
        val debts = repos.debts.all().filter { it.categoryId == sourceId }
        debts.forEach { d ->
            repos.debts.update(
                id = d.id,
                name = d.name,
                categoryId = targetId,
                monthlyAmount = d.monthlyAmount,
                totalPayments = d.totalPayments,
                dueDay = d.dueDay,
                useLastDayOfMonth = d.useLastDayOfMonth,
                startDate = d.startDate,
                firstPaymentDate = d.firstPaymentDate,
                notes = d.notes,
            )
        }
        repos.categories.delete(sourceId)
    }

    fun deleteEmpty(id: Long) {
        repos.categories.delete(id)
    }
}
