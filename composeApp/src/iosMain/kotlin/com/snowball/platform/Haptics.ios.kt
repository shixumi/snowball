package com.snowball.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

actual class Haptics {
    private val light = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val medium = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)

    actual fun tick() {
        light.prepare()
        light.impactOccurred()
    }

    actual fun thump() {
        medium.prepare()
        medium.impactOccurred()
    }
}

@Composable
actual fun rememberHaptics(): Haptics = remember { Haptics() }
