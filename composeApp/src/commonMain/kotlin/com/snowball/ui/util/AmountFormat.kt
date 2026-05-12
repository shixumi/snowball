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
    val fraction = (fractionalPart * 100).toInt()
    return if (fraction == 0) whole.toString()
    else "$whole.${fraction.toString().padStart(2, '0')}"
}
