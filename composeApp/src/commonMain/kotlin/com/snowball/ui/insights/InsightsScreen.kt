package com.snowball.ui.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.domain.CutoffForecast
import com.snowball.domain.SnapshotStats
import com.snowball.ui.components.PesoText
import com.snowball.ui.components.ScreenHeader
import com.snowball.ui.components.StaggeredItem
import com.snowball.ui.components.cutoffRangeLabel
import com.snowball.ui.components.icon
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.formatAmountWithSeparators
import kotlin.math.abs

@Composable
fun InsightsScreen(vm: InsightsViewModel) {
    val state = remember { vm.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        ScreenHeader("Insights")
        Spacer(Modifier.height(16.dp))
        StaggeredItem(index = 0) {
            SnapshotCard(stats = state.snapshot)
        }
        Spacer(Modifier.height(24.dp))
        var timelineExpanded by remember { mutableStateOf(false) }
        val chevronRotation by animateFloatAsState(
            targetValue = if (timelineExpanded) 180f else 0f,
            animationSpec = tween(200),
            label = "timelineChevron",
        )
        StaggeredItem(index = 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { timelineExpanded = !timelineExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "PAYOFF TIMELINE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp),
                    color = SnowColors.FrostDim,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (timelineExpanded) "Collapse payoff timeline" else "Expand payoff timeline",
                    tint = SnowColors.FrostDim,
                    modifier = Modifier.size(20.dp).rotate(chevronRotation),
                )
            }
        }
        AnimatedVisibility(visible = timelineExpanded) {
            Column {
                Spacer(Modifier.height(8.dp))
                if (state.payoffTimeline.isEmpty()) {
                    Text(
                        "No active debts — you're free.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = SnowColors.FrostMute,
                    )
                } else {
                    state.payoffTimeline.forEach { row ->
                        PayoffTimelineRow(row)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "UPCOMING (NEXT 6 MONTHS)",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(12.dp))
        if (state.forecast.isEmpty()) {
            var emptyVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { emptyVisible = true }
            AnimatedVisibility(
                visible = emptyVisible,
                enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 },
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Nothing on the horizon — you're caught up.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = SnowColors.FrostDim,
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.forecast.forEachIndexed { i, f ->
                    StaggeredItem(index = i + 1) {
                        ForecastRow(f)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
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
private fun PayoffTimelineRow(row: PayoffRow) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = row.category.icon(),
            contentDescription = null,
            tint = SnowColors.FrostDim,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.debt.name,
                style = MaterialTheme.typography.bodyLarge,
                color = SnowColors.Frost,
            )
            Text(
                "${"%.2f".format(row.monthlyAmount)}/mo",
                style = MaterialTheme.typography.bodySmall,
                color = SnowColors.FrostMute,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                row.endDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() } + " ${row.endDate.year}",
                style = MaterialTheme.typography.bodyMedium,
                color = SnowColors.Frost,
            )
            Text(
                row.endDate.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = SnowColors.FrostMute,
            )
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
            var allClearVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { allClearVisible = true }
            val allClearScale by animateFloatAsState(
                targetValue = if (allClearVisible) 1f else 0.8f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                label = "allClearScale",
            )
            val allClearAlpha by animateFloatAsState(
                targetValue = if (allClearVisible) 1f else 0f,
                animationSpec = tween(400),
                label = "allClearAlpha",
            )
            Row(
                modifier = Modifier
                    .scale(allClearScale)
                    .alpha(allClearAlpha),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AcUnit,
                    contentDescription = null,
                    tint = SnowColors.Ice,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "All clear",
                    style = MaterialTheme.typography.bodyLarge,
                    color = SnowColors.Ice,
                )
            }
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
