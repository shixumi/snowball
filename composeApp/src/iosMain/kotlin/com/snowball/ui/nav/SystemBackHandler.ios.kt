package com.snowball.ui.nav

import androidx.compose.runtime.Composable

/**
 * iOS has no system back button; in-app navigation handles "back". No-op so the
 * shared call site compiles and behaves correctly on iOS.
 */
@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
}
