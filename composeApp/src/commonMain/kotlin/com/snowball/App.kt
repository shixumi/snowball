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
import com.snowball.ui.debts.DebtsScreen
import com.snowball.ui.debts.DebtsViewModel
import com.snowball.ui.form.DebtFormScreen
import com.snowball.ui.form.DebtFormViewModel
import com.snowball.ui.home.HomeScreen
import com.snowball.ui.home.HomeViewModel
import com.snowball.ui.nav.BottomNav
import com.snowball.ui.nav.Tab
import com.snowball.ui.theme.SnowballTheme

sealed interface Route {
    data object Tabs : Route
    data class Form(val existingDebtId: Long?) : Route
}

@Composable
fun App(repos: Repos) {
    SnowballTheme {
        var route by remember { mutableStateOf<Route>(Route.Tabs) }
        var tab by remember { mutableStateOf(Tab.Home) }
        var refreshKey by remember { mutableStateOf(0) }

        val homeVm = remember(refreshKey) { HomeViewModel(repos) }
        val debtsVm = remember(refreshKey) { DebtsViewModel(repos) }

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (val r = route) {
                    is Route.Tabs -> {
                        when (tab) {
                            Tab.Home -> HomeScreen(homeVm)
                            Tab.Debts -> DebtsScreen(
                                vm = debtsVm,
                                onAddDebt = { route = Route.Form(null) },
                                onEdit = { id -> route = Route.Form(id) },
                            )
                            Tab.Settings -> PlaceholderScreen("Settings")
                        }
                    }
                    is Route.Form -> {
                        val existing = r.existingDebtId?.let { repos.debts.byId(it) }
                        val formVm = remember(r.existingDebtId) { DebtFormViewModel(repos, existing) }
                        DebtFormScreen(
                            vm = formVm,
                            onCancel = { route = Route.Tabs },
                            onSaved = { route = Route.Tabs; refreshKey++ },
                        )
                    }
                }
            }
            if (route is Route.Tabs) {
                BottomNav(selected = tab, onSelect = { tab = it })
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(name, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
    }
}
