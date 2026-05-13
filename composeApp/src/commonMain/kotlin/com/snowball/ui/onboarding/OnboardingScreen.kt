package com.snowball.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.snowball.ui.theme.SnowColors

private data class Slide(val title: String, val body: String, val icon: ImageVector)

private val slides = listOf(
    Slide(
        title = "Track every debt.",
        body = "Loans, cards, plans — all in one place, organized by bi-monthly cutoff.",
        icon = Icons.Outlined.AcUnit,
    ),
    Slide(
        title = "Swipe to mark paid.",
        body = "Settle a payment with a quick swipe right. Undo with a swipe left.",
        icon = Icons.Outlined.CheckCircle,
    ),
    Slide(
        title = "Forecast your freedom.",
        body = "See when each debt finishes — and when you're done.",
        icon = Icons.Outlined.Insights,
    ),
    Slide(
        title = "Get started.",
        body = "Set your income per cutoff in Settings to begin.",
        icon = Icons.Outlined.Tune,
    ),
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var index by remember { mutableStateOf(0) }
    val slide = slides[index]
    val isLast = index == slides.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SnowColors.Night)
            .padding(horizontal = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.3f))

            AnimatedContent(
                targetState = slide,
                transitionSpec = {
                    (slideInHorizontally { it / 4 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally { -it / 4 } + fadeOut(tween(300)))
                },
                label = "onboardingSlide",
            ) { s ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = s.icon,
                        contentDescription = null,
                        tint = SnowColors.Ice,
                        modifier = Modifier.size(96.dp),
                    )
                    Spacer(Modifier.height(32.dp))
                    Text(
                        s.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = SnowColors.Frost,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        s.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SnowColors.FrostDim,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Page dots
            Row(verticalAlignment = Alignment.CenterVertically) {
                slides.forEachIndexed { i, _ ->
                    val active = i == index
                    val color by animateColorAsState(
                        targetValue = if (active) SnowColors.Ice else SnowColors.FrostMute,
                        animationSpec = tween(200),
                        label = "dotColor",
                    )
                    Box(
                        modifier = Modifier
                            .size(width = if (active) 24.dp else 8.dp, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color),
                    )
                    if (i != slides.lastIndex) Spacer(Modifier.width(6.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isLast) onComplete() else index++
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SnowColors.Ice,
                    contentColor = SnowColors.Night,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    if (isLast) "Get started" else "Next",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(Modifier.height(8.dp))

            if (!isLast) {
                TextButton(onClick = onComplete) {
                    Text("Skip", color = SnowColors.FrostMute)
                }
            } else {
                Spacer(Modifier.height(48.dp))
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
