package com.snowball.ui.form

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebtFormStateValidationTest {

    private fun validState() = DebtFormState(
        name = "Sloan",
        categoryId = 1L,
        monthlyAmount = "1500",
        totalPayments = "12",
        paymentsAlreadyMade = "0",
        dueDay = "10",
    )

    @Test
    fun fullyValidStateIsValid() {
        assertTrue(validState().isValid())
    }

    @Test
    fun blankNameInvalid() {
        assertFalse(validState().copy(name = "").isValid())
        assertFalse(validState().copy(name = "   ").isValid())
    }

    @Test
    fun missingCategoryInvalid() {
        assertFalse(validState().copy(categoryId = null).isValid())
    }

    @Test
    fun amountMustBePositive() {
        assertFalse(validState().copy(monthlyAmount = "").isValid())
        assertFalse(validState().copy(monthlyAmount = "0").isValid())
        assertFalse(validState().copy(monthlyAmount = "-50").isValid())
        assertFalse(validState().copy(monthlyAmount = "abc").isValid())
        assertTrue(validState().copy(monthlyAmount = "0.01").isValid())
    }

    @Test
    fun totalPaymentsInRange1to600() {
        assertFalse(validState().copy(totalPayments = "").isValid())
        assertFalse(validState().copy(totalPayments = "0").isValid())
        assertFalse(validState().copy(totalPayments = "601").isValid())
        assertTrue(validState().copy(totalPayments = "1").isValid())
        assertTrue(validState().copy(totalPayments = "600").isValid())
    }

    @Test
    fun dueDayInRange1to31() {
        assertFalse(validState().copy(dueDay = "0").isValid())
        assertFalse(validState().copy(dueDay = "32").isValid())
        assertFalse(validState().copy(dueDay = "").isValid())
        assertTrue(validState().copy(dueDay = "1").isValid())
        assertTrue(validState().copy(dueDay = "31").isValid())
    }

    @Test
    fun paymentsAlreadyMadeBoundedByTotal() {
        assertFalse(validState().copy(totalPayments = "12", paymentsAlreadyMade = "13").isValid())
        assertFalse(validState().copy(paymentsAlreadyMade = "-1").isValid())
        assertTrue(validState().copy(totalPayments = "12", paymentsAlreadyMade = "12").isValid())
        assertTrue(validState().copy(paymentsAlreadyMade = "").isValid())  // empty == 0
    }

    @Test
    fun perFieldValidatorsMatchOverallValidity() {
        val s = validState().copy(monthlyAmount = "", dueDay = "99")
        assertFalse(s.isValid())
        assertTrue(s.isNameValid())
        assertTrue(s.isCategoryValid())
        assertFalse(s.isMonthlyAmountValid())
        assertTrue(s.isTotalPaymentsValid())
        assertFalse(s.isDueDayValid())
        assertTrue(s.isPaymentsAlreadyMadeValid())
    }
}
