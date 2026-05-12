# Snowball v0.2 Sub-project B — Debt Detail + Archive + MISC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `DebtDetailScreen` (the new destination for any debt tap), improve the Archive view, and add a MISC items entry path via a FAB dropdown menu.

**Architecture:** New routes (`Route.DebtDetail`, `Route.MiscForm`) added to existing `App.kt` sealed interface. Two new VM/Screen pairs (`detail/` and `misc/` packages). `DebtsViewModel.DebtsState` split into `scheduledByCategory` + `miscRows`; subtitle data includes archive metadata when applicable. Tap-to-edit becomes tap-to-detail; Edit is a Detail overflow action that drops into the existing form screen.

**Tech Stack:** Same as v0.2a (Compose Multiplatform, Material 3, kotlinx.datetime). No new dependencies. No schema changes.

**Reference:** Spec at `docs/superpowers/specs/2026-05-13-snowball-v02b-debt-detail-design.md`.

---

## Build / verification recipe

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:assembleDebug
```

Install on `emulator-5554`:

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

uiautomator:

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell uiautomator dump /sdcard/ui.xml
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe pull /sdcard/ui.xml /c/Users/Pika/AppData/Local/Temp/ui-<name>.xml
```

**NEVER call `Read` on a `.png` file.**

---

## File-level change inventory

| File | Tasks | Purpose |
|---|---|---|
| `composeApp/src/commonMain/kotlin/com/snowball/domain/JourneyCalculator.kt` | 1 | Promote `projectedEndDate` to top-level public |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/util/DateFormat.kt` | 2 | Add `formatLongDate(LocalDate): String` |
| `composeApp/src/commonTest/kotlin/com/snowball/ui/util/DateFormatLongTest.kt` *(new)* | 2 | Tests for above |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/misc/MiscFormViewModel.kt` *(new)* | 3 | Slim form VM + state |
| `composeApp/src/commonTest/kotlin/com/snowball/ui/misc/MiscFormStateValidationTest.kt` *(new)* | 3 | Validation tests (5 cases) |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/misc/MiscFormScreen.kt` *(new)* | 4 | Slim form composable |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailViewModel.kt` *(new)* | 5 | Detail VM (load, setArchived, delete, undoPayment) |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailScreen.kt` *(new)* | 6 | Detail composable (scheduled + MISC variants) |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsViewModel.kt` | 7 | Split DebtsState into scheduledByCategory + miscRows; archive subtitle metadata |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsScreen.kt` | 8 | FAB dropdown menu; MISC section; archived FAB hide; archive subtitle |
| `composeApp/src/commonMain/kotlin/com/snowball/App.kt` | 9 | New routes + wiring; tap-to-detail flow |

---

## Task 1: Promote `projectedEndDate` to a public top-level function

**File:** `composeApp/src/commonMain/kotlin/com/snowball/domain/JourneyCalculator.kt`

**Goal:** Make `projectedEndDate(debt)` callable from `DebtDetailViewModel`. Currently it's private inside `JourneyCalculator`.

- [ ] **Step 1: Edit the file**

Open `composeApp/src/commonMain/kotlin/com/snowball/domain/JourneyCalculator.kt`. Move `projectedEndDate` from a private member function to a top-level function in the same package. Final structure:

```kotlin
package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

data class JourneyStats(
    val percentCleared: Int,
    val totalMelted: Double,
    val forecastEndDate: LocalDate?,
)

/**
 * Projects the date a debt's final payment will land, assuming on-time future payments.
 * Returns null when totalPayments is non-positive or the projection produces an
 * impossible date (e.g. dueDay 31 in a 30-day month with useLastDayOfMonth=false).
 */
fun projectedEndDate(debt: Debt): LocalDate? {
    if (debt.totalPayments <= 0) return null
    val endMonth = debt.startDate.plus(debt.totalPayments - 1, DateTimeUnit.MONTH)
    return effectiveDueDate(
        year = endMonth.year,
        month = endMonth.monthNumber,
        dueDay = debt.dueDay,
        useLastDay = debt.useLastDayOfMonth,
    )
}

object JourneyCalculator {
    fun compute(allDebts: List<Debt>, allPayments: List<Payment>): JourneyStats? {
        val totalMelted = allPayments.sumOf { it.amount }
        if (totalMelted == 0.0) return null

        val scheduled = allDebts.sumOf { it.monthlyAmount * it.totalPayments }
        val rawPct = if (scheduled > 0.0) ((totalMelted / scheduled) * 100).toInt() else 0
        val percent = rawPct.coerceIn(0, 100)

        val active = allDebts.filterNot { it.isArchived }
        val forecast = active.mapNotNull(::projectedEndDate).maxOrNull()

        return JourneyStats(percent, totalMelted, forecast)
    }
}
```

`::projectedEndDate` inside `compute` now resolves to the top-level function — same name, same package, no import change needed.

- [ ] **Step 2: Run existing tests to confirm no regressions**

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.domain.JourneyCalculatorTest"
```

