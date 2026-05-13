package com.snowball.platform

import androidx.compose.runtime.Composable

expect class Haptics {
    fun tick()
    fun thump()
}

@Composable
expect fun rememberHaptics(): Haptics
