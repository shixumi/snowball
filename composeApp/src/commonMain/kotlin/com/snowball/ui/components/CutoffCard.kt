package com.snowball.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.domain.Cutoff
import com.snowball.domain.CutoffCalculator
import com.snowball.ui.theme.SnowColors
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
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.W300),
            pesoColor = SnowColors.FrostMute,
            numberColor = SnowColors.Frost,
        )

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            LedgerCell(
                label = "INCOME",
                amount = incomePerCutoff,
                color = SnowColors.Frost,
                modifier = Modifier.fillMaxWidth(),
            )
            val isShort = summary.breathingRoom < 0
            LedgerCell(
                label = if (isShort) "SHORT BY" else "LEFT OVER",
                amount = abs(summary.breathingRoom),
                color = if (isShort) SnowColors.Ember else SnowColors.Ice,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LedgerCell(label: String, amount: Double, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(SnowColors.Night.copy(alpha = 0.6f))
            .padding(20.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp), color = SnowColors.FrostDim)
        Spacer(Modifier.height(8.dp))
        PesoText(
            amount = amount,
            style = MaterialTheme.typography.headlineLarge,
            pesoColor = SnowColors.FrostDim,
            numberColor = color,
        )
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
