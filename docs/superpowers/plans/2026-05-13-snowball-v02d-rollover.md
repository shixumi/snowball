# Snowball v0.2 Sub-project D — Cutoff Rollover Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development or superpowers:executing-plans. Steps use `- [ ]` checkboxes.

**Goal:** Add overdue rollover (red OVERDUE section on Home + per-debt stat) and a periodic Home recompute so the cutoff advances near midnight without app restart.

**Architecture:** A new `OverdueCalculator` pure object in `domain/`; `HomeState` and `DebtDetailState` extended with overdue info; HomeScreen renders an OVERDUE section above PAYMENTS; DebtDetailScreen adds an OVERDUE stat row. A `LaunchedEffect` loop in HomeScreen polls every 60s to tick state.

**Tech Stack:** Same as v0.2.2. No schema changes.

**Reference:** Spec at `docs/superpowers/specs/2026-05-13-snowball-v02d-rollover-design.md`.

---

## Build / verification recipe

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:assembleDebug
```

---

## Task 1: OverdueCalculator + tests

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/domain/OverdueCalculator.kt`
- Create: `composeApp/src/commonTest/kotlin/com/snowball/domain/OverdueCalculatorTest.kt`

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/snowball/domain/OverdueCalculatorTest.kt`:

```kotlin
package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverdueCalculatorTest {

    private fun debt(
        id: Long = 1L,
        monthlyAmount: Double = 1500.0,
        totalPayments: Int = 12,
        startDate: LocalDate = LocalDate(2026, 1, 1),
        dueDay: Int = 10,
        useLastDayOfMonth: Boolean = false,
        isArchived: Boolean = false,
    ) = Debt(
        id = id, name = "D$id", categoryId = 1L,
        monthlyAmount = monthlyAmount, totalPayments = totalPayments,
        dueDay = dueDay, useLastDayOfMonth = useLastDayOfMonth,
        startDate = startDate, isArchived = isArchived, notes = null,
    )

    @Test
    fun upToDateDebtReturnsEmpty() {
        // startDate 2026-01-01, dueDay 10, 12 payments. Today is 2026-01-09 (before first due).
        val d = debt()
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to emptyList()),
            today = LocalDate(2026, 1, 9),
        )
        assertTrue(info.isEmpty())
    }

    @Test
    fun oneMissedCycle() {
        // Today is 2026-02-15. First due was 2026-01-10. Zero payments.
        val d = debt()
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to emptyList()),
            today = LocalDate(2026, 2, 15),
        )
        assertEquals(1, info.size)
        assertEquals(1, info[0].missedCycles)
        assertEquals(1500.0, info[0].missedAmount)
        assertEquals(LocalDate(2026, 1, 10), info[0].firstMissedDueDate)
    }

    @Test
    fun multipleMissedCycles() {
        // Today is 2026-04-15. 3 dues passed (Jan 10, Feb 10, Mar 10). Zero payments.
        val d = debt()
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to emptyList()),
            today = LocalDate(2026, 4, 15),
        )
        assertEquals(1, info.size)
        assertEquals(3, info[0].missedCycles)
        assertEquals(4500.0, info[0].missedAmount)
        assertEquals(LocalDate(2026, 1, 10), info[0].firstMissedDueDate)
    }

    @Test
    fun partiallyPaidDebtReportsRemainingMissed() {
        // Today is 2026-04-15. 3 dues passed. 1 payment recorded.
        val d = debt()
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to listOf(Payment(1L, d.id, LocalDate(2026, 1, 10), 1500.0))),
            today = LocalDate(2026, 4, 15),
        )
        assertEquals(1, info.size)
        assertEquals(2, info[0].missedCycles)
        // firstMissedDueDate is the 2nd cycle's due date (2nd payment is missed).
        assertEquals(LocalDate(2026, 2, 10), info[0].firstMissedDueDate)
    }

    @Test
    fun archivedDebtSkipped() {
        val d = debt(isArchived = true)
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to emptyList()),
            today = LocalDate(2026, 4, 15),
        )
        assertTrue(info.isEmpty())
    }

    @Test
    fun futureStartDebtReturnsEmpty() {
        // Today is 2026-01-01. Debt starts 2026-06-01.
        val d = debt(startDate = LocalDate(2026, 6, 1))
        val info = OverdueCalculator.computeOverdue(
            listOf(d),
            mapOf(d.id to emptyList()),
            today = LocalDate(2026, 1, 1),
        )
        assertTrue(info.isEmpty())
    }
}
```

- [ ] **Step 2: Run test to confirm failure**

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.domain.OverdueCalculatorTest"
```

Expected: FAIL — `OverdueCalculator` is unresolved.

- [ ] **Step 3: Create the implementation file**