Expected: PASS — all 10 tests still green.

- [ ] **Step 3: Build**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/domain/JourneyCalculator.kt
git commit -m "refactor(domain): promote projectedEndDate to top-level public function"
```

---

## Task 2: Add `formatLongDate` util + tests

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/util/DateFormat.kt`
- Create: `composeApp/src/commonTest/kotlin/com/snowball/ui/util/DateFormatLongTest.kt`

**Goal:** Add a "Jan 1, 2026" style date formatter for the Detail stats grid.

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/snowball/ui/util/DateFormatLongTest.kt`:

```kotlin
package com.snowball.ui.util

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormatLongTest {

    @Test
    fun formatsSingleDigitDay() {
        assertEquals("Jan 1, 2026", formatLongDate(LocalDate(2026, 1, 1)))
    }

    @Test
    fun formatsDoubleDigitDay() {
        assertEquals("Dec 25, 2026", formatLongDate(LocalDate(2026, 12, 25)))
    }

    @Test
    fun formatsEndOfMonth() {
        assertEquals("Feb 28, 2026", formatLongDate(LocalDate(2026, 2, 28)))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.ui.util.DateFormatLongTest"
```

Expected: FAIL — `formatLongDate` is unresolved.

- [ ] **Step 3: Add the function to DateFormat.kt**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/util/DateFormat.kt`. The current file has only `MONTHS` and `formatMonthYear`. Append:

```kotlin

/** Renders a date as "MMM D, YYYY" (e.g. "Jan 1, 2026"). */
fun formatLongDate(date: LocalDate): String =
    "${MONTHS[date.monthNumber - 1]} ${date.dayOfMonth}, ${date.year}"
```

`MONTHS` is already private to the file — the new function uses it directly.

- [ ] **Step 4: Verify tests pass**

```bash
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.ui.util.DateFormatLongTest"
```

Expected: PASS — all 3 tests green.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/util/DateFormat.kt composeApp/src/commonTest/kotlin/com/snowball/ui/util/DateFormatLongTest.kt
git commit -m "feat(util): add formatLongDate for stat-grid dates"
```

---

## Task 3: MiscFormViewModel + validation tests

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/misc/MiscFormViewModel.kt`
- Create: `composeApp/src/commonTest/kotlin/com/snowball/ui/misc/MiscFormStateValidationTest.kt`

**Goal:** State, validation extensions, and save logic for the slim MISC form.

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/snowball/ui/misc/MiscFormStateValidationTest.kt`:

```kotlin
package com.snowball.ui.misc

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MiscFormStateValidationTest {

    private fun validState() = MiscFormState(
        name = "Snack run",
        amount = "350",
        datePaidText = "2026-05-13",
    )

    @Test
    fun fullyValidStateIsValid() {
        assertTrue(validState().isValid())
    }

    @Test
    fun blankNameInvalid() {
        assertFalse(validState().copy(name = "").isValid())
        assertFalse(validState().copy(name = "   ").isValid())
    }

    @Test
    fun amountMustBePositive() {
        assertFalse(validState().copy(amount = "").isValid())
        assertFalse(validState().copy(amount = "0").isValid())
        assertFalse(validState().copy(amount = "-50").isValid())
        assertFalse(validState().copy(amount = "abc").isValid())
        assertTrue(validState().copy(amount = "0.01").isValid())
    }

    @Test
    fun datePaidMustParse() {
        assertFalse(validState().copy(datePaidText = "not a date").isValid())
        assertFalse(validState().copy(datePaidText = "").isValid())
        assertTrue(validState().copy(datePaidText = "2026-12-31").isValid())
    }

    @Test
    fun perFieldValidatorsExist() {
        val s = validState().copy(name = "", amount = "0")
        assertFalse(s.isValid())
        assertFalse(s.isNameValid())
        assertFalse(s.isAmountValid())
        assertTrue(s.isDatePaidValid())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.ui.misc.MiscFormStateValidationTest"
```

Expected: FAIL — `MiscFormState`, `isValid`, etc. unresolved.

- [ ] **Step 3: Create MiscFormViewModel.kt**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/misc/MiscFormViewModel.kt`:

```kotlin
package com.snowball.ui.misc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.snowball.data.Repos
import com.snowball.data.model.CategoryBehavior
import com.snowball.domain.today
import kotlinx.datetime.LocalDate

data class MiscFormState(
    val name: String = "",
    val amount: String = "",
    val datePaid: LocalDate = today(),
    val datePaidText: String = datePaid.toString(),
    val notes: String = "",
)

fun MiscFormState.isNameValid(): Boolean = name.trim().isNotEmpty()
fun MiscFormState.isAmountValid(): Boolean = (amount.toDoubleOrNull() ?: 0.0) > 0.0
fun MiscFormState.isDatePaidValid(): Boolean =
    runCatching { LocalDate.parse(datePaidText) }.isSuccess

fun MiscFormState.isValid(): Boolean =
    isNameValid() && isAmountValid() && isDatePaidValid()

class MiscFormViewModel(private val repos: Repos) {
    var state: MiscFormState by mutableStateOf(MiscFormState())
        private set

    val isValid: Boolean get() = state.isValid()

    fun update(transform: (MiscFormState) -> MiscFormState) { state = transform(state) }

    /** Creates the MISC debt + the single payment + auto-archives. Returns true on success. */
    fun save(): Boolean {
        if (!isValid) return false
        val miscCategory = repos.categories.all()
            .firstOrNull { it.behavior == CategoryBehavior.LEDGER }
            ?: return false
        val amount = state.amount.toDouble()
        val date = LocalDate.parse(state.datePaidText)
        val name = state.name.trim()

        repos.debts.add(
            name = name,
            categoryId = miscCategory.id,
            monthlyAmount = amount,
            totalPayments = 1,
            dueDay = 1,
            useLastDayOfMonth = false,
            startDate = date,
            notes = state.notes.ifBlank { null },
        )
        val newId = repos.debts.all().first().id
        repos.payments.markPaid(newId, date, amount)
        repos.debts.setArchived(newId, true)
        return true
    }
}
```

- [ ] **Step 4: Verify tests pass**

```bash
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.ui.misc.MiscFormStateValidationTest"
```

Expected: PASS — all 5 tests green.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/misc/MiscFormViewModel.kt composeApp/src/commonTest/kotlin/com/snowball/ui/misc/MiscFormStateValidationTest.kt
git commit -m "feat(misc): add MiscFormViewModel with validation extensions"
```

---

## Task 4: MiscFormScreen composable

**File:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/misc/MiscFormScreen.kt`

**Goal:** Slim form with TopAppBar + bottom Save pinned, four fields (Name, Amount, Date paid, Notes), inline validation with the same two-flag touched pattern as v0.1.1's DebtFormScreen.

- [ ] **Step 1: Create the file**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/misc/MiscFormScreen.kt`:

```kotlin
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
```

- [ ] **Step 2: Build**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/misc/MiscFormScreen.kt
git commit -m "feat(misc): add MiscFormScreen with TopAppBar + inline validation"
```

---

## Task 5: DebtDetailViewModel

**File:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailViewModel.kt`

**Goal:** State container + load/setArchived/delete/undoPayment.

- [ ] **Step 1: Create the file**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailViewModel.kt`:

```kotlin
package com.snowball.ui.detail

import com.snowball.data.Repos
import com.snowball.data.model.Category
import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import com.snowball.domain.projectedEndDate
import kotlinx.datetime.LocalDate

data class DebtDetailState(
    val debt: Debt,
    val category: Category,
    val paymentsMade: Int,
    val payments: List<Payment>,
    val projectedEndDate: LocalDate?,
    val amountLeft: Double,
)

class DebtDetailViewModel(private val repos: Repos, private val debtId: Long) {

    fun load(): DebtDetailState? {
        val debt = repos.debts.byId(debtId) ?: return null
        val category = repos.categories.byId(debt.categoryId) ?: return null
        val payments = repos.payments.historyForDebt(debtId)
        val made = payments.size
        val left = ((debt.totalPayments - made).coerceAtLeast(0)) * debt.monthlyAmount
        val projected = projectedEndDate(debt)
        return DebtDetailState(debt, category, made, payments, projected, left)
    }

    fun setArchived(archived: Boolean) { repos.debts.setArchived(debtId, archived) }

    fun delete(): Boolean {
        repos.debts.delete(debtId)
        return true
    }

    fun undoPayment(paymentId: Long) {
        repos.payments.delete(paymentId)
        val debt = repos.debts.byId(debtId) ?: return
        val remaining = repos.payments.countForDebt(debtId)
        if (debt.isArchived && remaining < debt.totalPayments) {
            repos.debts.setArchived(debtId, false)
        }
    }
}
```

- [ ] **Step 2: Build**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailViewModel.kt
git commit -m "feat(detail): add DebtDetailViewModel"
```

---

## Task 6: DebtDetailScreen composable

**File:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailScreen.kt`

**Goal:** Two layout variants in one file — scheduled (full Detail with arc + stats + history) and MISC (read-only ledger view). Selected by `category.behavior`.

- [ ] **Step 1: Create the file**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailScreen.kt`:

```kotlin
package com.snowball.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.data.model.CategoryBehavior
import com.snowball.data.model.Payment
import com.snowball.ui.components.PesoText
import com.snowball.ui.components.ProgressArc
import com.snowball.ui.components.icon
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.formatLongDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailScreen(
    vm: DebtDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    var tick by remember { mutableStateOf(0) }
    val state = remember(tick) { vm.load() }
    if (state == null) {
        // Debt was deleted out from under us; bounce back.
        onBack()
        return
    }
    val isMisc = state.category.behavior == CategoryBehavior.LEDGER

    var overflowOpen by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingUndoPayment by remember { mutableStateOf<Payment?>(null) }

    Scaffold(
        containerColor = SnowColors.Night,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.debt.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = SnowColors.Frost)
                    }
                },
                actions = {
                    IconButton(onClick = { overflowOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options", tint = SnowColors.Frost)
                    }
                    DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                        if (!isMisc && !state.debt.isArchived) {
                            DropdownMenuItem(
                                text = { Text("Edit", color = SnowColors.Frost) },
                                onClick = { overflowOpen = false; onEdit(state.debt.id) },
                            )
                        }
                        if (!isMisc) {
                            DropdownMenuItem(
                                text = { Text(if (state.debt.isArchived) "Unarchive" else "Archive", color = SnowColors.Frost) },
                                onClick = {
                                    overflowOpen = false
                                    vm.setArchived(!state.debt.isArchived)
                                    tick++
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete", color = SnowColors.Ember) },
                            onClick = { overflowOpen = false; showDeleteConfirm = true },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Header chips row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = state.category.icon(),
                    contentDescription = null,
                    tint = SnowColors.FrostDim,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    state.category.name.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                    color = SnowColors.FrostDim,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (state.debt.isArchived) "ARCHIVED" else "ACTIVE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                    color = if (state.debt.isArchived) SnowColors.FrostMute else SnowColors.Ice,
                )
            }
            Spacer(Modifier.height(24.dp))

            if (isMisc) {
                // MISC variant: big amount + paid date + notes
                PesoText(
                    amount = state.debt.monthlyAmount,
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.W300),
                    pesoColor = SnowColors.FrostMute,
                    numberColor = SnowColors.Frost,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Paid ${formatLongDate(state.debt.startDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnowColors.FrostMute,
                )
                if (!state.debt.notes.isNullOrBlank()) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "NOTES",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                        color = SnowColors.FrostDim,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.debt.notes!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SnowColors.Frost,
                    )
                }
            } else {
                // Scheduled variant: ProgressArc + amount left + stats + history
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ProgressArc(
                            progress = if (state.debt.totalPayments > 0) state.paymentsMade.toFloat() / state.debt.totalPayments else 0f,
                            modifier = Modifier.size(160.dp),
                        )
                        Text(
                            "${state.paymentsMade} of ${state.debt.totalPayments}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = SnowColors.Frost,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text("₱", color = SnowColors.FrostMute, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        com.snowball.ui.util.formatAmountWithSeparators(state.amountLeft),
                        style = MaterialTheme.typography.headlineSmall,
                        color = SnowColors.Frost,
                    )
                    Text(" left", color = SnowColors.FrostMute, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SnowColors.LineStrong),
                )
                Spacer(Modifier.height(20.dp))

                StatRow(label = "MONTHLY", value = "₱${com.snowball.ui.util.formatAmountWithSeparators(state.debt.monthlyAmount)}")
                Spacer(Modifier.height(12.dp))
                StatRow(
                    label = "DUE DAY",
                    value = state.debt.dueDay.toString() + if (state.debt.useLastDayOfMonth) " (or last day)" else "",
                )
                Spacer(Modifier.height(12.dp))
                StatRow(label = "STARTED", value = formatLongDate(state.debt.startDate))
                Spacer(Modifier.height(12.dp))
                StatRow(
                    label = "PROJECTED END",
                    value = state.projectedEndDate?.let { formatLongDate(it) } ?: "—",
                )

                if (state.payments.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(SnowColors.LineStrong),
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "PAYMENT HISTORY (${state.payments.size})",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                        color = SnowColors.FrostDim,
                    )
                    Spacer(Modifier.height(8.dp))
                    state.payments.forEach { payment ->
                        PaymentHistoryRow(payment = payment, onClick = { pendingUndoPayment = payment })
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete ${state.debt.name}?", style = MaterialTheme.typography.headlineSmall) },
                text = { Text("This removes the debt and all payment history.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        if (vm.delete()) onBack()
                    }) { Text("Delete", color = SnowColors.Ember) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = SnowColors.FrostMute)
                    }
                },
                containerColor = SnowColors.CardElev,
                titleContentColor = SnowColors.Frost,
                textContentColor = SnowColors.FrostMute,
            )
        }

        val undoTarget = pendingUndoPayment
        if (undoTarget != null) {
            AlertDialog(
                onDismissRequest = { pendingUndoPayment = null },
                title = { Text("Undo this payment?", style = MaterialTheme.typography.headlineSmall) },
                text = {
                    Text("₱${com.snowball.ui.util.formatAmountWithSeparators(undoTarget.amount)} recorded on ${formatLongDate(undoTarget.paidDate)} will be removed.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.undoPayment(undoTarget.id)
                        pendingUndoPayment = null
                        tick++
                    }) { Text("Undo", color = SnowColors.Ember) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingUndoPayment = null }) {
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
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = SnowColors.Frost,
        )
    }
}

@Composable
private fun PaymentHistoryRow(payment: Payment, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(SnowColors.Ice),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            formatLongDate(payment.paidDate),
            style = MaterialTheme.typography.bodyMedium,
            color = SnowColors.Frost,
            modifier = Modifier.weight(1f),
        )
        PesoText(
            amount = payment.amount,
            style = MaterialTheme.typography.bodyLarge,
            pesoColor = SnowColors.FrostDim,
            numberColor = SnowColors.Frost,
        )
    }
}
```

- [ ] **Step 2: Build**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailScreen.kt
git commit -m "feat(detail): add DebtDetailScreen with scheduled + MISC variants"
```

