package com.snowball.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.data.model.CategoryBehavior
import com.snowball.data.model.Payment
import com.snowball.ui.components.PesoText
import com.snowball.ui.components.ProgressArc
import com.snowball.ui.components.StaggeredItem
import com.snowball.ui.components.icon
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.formatAmountWithSeparators
import com.snowball.ui.util.formatLongDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailScreen(
    vm: DebtDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    var tick by remember { mutableStateOf(0) }
    val state = remember(tick) { vm.load() }
    if (state == null) {
        // Debt was deleted out from under us; bounce back.
        onBack()
        return
    }
    val isMisc = state.category.behavior == CategoryBehavior.LEDGER

    var overflowOpen by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingUndoPayment by remember { mutableStateOf<Payment?>(null) }

    Scaffold(
        containerColor = SnowColors.Night,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.debt.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = SnowColors.Frost)
                    }
                },
                actions = {
                    IconButton(onClick = { overflowOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options", tint = SnowColors.Frost)
                    }
                    DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                        if (!isMisc && !state.debt.isArchived) {
                            DropdownMenuItem(
                                text = { Text("Edit", color = SnowColors.Frost) },
                                onClick = { overflowOpen = false; onEdit(state.debt.id) },
                            )
                        }
                        if (!isMisc) {
                            DropdownMenuItem(
                                text = { Text(if (state.debt.isArchived) "Unarchive" else "Archive", color = SnowColors.Frost) },
                                onClick = {
                                    overflowOpen = false
                                    vm.setArchived(!state.debt.isArchived)
                                    tick++
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete", color = SnowColors.Ember) },
                            onClick = { overflowOpen = false; showDeleteConfirm = true },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SnowColors.Night,
                    titleContentColor = SnowColors.Frost,
                    navigationIconContentColor = SnowColors.Frost,
                    actionIconContentColor = SnowColors.Frost,
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
            // Header chips row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = state.category.icon(),
                    contentDescription = null,
                    tint = SnowColors.FrostDim,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    state.category.name.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                    color = SnowColors.FrostDim,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (state.debt.isArchived) "ARCHIVED" else "ACTIVE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                    color = if (state.debt.isArchived) SnowColors.FrostMute else SnowColors.Ice,
                )
            }
            Spacer(Modifier.height(24.dp))

            if (isMisc) {
                // MISC variant: big amount + paid date + notes
                StaggeredItem(index = 0) {
                    Column {
                        PesoText(
                            amount = state.debt.monthlyAmount,
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.W300),
                            pesoColor = SnowColors.FrostMute,
                            numberColor = SnowColors.Frost,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Paid ${formatLongDate(state.debt.startDate)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SnowColors.FrostMute,
                        )
                    }
                }
                val notes = state.debt.notes
                if (!notes.isNullOrBlank()) {
                    Spacer(Modifier.height(24.dp))
                    StaggeredItem(index = 1) {
                        Column {
                            Text(
                                "NOTES",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                                color = SnowColors.FrostDim,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SnowColors.Frost,
                            )
                        }
                    }
                }
            } else {
                // Scheduled variant: ProgressArc + amount left + stats + history
                StaggeredItem(index = 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                ProgressArc(
                                    progress = if (state.debt.totalPayments > 0) state.paymentsMade.toFloat() / state.debt.totalPayments else 0f,
                                    modifier = Modifier.size(160.dp),
                                )
                                Text(
                                    "${state.paymentsMade} of ${state.debt.totalPayments}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = SnowColors.Frost,
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text("₱", color = SnowColors.FrostMute, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatAmountWithSeparators(state.amountLeft),
                                style = MaterialTheme.typography.headlineSmall,
                                color = SnowColors.Frost,
                            )
                            Text(" left", color = SnowColors.FrostMute, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                StaggeredItem(index = 1) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SnowColors.LineStrong),
                        )
                        Spacer(Modifier.height(20.dp))

                        StatRow(label = "MONTHLY", value = "₱${formatAmountWithSeparators(state.debt.monthlyAmount)}")
                        Spacer(Modifier.height(12.dp))
                        StatRow(
                            label = "DUE DAY",
                            value = state.debt.dueDay.toString() + if (state.debt.useLastDayOfMonth) " (or last day)" else "",
                        )
                        Spacer(Modifier.height(12.dp))
                        StatRow(label = "STARTED", value = formatLongDate(state.debt.startDate))
                        Spacer(Modifier.height(12.dp))
                        StatRow(label = "FIRST PAYMENT", value = formatLongDate(state.debt.firstPaymentDate))
                        Spacer(Modifier.height(12.dp))
                        StatRow(
                            label = "PROJECTED END",
                            value = state.projectedEndDate?.let { formatLongDate(it) } ?: "—",
                        )

                        state.overdue?.let { info ->
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "OVERDUE",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                                    color = SnowColors.Ember,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "${info.missedCycles} ${if (info.missedCycles == 1) "cycle" else "cycles"} · ₱${formatAmountWithSeparators(info.missedAmount)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = SnowColors.Ember,
                                )
                            }
                        }

                        if (state.payments.isNotEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(SnowColors.LineStrong),
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "PAYMENT HISTORY (${state.payments.size})",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                                color = SnowColors.FrostDim,
                            )
                            Spacer(Modifier.height(8.dp))
                            state.payments.forEach { payment ->
                                PaymentHistoryRow(payment = payment, onClick = { pendingUndoPayment = payment })
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete ${state.debt.name}?", style = MaterialTheme.typography.headlineSmall) },
                text = { Text("This removes the debt and all payment history.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        if (vm.delete()) onBack()
                    }) { Text("Delete", color = SnowColors.Ember) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = SnowColors.FrostMute)
                    }
                },
                containerColor = SnowColors.CardElev,
                titleContentColor = SnowColors.Frost,
                textContentColor = SnowColors.FrostMute,
            )
        }

        val undoTarget = pendingUndoPayment
        if (undoTarget != null) {
            AlertDialog(
                onDismissRequest = { pendingUndoPayment = null },
                title = { Text("Undo this payment?", style = MaterialTheme.typography.headlineSmall) },
                text = {
                    Text("₱${formatAmountWithSeparators(undoTarget.amount)} recorded on ${formatLongDate(undoTarget.paidDate)} will be removed.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.undoPayment(undoTarget.id)
                        pendingUndoPayment = null
                        tick++
                    }) { Text("Undo", color = SnowColors.Ember) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingUndoPayment = null }) {
                        Text("Cancel", color = SnowColors.FrostMute)
                    }
                },
                containerColor = SnowColors.CardElev,
                titleContentColor = SnowColors.Frost,
                textContentColor = SnowColors.FrostMute,
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = SnowColors.Frost,
        )
    }
}

@Composable
private fun PaymentHistoryRow(payment: Payment, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(SnowColors.Ice),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            formatLongDate(payment.paidDate),
            style = MaterialTheme.typography.bodyMedium,
            color = SnowColors.Frost,
            modifier = Modifier.weight(1f),
        )
        PesoText(
            amount = payment.amount,
            style = MaterialTheme.typography.bodyLarge,
            pesoColor = SnowColors.FrostDim,
            numberColor = SnowColors.Frost,
        )
    }
}
