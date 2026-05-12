package com.snowball.data.model

import kotlinx.datetime.LocalDate

data class Debt(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val monthlyAmount: Double,
    val totalPayments: Int,
    val dueDay: Int,
    val useLastDayOfMonth: Boolean,
    val startDate: LocalDate,
    val isArchived: Boolean,
    val notes: String?,
)