---

## Task 7: DebtsViewModel — split scheduled vs MISC; archive subtitle data

**File:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsViewModel.kt`

**Goal:** Two changes:
1. Split the `Map<Long, List<DebtRow>>` into `scheduledByCategory` (SCHEDULED categories only) and `miscRows` (LEDGER category).
2. Add `clearedDate: LocalDate?` and `totalPaidAmount: Double` to `DebtRow` for archive subtitle.

- [ ] **Step 1: Modify the file**

Replace `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsViewModel.kt`:

```kotlin
package com.snowball.ui.debts

import com.snowball.data.Repos
import com.snowball.data.model.Category
import com.snowball.data.model.CategoryBehavior
import com.snowball.data.model.Debt
import kotlinx.datetime.LocalDate

data class DebtRow(
    val debt: Debt,
    val paymentsMade: Int,
    val clearedDate: LocalDate?,        // most recent payment's paidDate when archived
    val totalPaidAmount: Double,         // sum of all payment amounts for the debt
)

data class DebtsState(
    val categories: List<Category>,
    val scheduledByCategory: Map<Long, List<DebtRow>>,
    val miscRows: List<DebtRow>,
    val showArchived: Boolean,
)

class DebtsViewModel(private val repos: Repos) {
    var showArchived: Boolean = false
        private set

