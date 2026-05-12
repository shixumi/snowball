package com.snowball.ui.util

import kotlin.test.Test
import kotlin.test.assertEquals

class AmountFormatSeparatorsTest {

    @Test
    fun zeroRendersAsZero() {
        assertEquals("0", formatAmountWithSeparators(0.0))
    }

    @Test
    fun smallWholeNumberNoSeparator() {
        assertEquals("500", formatAmountWithSeparators(500.0))
    }

    @Test
    fun thousandsInsertSeparator() {
        assertEquals("1,500", formatAmountWithSeparators(1500.0))
    }

    @Test
    fun millionsInsertMultipleSeparators() {
        assertEquals("1,500,000", formatAmountWithSeparators(1_500_000.0))
    }

    @Test
    fun nonWholeAddsTwoDecimals() {
        assertEquals("1,500.50", formatAmountWithSeparators(1500.5))
    }

    @Test
    fun floatingPointEdgeCase() {
        // 1500.56 is stored as ~1500.5599999999999; the +0.5 rounding ensures we get 56.
        assertEquals("1,500.56", formatAmountWithSeparators(1500.56))
    }

    @Test
    fun negativeRendersWithLeadingSign() {
        assertEquals("-2,250.50", formatAmountWithSeparators(-2250.5))
    }
}
