package com.snowball.ui.util

import kotlin.math.abs

/**
 * Renders a Double as a string for a numeric text field.
 * Whole values render with no decimal (1500.0 -> "1500").
 * Non-whole values render with exactly two decimals (1500.5 -> "1500.50").
 * Zero renders as the empty string so an empty field doesn't display "0".
 */
fun Double.toFormFieldString(): String {
    if (this == 0.0) return ""
    val whole = this.toLong()
    val fractionalPart = abs(this) - abs(whole)
    val fraction = (fractionalPart * 100 + 0.5).toInt()
    return if (fraction == 0) whole.toString()
    else "$whole.${fraction.toString().padStart(2, '0')}"
}

/**
 * Renders a Double as a separator-grouped amount with at most 2 decimal places.
 * Whole values render with no decimals (1500.0 -> "1,500").
 * Non-whole values render with two decimals (1500.5 -> "1,500.50").
 * Zero renders as "0" (caller decides whether to prefix or hide).
 * Uses round-half-up at the hundredths place to absorb IEEE 754 representation errors
 * (1500.56 is stored as ~1500.5599999... ; without the +0.5 we'd truncate to "1,500.55").
 * Does NOT include the ₱ glyph — caller adds the currency prefix where needed.
 */
fun formatAmountWithSeparators(amount: Double): String {
    val whole = amount.toLong()
    val fraction = ((abs(amount) - abs(whole)) * 100 + 0.5).toInt()
    val absWholeStr = abs(whole).toString()
    val grouped = absWholeStr
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
    val signedGrouped = if (whole < 0) "-$grouped" else grouped
    return if (fraction == 0) signedGrouped
    else "$signedGrouped.${fraction.toString().padStart(2, '0')}"
}