Create `composeApp/src/commonMain/kotlin/com/snowball/domain/OverdueCalculator.kt`:

```kotlin
package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

data class OverdueInfo(
    val debt: Debt,
    val missedCycles: Int,
    val missedAmount: Double,
    val firstMissedDueDate: LocalDate,
)

object OverdueCalculator {
    fun computeOverdue(
        debts: List<Debt>,
        paymentsByDebt: Map<Long, List<Payment>>,
        today: LocalDate,
    ): List<OverdueInfo> = debts.mapNotNull { debt ->
        if (debt.isArchived) return@mapNotNull null
        val expectedSoFar = expectedPaymentsByDate(debt, today)
        val actual = paymentsByDebt[debt.id]?.size ?: 0
        val missed = expectedSoFar - actual
        if (missed <= 0) return@mapNotNull null

        val firstMissed = nthDueDate(debt, actual + 1) ?: return@mapNotNull null
        val cappedMissed = missed.coerceAtMost(debt.totalPayments - actual)
        OverdueInfo(
            debt = debt,
            missedCycles = cappedMissed,
            missedAmount = cappedMissed * debt.monthlyAmount,
            firstMissedDueDate = firstMissed,
        )
    }

    private fun expectedPaymentsByDate(debt: Debt, asOf: LocalDate): Int {
        var count = 0
        for (n in 1..debt.totalPayments) {
            val due = nthDueDate(debt, n) ?: continue
            if (due <= asOf) count++ else break
        }
        return count
    }

    private fun nthDueDate(debt: Debt, n: Int): LocalDate? {
        if (n < 1 || n > debt.totalPayments) return null
        val month = debt.startDate.plus(n - 1, DateTimeUnit.MONTH)
        return effectiveDueDate(
            year = month.year,
            month = month.monthNumber,
            dueDay = debt.dueDay,
            useLastDay = debt.useLastDayOfMonth,
        )
    }
}
```

- [ ] **Step 4: Verify tests pass**

```bash
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.domain.OverdueCalculatorTest"
```

Expected: PASS — all 6 tests green.

- [ ] **Step 5: Build**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/domain/OverdueCalculator.kt composeApp/src/commonTest/kotlin/com/snowball/domain/OverdueCalculatorTest.kt
git commit -m "feat(domain): add OverdueCalculator for cycle rollover"
```

---

## Task 2: HomeViewModel — overdue state + catchUp method

**File:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeViewModel.kt`

- [ ] **Step 1: Modify the file**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeViewModel.kt`.

Add these imports (alphabetical):

```kotlin
import com.snowball.domain.OverdueCalculator
import com.snowball.domain.OverdueInfo
```

Extend `HomeState`:

```kotlin
data class HomeState(
    val cutoff: Cutoff,
    val rows: List<DueRow>,
    val summary: CutoffCalculator.Summary,
    val income: Double,
    val nextCutoff: Cutoff,
    val nextRows: List<DueRow>,
    val nextTotal: Double,
    val journey: JourneyStats?,
    val overdue: List<OverdueInfo>,
)
```

Extend `load()` to compute overdue:

```kotlin
fun load(today: LocalDate = today()): HomeState {
    val cutoff = currentCutoff(today)
    val debts = repos.debts.allActive()
    val paymentsByDebt = debts.associate { it.id to repos.payments.historyForDebt(it.id) }
    val rows = CutoffCalculator.computeDueRows(cutoff, debts, paymentsByDebt)
    val income = repos.settings.get().incomePerCutoff
    val summary = CutoffCalculator.summarize(rows, income)

    val next = nextCutoff(today)
    val nextRows = CutoffCalculator.computeDueRows(next, debts, paymentsByDebt)
    val nextTotal = nextRows.sumOf { it.amount }

    val allDebts = repos.debts.all()
    val allPayments = allDebts.flatMap { repos.payments.historyForDebt(it.id) }
    val journey = JourneyCalculator.compute(allDebts, allPayments)

    val overdue = OverdueCalculator.computeOverdue(debts, paymentsByDebt, today)

    return HomeState(cutoff, rows, summary, income, next, nextRows, nextTotal, journey, overdue)
}
```

Add the catch-up method to the `HomeViewModel` class (next to `markPaid` and `undoPayment`):

```kotlin
fun catchUpOverdue(info: OverdueInfo, todayDate: LocalDate = today()) {
    repeat(info.missedCycles) {
        repos.payments.markPaid(info.debt.id, todayDate, info.debt.monthlyAmount)
    }
    val totalPayments = repos.payments.countForDebt(info.debt.id)
    if (totalPayments >= info.debt.totalPayments) {
        repos.debts.setArchived(info.debt.id, true)
    }
}
```

- [ ] **Step 2: Build**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL. `HomeScreen.kt` still references the original 8 HomeState fields — those still exist. The 9th field (`overdue`) is additive.

- [ ] **Step 3: Run tests**

```bash
./gradlew.bat :composeApp:testDebugUnitTest
```

Expected: all pass.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeViewModel.kt
git commit -m "feat(home): compute overdue list and add catchUpOverdue method"
```

