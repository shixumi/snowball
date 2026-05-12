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
    fun threeDecimalsRoundToTwo() {
        // The +0.5 trick performs round-half-up at the hundredths place;
        // 1500.555 is stored as ~1500.5550000000002, so it rounds up to 1500.56.
        assertEquals("1500.56", 1500.555.toFormFieldString())
    }

    @Test
    fun floatingPointEdgeCaseRendersCorrectly() {
        // 1500.56 is stored as ~1500.5599999999999 in IEEE 754;
        // naive truncation would yield "1500.55". The +0.5 in the
        // fractional calculation fixes this.
        assertEquals("1500.56", 1500.56.toFormFieldString())
    }
}
