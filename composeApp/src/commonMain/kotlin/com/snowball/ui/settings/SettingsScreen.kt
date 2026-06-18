package com.snowball.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.platform.rememberHaptics
import com.snowball.platform.rememberRequestNotificationPermission
import com.snowball.ui.components.ScreenHeader
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.formatAmountWithSeparators
import com.snowball.ui.util.toFormFieldString
import kotlinx.coroutines.delay

private fun Double.toFormattedPeso(): String {
    if (this == 0.0) return ""
    return "₱${formatAmountWithSeparators(this)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, onManageCategories: () -> Unit = {}) {
    val initial = remember { vm.load() }
    var rawInput by remember { mutableStateOf(initial.incomePerCutoff.toFormFieldString()) }
    var hasFocus by remember { mutableStateOf(false) }
    var lastCommitted by remember { mutableStateOf(initial.incomePerCutoff) }
    var ackVisible by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    LaunchedEffect(ackVisible) {
        if (ackVisible) {
            delay(1500)
            ackVisible = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        ScreenHeader("Settings")
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
                AnimatedVisibility(
                    visible = ackVisible,
                    enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.6f, animationSpec = tween(200)),
                    exit = fadeOut(tween(200)),
                ) {
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

        Spacer(Modifier.height(28.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onManageCategories() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "MANAGE CATEGORIES",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                color = SnowColors.Frost,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = SnowColors.Ice,
                modifier = Modifier.size(20.dp),
            )
        }

        // ── NOTIFICATIONS ──────────────────────────────────────────────
        Spacer(Modifier.height(28.dp))
        Text(
            "NOTIFICATIONS",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(8.dp))

        // Toggle row
        var notifEnabled by remember { mutableStateOf(vm.notificationsEnabled) }
        val requestPermission = rememberRequestNotificationPermission { granted ->
            if (granted) {
                vm.setNotificationsEnabled(true)
                notifEnabled = true
                haptics.tick()
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Payday reminders", modifier = Modifier.weight(1f), color = SnowColors.Frost)
            Switch(
                checked = notifEnabled,
                onCheckedChange = { wantOn ->
                    if (wantOn) {
                        requestPermission()
                    } else {
                        vm.setNotificationsEnabled(false)
                        notifEnabled = false
                        haptics.tick()
                    }
                },
            )
        }

        // Time picker row
        var showTimePicker by remember { mutableStateOf(false) }
        var notifHour by remember { mutableStateOf(vm.notificationHour) }
        var notifMinute by remember { mutableStateOf(vm.notificationMinute) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = notifEnabled) { showTimePicker = true }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Reminder time",
                modifier = Modifier.weight(1f),
                color = if (notifEnabled) SnowColors.Frost else SnowColors.FrostDim,
            )
            Text(
                "${notifHour.toString().padStart(2, '0')}:${notifMinute.toString().padStart(2, '0')}",
                color = if (notifEnabled) SnowColors.Frost else SnowColors.FrostDim,
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Reminds on the 15th and last day of each month.",
            style = MaterialTheme.typography.bodySmall,
            color = SnowColors.FrostMute,
        )

        if (showTimePicker) {
            val state = rememberTimePickerState(initialHour = notifHour, initialMinute = notifMinute)
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        vm.setNotificationTime(state.hour, state.minute)
                        notifHour = state.hour
                        notifMinute = state.minute
                        showTimePicker = false
                        haptics.tick()
                    }) { Text("Set") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                },
                text = {
                    TimePicker(state = state)
                },
                containerColor = SnowColors.CardElev,
            )
        }

        Spacer(Modifier.height(40.dp))
        Text(
            "Snowball v0.5.0",
            style = MaterialTheme.typography.labelSmall,
            color = SnowColors.FrostMute,
        )
    }
}
