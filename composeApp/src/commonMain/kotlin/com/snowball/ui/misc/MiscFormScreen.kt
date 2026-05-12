package com.snowball.ui.misc

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.ui.theme.SnowColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiscFormScreen(vm: MiscFormViewModel, onCancel: () -> Unit, onSaved: () -> Unit) {
    val state = vm.state

    var nameTouched by remember { mutableStateOf(false) }
    var nameHadFocus by remember { mutableStateOf(false) }
    var amountTouched by remember { mutableStateOf(false) }
    var amountHadFocus by remember { mutableStateOf(false) }
    var dateTouched by remember { mutableStateOf(false) }
    var dateHadFocus by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SnowColors.Night,
        topBar = {
            TopAppBar(
                title = { Text("New MISC item", style = MaterialTheme.typography.titleLarge) },
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
                ),
            )
        },
        bottomBar = {
            Surface(color = SnowColors.Night) {
                Button(
                    onClick = { if (vm.save()) onSaved() },
                    enabled = vm.isValid,
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
                    isError = nameTouched && !state.isNameValid(),
                    supportingText = {
                        if (nameTouched && !state.isNameValid()) Text("Enter a name", color = SnowColors.Ember)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focus ->
                            if (focus.isFocused) nameHadFocus = true
                            else if (nameHadFocus) nameTouched = true
                        },
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))

            Field("Amount") {
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = { v -> vm.update { it.copy(amount = v.filter { c -> c.isDigit() || c == '.' }) } },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountTouched && !state.isAmountValid(),
                    supportingText = {
                        if (amountTouched && !state.isAmountValid()) Text("Enter an amount greater than 0", color = SnowColors.Ember)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focus ->
                            if (focus.isFocused) amountHadFocus = true
                            else if (amountHadFocus) amountTouched = true
                        },
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))

            Field("Date paid (YYYY-MM-DD)") {
                OutlinedTextField(
                    value = state.datePaidText,
                    onValueChange = { v ->
                        val parsed = runCatching { kotlinx.datetime.LocalDate.parse(v) }.getOrNull()
                        vm.update { it.copy(datePaidText = v, datePaid = parsed ?: it.datePaid) }
                    },
                    isError = dateTouched && !state.isDatePaidValid(),
                    supportingText = {
                        if (dateTouched && !state.isDatePaidValid()) Text("Use YYYY-MM-DD format", color = SnowColors.Ember)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focus ->
                            if (focus.isFocused) dateHadFocus = true
                            else if (dateHadFocus) dateTouched = true
                        },
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
private fun Field(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SnowColors.Frost,
    unfocusedTextColor = SnowColors.Frost,
    focusedBorderColor = SnowColors.Ice,
    unfocusedBorderColor = SnowColors.LineStrong,
    cursorColor = SnowColors.Ice,
)
