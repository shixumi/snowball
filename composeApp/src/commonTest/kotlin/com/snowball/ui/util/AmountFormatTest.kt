package com.snowball.ui.util

import kotlin.test.Test
import kotlin.test.assertEquals

class AmountFormatTest {

    @Test
    fun zeroRendersAsBlank() {
        assertEquals("", 0.0.toFormFieldString())
    }

    @Test
    fun wholeNumberDropsDecimal() {
        assertEquals("1500", 1500.0.toFormFieldString())
    }

    @Test
    fun largeWholeNumberDropsDecimal() {
        assertEquals("9999999", 9_999_999.0.toFormFieldString())
    }

    @Test
    fun twoDecimalRoundsCorrectly() {
        assertEquals("1500.50", 1500.50.toFormFieldString())
    }

    @Test
    fun oneDecimalPadsTo2() {
        assertEquals("1500.50", 1500.5.toFormFieldString())
    }

    @Test
    fun threeDecimalsRoundDown() {
        assertEquals("1500.55", 1500.555.toFormFieldString())
    }
}
