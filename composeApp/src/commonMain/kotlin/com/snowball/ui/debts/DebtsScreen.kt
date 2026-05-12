package com.snowball.ui.debts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.ui.components.PesoText
import com.snowball.ui.components.StaggeredItem
import com.snowball.ui.components.icon
import com.snowball.ui.components.pressScale
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.formatAmountWithSeparators
import com.snowball.ui.util.formatLongDate

@Composable
fun DebtsScreen(
    vm: DebtsViewModel,
    onAddDebt: () -> Unit,
    onAddMisc: () -> Unit,
    onOpenDebt: (Long) -> Unit,
) {
    var tick by remember { mutableStateOf(0) }
    val state = remember(tick) { vm.load() }
    var fabExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AcUnit,
                        contentDescription = null,
                        tint = SnowColors.Frost,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.showArchived) "Archived" else "Active",
                        style = MaterialTheme.typography.headlineLarge,
                        color = SnowColors.Frost,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { vm.toggleArchive(); tick++ },
                ) {
                    Text(
                        if (state.showArchived) "View active" else "View archived",
                        style = MaterialTheme.typography.labelMedium.copy(textDecoration = TextDecoration.Underline),
                        color = SnowColors.Ice,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = SnowColors.Ice,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            var rowIndex = 0
            state.categories.forEach { cat ->
                val rows = state.scheduledByCategory[cat.id].orEmpty()
                if (rows.isEmpty()) return@forEach
                CategoryHeader(cat = cat)
                Spacer(Modifier.height(8.dp))
                rows.forEach { row ->
                    val idx = rowIndex++
                    StaggeredItem(index = idx) {
                        DebtRowItem(row = row, onClick = { onOpenDebt(row.debt.id) })
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(16.dp))
            }

            if (state.miscRows.isNotEmpty()) {
                val miscCat = state.categories.firstOrNull { c -> state.miscRows.any { it.debt.categoryId == c.id } }
                if (miscCat != null) CategoryHeader(cat = miscCat)
                Spacer(Modifier.height(8.dp))
                state.miscRows.forEach { row ->
                    val idx = rowIndex++
                    StaggeredItem(index = idx) {
                        MiscRowItem(row = row, onClick = { onOpenDebt(row.debt.id) })
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(16.dp))
            }

            if (state.scheduledByCategory.isEmpty() && state.miscRows.isEmpty()) {
                var emptyStateVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { emptyStateVisible = true }
                AnimatedVisibility(
                    visible = emptyStateVisible,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 },
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (state.showArchived) "Nothing archived yet."
                            else "No debts yet. Tap + to add your first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SnowColors.FrostDim,
                        )
                    }
                }
            }
        }

        if (!state.showArchived) {
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
                val fabInteractionSource = remember { MutableInteractionSource() }
                FloatingActionButton(
                    onClick = { fabExpanded = true },
                    modifier = Modifier.size(56.dp).clip(CircleShape).pressScale(fabInteractionSource),
                    containerColor = SnowColors.Ice,
                    contentColor = SnowColors.Night,
                    interactionSource = fabInteractionSource,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Add",
                        tint = SnowColors.Night,
                    )
                }
                DropdownMenu(
                    expanded = fabExpanded,
                    onDismissRequest = { fabExpanded = false },
                    offset = DpOffset(x = (-160).dp, y = 0.dp),
                ) {
                    DropdownMenuItem(
                        text = { Text("Add debt", color = SnowColors.Frost) },
                        onClick = { fabExpanded = false; onAddDebt() },
                    )
                    DropdownMenuItem(
                        text = { Text("Add MISC item", color = SnowColors.Frost) },
                        onClick = { fabExpanded = false; onAddMisc() },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(cat: com.snowball.data.model.Category) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = cat.icon(),
            contentDescription = null,
            tint = SnowColors.FrostDim,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            cat.name.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
    }
}

@Composable
private fun DebtRowItem(row: DebtRow, onClick: () -> Unit) {
    val d = row.debt
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SnowColors.CardElev)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                d.name,
                style = MaterialTheme.typography.headlineSmall,
                color = SnowColors.Frost,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = if (d.isArchived) {
                val date = row.clearedDate?.let { formatLongDate(it) } ?: "—"
                "Cleared $date · ₱${formatAmountWithSeparators(row.totalPaidAmount)}"
            } else {
                "Day ${d.dueDay} · ${d.totalPayments} months · ${row.paymentsMade}/${d.totalPayments} paid"
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SnowColors.FrostMute,
            )
        }
        Spacer(Modifier.width(8.dp))
        PesoText(
            amount = d.monthlyAmount,
            style = MaterialTheme.typography.headlineSmall,
            pesoColor = SnowColors.FrostDim,
            numberColor = SnowColors.Frost,
        )
    }
}

@Composable
private fun MiscRowItem(row: DebtRow, onClick: () -> Unit) {
    val d = row.debt
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SnowColors.CardElev)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                d.name,
                style = MaterialTheme.typography.headlineSmall,
                color = SnowColors.Frost,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Paid ${formatLongDate(d.startDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = SnowColors.FrostMute,
            )
        }
        Spacer(Modifier.width(8.dp))
        PesoText(
            amount = d.monthlyAmount,
            style = MaterialTheme.typography.headlineSmall,
            pesoColor = SnowColors.FrostDim,
            numberColor = SnowColors.Frost,
        )
    }
}
