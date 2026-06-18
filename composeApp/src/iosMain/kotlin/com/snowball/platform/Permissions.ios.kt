package com.snowball.platform

import androidx.compose.runtime.Composable

/**
 * Stubbed for the initial iOS port (see NotificationScheduler.ios.kt). Reports the
 * permission as granted so the Settings flow proceeds; the real
 * UNUserNotificationCenter.requestAuthorization prompt lands with iOS notifications.
 */
@Composable
actual fun rememberRequestNotificationPermission(onResult: (Boolean) -> Unit): () -> Unit =
    { onResult(true) }
