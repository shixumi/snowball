package com.snowball

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun App() {
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0B0F14)),
            contentAlignment = Alignment.Center
        ) {
            Text("Snowball", color = Color(0xFFF2F5F8))
        }
    }
}
