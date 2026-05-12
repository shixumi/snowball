package com.snowball.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.ui.components.icon
import com.snowball.ui.theme.SnowColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtFormScreen(vm: DebtFormViewModel, onCancel: () -> Unit, onSaved: () -> Unit) {
    val state = vm.state

    Scaffold(
        containerColor = SnowColors.Night,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (vm.isEditing) "Edit debt" else "New debt",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = SnowColors.Frost,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SnowColors.Night,
                    titleContentColor = SnowColors.Frost,
                    navigationIconContentColor = SnowColors.Frost,
                    actionIconContentColor = SnowColors.Frost,
                ),
            )
        },
        bottomBar = {
            Surface(color = SnowColors.Night) {
                Button(
                    onClick = { if (vm.save()) onSaved() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SnowColors.Ice,
                        contentColor = SnowColors.Night,
                        disabledContainerColor = SnowColors.FrostMute,
                        disabledContentColor = SnowColors.Night,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Save") }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Field("Name") {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { v -> vm.update { it.copy(name = v) } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))

            Field("Category") { CategoryDropdown(vm) }
            Spacer(Modifier.height(16.dp))

            Field("Monthly amount") {
                OutlinedTextField(
                    value = state.monthlyAmount,
                    onValueChange = { v -> vm.update { it.copy(monthlyAmount = v.filter { c -> c.isDigit() || c == '.' }) } },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field("Total payments", modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = state.totalPayments,
                        onValueChange = { v -> vm.update { it.copy(totalPayments = v.filter { c -> c.isDigit() }) } },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
                Field("Due day (1-31)", modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = state.dueDay,
                        onValueChange = { v -> vm.update { it.copy(dueDay = v.filter { c -> c.isDigit() }) } },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            if (!vm.isEditing) {
                Field("Payments already made (optional)") {
                    OutlinedTextField(
                        value = state.paymentsAlreadyMade,
                        onValueChange = { v -> vm.update { it.copy(paymentsAlreadyMade = v.filter { c -> c.isDigit() }) } },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("0", color = SnowColors.FrostDim) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Use this to import a debt mid-way. Backfills payment history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SnowColors.FrostMute,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = state.useLastDayOfMonth,
                    onCheckedChange = { v -> vm.update { it.copy(useLastDayOfMonth = v) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = SnowColors.Ice),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "Use last day of month (Feb adjusts)",
                    color = SnowColors.FrostMute,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(16.dp))

            Field("Start date (YYYY-MM-DD)") {
                OutlinedTextField(
                    value = state.startDateText,
                    onValueChange = { v ->
                        val parsed = runCatching { kotlinx.datetime.LocalDate.parse(v) }.getOrNull()
                        vm.update { it.copy(startDateText = v, startDate = parsed ?: it.startDate) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))

            Field("Notes (optional)") {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { v -> vm.update { it.copy(notes = v) } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Field(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(vm: DebtFormViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val current = vm.categories.firstOrNull { it.id == vm.state.categoryId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = current?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = textFieldColors(),
            shape = RoundedCornerShape(12.dp),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            vm.categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.name, color = SnowColors.Frost) },
                    leadingIcon = {
                        androidx.compose.material3.Icon(
                            imageVector = cat.icon(),
                            contentDescription = null,
                            tint = SnowColors.FrostMute,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    onClick = {
                        vm.update { it.copy(categoryId = cat.id) }
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SnowColors.Frost,
    unfocusedTextColor = SnowColors.Frost,
    focusedBorderColor = SnowColors.Ice,
    unfocusedBorderColor = SnowColors.LineStrong,
    cursorColor = SnowColors.Ice,
)
