package com.snowball

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.snowball.data.Repos
import com.snowball.ui.home.HomeScreen
import com.snowball.ui.home.HomeViewModel
import com.snowball.ui.nav.BottomNav
import com.snowball.ui.nav.Tab
import com.snowball.ui.theme.SnowballTheme

@Composable
fun App(repos: Repos) {
    SnowballTheme {
        var tab by remember { mutableStateOf(Tab.Home) }
        val homeVm = remember { HomeViewModel(repos) }
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (tab) {
                    Tab.Home -> HomeScreen(homeVm)
                    Tab.Debts -> PlaceholderScreen("Debts")
                    Tab.Settings -> PlaceholderScreen("Settings")
                }
            }
            BottomNav(selected = tab, onSelect = { tab = it })
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            name,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
