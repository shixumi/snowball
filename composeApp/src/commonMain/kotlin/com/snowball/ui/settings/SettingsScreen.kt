package com.snowball.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.ui.theme.SnowColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val initial = remember { vm.load() }
    var income by remember { mutableStateOf(initial.incomePerCutoff.toString()) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = SnowColors.Frost)
        Spacer(Modifier.height(24.dp))

        Text(
            "INCOME PER CUTOFF",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = income,
            onValueChange = { v ->
                income = v.filter { c -> c.isDigit() || c == '.' }
                income.toDoubleOrNull()?.let { vm.setIncome(it) }
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
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
            color = SnowColors.FrostDeep,
        )
    }
}
