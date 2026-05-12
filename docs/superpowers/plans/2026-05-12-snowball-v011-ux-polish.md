# Snowball v0.1.1 — UX Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the 20 numbered findings (+ accessibility set) from `docs/design/ux-review-2026-05-12.md`, excluding blocker #1 and blocker #2 which are reserved for v0.2.

**Architecture:** Pure UI polish on the existing Compose Multiplatform / Kotlin Multiplatform app. No domain or data-layer changes; no schema migrations; no new dependencies. Changes touch theme tokens, form/screen composables, two ViewModels, and add two small utility/validation pure functions. TDD for the pure functions; build + uiautomator verification for UI-only changes.

**Tech Stack:** Kotlin Multiplatform (commonMain), Jetpack Compose Multiplatform, Material 3, SQLDelight (no schema changes), kotlinx.datetime. JDK 21. AGP 8.7. Tests with `kotlin.test`.

**Reference docs:**
- Spec: `docs/superpowers/specs/2026-05-12-snowball-v011-ux-polish-design.md`
- Inspection: `docs/design/ux-review-2026-05-12.md`

---

## Build / verification recipe

Every task that touches UI ends with a build verification step. The recipe used throughout:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :composeApp:assembleDebug
```

For installing on the emulator (already at `emulator-5554`):

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
& $adb shell am start -n com.snowball/.MainActivity
```

For uiautomator dumps:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml C:\Users\Pika\AppData\Local\Temp\ui-<name>.xml
```

**NEVER call `Read` on a `.png` file.** Capture screenshots for the human user only — they will not be Read by the implementing agent.

---

## File-level change inventory

Files this plan modifies (with section anchor in the plan):

| File | Tasks | Purpose |
|---|---|---|
| `composeApp/src/commonMain/kotlin/com/snowball/ui/util/AmountFormat.kt` *(new)* | 1 | Shared `toFormFieldString` extension |
| `composeApp/src/commonTest/kotlin/com/snowball/ui/util/AmountFormatTest.kt` *(new)* | 1 | Tests for above |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/theme/Color.kt` | 2 | Lift FrostDim; bump Line alpha |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormViewModel.kt` | 3, 4, 5 | `isValid` derived state, init formatting fix, recordedPayments exposure |
| `composeApp/src/commonTest/kotlin/com/snowball/ui/form/DebtFormStateValidationTest.kt` *(new)* | 3 | Tests for validation logic |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt` | 6, 7, 8, 9, 10 | Scaffold restructure, validation UI, AlertDialog, conditional field, semantics |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/components/CutoffCard.kt` | 11 | SHORT BY/LEFT OVER conditional, DUE spacing |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt` | 12, 13 | Empty-state copy, spacing, bottom padding, semantics, swipe caption recolor |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/components/PesoText.kt` | 13 | mergeDescendants semantics |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsScreen.kt` | 14 | FAB icon, long-name layout, progress subtitle, View archived affordance |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsViewModel.kt` | 14 | Expose per-debt paymentsMade |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/nav/Nav.kt` | 15 | Active-tab pill background |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsScreen.kt` | 16 | Empty placeholder, commit-on-blur, formatted display, check ack, version stamp recolor |

Files NOT modified: anything under `data/`, `domain/`, the Android-specific `MainActivity.kt`, `Theme.kt`, `Type.kt`.

---

## Task 1: Add `toFormFieldString` utility + tests

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/util/AmountFormat.kt`
- Create: `composeApp/src/commonTest/kotlin/com/snowball/ui/util/AmountFormatTest.kt`

**Goal:** Pure-function helper that converts a `Double` to a string suitable for an editable text field — no decimal suffix when the value is whole, two decimal places when not. Distinct from `PesoText`'s existing private formatter, which adds thousand separators.

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/snowball/ui/util/AmountFormatTest.kt`:

```kotlin
package com.snowball.ui.util

import kotlin.test.Test
import kotlin.test.assertEquals

class AmountFormatTest {

    @Test
    fun zeroRendersAsBlank() {
        assertEquals("", 0.0.toFormFieldString())
    }

    @Test
    fun wholeNumberDropsDecimal() {
        assertEquals("1500", 1500.0.toFormFieldString())
    }

    @Test
    fun largeWholeNumberDropsDecimal() {
        assertEquals("9999999", 9_999_999.0.toFormFieldString())
    }

    @Test
    fun twoDecimalRoundsCorrectly() {
        assertEquals("1500.50", 1500.50.toFormFieldString())
    }

    @Test
    fun oneDecimalPadsTo2() {
        assertEquals("1500.50", 1500.5.toFormFieldString())
    }

