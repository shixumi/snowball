package com.snowball.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import com.snowball.ui.theme.SnowColors
import kotlinx.coroutines.delay

/**
 * Brief scale-up + frost glow when [paid] flips from false to true. Re-fires only
 * on the false→true transition. Apply to the payment row's outer Modifier.
 */
@Composable
fun Modifier.celebratePaid(paid: Boolean): Modifier {
    var celebrating by remember { mutableStateOf(false) }
    var previouslyPaid by remember { mutableStateOf(paid) }
    LaunchedEffect(paid) {
        if (paid && !previouslyPaid) {
            celebrating = true
            delay(600)
            celebrating = false
        }
        previouslyPaid = paid
    }
    val scale by animateFloatAsState(
        targetValue = if (celebrating) 1.04f else 1f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "celebrateScale",
    )
    val glow by animateColorAsState(
        targetValue = if (celebrating) SnowColors.Charge.copy(alpha = 0.22f) else Color.Transparent,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "celebrateGlow",
    )
    return this.scale(scale).background(glow)
}
