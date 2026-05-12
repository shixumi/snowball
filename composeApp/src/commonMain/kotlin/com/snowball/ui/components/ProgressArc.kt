package com.snowball.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.snowball.ui.theme.SnowColors

@Composable
fun ProgressArc(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    strokeDp: Dp = 2.dp,
    trackColor: Color = SnowColors.Line,
    arcColor: Color = SnowColors.Ice,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "progressArc",
    )
    Canvas(modifier = modifier) {
        val stroke = strokeDp.toPx()
        val s = Size(size.toPx() - stroke, size.toPx() - stroke)
        val topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = s,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = arcColor,
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            topLeft = topLeft,
            size = s,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}
