package com.snowball.ui.misc

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MiscFormStateValidationTest {

    private fun validState() = MiscFormState(
        name = "Snack run",
        amount = "350",
        datePaidText = "2026-05-13",
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
    fun amountMustBePositive() {
        assertFalse(validState().copy(amount = "").isValid())
        assertFalse(validState().copy(amount = "0").isValid())
        assertFalse(validState().copy(amount = "-50").isValid())
        assertFalse(validState().copy(amount = "abc").isValid())
        assertTrue(validState().copy(amount = "0.01").isValid())
    }

    @Test
    fun datePaidMustParse() {
        assertFalse(validState().copy(datePaidText = "not a date").isValid())
        assertFalse(validState().copy(datePaidText = "").isValid())
        assertTrue(validState().copy(datePaidText = "2026-12-31").isValid())
    }

    @Test
    fun perFieldValidatorsExist() {
        val s = validState().copy(name = "", amount = "0")
        assertFalse(s.isValid())
        assertFalse(s.isNameValid())
        assertFalse(s.isAmountValid())
        assertTrue(s.isDatePaidValid())
    }
}
