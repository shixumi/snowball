package com.snowball.data.model

data class Settings(
    val incomePerCutoff: Double,
    val currency: String,
    val notificationsEnabled: Boolean,
    val notificationHour: Int,
    val notificationMinute: Int,
)
