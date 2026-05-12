package com.snowball.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.toFormFieldString
import kotlinx.coroutines.delay

private fun Double.toFormattedPeso(): String {
    if (this == 0.0) return ""
    val whole = this.toLong()
    val grouped = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    val frac = ((this - whole) * 100 + 0.5).toInt()
    return if (frac == 0) "₱$grouped" else "₱$grouped.${frac.toString().padStart(2, '0')}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val initial = remember { vm.load() }
    var rawInput by remember { mutableStateOf(initial.incomePerCutoff.toFormFieldString()) }
    var hasFocus by remember { mutableStateOf(false) }
    var lastCommitted by remember { mutableStateOf(initial.incomePerCutoff) }
    var ackVisible by remember { mutableStateOf(false) }

    LaunchedEffect(ackVisible) {
        if (ackVisible) {
            delay(1500)
            ackVisible = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = SnowColors.Frost)
        Spacer(Modifier.height(24.dp))

        Text(
            "INCOME PER CUTOFF",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(8.dp))

        val displayValue = if (hasFocus) {
            rawInput
        } else {
            (rawInput.toDoubleOrNull() ?: 0.0).toFormattedPeso()
        }

        OutlinedTextField(
            value = displayValue,
            onValueChange = { v ->
                if (hasFocus) {
                    rawInput = v.filter { c -> c.isDigit() || c == '.' }
                }
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            placeholder = { Text("₱ 0", color = SnowColors.FrostDim) },
            trailingIcon = {
                if (ackVisible) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Saved",
                        tint = SnowColors.Ice,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    val wasFocused = hasFocus
                    hasFocus = focusState.isFocused
                    if (wasFocused && !focusState.isFocused) {
                        val parsed = rawInput.toDoubleOrNull() ?: 0.0
                        if (parsed != lastCommitted) {
                            vm.setIncome(parsed)
                            lastCommitted = parsed
                            ackVisible = true
                        }
                    }
                },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SnowColors.Frost,
                unfocusedTextColor = SnowColors.Frost,
                focusedBorderColor = SnowColors.Ice,
                unfocusedBorderColor = SnowColors.LineStrong,
                cursorColor = SnowColors.Ice,
            ),
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Same amount used for both 15th and 30th cutoffs.",
            style = MaterialTheme.typography.bodySmall,
            color = SnowColors.FrostMute,
        )

        Spacer(Modifier.height(40.dp))
        Text(
            "Snowball v0.1",
            style = MaterialTheme.typography.labelSmall,
            color = SnowColors.FrostMute,
        )
    }
}
