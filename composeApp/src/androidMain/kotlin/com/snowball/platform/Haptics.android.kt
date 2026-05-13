package com.snowball.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

actual class Haptics(private val feedback: HapticFeedback) {
    actual fun tick() {
        feedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    actual fun thump() {
        feedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
actual fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { Haptics(feedback) }
}
