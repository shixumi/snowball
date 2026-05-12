package com.snowball.ui.home

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.domain.DueRow
import com.snowball.ui.components.CutoffCard
import com.snowball.ui.components.PesoText
import com.snowball.ui.components.ProgressArc
import com.snowball.ui.theme.SnowColors

@Composable
fun HomeScreen(vm: HomeViewModel) {
    var tick by remember { mutableStateOf(0) }
    val state = remember(tick) { vm.load() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        CutoffCard(
            cutoff = state.cutoff,
            summary = state.summary,
            incomePerCutoff = state.income,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "PAYMENTS",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )

        Spacer(Modifier.height(8.dp))
        if (state.rows.isEmpty()) {
            EmptyHint()
        } else {
            state.rows.forEach { row ->
                PaymentRow(
                    row = row,
                    onMarkPaid = { vm.markPaid(row); tick++ },
                    onUndo = { vm.undoPayment(row); tick++ },
                )
            }
        }
    }
}

@Composable
private fun PaymentRow(row: DueRow, onMarkPaid: () -> Unit, onUndo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clickable { if (row.isPaidThisCycle) onUndo() else onMarkPaid() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressArc(
            progress = if (row.isPaidThisCycle) 1f else 0f,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.debt.name,
                style = MaterialTheme.typography.headlineSmall,
                color = if (row.isPaidThisCycle) SnowColors.FrostDim else SnowColors.Frost,
            )
            Text(
                "Due ${row.effectiveDueDate}",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = SnowColors.FrostMute,
            )
        }
        PesoText(
            amount = row.amount,
            style = MaterialTheme.typography.headlineMedium,
            pesoColor = SnowColors.FrostDim,
            numberColor = if (row.isPaidThisCycle) SnowColors.FrostMute else SnowColors.Frost,
        )
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SnowColors.Line))
}

@Composable
private fun EmptyHint() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "No payments due this cutoff yet.\nAdd debts from the Debts tab.",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = SnowColors.FrostDim,
        )
    }
}
