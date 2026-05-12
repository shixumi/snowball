package com.snowball.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun SnowballTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        background = SnowColors.Night,
        surface = SnowColors.NightElev,
        surfaceVariant = SnowColors.CardElev,
        onBackground = SnowColors.Frost,
        onSurface = SnowColors.Frost,
        primary = SnowColors.Ice,
        onPrimary = SnowColors.Night,
        secondary = SnowColors.Champagne,
        tertiary = SnowColors.Ember,
        error = SnowColors.Ember,
        outline = SnowColors.LineStrong,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = snowballTypography(),
        content = content,
    )
}
