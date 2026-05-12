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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
        Spacer(Modifier.height(2.dp))
        Text(
            "Swipe left to mark paid · swipe right to undo",
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = SnowColors.FrostDeep,
        )

        Spacer(Modifier.height(8.dp))
        if (state.rows.isEmpty()) {
            EmptyHint()
        } else {
            state.rows.forEach { row ->
                key(row.debt.id) {
                    SwipeablePaymentRow(
                        row = row,
                        onMarkPaid = { vm.markPaid(row); tick++ },
                        onUndo = { vm.undoPayment(row); tick++ },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeablePaymentRow(row: DueRow, onMarkPaid: () -> Unit, onUndo: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    if (!row.isPaidThisCycle) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMarkPaid()
                    }
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (row.isPaidThisCycle) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onUndo()
                    }
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground(dismissState.dismissDirection, row.isPaidThisCycle) },
        enableDismissFromEndToStart = !row.isPaidThisCycle,
        enableDismissFromStartToEnd = row.isPaidThisCycle,
    ) {
        PaymentRowContent(
            row = row,
            onClick = { if (row.isPaidThisCycle) onUndo() else onMarkPaid() },
        )
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SnowColors.Line))
}

@Composable
private fun PaymentRowContent(row: DueRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SnowColors.Night)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
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
}

@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue, isPaid: Boolean) {
    when (direction) {
        SwipeToDismissBoxValue.EndToStart -> if (!isPaid) {
            ActionBackground(
                color = SnowColors.Green,
                icon = Icons.Outlined.Check,
                label = "MARK PAID",
                alignment = Alignment.CenterEnd,
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(SnowColors.Night))
        }
        SwipeToDismissBoxValue.StartToEnd -> if (isPaid) {
            ActionBackground(
                color = SnowColors.Champagne,
                icon = Icons.Outlined.Undo,
                label = "UNDO",
                alignment = Alignment.CenterStart,
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(SnowColors.Night))
        }
        SwipeToDismissBoxValue.Settled -> {
            Box(modifier = Modifier.fillMaxSize().background(SnowColors.Night))
        }
    }
}

@Composable
private fun ActionBackground(
    color: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    alignment: Alignment,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 24.dp),
        contentAlignment = alignment,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (alignment == Alignment.CenterEnd) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = color,
                )
                Spacer(Modifier.width(10.dp))
                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
            } else {
                Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = color,
                )
            }
        }
    }
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
