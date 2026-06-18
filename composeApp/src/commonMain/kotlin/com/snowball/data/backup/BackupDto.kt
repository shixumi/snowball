package com.snowball.data.backup

import kotlinx.serialization.Serializable

/**
 * Wire format for a full Snowball backup. DTOs mirror the database rows (all
 * primitive fields, dates as ISO strings) so the format is decoupled from the
 * domain models and needs no custom serializers. See the design doc:
 * docs/superpowers/specs/2026-06-19-data-export-import-design.md
 */
@Serializable
data class BackupFile(
    val formatVersion: Int,
    val dbVersion: Long,
    val exportedAt: Long,
    val categories: List<CategoryDto>,
    val debts: List<DebtDto>,
    val payments: List<PaymentDto>,
    val settings: SettingsDto,
)

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String,
    val isSystem: Boolean,
    val behavior: String,
    val iconKey: String,
    val createdAt: Long,
)

@Serializable
data class DebtDto(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val monthlyAmount: Double,
    val totalPayments: Long,
    val dueDay: Long,
    val useLastDayOfMonth: Boolean,
    val startDate: String,
    val firstPaymentDate: String,
    val isArchived: Boolean,
    val notes: String?,
    val createdAt: Long,
)

@Serializable
data class PaymentDto(
    val id: Long,
    val debtId: Long,
    val paidDate: String,
    val amount: Double,
    val createdAt: Long,
)

@Serializable
data class SettingsDto(
    val incomePerCutoff: Double,
    val currency: String,
    val notificationsEnabled: Boolean,
    val notificationHour: Long,
    val notificationMinute: Long,
    val firstLaunchSeen: Boolean,
    val swipeCoachmarkSeen: Boolean,
    val paidAheadKey: String,
)
