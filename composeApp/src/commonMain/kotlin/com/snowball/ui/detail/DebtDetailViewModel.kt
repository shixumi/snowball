package com.snowball.ui.detail

import com.snowball.data.Repos
import com.snowball.data.model.Category
import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import com.snowball.domain.projectedEndDate
import kotlinx.datetime.LocalDate

data class DebtDetailState(
    val debt: Debt,
    val category: Category,
    val paymentsMade: Int,
    val payments: List<Payment>,
    val projectedEndDate: LocalDate?,
    val amountLeft: Double,
)

class DebtDetailViewModel(private val repos: Repos, private val debtId: Long) {

    fun load(): DebtDetailState? {
        val debt = repos.debts.byId(debtId) ?: return null
        val category = repos.categories.byId(debt.categoryId) ?: return null
        val payments = repos.payments.historyForDebt(debtId)
        val made = payments.size
        val left = ((debt.totalPayments - made).coerceAtLeast(0)) * debt.monthlyAmount
        val projected = projectedEndDate(debt)
        return DebtDetailState(debt, category, made, payments, projected, left)
    }

    fun setArchived(archived: Boolean) { repos.debts.setArchived(debtId, archived) }

    fun delete(): Boolean {
        repos.debts.delete(debtId)
        return true
    }

    fun undoPayment(paymentId: Long) {
        repos.payments.delete(paymentId)
        val debt = repos.debts.byId(debtId) ?: return
        val remaining = repos.payments.countForDebt(debtId)
        if (debt.isArchived && remaining < debt.totalPayments) {
            repos.debts.setArchived(debtId, false)
        }
    }
}
