package com.snowball.ui.settings

import com.snowball.data.Repos
import com.snowball.data.model.Settings

class SettingsViewModel(private val repos: Repos) {
    fun load(): Settings = repos.settings.get()
    fun setIncome(amount: Double) { repos.settings.setIncome(amount) }
}
