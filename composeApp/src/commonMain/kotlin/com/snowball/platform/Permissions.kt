package com.snowball.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberRequestNotificationPermission(onResult: (Boolean) -> Unit): () -> Unit
