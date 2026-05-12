package com.snowball.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.domain.CutoffForecast
import com.snowball.domain.SnapshotStats
import com.snowball.ui.components.PesoText
import com.snowball.ui.components.cutoffRangeLabel
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.formatAmountWithSeparators
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(vm: InsightsViewModel) {
    val state = remember { vm.load() }

    Scaffold(
        containerColor = SnowColors.Night,
        topBar = {
            TopAppBar(
                title = { Text("Insights", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SnowColors.Night,
                    titleContentColor = SnowColors.Frost,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            SnapshotCard(stats = state.snapshot)
            Spacer(Modifier.height(24.dp))
            Text(
                "UPCOMING (NEXT 6 MONTHS)",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                color = SnowColors.FrostDim,
            )
            Spacer(Modifier.height(12.dp))
            if (state.forecast.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No upcoming debts in your forecast window.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = SnowColors.FrostDim,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.forecast.forEach { f ->
                        ForecastRow(f)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SnapshotCard(stats: SnapshotStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(SnowColors.CardElev)
            .border(1.dp, SnowColors.LineStrong, RoundedCornerShape(28.dp))
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(
            "WHAT YOU OWE",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        if (stats.debtCount == 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Nothing right now.",
                style = MaterialTheme.typography.bodyLarge,
                color = SnowColors.Frost,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Add a debt from the Debts tab to start tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = SnowColors.FrostMute,
            )
        } else {
            Spacer(Modifier.height(12.dp))
            PesoText(
                amount = stats.remaining,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.W300),
                pesoColor = SnowColors.FrostMute,
                numberColor = SnowColors.Ice,
                animate = true,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "across ${stats.debtCount} ${if (stats.debtCount == 1) "debt" else "debts"}",
                style = MaterialTheme.typography.labelMedium,
                color = SnowColors.FrostMute,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "₱${formatAmountWithSeparators(stats.monthlyBurden)}/mo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnowColors.FrostMute,
                )
                Text(
                    " · ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnowColors.FrostMute,
                )
                val coverageText =
                    if (stats.coveragePercent == null) "— of monthly"
                    else "${stats.coveragePercent}% of monthly"
                Text(
                    coverageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnowColors.FrostMute,
                )
            }
        }
    }
}

@Composable
private fun ForecastRow(f: CutoffForecast) {
    val isShort = !f.isAllClear && f.leftOver < 0
    val borderColor = if (isShort) SnowColors.Ember.copy(alpha = 0.4f) else SnowColors.LineStrong
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SnowColors.NightElev)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            cutoffRangeLabel(f.cutoff),
            style = MaterialTheme.typography.bodyLarge,
            color = SnowColors.Frost,
            modifier = Modifier.weight(1f),
        )
        if (f.isAllClear) {
            Text(
                "All clear ✓",
                style = MaterialTheme.typography.bodyLarge,
                color = SnowColors.Ice,
            )
        } else {
            Column(horizontalAlignment = Alignment.End) {
                PesoText(
                    amount = f.dueTotal,
                    style = MaterialTheme.typography.headlineSmall,
                    pesoColor = SnowColors.FrostDim,
                    numberColor = SnowColors.Frost,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (isShort) "SHORT BY ₱${formatAmountWithSeparators(abs(f.leftOver))}"
                    else "₱${formatAmountWithSeparators(f.leftOver)} left",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isShort) SnowColors.Ember else SnowColors.FrostMute,
                )
            }
        }
    }
}