    fun load(): DebtsState {
        val cats = repos.categories.all()
        val schedCatIds = cats.filter { it.behavior == CategoryBehavior.SCHEDULED }.map { it.id }.toSet()
        val miscCatIds = cats.filter { it.behavior == CategoryBehavior.LEDGER }.map { it.id }.toSet()

        // Scheduled rows respect the archived toggle.
        val scheduledDebts = if (showArchived) {
            repos.debts.all().filter { it.isArchived && it.categoryId in schedCatIds }
        } else {
            repos.debts.allActive().filter { it.categoryId in schedCatIds }
        }
        val scheduledByCategory = scheduledDebts
            .groupBy { it.categoryId }
            .mapValues { (_, debts) -> debts.map { d -> rowFor(d) } }

        // MISC rows are always shown in the Active view; suppressed in Archived view.
        val miscRows = if (showArchived) emptyList() else {
            repos.debts.all()
                .filter { it.categoryId in miscCatIds }
                .map { d -> rowFor(d) }
        }

        return DebtsState(cats, scheduledByCategory, miscRows, showArchived)
    }

    fun toggleArchive() { showArchived = !showArchived }

    fun delete(id: Long) { repos.debts.delete(id) }

    private fun rowFor(d: Debt): DebtRow {
        val payments = repos.payments.historyForDebt(d.id)
        val cleared = if (d.isArchived) payments.maxByOrNull { it.paidDate }?.paidDate else null
        val totalPaid = payments.sumOf { it.amount }
        return DebtRow(d, payments.size, cleared, totalPaid)
    }
}
```

- [ ] **Step 2: Build**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL. (DebtsScreen still references `state.debtsByCategory` which no longer exists — this will fail. Task 8 fixes DebtsScreen. The build between Task 7 and Task 8 is allowed to fail temporarily, BUT this would block intermediate testing. To avoid that, do Step 3 of THIS task to add a compatibility shim, then Task 8 cleans it up.)

Actually — the build WILL fail at this point because DebtsScreen still uses `state.debtsByCategory`. We have two choices:
- (a) Squash Task 7 + Task 8 into one commit (single agent dispatch).
- (b) Add a compatibility property `debtsByCategory: Map<Long, List<DebtRow>> get() = scheduledByCategory` to DebtsState.

For subagent isolation, option (b) is safer. Add to DebtsState:

```kotlin
@Deprecated("Use scheduledByCategory directly")
val debtsByCategory: Map<Long, List<DebtRow>> get() = scheduledByCategory
```

This shim is removed in Task 8 once DebtsScreen migrates.

Add the shim now to DebtsState, INSIDE the data class body (use a regular val with custom getter — data classes don't allow @Deprecated on a property in a clean way, but a non-data-class-property is fine):

Actually data classes can have additional properties via getter syntax outside the constructor:

```kotlin
data class DebtsState(
    val categories: List<Category>,
    val scheduledByCategory: Map<Long, List<DebtRow>>,
    val miscRows: List<DebtRow>,
    val showArchived: Boolean,
) {
    val debtsByCategory: Map<Long, List<DebtRow>> get() = scheduledByCategory
}
```

This keeps both call sites working. Task 8 removes the alias property.

- [ ] **Step 3: Build with the shim**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run tests**

```bash
./gradlew.bat :composeApp:testDebugUnitTest
```

Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsViewModel.kt
git commit -m "refactor(debts): split state into scheduledByCategory + miscRows with archive metadata"
```

