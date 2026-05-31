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
        // Menus, dropdowns and sheets draw from the surfaceContainer roles — map them
        // to the Snowball palette so they don't fall back to M3's default tonal surface.
        surfaceContainerLowest = SnowColors.Night,
        surfaceContainerLow = SnowColors.NightElev,
        surfaceContainer = SnowColors.CardElev,
        surfaceContainerHigh = SnowColors.CardElev,
        surfaceContainerHighest = SnowColors.CardElev,
        onBackground = SnowColors.Frost,
        onSurface = SnowColors.Frost,
        onSurfaceVariant = SnowColors.FrostMute,
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
