package com.snowball.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DatePeriod

fun lastDayOfMonth(year: Int, month: Int): Int {
    val firstOfNext = if (month == 12) {
        LocalDate(year + 1, 1, 1)
    } else {
        LocalDate(year, month + 1, 1)
    }
    return firstOfNext.minus(DatePeriod(days = 1)).dayOfMonth
}

fun effectiveDueDate(year: Int, month: Int, dueDay: Int, useLastDay: Boolean): LocalDate? {
    val maxDay = lastDayOfMonth(year, month)
    val day = when {
        useLastDay && dueDay >= maxDay -> maxDay
        useLastDay -> dueDay.coerceAtMost(maxDay)
        dueDay > maxDay -> return null
        else -> dueDay
    }
    return LocalDate(year, month, day)
}

fun today(zone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    Clock.System.now().toLocalDateTime(zone).date

fun previousMonth(date: LocalDate): Pair<Int, Int> =
    if (date.monthNumber == 1) (date.year - 1) to 12 else date.year to (date.monthNumber - 1)

fun nextMonth(date: LocalDate): Pair<Int, Int> =
    if (date.monthNumber == 12) (date.year + 1) to 1 else date.year to (date.monthNumber + 1)
