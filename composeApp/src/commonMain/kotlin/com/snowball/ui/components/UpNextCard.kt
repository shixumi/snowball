package com.snowball.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.domain.Cutoff
import com.snowball.domain.DueRow
import com.snowball.ui.theme.SnowColors

@Composable
fun UpNextCard(
    cutoff: Cutoff,
    rows: List<DueRow>,
    total: Double,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "chevron",
    )
    val rangeLabel = cutoffRangeLabel(cutoff)
    val stateDesc = if (isExpanded) "Expanded" else "Collapsed"
    val debtLabel = if (rows.size == 1) "debt" else "debts"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(SnowColors.NightElev)
            .border(width = 1.dp, color = SnowColors.LineStrong, shape = RoundedCornerShape(28.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                stateDescription = stateDesc
                contentDescription = "Up next, $rangeLabel, ${total.toLong()} pesos, ${rows.size} $debtLabel"
            }
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            "UP NEXT · $rangeLabel",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PesoText(
                amount = total,
                style = MaterialTheme.typography.headlineMedium,
                pesoColor = SnowColors.FrostDim,
                numberColor = SnowColors.Frost,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "· ${rows.size} $debtLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = SnowColors.FrostMute,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = SnowColors.FrostMute,
                modifier = Modifier.size(20.dp).rotate(chevronRotation),
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SnowColors.Line),
                )
                Spacer(Modifier.height(4.dp))
                rows.forEach { row ->
                    UpNextRow(row = row)
                }
            }
        }
    }
}

@Composable
private fun UpNextRow(row: DueRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${row.debt.name}, ${row.amount.toLong()} pesos, due day ${row.debt.dueDay}"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            row.debt.name,
            style = MaterialTheme.typography.bodyMedium,
            color = SnowColors.Frost,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "Due ${row.debt.dueDay}",
            style = MaterialTheme.typography.bodySmall,
            color = SnowColors.FrostMute,
        )
        Spacer(Modifier.width(12.dp))
        PesoText(
            amount = row.amount,
            style = MaterialTheme.typography.bodyLarge,
            pesoColor = SnowColors.FrostDim,
            numberColor = SnowColors.Frost,
        )
    }
}
