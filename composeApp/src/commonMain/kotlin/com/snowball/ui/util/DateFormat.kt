package com.snowball.ui.util

import kotlinx.datetime.LocalDate

private val MONTHS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** Renders a date as "MMM YYYY" (e.g. "Aug 2027"). The day-of-month is ignored. */
fun formatMonthYear(date: LocalDate): String =
    "${MONTHS[date.monthNumber - 1]} ${date.year}"
