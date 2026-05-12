package com.snowball.data.repo

import com.snowball.data.model.Settings
import com.snowball.db.SnowballDb

class SettingsRepository(private val db: SnowballDb) {

    fun get(): Settings {
        val row = db.settingsQueries.select().executeAsOne()
        return Settings(
            incomePerCutoff = row.incomePerCutoff,
            currency = row.currency,
            notificationsEnabled = row.notificationsEnabled == 1L,
            notificationHour = row.notificationHour.toInt(),
            notificationMinute = row.notificationMinute.toInt(),
        )
    }

    fun setIncome(amount: Double) {
        db.settingsQueries.setIncome(amount)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        db.settingsQueries.setNotificationsEnabled(if (enabled) 1L else 0L)
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        db.settingsQueries.setNotificationTime(hour.toLong(), minute.toLong())
    }
}