---

## Task 8: DebtsScreen — FAB dropdown, MISC section, archived FAB hide, archive subtitle

**File:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsScreen.kt`

**Goal:** Multiple visual changes:
- Replace single-tap FAB with FAB + DropdownMenu (Add debt / Add MISC item).
- Hide FAB when `state.showArchived == true`.
- Render MISC section at the bottom of the Active view.
- For archived rows, show "Cleared {date} · ₱{total}" subtitle.
- Rename `onEdit` callback to `onOpenDebt` and add `onAddMisc`.
- Remove the `debtsByCategory` deprecation shim from DebtsState (do this at the end of the task).

- [ ] **Step 1: Rewrite DebtsScreen**

Replace `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsScreen.kt`:

```kotlin
package com.snowball.ui.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.ui.components.PesoText
import com.snowball.ui.components.icon
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.formatAmountWithSeparators
import com.snowball.ui.util.formatLongDate

@Composable
fun DebtsScreen(
    vm: DebtsViewModel,
    onAddDebt: () -> Unit,
    onAddMisc: () -> Unit,
    onOpenDebt: (Long) -> Unit,
) {
    var tick by remember { mutableStateOf(0) }
    val state = remember(tick) { vm.load() }
    var fabExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.showArchived) "Archived" else "Active",
                    style = MaterialTheme.typography.headlineLarge,
                    color = SnowColors.Frost,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { vm.toggleArchive(); tick++ },
                ) {
                    Text(
                        if (state.showArchived) "View active" else "View archived",
                        style = MaterialTheme.typography.labelMedium.copy(textDecoration = TextDecoration.Underline),
                        color = SnowColors.Ice,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = SnowColors.Ice,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            state.categories.forEach { cat ->
                val rows = state.scheduledByCategory[cat.id].orEmpty()
                if (rows.isEmpty()) return@forEach
                CategoryHeader(cat = cat)
                Spacer(Modifier.height(8.dp))
                rows.forEach { row ->
                    DebtRowItem(row = row, onClick = { onOpenDebt(row.debt.id) })
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(16.dp))
            }

            if (state.miscRows.isNotEmpty()) {
                // Render a MISC header treating the first miscRow's category as the source
                val miscCat = state.categories.firstOrNull { c -> state.miscRows.any { it.debt.categoryId == c.id } }
                if (miscCat != null) CategoryHeader(cat = miscCat)
                Spacer(Modifier.height(8.dp))
                state.miscRows.forEach { row ->
                    MiscRowItem(row = row, onClick = { onOpenDebt(row.debt.id) })
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(16.dp))
            }

            if (state.scheduledByCategory.isEmpty() && state.miscRows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (state.showArchived) "Nothing archived yet."
                        else "No debts yet. Tap + to add your first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SnowColors.FrostDim,
                    )
                }
            }
        }

        if (!state.showArchived) {
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
                FloatingActionButton(
                    onClick = { fabExpanded = true },
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    containerColor = SnowColors.Ice,
                    contentColor = SnowColors.Night,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "Add",
                        tint = SnowColors.Night,
                    )
                }
                DropdownMenu(
                    expanded = fabExpanded,
                    onDismissRequest = { fabExpanded = false },
                    offset = DpOffset(x = (-160).dp, y = 0.dp),
                ) {
                    DropdownMenuItem(
                        text = { Text("Add debt", color = SnowColors.Frost) },
                        onClick = { fabExpanded = false; onAddDebt() },
                    )
                    DropdownMenuItem(
                        text = { Text("Add MISC item", color = SnowColors.Frost) },
                        onClick = { fabExpanded = false; onAddMisc() },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(cat: com.snowball.data.model.Category) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = cat.icon(),
            contentDescription = null,
            tint = SnowColors.FrostDim,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            cat.name.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
    }
}

