package com.snowball.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snowball.ui.theme.SnowColors

enum class Tab(val label: String) { Home("Home"), Debts("Debts"), Settings("Settings") }

@Composable
fun BottomNav(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SnowColors.NightElev)
            .padding(vertical = 14.dp),
    ) {
        Tab.entries.forEach { tab ->
            val active = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (active) SnowColors.Frost else SnowColors.FrostDim,
                    modifier = if (active) Modifier else Modifier.alpha(0.7f),
                )
            }
        }
    }
}
