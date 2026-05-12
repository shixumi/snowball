package com.snowball.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.snowball.ui.theme.SnowColors

@Composable
fun HeroAmount(amount: Double, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        PesoText(
            amount = amount,
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.W300),
            numberColor = SnowColors.Frost,
            pesoColor = SnowColors.FrostMute,
        )
    }
}
