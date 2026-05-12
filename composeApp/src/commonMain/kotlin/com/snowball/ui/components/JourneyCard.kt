package com.snowball.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.domain.JourneyStats
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.formatAmountWithSeparators
import com.snowball.ui.util.formatMonthYear

@Composable
fun JourneyCard(stats: JourneyStats, modifier: Modifier = Modifier) {
    val meltedText = "₱${formatAmountWithSeparators(stats.totalMelted)} melted"
    val forecastText = if (stats.forecastEndDate == null) "All clear ✓"
                       else "Free by ${formatMonthYear(stats.forecastEndDate)}"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(SnowColors.CardElev)
            .border(width = 1.dp, color = SnowColors.LineStrong, shape = RoundedCornerShape(28.dp))
            .semantics(mergeDescendants = true) {
                contentDescription = "Your journey, ${stats.percentCleared} percent cleared, $meltedText, $forecastText"
            }
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(
            "YOUR JOURNEY",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "${stats.percentCleared}%",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.W300),
            color = SnowColors.Ice,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "cleared",
            style = MaterialTheme.typography.labelMedium,
            color = SnowColors.FrostMute,
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                meltedText,
                style = MaterialTheme.typography.bodyMedium,
                color = SnowColors.FrostMute,
            )
            Text(
                " · ",
                style = MaterialTheme.typography.bodyMedium,
                color = SnowColors.FrostMute,
            )
            Text(
                forecastText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (stats.forecastEndDate == null) SnowColors.Ice else SnowColors.FrostMute,
            )
        }
    }
}
