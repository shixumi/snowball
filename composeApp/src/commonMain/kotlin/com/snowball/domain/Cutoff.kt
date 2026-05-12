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
            Payday.FIFTEENTH -> LocalDate(year, month, 15)
            Payday.THIRTIETH -> {
                val (ny, nm) = nextYearMonth()
                LocalDate(ny, nm, 1)
            }
        }

    val windowEnd: LocalDate
        get() = when (payday) {
            Payday.FIFTEENTH -> LocalDate(year, month, minOf(30, lastDayOfMonth(year, month)))
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
    return if (day in 1..14) {
        val (py, pm) = if (today.monthNumber == 1) (today.year - 1) to 12 else today.year to (today.monthNumber - 1)
        Cutoff(py, pm, Payday.THIRTIETH)
    } else {
        Cutoff(today.year, today.monthNumber, Payday.FIFTEENTH)
    }
}

fun nextCutoff(today: LocalDate = today()): Cutoff = currentCutoff(today).next()
