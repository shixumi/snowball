package com.snowball.ui.nav

import androidx.compose.runtime.Composable

/**
 * Intercepts the platform's system back gesture/button. When [enabled] is true
 * and the user triggers system back, [onBack] is invoked instead of the
 * platform default (which would normally exit the app on Android).
 */
@Composable
expect fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit)
