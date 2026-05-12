package com.snowball.data.model

import kotlinx.datetime.LocalDate

data class Payment(
    val id: Long,
    val debtId: Long,
    val paidDate: LocalDate,
    val amount: Double,
)