@Composable
private fun DebtRowItem(row: DebtRow, onClick: () -> Unit) {
    val d = row.debt
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SnowColors.CardElev)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                d.name,
                style = MaterialTheme.typography.headlineSmall,
                color = SnowColors.Frost,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = if (d.isArchived) {
                val date = row.clearedDate?.let { formatLongDate(it) } ?: "—"
                "Cleared $date · ₱${formatAmountWithSeparators(row.totalPaidAmount)}"
            } else {
                "Day ${d.dueDay} · ${d.totalPayments} months · ${row.paymentsMade}/${d.totalPayments} paid"
            }
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SnowColors.FrostMute,
            )
        }
        Spacer(Modifier.width(8.dp))
        PesoText(
            amount = d.monthlyAmount,
            style = MaterialTheme.typography.headlineSmall,
            pesoColor = SnowColors.FrostDim,
            numberColor = SnowColors.Frost,
        )
    }
}

@Composable
private fun MiscRowItem(row: DebtRow, onClick: () -> Unit) {
    val d = row.debt
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SnowColors.CardElev)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                d.name,
                style = MaterialTheme.typography.headlineSmall,
                color = SnowColors.Frost,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Paid ${formatLongDate(d.startDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = SnowColors.FrostMute,
            )
        }
        Spacer(Modifier.width(8.dp))
        PesoText(
            amount = d.monthlyAmount,
            style = MaterialTheme.typography.headlineSmall,
            pesoColor = SnowColors.FrostDim,
            numberColor = SnowColors.Frost,
        )
    }
}
```

- [ ] **Step 2: Remove the deprecation shim from DebtsViewModel**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsViewModel.kt`. Remove the body block of DebtsState (the `{ val debtsByCategory: ... }` part). Final state:

