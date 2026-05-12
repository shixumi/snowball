package com.snowball.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.formatAmountWithSeparators

@Composable
fun PesoText(
    amount: Double,
    style: TextStyle,
    modifier: Modifier = Modifier,
    pesoColor: Color = SnowColors.FrostDim,
    numberColor: Color = MaterialTheme.colorScheme.onBackground,
    align: TextAlign = TextAlign.Start,
) {
    val formatted = formatAmountWithSeparators(amount)
    val pesoStyle = style.copy(
        color = pesoColor,
        fontStyle = FontStyle.Italic,
        fontSize = style.fontSize * 0.55f,
    )
    val numStyle = style.copy(color = numberColor)
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "₱$formatted"
        },
        verticalAlignment = Alignment.Bottom,
    ) {
        Text("₱", style = pesoStyle)
        Spacer(Modifier.width(2.dp))
        Text(formatted, style = numStyle)
    }
}
