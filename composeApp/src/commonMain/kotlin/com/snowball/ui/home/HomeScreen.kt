package com.snowball.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.domain.DueRow
import com.snowball.domain.OverdueInfo
import com.snowball.ui.components.CutoffCard
import com.snowball.ui.components.JourneyCard
import com.snowball.ui.components.PesoText
import com.snowball.ui.components.ProgressArc
import com.snowball.ui.components.StaggeredItem
import com.snowball.ui.components.UpNextCard
import com.snowball.ui.components.celebratePaid
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.formatAmountWithSeparators
import com.snowball.ui.util.formatLongDate

@Composable
fun HomeScreen(vm: HomeViewModel) {
    var tick by remember { mutableStateOf(0) }
    val state = remember(tick) { vm.load() }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            tick++
        }
    }

    var pendingCatchUp by remember { mutableStateOf<OverdueInfo?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AcUnit,
                contentDescription = null,
                tint = SnowColors.Frost,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Snowball",
                style = MaterialTheme.typography.titleLarge,
                color = SnowColors.Frost,
            )
        }
        Spacer(Modifier.height(16.dp))
        CutoffCard(
            cutoff = state.cutoff,
            summary = state.summary,
            incomePerCutoff = state.income,
        )

        if (state.nextRows.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            var upNextExpanded by remember { mutableStateOf(false) }
            UpNextCard(
                cutoff = state.nextCutoff,
                rows = state.nextRows,
                total = state.nextTotal,
                isExpanded = upNextExpanded,
                onToggle = { upNextExpanded = !upNextExpanded },
            )
        }

        AnimatedVisibility(
            visible = state.overdue.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                Spacer(Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(SnowColors.NightElev)
                        .border(1.dp, SnowColors.Ember.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Text(
                        "OVERDUE",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                        color = SnowColors.Ember,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap to mark caught up",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = SnowColors.FrostMute,
                    )
                    Spacer(Modifier.height(12.dp))
                    state.overdue.forEach { info ->
                        OverdueRow(info = info, onClick = { pendingCatchUp = info })
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "PAYMENTS",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Swipe left to mark paid · swipe right to undo",
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = SnowColors.FrostMute,
        )

        Spacer(Modifier.height(8.dp))
        if (state.rows.isEmpty()) {
            val message = when {
                state.income == 0.0 -> "Set your income in Settings to get started."
                else -> "No payments due this cutoff yet.\nAdd debts from the Debts tab."
            }
            var emptyVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { emptyVisible = true }
            AnimatedVisibility(
                visible = emptyVisible,
                enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 },
            ) {
                EmptyHint(message)
            }
        } else {
            state.rows.forEachIndexed { i, row ->
                key(row.debt.id) {
                    StaggeredItem(index = i) {
                        SwipeablePaymentRow(
                            row = row,
                            onMarkPaid = { vm.markPaid(row); tick++ },
                            onUndo = { vm.undoPayment(row); tick++ },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (state.journey != null) {
            Spacer(Modifier.height(24.dp))
            JourneyCard(stats = state.journey)
        }
    }

    val catchUpTarget = pendingCatchUp
    if (catchUpTarget != null) {
        AlertDialog(
            onDismissRequest = { pendingCatchUp = null },
            title = {
                Text(
                    "Catch up on ${catchUpTarget.debt.name}?",
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    "Records ${catchUpTarget.missedCycles} missed payment${if (catchUpTarget.missedCycles == 1) "" else "s"} totaling ₱${formatAmountWithSeparators(catchUpTarget.missedAmount)}. First missed due date: ${formatLongDate(catchUpTarget.firstMissedDueDate)}."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.catchUpOverdue(catchUpTarget)
                    pendingCatchUp = null
                    tick++
                }) { Text("Catch up", color = SnowColors.Ember) }
            },
            dismissButton = {
                TextButton(onClick = { pendingCatchUp = null }) {
                    Text("Cancel", color = SnowColors.FrostMute)
                }
            },
            containerColor = SnowColors.CardElev,
            titleContentColor = SnowColors.Frost,
            textContentColor = SnowColors.FrostMute,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeablePaymentRow(row: DueRow, onMarkPaid: () -> Unit, onUndo: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val onMarkPaidLatest by rememberUpdatedState(onMarkPaid)
    val onUndoLatest by rememberUpdatedState(onUndo)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            // enableDismissFromXxx (re-evaluated each recompose) gates which
            // direction is even draggable, so this lambda just fires the
            // matching callback. Captured lambdas are read via rememberUpdatedState
            // so we never call into a stale row's onMarkPaid/onUndo.
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMarkPaidLatest()
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUndoLatest()
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
            .celebratePaid(paid = row.isPaidThisCycle)
            .background(SnowColors.Night)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                stateDescription = if (row.isPaidThisCycle) "Paid" else "Not paid"
                contentDescription = "${row.debt.name}, ₱${row.amount.toLong()}, due ${row.effectiveDueDate}"
            }
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
private fun EmptyHint(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = SnowColors.FrostDim,
        )
    }
}

@Composable
private fun OverdueRow(info: OverdueInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = SnowColors.Ember,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(info.debt.name, style = MaterialTheme.typography.bodyLarge, color = SnowColors.Frost)
            Text(
                "${info.missedCycles} ${if (info.missedCycles == 1) "cycle" else "cycles"}",
                style = MaterialTheme.typography.bodySmall,
                color = SnowColors.FrostMute,
            )
        }
        Text(
            "₱${formatAmountWithSeparators(info.missedAmount)}",
            style = MaterialTheme.typography.bodyLarge,
            color = SnowColors.Ember,
        )
    }
}
