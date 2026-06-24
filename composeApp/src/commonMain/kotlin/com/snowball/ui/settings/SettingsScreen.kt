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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.data.backup.ImportResult
import com.snowball.platform.rememberBackupExporter
import com.snowball.platform.rememberBackupImporter
import com.snowball.platform.rememberHaptics
import com.snowball.platform.rememberRequestNotificationPermission
import com.snowball.ui.components.ScreenHeader
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.formatAmountWithSeparators
import com.snowball.ui.util.toFormFieldString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private fun Double.toFormattedPeso(): String {
    if (this == 0.0) return ""
    return "₱${formatAmountWithSeparators(this)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onManageCategories: () -> Unit = {},
    onDataReplaced: () -> Unit = {},
) {
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

        // ── BACKUP & RESTORE ───────────────────────────────────────────
        Spacer(Modifier.height(28.dp))
        Text(
            "BACKUP & RESTORE",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(8.dp))

        val scope = rememberCoroutineScope()
        val exporter = rememberBackupExporter()
        var pendingImport by remember { mutableStateOf<String?>(null) }
        var importError by remember { mutableStateOf<String?>(null) }
        val importer = rememberBackupImporter { picked ->
            // null = user cancelled the file picker; otherwise confirm before replacing.
            if (picked != null) pendingImport = picked
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    scope.launch {
                        val json = withContext(Dispatchers.Default) { vm.exportJson() }
                        exporter(defaultBackupFileName(), json)
                    }
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Export data", modifier = Modifier.weight(1f), color = SnowColors.Frost)
            Icon(Icons.Outlined.ChevronRight, null, tint = SnowColors.Ice, modifier = Modifier.size(20.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { importError = null; importer() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Import data", modifier = Modifier.weight(1f), color = SnowColors.Frost)
            Icon(Icons.Outlined.ChevronRight, null, tint = SnowColors.Ice, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Save a backup file, or open one to restore. Importing replaces everything currently in the app.",
            style = MaterialTheme.typography.bodySmall,
            color = SnowColors.FrostMute,
        )

        pendingImport?.let { json ->
            AlertDialog(
                onDismissRequest = { pendingImport = null },
                title = { Text("Replace all data?", color = SnowColors.Frost) },
                text = {
                    Text(
                        "This erases your current debts, payments and settings, then restores the backup. This can't be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SnowColors.FrostMute,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        when (val result = vm.import(json)) {
                            is ImportResult.Success -> {
                                haptics.thump()
                                pendingImport = null
                                onDataReplaced()
                            }
                            is ImportResult.Failure -> {
                                importError = result.message
                                pendingImport = null
                            }
                        }
                    }) { Text("Replace my data", color = SnowColors.Ember) }
                },
                dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel") } },
                containerColor = SnowColors.CardElev,
            )
        }

        importError?.let { msg ->
            AlertDialog(
                onDismissRequest = { importError = null },
                title = { Text("Import failed", color = SnowColors.Frost) },
                text = { Text(msg, style = MaterialTheme.typography.bodyMedium, color = SnowColors.FrostMute) },
                confirmButton = { TextButton(onClick = { importError = null }) { Text("OK") } },
                containerColor = SnowColors.CardElev,
            )
        }

        Spacer(Modifier.height(40.dp))
        Text(
            "Snowball v0.8.0",
            style = MaterialTheme.typography.labelSmall,
            color = SnowColors.FrostMute,
        )
    }
}

/** e.g. "snowball-backup-2026-06-19.json" (LocalDate renders as ISO yyyy-MM-dd). */
private fun defaultBackupFileName(): String {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "snowball-backup-$today.json"
}
