package com.snowball.ui.misc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.snowball.data.Repos
import com.snowball.data.model.CategoryBehavior
import com.snowball.domain.today
import kotlinx.datetime.LocalDate

data class MiscFormState(
    val name: String = "",
    val amount: String = "",
    val datePaid: LocalDate = today(),
    val datePaidText: String = datePaid.toString(),
    val notes: String = "",
)

fun MiscFormState.isNameValid(): Boolean = name.trim().isNotEmpty()
fun MiscFormState.isAmountValid(): Boolean = (amount.toDoubleOrNull() ?: 0.0) > 0.0
fun MiscFormState.isDatePaidValid(): Boolean =
    runCatching { LocalDate.parse(datePaidText) }.isSuccess

fun MiscFormState.isValid(): Boolean =
    isNameValid() && isAmountValid() && isDatePaidValid()

class MiscFormViewModel(private val repos: Repos) {
    var state: MiscFormState by mutableStateOf(MiscFormState())
        private set

    val isValid: Boolean get() = state.isValid()

    fun update(transform: (MiscFormState) -> MiscFormState) { state = transform(state) }

    /** Creates the MISC debt + the single payment + auto-archives. Returns true on success. */
    fun save(): Boolean {
        if (!isValid) return false
        val miscCategory = repos.categories.all()
            .firstOrNull { it.behavior == CategoryBehavior.LEDGER }
            ?: return false
        val amount = state.amount.toDouble()
        val date = LocalDate.parse(state.datePaidText)
        val name = state.name.trim()

        repos.debts.add(
            name = name,
            categoryId = miscCategory.id,
            monthlyAmount = amount,
            totalPayments = 1,
            dueDay = 1,
            useLastDayOfMonth = false,
            startDate = date,
            notes = state.notes.ifBlank { null },
        )
        val newId = repos.debts.all().first().id
        repos.payments.markPaid(newId, date, amount)
        repos.debts.setArchived(newId, true)
        return true
    }
}
