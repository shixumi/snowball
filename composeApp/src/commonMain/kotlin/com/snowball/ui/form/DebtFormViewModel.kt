package com.snowball.ui.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.snowball.data.Repos
import com.snowball.data.model.Category
import com.snowball.data.model.CategoryBehavior
import com.snowball.data.model.Debt
import com.snowball.domain.today
import kotlinx.datetime.LocalDate

data class DebtFormState(
    val name: String = "",
    val categoryId: Long? = null,
    val monthlyAmount: String = "",
    val totalPayments: String = "",
    val paymentsAlreadyMade: String = "",
    val dueDay: String = "",
    val useLastDayOfMonth: Boolean = false,
    val startDate: LocalDate = today(),
    val startDateText: String = startDate.toString(),
    val notes: String = "",
)

class DebtFormViewModel(private val repos: Repos, existing: Debt? = null) {
    var state: DebtFormState by mutableStateOf(
        if (existing == null) {
            DebtFormState()
        } else {
            DebtFormState(
                name = existing.name,
                categoryId = existing.categoryId,
                monthlyAmount = existing.monthlyAmount.toString(),
                totalPayments = existing.totalPayments.toString(),
                paymentsAlreadyMade = repos.payments.countForDebt(existing.id).toString(),
                dueDay = existing.dueDay.toString(),
                useLastDayOfMonth = existing.useLastDayOfMonth,
                startDate = existing.startDate,
                startDateText = existing.startDate.toString(),
                notes = existing.notes.orEmpty(),
            )
        }
    )
        private set

    private val existingId: Long? = existing?.id
    val isEditing: Boolean = existingId != null

    val categories: List<Category> = repos.categories.all().filter { it.behavior == CategoryBehavior.SCHEDULED }

    fun update(transform: (DebtFormState) -> DebtFormState) { state = transform(state) }

    fun save(): Boolean {
        val name = state.name.trim()
        val catId = state.categoryId ?: return false
        val monthly = state.monthlyAmount.toDoubleOrNull() ?: return false
        val total = state.totalPayments.toIntOrNull() ?: return false
        val due = state.dueDay.toIntOrNull() ?: return false
        val alreadyPaid = state.paymentsAlreadyMade.toIntOrNull() ?: 0
        val startDate = runCatching { LocalDate.parse(state.startDateText) }.getOrNull() ?: return false
        if (name.isBlank() || monthly <= 0.0 || total <= 0 || due !in 1..31) return false
        if (alreadyPaid < 0 || alreadyPaid > total) return false

        if (existingId == null) {
            repos.debts.add(
                name = name,
                categoryId = catId,
                monthlyAmount = monthly,
                totalPayments = total,
                dueDay = due,
                useLastDayOfMonth = state.useLastDayOfMonth,
                startDate = startDate,
                notes = state.notes.ifBlank { null },
            )
            val newId = repos.debts.all().first().id
            backfillPayments(newId, monthly, startDate, alreadyPaid)
            if (alreadyPaid >= total) repos.debts.setArchived(newId, true)
        } else {
            repos.debts.update(
                id = existingId,
                name = name,
                categoryId = catId,
                monthlyAmount = monthly,
                totalPayments = total,
                dueDay = due,
                useLastDayOfMonth = state.useLastDayOfMonth,
                startDate = startDate,
                notes = state.notes.ifBlank { null },
            )
            reconcilePayments(existingId, monthly, startDate, alreadyPaid)
            if (alreadyPaid >= total) repos.debts.setArchived(existingId, true)
            else if (repos.debts.byId(existingId)?.isArchived == true) repos.debts.setArchived(existingId, false)
        }
        return true
    }

    fun delete(): Boolean {
        val id = existingId ?: return false
        repos.debts.delete(id)
        return true
    }

    /** Insert N synthetic payments dated at the start so they count toward paymentsMade. */
    private fun backfillPayments(debtId: Long, monthly: Double, startDate: LocalDate, count: Int) {
        repeat(count) {
            repos.payments.markPaid(debtId, startDate, monthly)
        }
    }

    /** Adjust the recorded payment count to match `alreadyPaid` by adding or removing rows. */
    private fun reconcilePayments(debtId: Long, monthly: Double, startDate: LocalDate, alreadyPaid: Int) {
        val current = repos.payments.countForDebt(debtId)
        when {
            current < alreadyPaid -> repeat(alreadyPaid - current) {
                repos.payments.markPaid(debtId, startDate, monthly)
            }
            current > alreadyPaid -> {
                val history = repos.payments.historyForDebt(debtId)
                val toRemove = current - alreadyPaid
                history.take(toRemove).forEach { repos.payments.delete(it.id) }
            }
        }
    }
}
