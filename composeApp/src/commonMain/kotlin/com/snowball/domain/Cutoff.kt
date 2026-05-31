package com.snowball.domain

import kotlinx.datetime.LocalDate

enum class Payday { FIFTEENTH, THIRTIETH }

data class Cutoff(
    val year: Int,
    val month: Int,
    val payday: Payday,
) {
    val payDate: LocalDate
        get() = when (payday) {
            Payday.FIFTEENTH -> LocalDate(year, month, 15)
            Payday.THIRTIETH -> LocalDate(year, month, minOf(30, lastDayOfMonth(year, month)))
        }

    val windowStart: LocalDate
        get() = when (payday) {
            // The 15th paycheck covers from the 15th.
            Payday.FIFTEENTH -> LocalDate(year, month, 15)
            // The 30th paycheck covers from the 30th (or the last day in shorter months).
            Payday.THIRTIETH -> LocalDate(year, month, minOf(30, lastDayOfMonth(year, month)))
        }

    val windowEnd: LocalDate
        get() = when (payday) {
            // The 15th cutoff ends the day before the 30th payday (29th, or earlier in
            // shorter months — e.g. the 27th in a 28-day February).
            Payday.FIFTEENTH -> LocalDate(year, month, minOf(30, lastDayOfMonth(year, month)) - 1)
            // The 30th cutoff runs through the 14th of the next month.
            Payday.THIRTIETH -> {
                val (ny, nm) = nextYearMonth()
                LocalDate(ny, nm, 14)
            }
        }

    fun next(): Cutoff = when (payday) {
        Payday.FIFTEENTH -> Cutoff(year, month, Payday.THIRTIETH)
        Payday.THIRTIETH -> {
            val (ny, nm) = nextYearMonth()
            Cutoff(ny, nm, Payday.FIFTEENTH)
        }
    }

    fun previous(): Cutoff = when (payday) {
        Payday.FIFTEENTH -> {
            val (py, pm) = if (month == 1) (year - 1) to 12 else year to (month - 1)
            Cutoff(py, pm, Payday.THIRTIETH)
        }
        Payday.THIRTIETH -> Cutoff(year, month, Payday.FIFTEENTH)
    }

    private fun nextYearMonth(): Pair<Int, Int> =
        if (month == 12) (year + 1) to 1 else year to (month + 1)
}

fun currentCutoff(today: LocalDate): Cutoff {
    val day = today.dayOfMonth
    val thirtiethPayday = minOf(30, lastDayOfMonth(today.year, today.monthNumber))
    return when {
        // Days 1-14 belong to the previous month's 30th cutoff (which runs through the 14th).
        day in 1..14 -> {
            val (py, pm) = if (today.monthNumber == 1) (today.year - 1) to 12 else today.year to (today.monthNumber - 1)
            Cutoff(py, pm, Payday.THIRTIETH)
        }
        // Days 15 up to (but not including) the 30th payday belong to the 15th cutoff.
        day < thirtiethPayday -> Cutoff(today.year, today.monthNumber, Payday.FIFTEENTH)
        // The 30th payday onward (30th, 31st, or the last day in shorter months) belongs to the 30th cutoff.
        else -> Cutoff(today.year, today.monthNumber, Payday.THIRTIETH)
    }
}

fun nextCutoff(today: LocalDate = today()): Cutoff = currentCutoff(today).next()