```kotlin
data class DebtsState(
    val categories: List<Category>,
    val scheduledByCategory: Map<Long, List<DebtRow>>,
    val miscRows: List<DebtRow>,
    val showArchived: Boolean,
)
```

- [ ] **Step 3: Build**

This will fail because `App.kt` still passes `onEdit = ...` to DebtsScreen. Don't worry — Task 9 fixes App.kt. For now, ignore the build error for App.kt only.

To proceed, modify App.kt temporarily to pass:

```kotlin
DebtsScreen(
    vm = debtsVm,
    onAddDebt = { route = Route.Form(null) },
    onAddMisc = { /* TODO Task 9 */ },
    onOpenDebt = { id -> route = Route.Form(id) },  // temporarily go to Form; Task 9 changes to DebtDetail
)
```

The whole-system build should pass now.

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsScreen.kt composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsViewModel.kt composeApp/src/commonMain/kotlin/com/snowball/App.kt
git commit -m "feat(debts): FAB dropdown menu, MISC section, archive subtitle, hidden FAB in archive view"
```

---

## Task 9: App.kt routing — new routes + tap-to-detail

**File:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/App.kt`

**Goal:** Add `Route.DebtDetail` and `Route.MiscForm` to the sealed interface; wire them into the `when (route)` branch; route the Debts tap to Detail instead of Form.

- [ ] **Step 1: Replace App.kt**

Replace `composeApp/src/commonMain/kotlin/com/snowball/App.kt`:

```kotlin
package com.snowball

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.snowball.data.Repos
import com.snowball.ui.debts.DebtsScreen
import com.snowball.ui.debts.DebtsViewModel
import com.snowball.ui.detail.DebtDetailScreen
import com.snowball.ui.detail.DebtDetailViewModel
import com.snowball.ui.form.DebtFormScreen
import com.snowball.ui.form.DebtFormViewModel
import com.snowball.ui.home.HomeScreen
import com.snowball.ui.home.HomeViewModel
import com.snowball.ui.misc.MiscFormScreen
import com.snowball.ui.misc.MiscFormViewModel
import com.snowball.ui.nav.BottomNav
import com.snowball.ui.nav.Tab
import com.snowball.ui.settings.SettingsScreen
import com.snowball.ui.settings.SettingsViewModel
import com.snowball.ui.theme.SnowballTheme

sealed interface Route {
    data object Tabs : Route
    data class Form(val existingDebtId: Long?) : Route
    data class DebtDetail(val debtId: Long) : Route
    data object MiscForm : Route
}

@Composable
fun App(repos: Repos) {
    SnowballTheme {
        var route by remember { mutableStateOf<Route>(Route.Tabs) }
        var tab by remember { mutableStateOf(Tab.Home) }
        var refreshKey by remember { mutableStateOf(0) }

        val homeVm = remember(refreshKey) { HomeViewModel(repos) }
        val debtsVm = remember(refreshKey) { DebtsViewModel(repos) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            val isTabs = route is Route.Tabs
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .then(if (isTabs) Modifier else Modifier.navigationBarsPadding())
            ) {
                when (val r = route) {
                    is Route.Tabs -> {
                        when (tab) {
                            Tab.Home -> HomeScreen(homeVm)
                            Tab.Debts -> DebtsScreen(
                                vm = debtsVm,
                                onAddDebt = { route = Route.Form(null) },
                                onAddMisc = { route = Route.MiscForm },
                                onOpenDebt = { id -> route = Route.DebtDetail(id) },
                            )
                            Tab.Settings -> {
                                val settingsVm = remember(refreshKey) { SettingsViewModel(repos) }
                                SettingsScreen(settingsVm)
                            }
                        }
                    }
                    is Route.Form -> {
                        val existing = r.existingDebtId?.let { repos.debts.byId(it) }
                        val formVm = remember(r.existingDebtId) { DebtFormViewModel(repos, existing) }
                        DebtFormScreen(
                            vm = formVm,
                            onCancel = { route = Route.Tabs },
                            onSaved = { route = Route.Tabs; refreshKey++ },
                        )
                    }
                    is Route.DebtDetail -> {
                        val detailVm = remember(r.debtId, refreshKey) { DebtDetailViewModel(repos, r.debtId) }
                        DebtDetailScreen(
                            vm = detailVm,
                            onBack = { route = Route.Tabs; refreshKey++ },
                            onEdit = { id -> route = Route.Form(id) },
                        )
                    }
                    is Route.MiscForm -> {
                        val miscVm = remember { MiscFormViewModel(repos) }
                        MiscFormScreen(
                            vm = miscVm,
                            onCancel = { route = Route.Tabs },
                            onSaved = { route = Route.Tabs; refreshKey++ },
                        )
                    }
                }
            }
            if (isTabs) {
                BottomNav(selected = tab, onSelect = { tab = it })
            }
        }
    }
}
```

