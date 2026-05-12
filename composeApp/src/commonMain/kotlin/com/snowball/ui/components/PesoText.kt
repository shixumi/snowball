package com.snowball.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.formatAmountWithSeparators

/**
 * Renders an amount as "₱ N,NNN.NN" where the ₱ glyph is smaller (0.55x) and
 * italic, bottom-aligned with the number. Auto-shrinks the font size to fit the
 * available horizontal width (from BoxWithConstraints) so long numbers never
 * wrap or get clipped — they just render slightly smaller.
 */
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
    BoxWithConstraints(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "₱$formatted"
        },
    ) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val basePesoFontSize = style.fontSize * 0.55f
        val pesoStyle = style.copy(
            color = pesoColor,
            fontStyle = FontStyle.Italic,
            fontSize = basePesoFontSize,
        )
        val numStyle = style.copy(color = numberColor)

        val pesoWidthPx = measurer.measure("₱", style = pesoStyle).size.width
        val numberWidthPx = measurer.measure(formatted, style = numStyle).size.width
        val gapPx = with(density) { 2.dp.toPx() }
        val totalWidthPx = pesoWidthPx + gapPx + numberWidthPx
        val availablePx = constraints.maxWidth.toFloat()

        val scale = if (availablePx > 0f && totalWidthPx > availablePx) {
            availablePx / totalWidthPx
        } else {
            1f
        }

        val finalNumStyle = numStyle.copy(fontSize = style.fontSize * scale)
        val finalPesoStyle = pesoStyle.copy(fontSize = basePesoFontSize * scale)

        Row(verticalAlignment = Alignment.Bottom) {
            Text("₱", style = finalPesoStyle, maxLines = 1, softWrap = false)
            Spacer(Modifier.width(2.dp))
            Text(formatted, style = finalNumStyle, maxLines = 1, softWrap = false)
        }
    }
}
