package com.snowball.ui.util

import kotlinx.datetime.LocalDate

private val MONTHS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** Renders a date as "MMM YYYY" (e.g. "Aug 2027"). The day-of-month is ignored. */
fun formatMonthYear(date: LocalDate): String =
    "${MONTHS[date.monthNumber - 1]} ${date.year}"

/** Renders a date as "MMM D, YYYY" (e.g. "Jan 1, 2026"). */
fun formatLongDate(date: LocalDate): String =
    "${MONTHS[date.monthNumber - 1]} ${date.dayOfMonth}, ${date.year}"

/** The day-of-month of [date] as an ordinal: 1 -> "1st", 2 -> "2nd", 11 -> "11th", 30 -> "30th". */
fun ordinalDay(date: LocalDate): String {
    val d = date.dayOfMonth
    val suffix = if (d in 11..13) "th" else when (d % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    return "$d$suffix"
}
