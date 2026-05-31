package com.snowball.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.domain.Cutoff
import com.snowball.domain.CutoffCalculator
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.formatAmountWithSeparators
import kotlin.math.abs

@Composable
fun CutoffCard(
    cutoff: Cutoff,
    summary: CutoffCalculator.Summary,
    incomePerCutoff: Double,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(SnowColors.CardElev, SnowColors.NightElev)
                )
            )
            .border(width = 1.dp, color = SnowColors.LineStrong, shape = RoundedCornerShape(28.dp))
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Text(
            "THIS CUTOFF",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            cutoffRangeLabel(cutoff),
            style = MaterialTheme.typography.headlineMedium,
            color = SnowColors.Frost,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "DUE",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(12.dp))
        PesoText(
            amount = summary.dueTotal,
            style = MaterialTheme.typography.displayLarge,
            pesoColor = SnowColors.FrostMute,
            numberColor = SnowColors.Frost,
            animate = true,
        )

        // Momentum: paid/due progress this cutoff.
        if (summary.dueTotal > 0.0) {
            Spacer(Modifier.height(18.dp))
            val fraction = (summary.paidTotal / summary.dueTotal).coerceIn(0.0, 1.0).toFloat()
            val animFraction by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(600, easing = FastOutSlowInEasing),
                label = "cutoffProgress",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SnowColors.ChargeSoft),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animFraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SnowColors.Charge),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "₱${formatAmountWithSeparators(summary.paidTotal)} of ₱${formatAmountWithSeparators(summary.dueTotal)} paid this cutoff",
                style = MaterialTheme.typography.bodySmall,
                color = SnowColors.FrostMute,
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Income ₱${formatAmountWithSeparators(incomePerCutoff)}",
            style = MaterialTheme.typography.bodySmall,
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(6.dp))
        val isShort = summary.breathingRoom < 0
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (isShort) "SHORT BY" else "LEFT OVER",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                color = SnowColors.FrostDim,
            )
            Spacer(Modifier.width(10.dp))
            PesoText(
                amount = abs(summary.breathingRoom),
                style = MaterialTheme.typography.headlineMedium,
                pesoColor = SnowColors.FrostDim,
                numberColor = if (isShort) SnowColors.Ember else SnowColors.Ice,
            )
        }
    }
}

fun cutoffRangeLabel(c: Cutoff): String {
    val start = c.windowStart
    val end = c.windowEnd
    val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return if (start.monthNumber == end.monthNumber) {
        "${months[start.monthNumber - 1]} ${start.dayOfMonth} → ${end.dayOfMonth}"
    } else {
        "${months[start.monthNumber - 1]} ${start.dayOfMonth} → ${months[end.monthNumber - 1]} ${end.dayOfMonth}"
    }
}
