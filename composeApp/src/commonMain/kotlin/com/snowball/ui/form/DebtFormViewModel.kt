package com.snowball.ui.form

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.snowball.data.Repos
import com.snowball.data.model.Category
import com.snowball.data.model.CategoryBehavior
import com.snowball.data.model.Debt
import com.snowball.domain.today
import com.snowball.ui.util.toFormFieldString
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

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
    val firstPaymentDate: LocalDate = today().plus(1, DateTimeUnit.MONTH),
    val firstPaymentDateText: String = firstPaymentDate.toString(),
    val notes: String = "",
)

fun DebtFormState.isNameValid(): Boolean = name.trim().isNotEmpty()
fun DebtFormState.isCategoryValid(): Boolean = categoryId != null
fun DebtFormState.isMonthlyAmountValid(): Boolean =
    (monthlyAmount.toDoubleOrNull() ?: 0.0) > 0.0
fun DebtFormState.isTotalPaymentsValid(): Boolean =
    (totalPayments.toIntOrNull() ?: 0) in 1..600
fun DebtFormState.isDueDayValid(): Boolean =
    (dueDay.toIntOrNull() ?: 0) in 1..31
fun DebtFormState.isPaymentsAlreadyMadeValid(): Boolean {
    val already = paymentsAlreadyMade.toIntOrNull() ?: 0
    val total = totalPayments.toIntOrNull() ?: 0
    return already >= 0 && already <= total
}

fun DebtFormState.isStartDateValid(): Boolean =
    runCatching { LocalDate.parse(startDateText) }.isSuccess

fun DebtFormState.isFirstPaymentDateValid(): Boolean =
    runCatching { LocalDate.parse(firstPaymentDateText) }.isSuccess

fun DebtFormState.isValid(): Boolean =
    isNameValid() &&
        isCategoryValid() &&
        isMonthlyAmountValid() &&
        isTotalPaymentsValid() &&
        isDueDayValid() &&
        isPaymentsAlreadyMadeValid() &&
        isStartDateValid() &&
        isFirstPaymentDateValid()

class DebtFormViewModel(private val repos: Repos, existing: Debt? = null) {
    var state: DebtFormState by mutableStateOf(
        if (existing == null) {
            DebtFormState()
        } else {
            DebtFormState(
                name = existing.name,
                categoryId = existing.categoryId,
                monthlyAmount = existing.monthlyAmount.toFormFieldString(),
                totalPayments = existing.totalPayments.toString(),
                paymentsAlreadyMade = repos.payments.countForDebt(existing.id).toString(),
                dueDay = existing.dueDay.toString(),
                useLastDayOfMonth = existing.useLastDayOfMonth,
                startDate = existing.startDate,
                startDateText = existing.startDate.toString(),
                firstPaymentDate = existing.firstPaymentDate,
                firstPaymentDateText = existing.firstPaymentDate.toString(),
                notes = existing.notes.orEmpty(),
            )
        }
    )
        private set

    private val existingId: Long? = existing?.id
    val isEditing: Boolean = existingId != null
    val recordedPayments: Int = existing?.let { repos.payments.countForDebt(it.id) } ?: 0
    val originalTotalPayments: Int = existing?.totalPayments ?: 0

    val categories: List<Category> = repos.categories.all().filter { it.behavior == CategoryBehavior.SCHEDULED }

    val isValid: Boolean get() = state.isValid()

    fun update(transform: (DebtFormState) -> DebtFormState) { state = transform(state) }

    fun save(): Boolean {
        val name = state.name.trim()
        val catId = state.categoryId ?: return false
        val monthly = state.monthlyAmount.toDoubleOrNull() ?: return false
        val total = state.totalPayments.toIntOrNull() ?: return false
        val due = state.dueDay.toIntOrNull() ?: return false
        val alreadyPaid = state.paymentsAlreadyMade.toIntOrNull() ?: 0
        val startDate = runCatching { LocalDate.parse(state.startDateText) }.getOrNull() ?: return false
        val firstPaymentDate = runCatching { LocalDate.parse(state.firstPaymentDateText) }.getOrNull() ?: return false
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
                firstPaymentDate = firstPaymentDate,
                notes = state.notes.ifBlank { null },
            )
            val newId = repos.debts.all().first().id
            backfillPayments(newId, monthly, firstPaymentDate, alreadyPaid)
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
                firstPaymentDate = firstPaymentDate,
                notes = state.notes.ifBlank { null },
            )
            reconcilePayments(existingId, monthly, firstPaymentDate, alreadyPaid)
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

    /**
     * Insert N synthetic payments distributed across the first N cycle months,
     * so each payment falls within its own cutoff window. Without this distribution
     * the cutoff calculator can't tell which cycles are paid (it looks at paidDate
     * vs cutoff window), causing fully-paid debts to keep appearing in "due this
     * cutoff" lists.
     */
    private fun backfillPayments(debtId: Long, monthly: Double, firstPaymentDate: LocalDate, count: Int) {
        repeat(count) { i ->
            val date = firstPaymentDate.plus(i, DateTimeUnit.MONTH)
            repos.payments.markPaid(debtId, date, monthly)
        }
    }

    /**
     * Adjust the recorded payment count to match `alreadyPaid`.
     * Adds at distributed cycle dates; removes oldest rows when shrinking.
     * Also redistributes legacy uniform-date backfills (all rows share the same
     * paidDate) so existing data created before this fix gets healed on save.
     */
    private fun reconcilePayments(debtId: Long, monthly: Double, firstPaymentDate: LocalDate, alreadyPaid: Int) {
        val history = repos.payments.historyForDebt(debtId)
        val currentCount = history.size
        val isUniformBackfill = currentCount > 1 && history.all { it.paidDate == history.first().paidDate }

        when {
            currentCount < alreadyPaid -> {
                for (i in currentCount until alreadyPaid) {
                    val date = firstPaymentDate.plus(i, DateTimeUnit.MONTH)
                    repos.payments.markPaid(debtId, date, monthly)
                }
            }
            currentCount > alreadyPaid -> {
                history.take(currentCount - alreadyPaid).forEach { repos.payments.delete(it.id) }
            }
            isUniformBackfill && alreadyPaid > 0 -> {
                history.forEach { repos.payments.delete(it.id) }
                for (i in 0 until alreadyPaid) {
                    val date = firstPaymentDate.plus(i, DateTimeUnit.MONTH)
                    repos.payments.markPaid(debtId, date, monthly)
                }
            }
        }
    }
}