---

## Task 3: HomeScreen — OVERDUE section + LaunchedEffect ticker

**File:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt`

- [ ] **Step 1: Edit HomeScreen.kt**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt`.

Add imports (alphabetical):

```kotlin
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import com.snowball.domain.OverdueInfo
import com.snowball.ui.util.formatAmountWithSeparators
import com.snowball.ui.util.formatLongDate
```

(Some of these may already be imported — add only the missing ones.)

Inside the `HomeScreen` composable, immediately after `val state = remember(tick) { vm.load() }` and BEFORE the outer `Column(...)`, add:

```kotlin
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            tick++
        }
    }

    var pendingCatchUp by remember { mutableStateOf<OverdueInfo?>(null) }
```

Inside the outer `Column`, between `CutoffCard(...)` + the existing UpNext block, AND the existing PAYMENTS section header, insert the OVERDUE block:

```kotlin
        // (after the existing UpNextCard block, before the existing Spacer + PAYMENTS Text)

        if (state.overdue.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(SnowColors.NightElev)
                    .border(1.dp, SnowColors.Ember.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text(
                    "OVERDUE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                    color = SnowColors.Ember,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap to mark caught up",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = SnowColors.FrostMute,
                )
                Spacer(Modifier.height(12.dp))
                state.overdue.forEach { info ->
                    OverdueRow(info = info, onClick = { pendingCatchUp = info })
                }
            }
        }
```

