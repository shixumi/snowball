package com.snowball.ui.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.ui.components.PesoText
import com.snowball.ui.theme.SnowColors

@Composable
fun DebtsScreen(
    vm: DebtsViewModel,
    onAddDebt: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    var tick by remember { mutableStateOf(0) }
    val state = remember(tick) { vm.load() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.showArchived) "Archived" else "Active",
                    style = MaterialTheme.typography.headlineLarge,
                    color = SnowColors.Frost,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (state.showArchived) "View active" else "View archived",
                    style = MaterialTheme.typography.labelMedium,
                    color = SnowColors.Ice,
                    modifier = Modifier.clickable { vm.toggleArchive(); tick++ },
                )
            }

            Spacer(Modifier.height(16.dp))

            state.categories.forEach { cat ->
                val debts = state.debtsByCategory[cat.id].orEmpty()
                if (debts.isEmpty()) return@forEach
                Text(
                    cat.name.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                    color = SnowColors.FrostDim,
                )
                Spacer(Modifier.height(8.dp))
                debts.forEach { d ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SnowColors.CardElev)
                            .clickable { onEdit(d.id) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.name, style = MaterialTheme.typography.headlineSmall, color = SnowColors.Frost)
                            Text(
                                "Day ${d.dueDay} · ${d.totalPayments} months",
                                style = MaterialTheme.typography.bodySmall,
                                color = SnowColors.FrostMute,
                            )
                        }
                        PesoText(
                            amount = d.monthlyAmount,
                            style = MaterialTheme.typography.headlineSmall,
                            pesoColor = SnowColors.FrostDim,
                            numberColor = SnowColors.Frost,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(16.dp))
            }

            if (state.debtsByCategory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No debts yet. Tap + to add your first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SnowColors.FrostDim,
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddDebt,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(56.dp)
                .clip(CircleShape),
            containerColor = SnowColors.Ice,
            contentColor = SnowColors.Night,
        ) {
            Text("+", style = MaterialTheme.typography.headlineLarge)
        }
    }
}
