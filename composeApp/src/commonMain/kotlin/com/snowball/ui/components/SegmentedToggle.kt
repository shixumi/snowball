package com.snowball.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snowball.ui.theme.SnowColors

/**
 * Outlined sliding-pill segmented control. The thumb (an Ice outline, transparent fill)
 * slides to the selected segment; the active label is Ice, inactive FrostDim.
 */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    val fraction by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "segThumb",
    )
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(SnowColors.Night)
            .border(1.dp, SnowColors.LineStrong, RoundedCornerShape(50))
            .padding(4.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val segWidth = maxWidth / options.size
            Box(
                Modifier
                    .offset(x = segWidth * fraction)
                    .width(segWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .border(1.5.dp, SnowColors.Ice, RoundedCornerShape(50)),
            )
            Row(Modifier.fillMaxSize()) {
                options.forEachIndexed { i, label ->
                    val active = i == selectedIndex
                    val color by animateColorAsState(
                        targetValue = if (active) SnowColors.Ice else SnowColors.FrostDim,
                        animationSpec = tween(200),
                        label = "segText$i",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelect(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = color,
                        )
                    }
                }
            }
        }
    }
}
