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
    val startDate: LocalDate,         // loan origination — informational
    val firstPaymentDate: LocalDate,  // when cycle 1 falls due (drives all schedule math)
    val isArchived: Boolean,
    val notes: String?,
)
