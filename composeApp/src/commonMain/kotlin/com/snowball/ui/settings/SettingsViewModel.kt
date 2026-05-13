package com.snowball.ui.settings

import com.snowball.data.Repos
import com.snowball.data.model.Settings
import com.snowball.platform.NotificationScheduler

class SettingsViewModel(private val repos: Repos, private val scheduler: NotificationScheduler) {

    private var _settings: Settings = repos.settings.get()

    fun load(): Settings {
        _settings = repos.settings.get()
        return _settings
    }

    private fun refresh() {
        _settings = repos.settings.get()
    }

    val notificationsEnabled: Boolean get() = _settings.notificationsEnabled
    val notificationHour: Int get() = _settings.notificationHour
    val notificationMinute: Int get() = _settings.notificationMinute

    fun setIncome(amount: Double) { repos.settings.setIncome(amount) }

    fun setNotificationsEnabled(enabled: Boolean) {
        repos.settings.setNotificationsEnabled(enabled)
        refresh()
        scheduler.schedule(enabled, notificationHour, notificationMinute)
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        repos.settings.setNotificationTime(hour, minute)
        refresh()
        if (notificationsEnabled) {
            scheduler.schedule(true, hour, minute)
        }
    }
}