After the outer Column closes (where the file's `if (state.journey != null)` block already is), the existing structure stays unchanged.

Add `OverdueRow` and the catch-up AlertDialog inside the composable body. Place the `OverdueRow` helper as a private composable at the bottom of the file (next to EmptyHint), and the AlertDialog inline at the END of the outer Column or AFTER the Column closes inside the same composable:

Actually for the dialog, place it right after the outer Column's closing brace but inside `HomeScreen`'s outer composable scope. Final structure:

```kotlin
@Composable
fun HomeScreen(vm: HomeViewModel) {
    var tick by remember { mutableStateOf(0) }
    val state = remember(tick) { vm.load() }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            tick++
        }
    }

    var pendingCatchUp by remember { mutableStateOf<OverdueInfo?>(null) }

    Column( ... ) {
        // existing CutoffCard, UpNextCard, OVERDUE (new), PAYMENTS, JourneyCard
    }

    val catchUpTarget = pendingCatchUp
    if (catchUpTarget != null) {
        AlertDialog(
            onDismissRequest = { pendingCatchUp = null },
            title = { Text("Catch up on ${catchUpTarget.debt.name}?", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    "Records ${catchUpTarget.missedCycles} missed payment${if (catchUpTarget.missedCycles == 1) "" else "s"} totaling ₱${formatAmountWithSeparators(catchUpTarget.missedAmount)}. First missed due date: ${formatLongDate(catchUpTarget.firstMissedDueDate)}."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.catchUpOverdue(catchUpTarget)
                    pendingCatchUp = null
                    tick++
                }) { Text("Catch up", color = SnowColors.Ember) }
            },
            dismissButton = {
                TextButton(onClick = { pendingCatchUp = null }) {
                    Text("Cancel", color = SnowColors.FrostMute)
                }
            },
            containerColor = SnowColors.CardElev,
            titleContentColor = SnowColors.Frost,
            textContentColor = SnowColors.FrostMute,
        )
    }
}
```

Define the `OverdueRow` private composable at the bottom of the file:

```kotlin
@Composable
private fun OverdueRow(info: OverdueInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = SnowColors.Ember,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(info.debt.name, style = MaterialTheme.typography.bodyLarge, color = SnowColors.Frost)
            Text(
                "${info.missedCycles} ${if (info.missedCycles == 1) "cycle" else "cycles"}",
                style = MaterialTheme.typography.bodySmall,
                color = SnowColors.FrostMute,
            )
        }
        Text(
            "₱${formatAmountWithSeparators(info.missedAmount)}",
            style = MaterialTheme.typography.bodyLarge,
            color = SnowColors.Ember,
        )
    }
}
```

- [ ] **Step 2: Build**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run tests**

```bash
./gradlew.bat :composeApp:testDebugUnitTest
```

Expected: all pass.

- [ ] **Step 4: Install + spot-check**

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

To simulate overdue: ensure there's a debt with a `startDate` several months before today and zero (or few) payments. The OVERDUE section should appear with that debt. Tap → catch-up dialog → confirm → section disappears.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt
git commit -m "feat(home): render OVERDUE section + tick every 60s for cutoff rollover"
```

---

## Task 4: DebtDetailViewModel + Screen — OVERDUE stat

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailViewModel.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailScreen.kt`

- [ ] **Step 1: Modify DebtDetailViewModel.kt**

Add to the imports:

```kotlin
import com.snowball.domain.OverdueCalculator
import com.snowball.domain.OverdueInfo
```

Extend `DebtDetailState`:

```kotlin
data class DebtDetailState(
    val debt: Debt,
    val category: Category,
    val paymentsMade: Int,
    val payments: List<Payment>,
    val projectedEndDate: LocalDate?,
    val amountLeft: Double,
    val overdue: OverdueInfo?,
)
```

Update `load()` (only the construction at the end):

```kotlin
fun load(today: LocalDate = com.snowball.domain.today()): DebtDetailState? {
    val debt = repos.debts.byId(debtId) ?: return null
    val category = repos.categories.byId(debt.categoryId) ?: return null
    val payments = repos.payments.historyForDebt(debtId)
    val made = payments.size
    val left = ((debt.totalPayments - made).coerceAtLeast(0)) * debt.monthlyAmount
    val projected = projectedEndDate(debt)
    val overdue = OverdueCalculator
        .computeOverdue(listOf(debt), mapOf(debtId to payments), today)
        .firstOrNull()
    return DebtDetailState(debt, category, made, payments, projected, left, overdue)
}
```

The existing `load()` signature changes to accept a `today` parameter (default value of `today()`). Existing callers (HomeScreen and App.kt) call it without args, so the default takes over.

- [ ] **Step 2: Modify DebtDetailScreen.kt**

In `DebtDetailScreen`, find the scheduled-variant stats grid (the section with MONTHLY / DUE DAY / STARTED / PROJECTED END). After the PROJECTED END StatRow, conditionally add:

```kotlin
                state.overdue?.let { info ->
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "OVERDUE",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                            color = SnowColors.Ember,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${info.missedCycles} ${if (info.missedCycles == 1) "cycle" else "cycles"} · ₱${com.snowball.ui.util.formatAmountWithSeparators(info.missedAmount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = SnowColors.Ember,
                        )
                    }
                }
```

This sits inside the same conditional that renders the rest of the stats (non-MISC scheduled variant). The MISC variant doesn't get an OVERDUE row.

- [ ] **Step 3: Build + tests**

```bash
./gradlew.bat :composeApp:assembleDebug :composeApp:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL and all tests pass.

- [ ] **Step 4: Install + spot-check**

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

Tap an overdue debt → DebtDetail shows an OVERDUE stat row in Ember.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailViewModel.kt composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailScreen.kt
git commit -m "feat(detail): show OVERDUE stat on scheduled-debt detail when applicable"
```

---

## Task 5: End-to-end verification + tag v0.2.3 + push

- [ ] **Step 1: Full test suite**

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:test
```

Expected: BUILD SUCCESSFUL with 6 new OverdueCalculator tests on top of the v0.2.2 total.

- [ ] **Step 2: Tag + push**

```bash
git tag -a v0.2.3 -m "v0.2.3 — Cutoff rollover (overdue section + 60s tick) (sub-project D)"
git push origin main
git push origin v0.2.3
```

- [ ] **Step 3: Done**

---

## Spec coverage check

| Spec section | Plan task(s) |
|---|---|
| OverdueCalculator + OverdueInfo | 1 |
| HomeState + catchUpOverdue | 2 |
| HomeScreen OVERDUE section + LaunchedEffect | 3 |
| DebtDetail OVERDUE stat | 4 |
| Verify + tag | 5 |

## Risks

- **LaunchedEffect's 60s polling burns a tiny amount of battery** while Home is foregrounded. Acceptable for v0.2.3. A real implementation would key the next delay to the time-until-midnight-or-cutoff-boundary for efficiency.
- **`load()` is now called every 60s on Home.** With `repos.debts.all()` + a `flatMap` over payments, this is sub-millisecond at app scale. No concern.
- **The OVERDUE rendering is duplicated semantically with the PAYMENTS section** when a debt is both overdue AND due in current cutoff. The OVERDUE list shows the missed amount (past); the PAYMENTS list shows the current-cycle amount. Both correct in their own context. Risk: visual clutter for power users with many overdue debts. Acceptable for v0.2.3.
- **Auto-archive on catch-up:** if `missedCycles + previousPaymentsMade >= totalPayments`, the debt auto-archives — same behavior as normal `markPaid`. Tested by the existing pattern.
