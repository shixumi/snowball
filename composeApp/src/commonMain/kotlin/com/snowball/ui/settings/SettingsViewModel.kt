package com.snowball.ui.settings

import com.snowball.data.Repos
import com.snowball.data.backup.ImportResult
import com.snowball.data.model.Settings
import com.snowball.platform.NotificationScheduler
import kotlinx.datetime.Clock

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

    fun exportJson(): String =
        repos.backup.export(exportedAt = Clock.System.now().toEpochMilliseconds())

    /** Replaces all data with the backup. On success the caller must refresh the app. */
    fun import(json: String): ImportResult {
        val result = repos.backup.import(json)
        if (result is ImportResult.Success) {
            refresh()
            // Imported settings decide notifications; re-sync the OS schedule.
            scheduler.cancel()
            if (notificationsEnabled) scheduler.schedule(true, notificationHour, notificationMinute)
        }
        return result
    }
}
