package com.snowball

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.snowball.data.Repos
import com.snowball.ui.categories.CategoryManagementScreen
import com.snowball.ui.categories.CategoryManagementViewModel
import com.snowball.ui.debts.DebtsScreen
import com.snowball.ui.debts.DebtsViewModel
import com.snowball.ui.detail.DebtDetailScreen
import com.snowball.ui.detail.DebtDetailViewModel
import com.snowball.ui.insights.InsightsScreen
import com.snowball.ui.insights.InsightsViewModel
import com.snowball.ui.form.DebtFormScreen
import com.snowball.ui.form.DebtFormViewModel
import com.snowball.ui.home.HomeScreen
import com.snowball.ui.home.HomeViewModel
import com.snowball.ui.misc.MiscFormScreen
import com.snowball.ui.misc.MiscFormViewModel
import com.snowball.ui.nav.BottomNav
import com.snowball.ui.nav.SystemBackHandler
import com.snowball.ui.nav.Tab
import com.snowball.ui.settings.SettingsScreen
import com.snowball.ui.settings.SettingsViewModel
import com.snowball.ui.theme.SnowballTheme

sealed interface Route {
    data object Tabs : Route
    data class Form(val existingDebtId: Long?) : Route
    data class DebtDetail(val debtId: Long) : Route
    data object MiscForm : Route
    data object CategoryManagement : Route
}

@Composable
fun App(repos: Repos) {
    SnowballTheme {
        var route by remember { mutableStateOf<Route>(Route.Tabs) }
        var tab by remember { mutableStateOf(Tab.Home) }
        var refreshKey by remember { mutableStateOf(0) }

        val homeVm = remember(refreshKey) { HomeViewModel(repos) }
        val debtsVm = remember(refreshKey) { DebtsViewModel(repos) }

        // Intercept system back to navigate within the app instead of exiting.
        SystemBackHandler(enabled = route !is Route.Tabs) {
            route = Route.Tabs
            refreshKey++
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            val isTabs = route is Route.Tabs
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .then(if (isTabs) Modifier else Modifier.navigationBarsPadding())
            ) {
                when (val r = route) {
                    is Route.Tabs -> {
                        when (tab) {
                            Tab.Home -> HomeScreen(homeVm)
                            Tab.Debts -> DebtsScreen(
                                vm = debtsVm,
                                onAddDebt = { route = Route.Form(null) },
                                onAddMisc = { route = Route.MiscForm },
                                onOpenDebt = { id -> route = Route.DebtDetail(id) },
                            )
                            Tab.Insights -> {
                                val insightsVm = remember(refreshKey) { InsightsViewModel(repos) }
                                InsightsScreen(insightsVm)
                            }
                            Tab.Settings -> {
                                val settingsVm = remember(refreshKey) { SettingsViewModel(repos) }
                                SettingsScreen(
                                    vm = settingsVm,
                                    onManageCategories = { route = Route.CategoryManagement },
                                )
                            }
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
                    is Route.DebtDetail -> {
                        val detailVm = remember(r.debtId, refreshKey) { DebtDetailViewModel(repos, r.debtId) }
                        DebtDetailScreen(
                            vm = detailVm,
                            onBack = { route = Route.Tabs; refreshKey++ },
                            onEdit = { id -> route = Route.Form(id) },
                        )
                    }
                    is Route.MiscForm -> {
                        val miscVm = remember { MiscFormViewModel(repos) }
                        MiscFormScreen(
                            vm = miscVm,
                            onCancel = { route = Route.Tabs },
                            onSaved = { route = Route.Tabs; refreshKey++ },
                        )
                    }
                    is Route.CategoryManagement -> {
                        val catVm = remember(refreshKey) { CategoryManagementViewModel(repos) }
                        CategoryManagementScreen(
                            vm = catVm,
                            onBack = { route = Route.Tabs; refreshKey++ },
                        )
                    }
                }
            }
            if (isTabs) {
                BottomNav(selected = tab, onSelect = { tab = it })
            }
        }
    }
}