    @Test
    fun threeDecimalsRoundDown() {
        assertEquals("1500.55", 1500.555.toFormFieldString())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.ui.util.AmountFormatTest"
```

Expected: FAIL — `toFormFieldString` is unresolved.

- [ ] **Step 3: Create the implementation file**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/util/AmountFormat.kt`:

```kotlin
package com.snowball.ui.util

import kotlin.math.abs

/**
 * Renders a Double as a string for a numeric text field.
 * Whole values render with no decimal (1500.0 -> "1500").
 * Non-whole values render with exactly two decimals (1500.5 -> "1500.50").
 * Zero renders as the empty string so an empty field doesn't display "0".
 */
fun Double.toFormFieldString(): String {
    if (this == 0.0) return ""
    val whole = this.toLong()
    val fraction = ((abs(this) - abs(whole)) * 100 + 0.5).toInt()
    return if (fraction == 0) whole.toString()
    else "$whole.${fraction.toString().padStart(2, '0')}"
}
```

- [ ] **Step 4: Run tests to verify they pass**

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.ui.util.AmountFormatTest"
```

Expected: PASS — all 6 tests green.

- [ ] **Step 5: Commit**

```powershell
git add composeApp/src/commonMain/kotlin/com/snowball/ui/util/AmountFormat.kt composeApp/src/commonTest/kotlin/com/snowball/ui/util/AmountFormatTest.kt
git commit -m "feat(util): add toFormFieldString helper for numeric inputs"
```

---

## Task 2: Lift `FrostDim` + bump `Line` alpha in Color.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/theme/Color.kt:13` (FrostDim) and `:9` (Line)

**Goal:** Single token changes that cascade to every site using these colors. FrostDim shifts from 2.7:1 to ~4.6:1 contrast on Night; Line bumps from invisible (4% white) to faint (14% white).

- [ ] **Step 1: Modify Color.kt**

Replace lines 9 and 13. Current file lines 1-21:

```kotlin
package com.snowball.ui.theme

import androidx.compose.ui.graphics.Color

object SnowColors {
    val Night = Color(0xFF0B0F14)
    val NightElev = Color(0xFF11161D)
    val CardElev = Color(0xFF161C25)
    val Line = Color(0x12FFFFFF)
    val LineStrong = Color(0x21FFFFFF)
    val Frost = Color(0xFFF2F5F8)
    val FrostMute = Color(0xFFA8B2BF)
    val FrostDim = Color(0xFF5E6874)
    val FrostDeep = Color(0xFF3C4452)
    val Ice = Color(0xFF9FCEE3)
    val IceSoft = Color(0x299FCEE3)
    val Champagne = Color(0xFFE8C68A)
    val Ember = Color(0xFFE07856)
    val Green = Color(0xFF8FD9B2)
}
```

Change line 9 `val Line = Color(0x12FFFFFF)` to:
```kotlin
    val Line = Color(0x24FFFFFF)
```

Change line 13 `val FrostDim = Color(0xFF5E6874)` to:
```kotlin
    val FrostDim = Color(0xFF7A8696)
```

`FrostDeep` stays in the file but no `Text(...)` will reference it after later tasks.

- [ ] **Step 2: Build to verify compile**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL. (Color references are by name, not by literal — no other files need changes for this task.)

- [ ] **Step 3: Install and visually spot-check**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
& $adb shell am start -n com.snowball/.MainActivity
```

Open the app. The "PAYMENTS", "DUE", "INCOME" labels should appear more legible than before (was muddy gray, now slightly lighter). Don't worry about exact visual comparison — the next tasks rely on this color taking effect.

- [ ] **Step 4: Commit**

```powershell
git add composeApp/src/commonMain/kotlin/com/snowball/ui/theme/Color.kt
git commit -m "feat(theme): lift FrostDim to AA-compliant contrast and bump Line alpha"
```

---

## Task 3: Extract validation logic + add `isValid` derived state to DebtFormViewModel

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormViewModel.kt`
- Create: `composeApp/src/commonTest/kotlin/com/snowball/ui/form/DebtFormStateValidationTest.kt`

**Goal:** Move the validation rules currently inline in `save()` (lines 54-63) into pure functions on `DebtFormState` so they can be unit-tested in `commonTest` without a real `Repos` instance. Expose a `DebtFormViewModel.isValid: Boolean` derived property the form can observe.

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/snowball/ui/form/DebtFormStateValidationTest.kt`:

```kotlin
package com.snowball.ui.form

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebtFormStateValidationTest {

    private fun validState() = DebtFormState(
        name = "Sloan",
        categoryId = 1L,
        monthlyAmount = "1500",
        totalPayments = "12",
        paymentsAlreadyMade = "0",
        dueDay = "10",
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
    fun missingCategoryInvalid() {
        assertFalse(validState().copy(categoryId = null).isValid())
    }

    @Test
    fun amountMustBePositive() {
        assertFalse(validState().copy(monthlyAmount = "").isValid())
        assertFalse(validState().copy(monthlyAmount = "0").isValid())
        assertFalse(validState().copy(monthlyAmount = "-50").isValid())
        assertFalse(validState().copy(monthlyAmount = "abc").isValid())
        assertTrue(validState().copy(monthlyAmount = "0.01").isValid())
    }

    @Test
    fun totalPaymentsInRange1to600() {
        assertFalse(validState().copy(totalPayments = "").isValid())
        assertFalse(validState().copy(totalPayments = "0").isValid())
        assertFalse(validState().copy(totalPayments = "601").isValid())
        assertTrue(validState().copy(totalPayments = "1").isValid())
        assertTrue(validState().copy(totalPayments = "600").isValid())
    }

    @Test
    fun dueDayInRange1to31() {
        assertFalse(validState().copy(dueDay = "0").isValid())
        assertFalse(validState().copy(dueDay = "32").isValid())
        assertFalse(validState().copy(dueDay = "").isValid())
        assertTrue(validState().copy(dueDay = "1").isValid())
        assertTrue(validState().copy(dueDay = "31").isValid())
    }

    @Test
    fun paymentsAlreadyMadeBoundedByTotal() {
        assertFalse(validState().copy(totalPayments = "12", paymentsAlreadyMade = "13").isValid())
        assertFalse(validState().copy(paymentsAlreadyMade = "-1").isValid())
        assertTrue(validState().copy(totalPayments = "12", paymentsAlreadyMade = "12").isValid())
        assertTrue(validState().copy(paymentsAlreadyMade = "").isValid())  // empty == 0
    }

    @Test
    fun perFieldValidatorsMatchOverallValidity() {
        val s = validState().copy(monthlyAmount = "", dueDay = "99")
        assertFalse(s.isValid())
        assertTrue(s.isNameValid())
        assertTrue(s.isCategoryValid())
        assertFalse(s.isMonthlyAmountValid())
        assertTrue(s.isTotalPaymentsValid())
        assertFalse(s.isDueDayValid())
        assertTrue(s.isPaymentsAlreadyMadeValid())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.ui.form.DebtFormStateValidationTest"
```

Expected: FAIL — `isValid`, `isNameValid`, etc. are unresolved.

- [ ] **Step 3: Add the extension functions to DebtFormViewModel.kt**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormViewModel.kt` and add these top-level extension functions immediately after the `DebtFormState` data class definition (between line 24 and line 26):

```kotlin
fun DebtFormState.isNameValid(): Boolean = name.trim().isNotEmpty()
fun DebtFormState.isCategoryValid(): Boolean = categoryId != null
fun DebtFormState.isMonthlyAmountValid(): Boolean =
    (monthlyAmount.toDoubleOrNull() ?: 0.0) > 0.0
fun DebtFormState.isTotalPaymentsValid(): Boolean =
    (totalPayments.toIntOrNull() ?: 0) in 1..600
fun DebtFormState.isDueDayValid(): Boolean =
    (dueDay.toIntOrNull() ?: 0) in 1..31
fun DebtFormState.isPaymentsAlreadyMadeValid(): Boolean {
    val already = paymentsAlreadyMade.toIntOrNull() ?: 0
    val total = totalPayments.toIntOrNull() ?: 0
    return already >= 0 && already <= total
}

fun DebtFormState.isValid(): Boolean =
    isNameValid() &&
        isCategoryValid() &&
        isMonthlyAmountValid() &&
        isTotalPaymentsValid() &&
        isDueDayValid() &&
        isPaymentsAlreadyMadeValid()
```

Then add this property to the `DebtFormViewModel` class — append it after the `categories` declaration around line 50:

```kotlin
    val isValid: Boolean get() = state.isValid()
```

Because `state` is a Compose `mutableStateOf`, reading it inside this getter participates in recomposition: Compose recomposes the form whenever `state` changes, which re-reads `isValid`, which reflects the new validity.

- [ ] **Step 4: Run tests to verify they pass**

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.ui.form.DebtFormStateValidationTest"
```

Expected: PASS — all 8 tests green.

- [ ] **Step 5: Build the full debug variant to verify no breakage elsewhere**

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```powershell
git add composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormViewModel.kt composeApp/src/commonTest/kotlin/com/snowball/ui/form/DebtFormStateValidationTest.kt
git commit -m "feat(form): extract validation into testable extensions; add isValid derived state"
```

---

## Task 4: Use `toFormFieldString` in DebtFormViewModel init

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormViewModel.kt:34-37`

**Goal:** Eliminate the "1500.0" display in edit mode by using the new utility.

- [ ] **Step 1: Modify the init block**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormViewModel.kt`. Add the import near the existing imports (after line 11):

```kotlin
import com.snowball.ui.util.toFormFieldString
```

Then replace the existing edit-mode init code at lines 34-37:

```kotlin
                monthlyAmount = existing.monthlyAmount.toString(),
                totalPayments = existing.totalPayments.toString(),
                paymentsAlreadyMade = repos.payments.countForDebt(existing.id).toString(),
                dueDay = existing.dueDay.toString(),
```

With:

```kotlin
                monthlyAmount = existing.monthlyAmount.toFormFieldString(),
                totalPayments = existing.totalPayments.toString(),
                paymentsAlreadyMade = repos.payments.countForDebt(existing.id).toString(),
                dueDay = existing.dueDay.toString(),
```

(`totalPayments`, `paymentsAlreadyMade`, and `dueDay` are already `Int`, so `.toString()` is correct — no `.0` issue. Only `monthlyAmount` is a `Double` and needs the new helper.)

- [ ] **Step 2: Build**

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Install + verify on emulator**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
& $adb shell am start -n com.snowball/.MainActivity
```

Tap an existing debt to open the edit form. The `MONTHLY AMOUNT` field should show `1500` (not `1500.0`). If no debt exists, add one with monthly = 1500, save, then re-enter edit mode.

Capture and inspect:
```powershell
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml C:\Users\Pika\AppData\Local\Temp\ui-edit-form-monthly.xml
```

Open the XML and confirm the monthly-amount field's `text` attribute reads `1500` rather than `1500.0`. (Use `Select-String` or open in a text editor.)

- [ ] **Step 4: Commit**

```powershell
git add composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormViewModel.kt
git commit -m "fix(form): drop .0 suffix when editing whole-number amounts"
```

---

## Task 5: Add `recordedPayments` exposure on DebtFormViewModel

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormViewModel.kt`

**Goal:** Edit mode needs a stable "X of Y" summary that doesn't change as the user edits the `paymentsAlreadyMade` field (which currently doubles as both the import-backfill input AND the running count display). Capture the running count at VM construction time and expose it separately.

- [ ] **Step 1: Modify DebtFormViewModel**

In `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormViewModel.kt`:

a. Add a property right next to `isEditing` (around line 48):

```kotlin
    val isEditing: Boolean = existingId != null
    val recordedPayments: Int = existing?.let { repos.payments.countForDebt(it.id) } ?: 0
    val originalTotalPayments: Int = existing?.totalPayments ?: 0
```

(`originalTotalPayments` captures the form's `existing.totalPayments` so the edit-mode summary uses the persisted total, not whatever the user might be typing into the total-payments field.)

- [ ] **Step 2: Build**

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```powershell
git add composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormViewModel.kt
git commit -m "feat(form): expose recordedPayments and originalTotalPayments for edit summary"
```

---

## Task 6: DebtFormScreen Scaffold restructure (TopAppBar + bottom-pinned Save)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt`

**Goal:** Replace the current `Column.verticalScroll` body and bottom Cancel/Save/Delete row with a `Scaffold(topBar = ..., bottomBar = ...)`. The TopAppBar shows the existing title plus a back-arrow navigation icon. The bottom bar pins the Save button. Cancel is removed (back arrow handles it). Delete moves into a TopAppBar overflow menu (Task 8).

This is the largest single change in the plan. Task 7 layers in validation feedback on the existing form fields, Task 8 layers in the Delete dialog. This task gets the chrome scaffolding in place first.

- [ ] **Step 1: Replace the file body**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt`. Update the imports block (top of file). Remove this import (no longer used after the Scaffold wraps the body):

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
```

(Keep `rememberScrollState` and `verticalScroll` — the new Scaffold body's `Column` still uses them.)

Add these new imports (alphabetically grouped):

```kotlin
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
```

Now replace the entire `DebtFormScreen` composable (lines 43-183 of the original file) with this implementation. The Field helpers and CategoryDropdown at the bottom of the file stay unchanged:

```kotlin
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
```

Notes on what's in / what's out of this task:
- `Cancel` button removed. The TopAppBar back arrow calls `onCancel`, preserving navigation semantics.
- `Delete` button removed — Task 8 puts it in the TopAppBar overflow menu.
- The form's `verticalScroll(rememberScrollState())` is preserved inside the Scaffold body (replacing `LazyColumn` from my earlier sketch — the form is short enough that `Column.verticalScroll` is fine, and it matches the existing pattern).
- `paymentsAlreadyMade` Field now wraps in `if (!vm.isEditing)` — Task 9 will add the edit-mode summary that replaces it. Until Task 9 lands, edit mode just shows nothing in that slot. (This is intermediate state, fine within a task.)
- Save button uses `enabled = vm.isValid` is NOT added yet — Task 7 wires that in.
- `placeholder` for the paymentsAlreadyMade field switches from `FrostDeep` to `FrostDim` (folds in part of the contrast fix).

- [ ] **Step 2: Build**

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Install + verify on emulator**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
& $adb shell am start -n com.snowball/.MainActivity
```

- Navigate to Debts → FAB. Form should appear with a TopAppBar showing "New debt" with a back arrow on the left.
- Tap the back arrow — should dismiss the form back to Debts (same effect as Cancel did).
- Re-open the FAB and scroll the form. The Save button at the bottom should stay pinned regardless of scroll position. The system gesture-nav inset should be respected (button doesn't sit under the gesture pill).

Capture and inspect:
```powershell
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml C:\Users\Pika\AppData\Local\Temp\ui-form-scaffold-new.xml
```

Open the XML and verify a `Toolbar` / `androidx.compose.ui.platform.ComposeView` containing "New debt" appears at the top, and "Save" is anchored near the bottom-nav (which is hidden — the form route hides the nav per `App.kt:83`).

- [ ] **Step 4: Commit**

```powershell
git add composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt
git commit -m "feat(form): restructure to Scaffold with TopAppBar + bottom-pinned Save"
```

---

## Task 7: Inline validation on form fields + disabled Save

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt`

**Goal:** Wire `vm.isValid` to the Save button's `enabled` state. Add per-field "touched" tracking — each `OutlinedTextField` flips to `isError = true` after the user has interacted with it AND left it AND the value is still invalid. Errors appear under each offending field via `supportingText`.

- [ ] **Step 1: Add the import for onFocusChanged**

In the imports block of `DebtFormScreen.kt`, add:

```kotlin
import androidx.compose.ui.focus.onFocusChanged
import com.snowball.ui.form.isCategoryValid
import com.snowball.ui.form.isDueDayValid
import com.snowball.ui.form.isMonthlyAmountValid
import com.snowball.ui.form.isNameValid
import com.snowball.ui.form.isPaymentsAlreadyMadeValid
import com.snowball.ui.form.isTotalPaymentsValid
```

(The `isXxxValid` imports are from the same package — actually the same file as `DebtFormViewModel`. Same-package imports are unnecessary in Kotlin. Strike those; they're already in scope.)

So the only new import is:

```kotlin
import androidx.compose.ui.focus.onFocusChanged
```

- [ ] **Step 2: Add per-field touched state at the top of the composable**

At the top of `DebtFormScreen`'s body (right after `val state = vm.state`), declare:

```kotlin
    var nameTouched by remember { mutableStateOf(false) }
    var amountTouched by remember { mutableStateOf(false) }
    var totalTouched by remember { mutableStateOf(false) }
    var dueDayTouched by remember { mutableStateOf(false) }
    var alreadyMadeTouched by remember { mutableStateOf(false) }
```

Category and start-date don't need touched tracking — category is a dropdown selection (it can't be partially typed), and start-date has its own parsing fallback. (If the spec wants stricter date validation later we can add it.)

- [ ] **Step 3: Wire each field's `isError` + `supportingText` + `onFocusChanged`**

For the **Name** field, replace the existing `OutlinedTextField` block (inside `Field("Name") { ... }`):

```kotlin
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
                        .onFocusChanged { if (!it.isFocused) nameTouched = true },
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
```

For **Monthly amount**:

```kotlin
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
                        .onFocusChanged { if (!it.isFocused) amountTouched = true },
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
```

For the **Total payments** + **Due day** Row:

```kotlin
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Field("Total payments", modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = state.totalPayments,
                        onValueChange = { v -> vm.update { it.copy(totalPayments = v.filter { c -> c.isDigit() }) } },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = totalTouched && !state.isTotalPaymentsValid(),
                        supportingText = {
                            if (totalTouched && !state.isTotalPaymentsValid()) Text("1 to 600", color = SnowColors.Ember)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (!it.isFocused) totalTouched = true },
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
                            if (dueDayTouched && !state.isDueDayValid()) Text("1 to 31", color = SnowColors.Ember)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (!it.isFocused) dueDayTouched = true },
                        colors = textFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
```

For **Payments already made** (inside the `if (!vm.isEditing)` block from Task 6):

```kotlin
            if (!vm.isEditing) {
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
                            .onFocusChanged { if (!it.isFocused) alreadyMadeTouched = true },
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
```

- [ ] **Step 4: Add `enabled = vm.isValid` on the Save button**

In the `bottomBar` Surface, modify the `Button(...)` to add `enabled = vm.isValid`:

```kotlin
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
```

- [ ] **Step 5: Build**

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Install + verify on emulator**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
& $adb shell am start -n com.snowball/.MainActivity
```

Tests to run by hand:
1. Open the new-debt form (Debts → FAB). Save button visually muted (FrostMute background, not Ice). Tapping it does nothing.
2. Type a name "Sloan", tap out of the field, then back to the empty name field — no error yet (name is now valid).
3. Tap Monthly amount, leave it empty, tap out — error "Enter an amount greater than 0" appears under the field. Field border turns Ember.
4. Type 1500, tap out — error clears, border returns to neutral.
5. Fill remaining required fields (Category dropdown, Total payments 12, Due day 10). Save button background flips to Ice. Tap Save — form dismisses to Debts list.

Capture both states:
```powershell
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml C:\Users\Pika\AppData\Local\Temp\ui-form-save-disabled.xml
# (after filling all fields)
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml C:\Users\Pika\AppData\Local\Temp\ui-form-save-enabled.xml
```

- [ ] **Step 7: Commit**

```powershell
git add composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt
git commit -m "feat(form): wire inline validation errors and disable Save until valid"
```

---

## Task 8: Delete AlertDialog from TopAppBar overflow menu

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt`

**Goal:** Wire a Material `AlertDialog` confirmation. The Delete trigger lives in a TopAppBar overflow menu (3-dot icon) that only renders in edit mode.

- [ ] **Step 1: Add the import for AlertDialog**

In the imports block of `DebtFormScreen.kt`, add:

```kotlin
import androidx.compose.material3.AlertDialog
```

(`DropdownMenu` and `DropdownMenuItem` are already imported per the original file.)

- [ ] **Step 2: Add a dialog-state variable at the top of the composable**

Inside `DebtFormScreen`, near the other `remember { mutableStateOf(...) }` calls:

```kotlin
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
```

- [ ] **Step 3: Add the `actions` slot to the TopAppBar**

Edit the existing `TopAppBar(...)` block to include an `actions` slot before `colors`:

```kotlin
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
```

- [ ] **Step 4: Render the AlertDialog at the end of the Scaffold body**

After the closing brace of the `Column` inside the Scaffold body but BEFORE the closing brace of the Scaffold's content lambda, add:

```kotlin
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
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        if (vm.delete()) onSaved()
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
```

- [ ] **Step 5: Build**

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Install + verify on emulator**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
& $adb shell am start -n com.snowball/.MainActivity
```

Verify:
1. New-debt form (FAB): no 3-dot overflow icon in the TopAppBar.
2. Edit an existing debt: 3-dot icon visible top-right. Tap it → DropdownMenu shows a single "Delete" item in Ember.
3. Tap Delete → AlertDialog appears with "Delete {name}?" title, body text, Cancel + Delete buttons.
4. Tap Cancel → dialog dismisses, debt unchanged.
5. Tap Delete in the dialog → debt is removed, form dismisses to Debts list (no debt shown).

Capture:
```powershell
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml C:\Users\Pika\AppData\Local\Temp\ui-form-delete-dialog.xml
```

- [ ] **Step 7: Commit**

```powershell
git add composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt
git commit -m "feat(form): confirm delete via AlertDialog from TopAppBar overflow"
```

---

## Task 9: Edit-mode "Payments recorded" summary

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt`

**Goal:** In edit mode, replace the empty space where the "Payments already made" field used to be (Task 6 wrapped it in `if (!vm.isEditing)`) with a static `PAYMENTS RECORDED — X of Y` summary line.

- [ ] **Step 1: Add the edit-mode summary block**

In `DebtFormScreen.kt`, find the existing `if (!vm.isEditing)` block (added in Task 6). Add an `else` branch immediately after it that renders the summary:

```kotlin
            if (!vm.isEditing) {
                Field("Payments already made (optional)") {
                    /* existing field code from Task 7 */
                }
                Spacer(Modifier.height(16.dp))
            } else {
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
                Spacer(Modifier.height(16.dp))
            }
```

- [ ] **Step 2: Build**

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Install + verify on emulator**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
& $adb shell am start -n com.snowball/.MainActivity
```

Steps:
1. Add a new debt (e.g. Sloan, ₱1500, 12 months, due 10). Save.
2. On Home, swipe one row left to mark paid (or tap-toggle).
3. Open Debts → tap Sloan → Edit form opens. Verify a `PAYMENTS RECORDED` label appears with `1 of 12` underneath, where the `Payments already made` field used to be.
4. Verify the new-debt form still shows the original editable `Payments already made` field (FAB on Debts → no `PAYMENTS RECORDED` line).

Capture:
```powershell
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml C:\Users\Pika\AppData\Local\Temp\ui-form-edit-summary.xml
```

- [ ] **Step 4: Commit**

```powershell
git add composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt
git commit -m "feat(form): show static Payments Recorded summary in edit mode"
```

---

## Task 10: Form field semantics (TalkBack labels)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt`

**Goal:** Wrap each `Field` column with `Modifier.semantics(mergeDescendants = true) { contentDescription = label }` so TalkBack announces the visible all-caps label rather than "edit box, no name". Also wire the "Use last day of month" Switch row's semantics.

- [ ] **Step 1: Add semantics imports**

In `DebtFormScreen.kt`:

```kotlin
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
```

- [ ] **Step 2: Modify the `Field` helper composable**

Find the private `Field` helper near the bottom of the file. Replace it with:

```kotlin
@Composable
private fun Field(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
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
```

Every `Field("Xxx") { ... }` call automatically inherits the new semantics.

- [ ] **Step 3: Add semantics to the Switch row**

Find the existing Switch row (lines 125-137 of the original file — the `Row(verticalAlignment = Alignment.CenterVertically) { Switch(...); Spacer(...); Text("Use last day of month...") }`). Wrap the Row's modifier:

```kotlin
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
```

- [ ] **Step 4: Build**

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Install + verify NAF flag cleared via uiautomator**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
& $adb shell am start -n com.snowball/.MainActivity
```

Navigate to Debts → FAB (new debt form). Dump UI:

```powershell
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml C:\Users\Pika\AppData\Local\Temp\ui-form-semantics.xml
```

Open the XML in a text editor and grep for `NAF="true"` — the form fields should no longer have this attribute. The nodes representing form fields should have `content-desc` populated with the label string ("Name", "Monthly amount", etc.) instead of empty.

If TalkBack is available on the emulator:
```powershell
& $adb shell settings put secure enabled_accessibility_services com.google.android.marvin.talkback/.TalkBackService
```
Walk through the form by swiping right between fields. TalkBack announces "Name, edit box" instead of just "edit box". Disable when done:
```powershell
& $adb shell settings put secure enabled_accessibility_services ""
```

- [ ] **Step 6: Commit**

```powershell
git add composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt
git commit -m "fix(form): wire field labels into semantics so TalkBack announces them"
```

---

## Task 11: CutoffCard — SHORT BY / LEFT OVER conditional + DUE spacing

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/CutoffCard.kt`

**Goal:** Negative breathing room flips the LEFT OVER cell to Ember color and "SHORT BY" label (sign-stripped). Asymmetric DUE label spacing (8/24) becomes 12/20.

- [ ] **Step 1: Add `kotlin.math.abs` import**

At the top of `CutoffCard.kt`:

```kotlin
import kotlin.math.abs
```

- [ ] **Step 2: Modify the LEFT OVER LedgerCell**

In `CutoffCard.kt`, find the Row at lines 74-80 that contains the two `LedgerCell` calls. Replace the second `LedgerCell` call with:

```kotlin
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            LedgerCell(label = "INCOME", amount = incomePerCutoff, color = SnowColors.Frost, modifier = Modifier.weight(1f))
            val isShort = summary.breathingRoom < 0
            LedgerCell(
                label = if (isShort) "SHORT BY" else "LEFT OVER",
                amount = abs(summary.breathingRoom),
                color = if (isShort) SnowColors.Ember else SnowColors.Ice,
                modifier = Modifier.weight(1f),
            )
        }
```

- [ ] **Step 3: Adjust DUE label spacing**

In `CutoffCard.kt`, find lines 58-72 (the "DUE" label + PesoText + bottom spacer):

Current:
```kotlin
        Spacer(Modifier.height(24.dp))
        Text(
            "DUE",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(8.dp))
        PesoText(
            amount = summary.dueTotal,
            ...
        )

        Spacer(Modifier.height(24.dp))
```

Change the `Spacer(Modifier.height(8.dp))` between "DUE" and PesoText to `12.dp`, and the `Spacer(Modifier.height(24.dp))` between PesoText and the Row of LedgerCells to `20.dp`:

```kotlin
        Spacer(Modifier.height(24.dp))
        Text(
            "DUE",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(12.dp))
        PesoText(
            amount = summary.dueTotal,
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.W300),
            pesoColor = SnowColors.FrostMute,
            numberColor = SnowColors.Frost,
        )

        Spacer(Modifier.height(20.dp))
```

(The 24.dp above the DUE label and the 28.dp `padding(vertical = 28.dp)` on the outer Column are unchanged.)

- [ ] **Step 4: Build**

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Install + verify on emulator**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
& $adb shell am start -n com.snowball/.MainActivity
```

Verify the positive case:
1. Settings → set income = 25000.
2. Add a debt with monthly = 1500 (or whatever keeps you positive).
3. Home: LEFT OVER cell shows Ice-blue value.

Verify the negative case:
1. Add a second debt with monthly = 30000 (or any amount that pushes total above income).
2. Home: SHORT BY cell shows Ember value, no minus sign in the displayed number.

Capture both:
```powershell
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml C:\Users\Pika\AppData\Local\Temp\ui-home-positive.xml
# (after adding the second debt)
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml C:\Users\Pika\AppData\Local\Temp\ui-home-negative.xml
```

Open both XMLs and confirm the label text strings ("LEFT OVER" vs "SHORT BY") match the income-vs-debt relationship.

- [ ] **Step 6: Commit**

```powershell
git add composeApp/src/commonMain/kotlin/com/snowball/ui/components/CutoffCard.kt
git commit -m "feat(home): flip LEFT OVER to SHORT BY with Ember when income is short"
```

---

## Task 12: HomeScreen — empty-state copy + spacing + last-row padding + swipe caption recolor

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt`

**Goal:** Fold four small Home polish items into a single edit pass.

- [ ] **Step 1: Replace EmptyHint signature + body**

In `HomeScreen.kt`, find the `EmptyHint` composable (lines 234-246). Replace it with:

```kotlin
@Composable
private fun EmptyHint(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = SnowColors.FrostDim,
        )
    }
}
```

- [ ] **Step 2: Wire conditional message in HomeScreen body**

In `HomeScreen.kt`, find the empty-state branch (around line 75-77):

```kotlin
        if (state.rows.isEmpty()) {
            EmptyHint()
```

Replace with:

```kotlin
        if (state.rows.isEmpty()) {
            val message = when {
                state.income == 0.0 -> "Start by setting your income in Settings."
                else -> "No payments due this cutoff yet.\nAdd debts from the Debts tab."
            }
            EmptyHint(message)
```

(Note: the spec proposed three branches; the actual `HomeState` only carries `income` separately from `rows`, and the inspector's UX intent boils down to "income first, then debts" — so the binary branch captures the design.)

- [ ] **Step 3: Bump PAYMENTS section header spacer**

In `HomeScreen.kt`, find line 67 (`Spacer(Modifier.height(2.dp))` between "PAYMENTS" label and the italic instructions). Change to:

```kotlin
        Spacer(Modifier.height(8.dp))
```

- [ ] **Step 4: Recolor swipe caption**

In `HomeScreen.kt`, find line 71 (the swipe instruction `Text` with `color = SnowColors.FrostDeep`). Change to:

```kotlin
        Text(
            "Swipe left to mark paid · swipe right to undo",
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = SnowColors.FrostMute,
        )
```

- [ ] **Step 5: Add bottom padding to the scroll column**

In `HomeScreen.kt`, after the existing `state.rows.forEach { row -> ... }` block (which ends around line 86), add a `Spacer` before the closing brace of the outer Column to keep the last row clear of the bottom nav at font_scale 1.30:

```kotlin
            state.rows.forEach { row ->
                key(row.debt.id) {
                    SwipeablePaymentRow(...)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
```

(The `Spacer` sits inside the `else` branch of `if (state.rows.isEmpty())`, after the `forEach`. If your editor's auto-format puts it elsewhere, manually place it.)

- [ ] **Step 6: Build**

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Install + verify**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
& $adb shell pm clear com.snowball
& $adb shell am start -n com.snowball/.MainActivity
```

Verify:
1. Cleared install → Home shows "Start by setting your income in Settings."
2. Settings → enter 25000 → return to Home. Empty message now reads "No payments due this cutoff yet. Add debts from the Debts tab."
3. PAYMENTS / swipe-caption gap should be visibly larger (8dp vs 2dp). Caption text is now lighter gray (FrostMute), legible at arm's length.
4. Add 3-4 debts (all dueDay 1-14, startDate before May 1 so they appear on Home). Set `font_scale 1.30`:
   ```powershell
   & $adb shell settings put system font_scale 1.30
   & $adb shell am force-stop com.snowball
   & $adb shell am start -n com.snowball/.MainActivity
   ```
   The last payment row should have a ~16dp gap above the bottom nav. Revert:
   ```powershell
   & $adb shell settings put system font_scale 1.0
   & $adb shell am force-stop com.snowball
   & $adb shell am start -n com.snowball/.MainActivity
   ```

- [ ] **Step 8: Commit**

```powershell
git add composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt
git commit -m "feat(home): conditional empty copy, looser PAYMENTS gap, contrast-safe caption, bottom inset"
```

---

## Task 13: PesoText + payment row semantics

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/PesoText.kt`

**Goal:** Make the Home payment row announce a meaningful description (debt name, amount, day) and a checkbox role with paid/not-paid state. Make PesoText announce as a single number+unit rather than two separate text elements.

- [ ] **Step 1: PesoText semantics**

In `composeApp/src/commonMain/kotlin/com/snowball/ui/components/PesoText.kt`, add imports:

```kotlin
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
```

Modify the outer `Row` to wrap with mergeDescendants semantics. Replace lines 34-39:

```kotlin
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text("₱", style = pesoStyle)
        Spacer(Modifier.width(2.dp))
        Text(formatted, style = numStyle)
    }
```

With:

```kotlin
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "₱$formatted"
        },
        verticalAlignment = Alignment.Bottom,
    ) {
        Text("₱", style = pesoStyle)
        Spacer(Modifier.width(2.dp))
        Text(formatted, style = numStyle)
    }
```

- [ ] **Step 2: Payment row semantics in HomeScreen**

In `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt`, add imports:

```kotlin
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
```

Modify `PaymentRowContent` (line 133-167). Update the outer Row's modifier:

```kotlin
@Composable
private fun PaymentRowContent(row: DueRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SnowColors.Night)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                stateDescription = if (row.isPaidThisCycle) "Paid" else "Not paid"
                contentDescription = "${row.debt.name}, ₱${row.amount.toLong()}, due ${row.effectiveDueDate}"
            }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressArc(
            progress = if (row.isPaidThisCycle) 1f else 0f,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.debt.name,
                style = MaterialTheme.typography.headlineSmall,
                color = if (row.isPaidThisCycle) SnowColors.FrostDim else SnowColors.Frost,
            )
            Text(
                "Due ${row.effectiveDueDate}",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = SnowColors.FrostMute,
            )
        }
        PesoText(
            amount = row.amount,
            style = MaterialTheme.typography.headlineMedium,
            pesoColor = SnowColors.FrostDim,
            numberColor = if (row.isPaidThisCycle) SnowColors.FrostMute else SnowColors.Frost,
        )
    }
}
```

- [ ] **Step 3: Build**

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Install + verify**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
& $adb shell am start -n com.snowball/.MainActivity
```

Dump and inspect:
```powershell
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml C:\Users\Pika\AppData\Local\Temp\ui-home-semantics.xml
```

Open the XML and confirm each payment row node has `content-desc="Sloan, ₱1500, due ..."` and `class="android.view.View"` with `checkable="true"` (or similar — uiautomator surfaces the Role.Checkbox semantics as `checkable`). PesoText rows show a merged content-desc on their parent Row node.

- [ ] **Step 5: Commit**

```powershell
git add composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt composeApp/src/commonMain/kotlin/com/snowball/ui/components/PesoText.kt
git commit -m "fix(a11y): announce payment rows with role and state; merge PesoText for screen readers"
```

---

## Task 14: DebtsScreen — FAB icon, long-name layout, progress subtitle, View archived affordance

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsViewModel.kt`

**Goal:** Fold four DebtsScreen polish items into a single pass. Also expose per-debt paymentsMade from the ViewModel so the subtitle can show it.

- [ ] **Step 1: Expose paymentsMade in DebtsViewModel**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsViewModel.kt`. Replace it with:

```kotlin
package com.snowball.ui.debts

import com.snowball.data.Repos
import com.snowball.data.model.Category
import com.snowball.data.model.Debt

data class DebtRow(val debt: Debt, val paymentsMade: Int)

data class DebtsState(
    val categories: List<Category>,
    val debtsByCategory: Map<Long, List<DebtRow>>,
    val showArchived: Boolean,
)

class DebtsViewModel(private val repos: Repos) {
    var showArchived: Boolean = false
        private set

    fun load(): DebtsState {
        val cats = repos.categories.all()
        val all = if (showArchived) repos.debts.all().filter { it.isArchived } else repos.debts.allActive()
        val grouped = all.groupBy { it.categoryId }
            .mapValues { (_, debts) ->
                debts.map { d -> DebtRow(d, repos.payments.countForDebt(d.id)) }
            }
        return DebtsState(cats, grouped, showArchived)
    }

    fun toggleArchive() { showArchived = !showArchived }

    fun delete(id: Long) { repos.debts.delete(id) }
}
```

- [ ] **Step 2: Rewrite DebtsScreen with all four fixes**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsScreen.kt`. Replace it entirely with:

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.ui.components.PesoText
import com.snowball.ui.components.icon
import com.snowball.ui.theme.SnowColors

@Composable
fun DebtsScreen(
    vm: DebtsViewModel,
    onAddDebt: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    var tick by remember { mutableStateOf(0) }
    val state = remember(tick) { vm.load() }

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
                val debts = state.debtsByCategory[cat.id].orEmpty()
                if (debts.isEmpty()) return@forEach
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
                Spacer(Modifier.height(8.dp))
                debts.forEach { row ->
                    val d = row.debt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SnowColors.CardElev)
                            .clickable { onEdit(d.id) }
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
                                "Day ${d.dueDay} · ${d.totalPayments} months · ${row.paymentsMade}/${d.totalPayments} paid",
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
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(16.dp))
            }

            if (state.debtsByCategory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No debts yet. Tap + to add your first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SnowColors.FrostDim,
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddDebt,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(56.dp)
                .clip(CircleShape),
            containerColor = SnowColors.Ice,
            contentColor = SnowColors.Night,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Add debt",
                tint = SnowColors.Night,
            )
        }
    }
}
```

Changes from the original DebtsScreen (in order):
- Removed unused `import androidx.compose.material3.Text` duplication, added new icon + decoration imports.
- "View archived" is now a Row containing an underlined Text + ChevronRight icon (was bare clickable Text).
- Each debt row in the inner `debts.forEach { ... }` loop:
  - `verticalAlignment = Alignment.Top` (was `CenterVertically`)
  - Name `Text` gains `maxLines = 2, overflow = TextOverflow.Ellipsis`.
  - Subtitle includes the new `· ${row.paymentsMade}/${d.totalPayments} paid` segment.
  - Loop variable renamed from `d` to `row` and `d = row.debt` is introduced — adapts to new `DebtRow` type.
  - 8.dp spacer added between the Column and the PesoText to keep them from touching at narrow widths.
- FAB content is now an `Icon(Icons.Outlined.Add, ...)` instead of `Text("+")` — centered correctly and announces "Add debt" to TalkBack.

- [ ] **Step 3: Build**

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

If the build fails because something elsewhere passes a `Debt` to a function that now expects a `DebtRow`, check that nothing outside `DebtsScreen.kt` consumes `DebtsState.debtsByCategory` directly. The current codebase has no such consumer (verify with grep `debtsByCategory` across the repo). The data shape change is internal to the Debts feature.

- [ ] **Step 4: Install + verify**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
& $adb shell am start -n com.snowball/.MainActivity
```

Verify:
1. Debts tab → FAB shows a `+` icon (Material Add icon, centered vertically in the circle, not a typographic plus).
2. With at least one debt: row subtitle reads `Day 10 · 12 months · 0/12 paid` (or similar). Mark one payment paid on Home, return to Debts: subtitle updates to `0/12 paid` → `1/12 paid` after the data reloads on tab visit.
3. Add a debt with name "A really really really long debt name that should test wrapping" and amount 9999999. On Debts, the name wraps to at most 2 lines with ellipsis; the PesoText right-anchor sits at the top, not vertically centered.
4. Tap "View archived" — the chevron is visible and the text is underlined; tapping flips to the Archived view.

Capture:
```powershell
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml C:\Users\Pika\AppData\Local\Temp\ui-debts-polished.xml
```

Confirm the FAB node has `content-desc="Add debt"` instead of being empty.

- [ ] **Step 5: Commit**

```powershell
git add composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsScreen.kt composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsViewModel.kt
git commit -m "feat(debts): Material FAB icon, long-name ellipsis, paid count, archive affordance"
```

---

## Task 15: Nav.kt — active-tab pill

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/nav/Nav.kt`

**Goal:** Add a translucent Ice-tinted rounded-rectangle background behind the selected tab's icon so the active state is visible at arm's length, not just by tint shift.

- [ ] **Step 1: Modify Nav.kt**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/nav/Nav.kt`. Add imports:

```kotlin
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
```

Replace the existing tab loop (lines 47-72) with:

```kotlin
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Tab.entries.forEach { tab ->
                val active = tab == selected
                val tint = if (active) SnowColors.Frost else SnowColors.FrostDim
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (active) SnowColors.Ice.copy(alpha = 0.10f)
                                else androidx.compose.ui.graphics.Color.Transparent
                            )
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.2.sp),
                        color = tint,
                    )
                }
            }
        }
```

The inner Column wraps just the Icon — the label remains outside the pill so it doesn't get a background tint on top of the existing color shift.

- [ ] **Step 2: Build**

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Install + verify**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
& $adb shell am start -n com.snowball/.MainActivity
```

Switch between tabs. The selected tab's icon should have a faint ice-blue rounded background behind it. Tapping another tab moves the pill to that tab.

- [ ] **Step 4: Commit**

```powershell
git add composeApp/src/commonMain/kotlin/com/snowball/ui/nav/Nav.kt
git commit -m "feat(nav): add ice-tinted pill behind active tab icon"
```

---

## Task 16: SettingsScreen — empty placeholder, commit-on-blur, formatted display, check ack, version stamp recolor

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsScreen.kt`

**Goal:** First-use empty state with `₱ 0` placeholder. Commit on blur (not on every keystroke). Format with thousand separators when unfocused. Brief check icon after a successful commit. Recolor the "Snowball v0.1" stamp from FrostDeep to FrostMute.

- [ ] **Step 1: Rewrite SettingsScreen**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsScreen.kt`. Replace it entirely with:

```kotlin
package com.snowball.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
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
```

Notes on what changed:
- `initial.incomePerCutoff.toString()` (which printed `0.0`) is replaced with `toFormFieldString()` (which returns `""` for `0.0`). Combined with the placeholder text `"₱ 0"`, first-use shows a clean empty field with a hint.
- `onFocusChanged` is used to detect blur. On blur (`wasFocused && !focusState.isFocused`), the parsed value is committed via `vm.setIncome(...)` and a check-icon acknowledgement is shown for 1.5s via `LaunchedEffect + delay`.
- When the field has focus, `displayValue` shows the raw digits the user is typing. When unfocused, it shows the formatted peso string (e.g. `₱25,000`).
- Editing-mode `onValueChange` only fires when focused — protects against the formatted-string path triggering recursive parses.
- Version stamp color changes from `FrostDeep` (1.65:1) to `FrostMute` (~9:1).

- [ ] **Step 2: Verify the SettingsViewModel has `setIncome`**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsViewModel.kt`. Confirm a `setIncome(income: Double)` method exists. If it doesn't (the current Settings screen already calls `vm.setIncome(it)` so it should), no change needed — skip to Step 3.

- [ ] **Step 3: Build**

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Install + verify**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
& $adb shell pm clear com.snowball
& $adb shell am start -n com.snowball/.MainActivity
```

Verify:
1. Settings tab → income field shows `₱ 0` placeholder (gray hint, no value).
2. Tap the field → focus state. Field is empty. Type `25000` — digits appear as raw text.
3. Tap outside the field (e.g., on the "Same amount used for..." text). Field text changes to `₱25,000`. A check icon briefly appears on the trailing edge of the field and fades after ~1.5 seconds.
4. Tap back into the field — text reverts to raw `25000` for editing.
5. Modify to `30000`, tap outside. Check icon shows again. Re-focusing — `30000` raw, blur — `₱30,000`.
6. Scroll to bottom — "Snowball v0.1" is now lighter gray (FrostMute) and clearly visible.

Capture:
```powershell
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml C:\Users\Pika\AppData\Local\Temp\ui-settings-blur.xml
```

- [ ] **Step 5: Commit**

```powershell
git add composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsScreen.kt
git commit -m "feat(settings): empty placeholder, commit-on-blur, formatted display, save ack"
```

---

## Task 17: End-to-end verification + final commit

**Files:**
- No code changes
- Optional: capture screenshots for the human user

**Goal:** Walk through the inspector's original flow end-to-end against the updated build, capture uiautomator dumps for the records, run unit tests one more time to confirm 0 regressions.

- [ ] **Step 1: Run the full unit test suite**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :composeApp:test
```

Expected: all existing tests + new `AmountFormatTest` (6 tests) + `DebtFormStateValidationTest` (8 tests) pass. Original 36 tests still green, total 50.

- [ ] **Step 2: Clear install on emulator + walkthrough**

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb shell pm clear com.snowball
& $adb shell am start -n com.snowball/.MainActivity
```

Walk through each of the inspector's flows and confirm the expected behavior:

| Flow | Expected after v0.1.1 |
|---|---|
| Cleared Home, no income, no debts | "Start by setting your income in Settings." (was: "No payments due...") |
| Set income, return to Home | Empty hint flips to "No payments due this cutoff yet. Add debts from the Debts tab." |
| Add 2-3 debts that fall in current cutoff | Debts appear with `0/N paid` in subtitle |
| Try saving an empty form | Save button is visibly disabled (FrostMute background); tapping it does nothing |
| Focus Name field, blur empty | Field border turns Ember, "Enter a name" appears below |
| Edit a saved debt | Form opens with TopAppBar "Edit debt", back arrow, 3-dot menu; monthly amount shows "1500" not "1500.0"; PAYMENTS RECORDED line replaces the editable backfill field |
| Tap 3-dot → Delete | AlertDialog "Delete Sloan?" with Cancel + Delete; Delete commits, Cancel dismisses |
| Income covers debts | LEFT OVER cell in Ice |
| Income < debts | SHORT BY cell in Ember, no minus sign |
| Bottom-nav active tab | Ice-tinted pill behind icon |
| FAB tap | Material Add icon, announces "Add debt" |
| font_scale 1.30 | Last Home payment row has ≥ 16dp clearance to nav |

Capture a final set of dumps to commit alongside the work:

```powershell
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml C:\Users\Pika\AppData\Local\Temp\ui-v011-final-home.xml

# (navigate to Debts)
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml C:\Users\Pika\AppData\Local\Temp\ui-v011-final-debts.xml

# (navigate to Settings)
& $adb shell uiautomator dump /sdcard/ui.xml
& $adb pull /sdcard/ui.xml C:\Users\Pika\AppData\Local\Temp\ui-v011-final-settings.xml
```

(These remain in `%TEMP%` — they are evidence for the human user and don't need to be checked into the repo.)

- [ ] **Step 3: Tag the release**

```powershell
git tag -a v0.1.1 -m "v0.1.1 — UX polish pass (blocker #1 and #2 deferred to v0.2)"
git log --oneline -20
```

(Push the tag later — `git push --tags` is a deliberate action the user can run when ready. Do NOT push from this plan execution unless explicitly told to.)

- [ ] **Step 4: Done**

No commit required for this task — it's verification. The plan is complete.

---

## Spec coverage check

| Spec section | Plan task(s) |
|---|---|
| 1. Form blockers — validation | 3 (logic), 7 (UI) |
| 1. Form blockers — delete | 8 |
| 2. Form chrome | 6 (Scaffold), 8 (overflow), 10 (semantics) |
| 3. Home polish — negative LEFT OVER | 11 |
| 3. Home polish — empty copy | 12 |
| 3. Home polish — payments-already-made | 5 (VM), 6 (hide on edit), 9 (summary line) |
| 4. Nitpicks — formatAmount | 1 (util), 4 (use in init) |
| 4. Nitpicks — debts row layout + progress | 14 |
| 4. Nitpicks — PAYMENTS spacing | 12 |
| 4. Nitpicks — FAB icon | 14 |
| 4. Nitpicks — nav active pill | 15 |
| 4. Nitpicks — View archived | 14 |
| 4. Nitpicks — DUE spacing | 11 |
| 5. Accessibility — FrostDim lift + Line alpha | 2 |
| 5. Accessibility — FrostDeep retire from text | 12 (swipe caption), 16 (version stamp), 6 (placeholder) |
| 5. Accessibility — Field semantics | 10 |
| 5. Accessibility — payment row + PesoText semantics | 13 |
| 5. Accessibility — bottom padding at font_scale 1.30 | 12 |

Every numbered spec item maps to at least one task. No orphans.

## Risks recap

- The visual lift on `FrostDim` (Task 2) is a small but real shift. If after install the new color reads as too light, fallback is to introduce a separate `FrostDimAccessible` token for text-only usages and leave the icon/chevron uses on the old value. Make this judgment on the emulator before committing further work.
- `OutlinedTextField` semantics-via-wrapper-Column (Task 10) preserves the visual identity but is non-standard. If a TalkBack edge case misbehaves, fallback is Material 3's built-in `label = { Text(...) }` floating-label slot — would change the visual.
- The `DebtsState.debtsByCategory` shape change (Task 14) is internal to the Debts feature. No external consumer exists in the current code; verify with grep before committing.
- AlertDialog uses Material's default surface color overridden to `CardElev` (Task 8). Verify no other dialog usage in the app expects the Material default.
