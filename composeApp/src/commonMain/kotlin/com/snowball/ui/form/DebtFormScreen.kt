package com.snowball.ui.form

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.ui.components.StaggeredItem
import com.snowball.ui.components.icon
import com.snowball.ui.components.pressScale
import com.snowball.ui.theme.SnowColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtFormScreen(vm: DebtFormViewModel, onCancel: () -> Unit, onSaved: () -> Unit) {
    val state = vm.state

    var nameTouched by remember { mutableStateOf(false) }
    var nameHadFocus by remember { mutableStateOf(false) }
    var amountTouched by remember { mutableStateOf(false) }
    var amountHadFocus by remember { mutableStateOf(false) }
    var totalTouched by remember { mutableStateOf(false) }
    var totalHadFocus by remember { mutableStateOf(false) }
    var dueDayTouched by remember { mutableStateOf(false) }
    var dueDayHadFocus by remember { mutableStateOf(false) }
    var alreadyMadeTouched by remember { mutableStateOf(false) }
    var alreadyMadeHadFocus by remember { mutableStateOf(false) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }

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
                actions = {
                    if (vm.isEditing) {
                        IconButton(onClick = { overflowOpen = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More options",
                                tint = SnowColors.Frost,
                            )
                        }
                        DropdownMenu(
                            expanded = overflowOpen,
                            onDismissRequest = { overflowOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = SnowColors.Ember) },
                                onClick = {
                                    overflowOpen = false
                                    showDeleteConfirm = true
                                },
                            )
                        }
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
                val saveInteractionSource = remember { MutableInteractionSource() }
                Button(
                    onClick = { if (vm.save()) onSaved() },
                    enabled = vm.isValid,
                    interactionSource = saveInteractionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .height(56.dp)
                        .pressScale(saveInteractionSource),
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
            StaggeredItem(index = 0) {
                Field("Name") {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { v -> vm.update { it.copy(name = v) } },
                        isError = nameTouched && !state.isNameValid(),
                        supportingText = {
                            if (nameTouched && !state.isNameValid()) Text("Enter a name", color = SnowColors.Ember)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focus ->
                                if (focus.isFocused) {
                                    nameHadFocus = true
                                } else if (nameHadFocus) {
                                    nameTouched = true
                                }
                            },
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            StaggeredItem(index = 1) {
                Field("Category") { CategoryDropdown(vm) }
            }
            Spacer(Modifier.height(16.dp))

            StaggeredItem(index = 2) {
                Field("Monthly amount") {
                    OutlinedTextField(
                        value = state.monthlyAmount,
                        onValueChange = { v -> vm.update { it.copy(monthlyAmount = v.filter { c -> c.isDigit() || c == '.' }) } },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = amountTouched && !state.isMonthlyAmountValid(),
                        supportingText = {
                            if (amountTouched && !state.isMonthlyAmountValid()) Text("Enter an amount greater than 0", color = SnowColors.Ember)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focus ->
                                if (focus.isFocused) {
                                    amountHadFocus = true
                                } else if (amountHadFocus) {
                                    amountTouched = true
                                }
                            },
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            StaggeredItem(index = 3) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field("Total payments", modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = state.totalPayments,
                            onValueChange = { v -> vm.update { it.copy(totalPayments = v.filter { c -> c.isDigit() }) } },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = totalTouched && !state.isTotalPaymentsValid(),
                            supportingText = {
                                if (totalTouched && !state.isTotalPaymentsValid()) Text("Enter 1 to 600", color = SnowColors.Ember)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focus ->
                                    if (focus.isFocused) {
                                        totalHadFocus = true
                                    } else if (totalHadFocus) {
                                        totalTouched = true
                                    }
                                },
                            colors = textFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                    Field("Due day (1-31)", modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = state.dueDay,
                            onValueChange = { v -> vm.update { it.copy(dueDay = v.filter { c -> c.isDigit() }) } },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = dueDayTouched && !state.isDueDayValid(),
                            supportingText = {
                                if (dueDayTouched && !state.isDueDayValid()) Text("Enter 1 to 31", color = SnowColors.Ember)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focus ->
                                    if (focus.isFocused) {
                                        dueDayHadFocus = true
                                    } else if (dueDayHadFocus) {
                                        dueDayTouched = true
                                    }
                                },
                            colors = textFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            StaggeredItem(index = 4) {
                Column {
                    AnimatedVisibility(
                        visible = !vm.isEditing,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column {
                            Field("Payments already made (optional)") {
                                OutlinedTextField(
                                    value = state.paymentsAlreadyMade,
                                    onValueChange = { v -> vm.update { it.copy(paymentsAlreadyMade = v.filter { c -> c.isDigit() }) } },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                    placeholder = { Text("0", color = SnowColors.FrostDim) },
                                    isError = alreadyMadeTouched && !state.isPaymentsAlreadyMadeValid(),
                                    supportingText = {
                                        if (alreadyMadeTouched && !state.isPaymentsAlreadyMadeValid()) Text("Must be between 0 and total payments", color = SnowColors.Ember)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { focus ->
                                            if (focus.isFocused) {
                                                alreadyMadeHadFocus = true
                                            } else if (alreadyMadeHadFocus) {
                                                alreadyMadeTouched = true
                                            }
                                        },
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
                        }
                    }
                    AnimatedVisibility(
                        visible = vm.isEditing,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Column {
                            Text(
                                "PAYMENTS RECORDED",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp),
                                color = SnowColors.FrostDim,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${vm.recordedPayments} of ${vm.originalTotalPayments}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = SnowColors.Frost,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            StaggeredItem(index = 5) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "Use last day of month, Feb adjusts"
                    },
                ) {
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
            }
            Spacer(Modifier.height(16.dp))

            StaggeredItem(index = 6) {
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
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "When you got the loan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SnowColors.FrostMute,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            StaggeredItem(index = 7) {
                Field("First payment date (YYYY-MM-DD)") {
                    OutlinedTextField(
                        value = state.firstPaymentDateText,
                        onValueChange = { v ->
                            val parsed = runCatching { kotlinx.datetime.LocalDate.parse(v) }.getOrNull()
                            vm.update { it.copy(firstPaymentDateText = v, firstPaymentDate = parsed ?: it.firstPaymentDate) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "When your first payment is due. Usually one month after start.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SnowColors.FrostMute,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            StaggeredItem(index = 8) {
                Field("Notes (optional)") {
                    OutlinedTextField(
                        value = state.notes,
                        onValueChange = { v -> vm.update { it.copy(notes = v) } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = {
                    Text(
                        "Delete ${state.name.ifBlank { "this debt" }}?",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                text = { Text("This removes the debt and all payment history.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        showDeleteConfirm = false
                        if (vm.delete()) onSaved()
                    }) { Text("Delete", color = SnowColors.Ember) }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = SnowColors.FrostMute)
                    }
                },
                containerColor = SnowColors.CardElev,
                titleContentColor = SnowColors.Frost,
                textContentColor = SnowColors.FrostMute,
            )
        }
    }
}

@Composable
private fun Field(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = label
        },
    ) {
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