- [ ] **Step 2: Build**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run all unit tests**

```bash
./gradlew.bat :composeApp:testDebugUnitTest
```

Expected: all tests pass.

- [ ] **Step 4: Install + verify**

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell pm clear com.snowball
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

Spot-check sequence:
1. Cleared install → Debts tab → "No debts yet" empty state, FAB visible.
2. Tap FAB → dropdown shows "Add debt" + "Add MISC item".
3. Tap "Add debt" → existing DebtFormScreen opens with "New debt".
4. Cancel out, tap FAB → tap "Add MISC item" → MiscFormScreen opens with "New MISC item".
5. Fill in name "Snack run", amount 350, default date, save → returns to Debts list. MISC section shows at bottom with "Snack run / Paid {today's date} / ₱350".
6. Tap "Snack run" → DebtDetailScreen opens, MISC variant (no progress arc, big amount).
7. Back to Debts. Add a regular debt via FAB → "Add debt" → fill in (dueDay 5, totalPayments 12, startDate before May 1) → save.
8. Tap that debt → DebtDetailScreen opens, scheduled variant. ProgressArc, stats, payment history (empty initially).
9. Overflow → Edit → DebtFormScreen opens with "Edit debt".
10. Back, mark a payment paid on Home (swipe row). Return to Detail → payment history shows one entry.
11. Tap that history row → confirm dialog → undo → row disappears.
12. Overflow → Archive → debt moves to archived state. Toggle Debts to "View archived" → FAB hidden, the archived debt shows "Cleared {date} · ₱0" subtitle (or amount of recorded payments if any).

Capture dumps:

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell uiautomator dump /sdcard/ui.xml
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe pull /sdcard/ui.xml /c/Users/Pika/AppData/Local/Temp/ui-v02b-detail.xml
```

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/App.kt
git commit -m "feat(nav): wire DebtDetail and MiscForm routes; tap-to-detail flow"
```

---

## Task 10: End-to-end verification + tag v0.2.1 + push

- [ ] **Step 1: Run the full test suite**

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:test
```

Expected: BUILD SUCCESSFUL. New tests added: 3 DateFormatLong + 5 MiscFormStateValidation = 8 new tests on top of the 52 from v0.2.0.

- [ ] **Step 2: Clear-install walkthrough**

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell pm clear com.snowball
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

Walk through the full flow as in Task 9 Step 4.

- [ ] **Step 3: Tag locally + push**

```bash
git tag -a v0.2.1 -m "v0.2.1 — Debt Detail screen + Archive subtitle + MISC items (sub-project B)"
git push origin main
git push origin v0.2.1
git log --oneline -3
git tag --list 'v*'
```

- [ ] **Step 4: Done**

---

## Spec coverage check

| Spec section | Plan task(s) |
|---|---|
| 1. Navigation routes (DebtDetail, MiscForm) | 9 |
| 2. DebtDetailScreen layout (scheduled + MISC variants) | 6 |
| 3. DebtDetailViewModel | 5 |
| 4. DebtsScreen — tap-to-detail rename | 8, 9 |
| 4. DebtsScreen — FAB dropdown | 8 |
| 4. DebtsScreen — archive subtitle | 8 (consumes data from 7) |
| 4. DebtsScreen — MISC section | 8 (consumes data from 7) |
| 5. MiscFormScreen | 4 |
| 6. MiscFormViewModel | 3 |
| Promote `projectedEndDate` | 1 |
| `formatLongDate` util | 2 |

All in-scope spec items mapped.

## Risks recap

- **Build between Tasks 7 and 8** — handled via the deprecation shim in DebtsState. Don't skip the shim or the build will break.
- **`repos.categories.byId` exists** (confirmed via grep). If for any reason it doesn't, `DebtDetailViewModel.load()` falls back to `repos.categories.all().firstOrNull { it.id == debt.categoryId }`.
- **MISC category lookup in MiscFormViewModel** uses `firstOrNull { it.behavior == LEDGER }`. If the seed inserted multiple LEDGER categories (it doesn't — only MISC is seeded as LEDGER), this picks the first. Safe for v0.2.1.
- **Archive subtitle when no payments exist** — `clearedDate` is null and `totalPaidAmount` is 0.0. Subtitle would read "Cleared — · ₱0". This shouldn't happen in practice (a debt is only archived when fully paid OR when MISC inserts payment immediately), but if it does the dash + ₱0 communicates "no recorded data" reasonably.
