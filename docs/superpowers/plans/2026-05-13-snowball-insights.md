# Snowball Insights Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a 4th bottom-nav tab "Insights" with a snapshot card (total debt remaining / monthly burden / income coverage) and a forecast list of the next 12 cutoffs.

**Architecture:** Pure UI addition. New `InsightsCalculator` in `domain/` computes snapshot + forecast as pure functions (no I/O). New `InsightsViewModel` + `InsightsScreen` consume it. `Nav.kt` gains a 4th `Tab` entry; `App.kt` wires the route. No schema changes, no data-layer changes, no new dependencies.

**Tech Stack:** Kotlin Multiplatform commonMain, Jetpack Compose Multiplatform, Material 3, kotlinx.datetime. JDK 21. Tests with `kotlin.test`.

**Reference:** Spec at `docs/superpowers/specs/2026-05-13-snowball-insights-design.md`.

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

**NEVER call `Read` on a `.png` file.** Capture uiautomator XML dumps for verification instead.

---

## File-level change inventory

| File | Tasks | Purpose |
|---|---|---|
| `composeApp/src/commonMain/kotlin/com/snowball/domain/InsightsCalculator.kt` *(new)* | 1 | `SnapshotStats` + `CutoffForecast` + `InsightsCalculator` pure functions |
| `composeApp/src/commonTest/kotlin/com/snowball/domain/InsightsCalculatorTest.kt` *(new)* | 1 | TDD tests (8 cases) |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsViewModel.kt` *(new)* | 2 | Pulls debts/payments/income from Repos, filters LEDGER, calls calculator |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsScreen.kt` *(new)* | 3 | TopAppBar + SnapshotCard + ForecastRow list |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/nav/Nav.kt` | 4 | Add `Tab.Insights` enum entry |
| `composeApp/src/commonMain/kotlin/com/snowball/App.kt` | 5 | Wire `Tab.Insights` branch + imports |

---

## Task 1: InsightsCalculator + tests (TDD)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/domain/InsightsCalculator.kt`
- Create: `composeApp/src/commonTest/kotlin/com/snowball/domain/InsightsCalculatorTest.kt`

**Goal:** Pure-function snapshot computation + forward cutoff forecast with virtual-payment simulation.

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/snowball/domain/InsightsCalculatorTest.kt`:

```kotlin
package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InsightsCalculatorTest {

    private fun debt(
        id: Long = 1L,
        monthlyAmount: Double = 1500.0,
        totalPayments: Int = 6,
        startDate: LocalDate = LocalDate(2026, 1, 1),
        firstPaymentDate: LocalDate = startDate,
        dueDay: Int = 10,
        useLastDayOfMonth: Boolean = false,
        isArchived: Boolean = false,
    ) = Debt(
        id = id, name = "D$id", categoryId = 1L,
        monthlyAmount = monthlyAmount, totalPayments = totalPayments,
        dueDay = dueDay, useLastDayOfMonth = useLastDayOfMonth,
        startDate = startDate, firstPaymentDate = firstPaymentDate,
        isArchived = isArchived, notes = null,
    )

    private fun payment(id: Long, debtId: Long, amount: Double = 1500.0, date: LocalDate = LocalDate(2026, 1, 10)) =
        Payment(id = id, debtId = debtId, paidDate = date, amount = amount)

    // ---- snapshot ----

    @Test
    fun snapshot_no_debts() {
        val stats = InsightsCalculator.snapshot(
            activeScheduledDebts = emptyList(),
            paymentsByDebt = emptyMap(),
            incomePerCutoff = 25000.0,
        )
        assertEquals(0.0, stats.remaining)
        assertEquals(0, stats.debtCount)
        assertEquals(0.0, stats.monthlyBurden)
        assertEquals(50000.0, stats.monthlyIncome)
        assertEquals(0, stats.coveragePercent)
    }

    @Test
    fun snapshot_one_debt_partial() {
        // 1500 monthly × 6 total = 9000 scheduled; 2 paid; 4 × 1500 = 6000 remaining
        val d = debt()
        val pays = listOf(
            payment(1L, d.id, date = LocalDate(2026, 1, 10)),
            payment(2L, d.id, date = LocalDate(2026, 2, 10)),
        )
        val stats = InsightsCalculator.snapshot(
            activeScheduledDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to pays),
            incomePerCutoff = 25000.0,
        )
        assertEquals(6000.0, stats.remaining)
        assertEquals(1, stats.debtCount)
        assertEquals(1500.0, stats.monthlyBurden)
        assertEquals(50000.0, stats.monthlyIncome)
        // 1500 / 50000 = 0.03 → 3
        assertEquals(3, stats.coveragePercent)
    }

    @Test
    fun snapshot_income_zero_gives_null_coverage() {
        val d = debt()
        val stats = InsightsCalculator.snapshot(
            activeScheduledDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to emptyList()),
            incomePerCutoff = 0.0,
        )
        assertEquals(0.0, stats.monthlyIncome)
        assertNull(stats.coveragePercent)
    }

    @Test
    fun snapshot_caps_remaining_at_zero_when_overpaid() {
        // Edge case: more payments than totalPayments. Remaining shouldn't go negative.
        val d = debt(totalPayments = 2)
        val pays = (1L..5L).map { payment(it, d.id) }
        val stats = InsightsCalculator.snapshot(
            activeScheduledDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to pays),
            incomePerCutoff = 25000.0,
        )
        assertEquals(0.0, stats.remaining)
    }

    // ---- forecast ----

    @Test
    fun forecast_empty_when_no_debts() {
        val f = InsightsCalculator.forecastCutoffs(
            today = LocalDate(2026, 5, 13),
            activeScheduledDebts = emptyList(),
            paymentsByDebt = emptyMap(),
            incomePerCutoff = 25000.0,
            count = 12,
        )
        assertTrue(f.isEmpty())
    }

    @Test
    fun forecast_emits_count_rows_with_active_debts() {
        val d = debt(
            firstPaymentDate = LocalDate(2026, 1, 10),
            totalPayments = 36,
            dueDay = 10,
        )
        val f = InsightsCalculator.forecastCutoffs(
            today = LocalDate(2026, 5, 13),
            activeScheduledDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to emptyList()),
            incomePerCutoff = 25000.0,
            count = 12,
        )
        assertEquals(12, f.size)
    }

    @Test
    fun forecast_starts_two_cutoffs_after_today() {
        // today=May 13 → current cutoff: Apr 30 payday (windowStart May 1, windowEnd May 14)
        // nextCutoff: May 15 payday (May 15-30)
        // nextCutoff.next(): May 30 payday (June 1-14)
        // So first forecast cutoff should have windowStart June 1.
        val d = debt(firstPaymentDate = LocalDate(2026, 1, 10), totalPayments = 36, dueDay = 10)
        val f = InsightsCalculator.forecastCutoffs(
            today = LocalDate(2026, 5, 13),
            activeScheduledDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to emptyList()),
            incomePerCutoff = 25000.0,
            count = 1,
        )
        assertEquals(1, f.size)
        assertEquals(LocalDate(2026, 6, 1), f[0].cutoff.windowStart)
        assertEquals(LocalDate(2026, 6, 14), f[0].cutoff.windowEnd)
    }

    @Test
    fun forecast_debt_rolls_off_when_finished() {
        // 6-cycle debt, all 6 payments will be billed across forecast.
        // After cycle 6 the debt should stop appearing — subsequent rows are All Clear.
        val d = debt(
            firstPaymentDate = LocalDate(2026, 1, 10),
            totalPayments = 6,
            dueDay = 10,
            monthlyAmount = 1500.0,
        )
        val f = InsightsCalculator.forecastCutoffs(
            today = LocalDate(2026, 1, 1),
            // today is so early that no cycles have been billed yet
            activeScheduledDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to emptyList()),
            incomePerCutoff = 25000.0,
            count = 12,
        )
        assertEquals(12, f.size)
        // The early rows should show the 1500 monthly being billed.
        val totalBilled = f.sumOf { it.dueTotal }
        assertEquals(6 * 1500.0, totalBilled)
        // The last rows should be All Clear.
        assertTrue(f.last().isAllClear)
    }

    @Test
    fun forecast_heavy_cutoff_has_negative_left_over() {
        // Big debt, small income → forecast rows show negative leftOver.
        val d = debt(
            firstPaymentDate = LocalDate(2026, 1, 10),
            totalPayments = 36,
            dueDay = 10,
            monthlyAmount = 30000.0,
        )
        val f = InsightsCalculator.forecastCutoffs(
            today = LocalDate(2026, 5, 13),
            activeScheduledDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to emptyList()),
            incomePerCutoff = 10000.0,
            count = 3,
        )
        // Each cycle bills 30000 in some cutoff; leftOver = 10000 - 30000 = -20000
        val billedRows = f.filterNot { it.isAllClear }.filter { it.dueTotal > 0.0 }
        assertTrue(billedRows.isNotEmpty())
        billedRows.forEach { row ->
            assertTrue(row.leftOver < 0, "Expected negative leftOver for billed row $row")
        }
    }
}
```

- [ ] **Step 2: Verify the test fails**

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.domain.InsightsCalculatorTest"
```

Expected: FAIL — `InsightsCalculator`, `SnapshotStats`, `CutoffForecast` are unresolved.

- [ ] **Step 3: Create the implementation file**

Create `composeApp/src/commonMain/kotlin/com/snowball/domain/InsightsCalculator.kt`:

```kotlin
package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.LocalDate

data class SnapshotStats(
    val remaining: Double,
    val debtCount: Int,
    val monthlyBurden: Double,
    val monthlyIncome: Double,
    val coveragePercent: Int?,
)

data class CutoffForecast(
    val cutoff: Cutoff,
    val dueTotal: Double,
    val leftOver: Double,
    val isAllClear: Boolean,
)

object InsightsCalculator {

    fun snapshot(
        activeScheduledDebts: List<Debt>,
        paymentsByDebt: Map<Long, List<Payment>>,
        incomePerCutoff: Double,
    ): SnapshotStats {
        val remaining = activeScheduledDebts.sumOf { d ->
            val made = paymentsByDebt[d.id]?.size ?: 0
            (d.totalPayments - made).coerceAtLeast(0) * d.monthlyAmount
        }
        val monthlyBurden = activeScheduledDebts.sumOf { it.monthlyAmount }
        val monthlyIncome = incomePerCutoff * 2.0
        val coverage = if (monthlyIncome > 0.0) {
            ((monthlyBurden / monthlyIncome) * 100).toInt().coerceIn(0, 999)
        } else null
        return SnapshotStats(
            remaining = remaining,
            debtCount = activeScheduledDebts.size,
            monthlyBurden = monthlyBurden,
            monthlyIncome = monthlyIncome,
            coveragePercent = coverage,
        )
    }

    fun forecastCutoffs(
        today: LocalDate,
        activeScheduledDebts: List<Debt>,
        paymentsByDebt: Map<Long, List<Payment>>,
        incomePerCutoff: Double,
        count: Int = 12,
    ): List<CutoffForecast> {
        if (activeScheduledDebts.isEmpty()) return emptyList()

        val results = mutableListOf<CutoffForecast>()
        val virtual: MutableMap<Long, MutableList<Payment>> =
            paymentsByDebt.mapValues { it.value.toMutableList() }.toMutableMap()
        var c = nextCutoff(today).next()

        repeat(count) {
            val stillOwed = activeScheduledDebts.filter { d ->
                (virtual[d.id]?.size ?: 0) < d.totalPayments
            }
            val rows = CutoffCalculator.computeDueRows(c, stillOwed, virtual)
            val dueTotal = rows.sumOf { it.amount }
            val leftOver = incomePerCutoff - dueTotal
            results.add(
                CutoffForecast(
                    cutoff = c,
                    dueTotal = dueTotal,
                    leftOver = leftOver,
                    isAllClear = rows.isEmpty(),
                )
            )
            rows.forEach { row ->
                val list = virtual.getOrPut(row.debt.id) { mutableListOf() }
                list.add(
                    Payment(
                        id = -1L,
                        debtId = row.debt.id,
                        paidDate = row.effectiveDueDate,
                        amount = row.amount,
                    )
                )
            }
            c = c.next()
        }
        return results
    }
}
```

- [ ] **Step 4: Verify tests pass**

```bash
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.domain.InsightsCalculatorTest"
```

Expected: PASS — all 8 tests green.

- [ ] **Step 5: Build the full debug variant**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/domain/InsightsCalculator.kt composeApp/src/commonTest/kotlin/com/snowball/domain/InsightsCalculatorTest.kt
git commit -m "feat(domain): add InsightsCalculator with snapshot and 12-cutoff forecast"
```

---

## Task 2: InsightsViewModel

**File:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsViewModel.kt`

**Goal:** Pull active SCHEDULED debts, payments, and income from Repos; pass to calculator; expose `InsightsState`.

- [ ] **Step 1: Create the file**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsViewModel.kt`:

```kotlin
package com.snowball.ui.insights

import com.snowball.data.Repos
import com.snowball.data.model.CategoryBehavior
import com.snowball.domain.CutoffForecast
import com.snowball.domain.InsightsCalculator
import com.snowball.domain.SnapshotStats
import com.snowball.domain.today
import kotlinx.datetime.LocalDate

data class InsightsState(
    val snapshot: SnapshotStats,
    val forecast: List<CutoffForecast>,
)

class InsightsViewModel(private val repos: Repos) {
    fun load(today: LocalDate = today()): InsightsState {
        val scheduledCatIds = repos.categories.all()
            .filter { it.behavior == CategoryBehavior.SCHEDULED }
            .map { it.id }
            .toSet()
        val active = repos.debts.allActive().filter { it.categoryId in scheduledCatIds }
        val paymentsByDebt = active.associate { it.id to repos.payments.historyForDebt(it.id) }
        val income = repos.settings.get().incomePerCutoff
        val snapshot = InsightsCalculator.snapshot(active, paymentsByDebt, income)
        val forecast = InsightsCalculator.forecastCutoffs(today, active, paymentsByDebt, income, count = 12)
        return InsightsState(snapshot, forecast)
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
git add composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsViewModel.kt
git commit -m "feat(insights): add InsightsViewModel"
```

---

## Task 3: InsightsScreen

**File:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsScreen.kt`

**Goal:** TopAppBar + SnapshotCard + 12 ForecastRows (or empty-state). Visually mirrors Journey card and the v0.2 components.

- [ ] **Step 1: Create the file**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsScreen.kt`:

```kotlin
package com.snowball.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.domain.CutoffForecast
import com.snowball.domain.SnapshotStats
import com.snowball.ui.components.PesoText
import com.snowball.ui.components.cutoffRangeLabel
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.formatAmountWithSeparators
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(vm: InsightsViewModel) {
    val state = remember { vm.load() }

    Scaffold(
        containerColor = SnowColors.Night,
        topBar = {
            TopAppBar(
                title = { Text("Insights", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SnowColors.Night,
                    titleContentColor = SnowColors.Frost,
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
            SnapshotCard(stats = state.snapshot)
            Spacer(Modifier.height(24.dp))
            Text(
                "UPCOMING (NEXT 6 MONTHS)",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                color = SnowColors.FrostDim,
            )
            Spacer(Modifier.height(12.dp))
            if (state.forecast.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No upcoming debts in your forecast window.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = SnowColors.FrostDim,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.forecast.forEach { f ->
                        ForecastRow(f)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SnapshotCard(stats: SnapshotStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(SnowColors.CardElev)
            .border(1.dp, SnowColors.LineStrong, RoundedCornerShape(28.dp))
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(
            "WHAT YOU OWE",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        if (stats.debtCount == 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Nothing right now.",
                style = MaterialTheme.typography.bodyLarge,
                color = SnowColors.Frost,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Add a debt from the Debts tab to start tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = SnowColors.FrostMute,
            )
        } else {
            Spacer(Modifier.height(12.dp))
            PesoText(
                amount = stats.remaining,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.W300),
                pesoColor = SnowColors.FrostMute,
                numberColor = SnowColors.Ice,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "across ${stats.debtCount} ${if (stats.debtCount == 1) "debt" else "debts"}",
                style = MaterialTheme.typography.labelMedium,
                color = SnowColors.FrostMute,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "₱${formatAmountWithSeparators(stats.monthlyBurden)}/mo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnowColors.FrostMute,
                )
                Text(
                    " · ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnowColors.FrostMute,
                )
                val coverageText =
                    if (stats.coveragePercent == null) "— of monthly"
                    else "${stats.coveragePercent}% of monthly"
                Text(
                    coverageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnowColors.FrostMute,
                )
            }
        }
    }
}

@Composable
private fun ForecastRow(f: CutoffForecast) {
    val isShort = !f.isAllClear && f.leftOver < 0
    val borderColor = if (isShort) SnowColors.Ember.copy(alpha = 0.4f) else SnowColors.LineStrong
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SnowColors.NightElev)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            cutoffRangeLabel(f.cutoff),
            style = MaterialTheme.typography.bodyLarge,
            color = SnowColors.Frost,
            modifier = Modifier.weight(1f),
        )
        if (f.isAllClear) {
            Text(
                "All clear ✓",
                style = MaterialTheme.typography.bodyLarge,
                color = SnowColors.Ice,
            )
        } else {
            Column(horizontalAlignment = Alignment.End) {
                PesoText(
                    amount = f.dueTotal,
                    style = MaterialTheme.typography.headlineSmall,
                    pesoColor = SnowColors.FrostDim,
                    numberColor = SnowColors.Frost,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (isShort) "SHORT BY ₱${formatAmountWithSeparators(abs(f.leftOver))}"
                    else "₱${formatAmountWithSeparators(f.leftOver)} left",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isShort) SnowColors.Ember else SnowColors.FrostMute,
                )
            }
        }
    }
}
```

`cutoffRangeLabel` is a top-level function in `com.snowball.ui.components` (`CutoffCard.kt`). `formatAmountWithSeparators` is in `com.snowball.ui.util`. Both already exist.

- [ ] **Step 2: Build**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL. Lint may emit an "unused composable" warning for `InsightsScreen` until Task 5 wires the route — non-fatal.

- [ ] **Step 3: Run tests**

```bash
./gradlew.bat :composeApp:testDebugUnitTest
```

Expected: all tests still pass.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsScreen.kt
git commit -m "feat(insights): add InsightsScreen with snapshot card and forecast list"
```

---

## Task 4: Add `Tab.Insights` to Nav.kt

**File:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/nav/Nav.kt`

**Goal:** Add the 4th tab enum entry. The render loop is data-driven (`Tab.entries.forEach`), so no other changes needed in Nav.kt.

- [ ] **Step 1: Read the current Nav.kt to confirm the existing enum**

Use the Read tool to inspect `composeApp/src/commonMain/kotlin/com/snowball/ui/nav/Nav.kt`. Note the existing `Tab` enum entries and imports.

- [ ] **Step 2: Modify Nav.kt**

Add the import (alphabetical, with the other `androidx.compose.material.icons.outlined.*` imports):

```kotlin
import androidx.compose.material.icons.outlined.Insights
```

Update the `Tab` enum to insert `Insights` between `Debts` and `Settings`:

```kotlin
enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Outlined.AcUnit),
    Debts("Debts", Icons.Outlined.ReceiptLong),
    Insights("Insights", Icons.Outlined.Insights),
    Settings("Settings", Icons.Outlined.Tune),
}
```

- [ ] **Step 3: Build**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD FAILS — `App.kt`'s `when (tab) { ... }` block doesn't have a branch for `Tab.Insights` yet. The Kotlin compiler will flag this as a non-exhaustive `when`. That's expected; Task 5 fixes it.

If your build succeeds at this step, the existing `when` was actually exhaustive in a way that doesn't error on missing branches (e.g., it uses `else` or was generic). Either way, Task 5 must add the explicit branch.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/nav/Nav.kt
git commit -m "feat(nav): add Tab.Insights enum entry"
```

(Commit even though build is broken — Task 5 completes the wiring. This keeps the changes atomic per file.)

---

## Task 5: Wire Insights route in App.kt

**File:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/App.kt`

**Goal:** Add imports and the `Tab.Insights` branch in the route `when`.

- [ ] **Step 1: Modify App.kt**

Add these imports (alphabetical, with the other `com.snowball.ui.*` imports):

```kotlin
import com.snowball.ui.insights.InsightsScreen
import com.snowball.ui.insights.InsightsViewModel
```

Find the `Tab.Debts -> DebtsScreen(...)` branch in the `when (tab)` block. Add a new branch after `Tab.Debts` and before `Tab.Settings`:

```kotlin
Tab.Insights -> {
    val insightsVm = remember(refreshKey) { InsightsViewModel(repos) }
    InsightsScreen(insightsVm)
}
```

The final `when (tab)` block should look like:

```kotlin
when (tab) {
    Tab.Home -> HomeScreen(homeVm)
    Tab.Debts -> DebtsScreen(
        vm = debtsVm,
        onAddDebt = { route = Route.Form(null) },
        onAddMisc = { route = Route.MiscForm },
        onOpenDebt = { id -> route = Route.DebtDetail(id) },
    )
    Tab.Insights -> {
        val insightsVm = remember(refreshKey) { InsightsViewModel(repos) }
        InsightsScreen(insightsVm)
    }
    Tab.Settings -> {
        val settingsVm = remember(refreshKey) { SettingsViewModel(repos) }
        SettingsScreen(
            vm = settingsVm,
            onManageCategories = { route = Route.CategoryManagement },
        )
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

Expected: all tests pass (including the 8 new Insights tests + all v0.2.x tests).

- [ ] **Step 4: Install on emulator + verify**

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

Verify:
1. Bottom nav now shows 4 tabs: Home / Debts / Insights / Settings (in that order).
2. Tap Insights → screen opens with "Insights" title.
3. If at least one active SCHEDULED debt exists: snapshot card shows total remaining + count + monthly burden + coverage%. Forecast list shows up to 12 cutoff rows.
4. If no active debts: snapshot card shows "Nothing right now / Add a debt..." text. Forecast section shows empty-state line.
5. Scroll the forecast — bottom rows for a finished debt show "All clear ✓".

Capture an XML dump for verification:

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell uiautomator dump /sdcard/ui.xml
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe pull /sdcard/ui.xml /c/Users/Pika/AppData/Local/Temp/ui-insights.xml
```

Grep for "Insights" and "WHAT YOU OWE" to confirm presence:

```bash
grep -E "Insights|WHAT YOU OWE|UPCOMING" /c/Users/Pika/AppData/Local/Temp/ui-insights.xml
```

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/App.kt
git commit -m "feat(nav): wire Tab.Insights route in App.kt"
```

---

## Task 6: End-to-end verification + tag v0.2.9 + push

**Files:** No code changes.

- [ ] **Step 1: Run the full test suite (debug + release variants)**

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:test
```

Expected: BUILD SUCCESSFUL. 8 new InsightsCalculator tests on top of existing 60+.

- [ ] **Step 2: Clear-install walkthrough**

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell pm clear com.snowball
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

Cleared install steps:
1. Tap Insights tab → snapshot shows empty state ("Nothing right now") and forecast shows the empty-state line.
2. Switch to Debts → add a debt (e.g. monthly 1500, totalPayments 6, dueDay 10, firstPaymentDate Feb 10, 0 paymentsAlreadyMade).
3. Switch to Settings → set income to 25000.
4. Switch back to Insights → snapshot now shows ₱9,000 remaining, 1 debt, ₱1,500/mo, 3% of monthly. Forecast shows 12 rows; first ~6 have due ₱1,500; rest are "All clear ✓".
5. Tap a forecast row (no-op for now — interaction reserved for v0.3).

- [ ] **Step 3: Font scale stress test**

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell settings put system font_scale 1.30
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am force-stop com.snowball
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

Visit Insights tab. Confirm: bottom nav's 4 labels remain readable (no truncation), forecast rows don't overflow, "SHORT BY ₱X" label fits.

Revert:

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell settings put system font_scale 1.0
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am force-stop com.snowball
```

- [ ] **Step 4: Tag + push**

```bash
git tag -a v0.2.9 -m "v0.2.9 — Insights page (snapshot + 12-cutoff forecast)"
git push origin main
git push origin v0.2.9
```

- [ ] **Step 5: Done**

---

## Spec coverage check

| Spec section | Plan task(s) |
|---|---|
| `SnapshotStats` + `CutoffForecast` data classes | 1 |
| `InsightsCalculator.snapshot(...)` | 1 |
| `InsightsCalculator.forecastCutoffs(...)` with virtual-payment simulation + `stillOwed` filter | 1 |
| Snapshot card UI (hero number, subtitle, monthly/coverage row, empty state) | 3 |
| Forecast section (row layout, Ember heavy-cutoff style, All-clear marker, empty-state line) | 3 |
| `InsightsViewModel.load()` filtering LEDGER and pulling from Repos | 2 |
| `Tab.Insights` 4th bottom-nav tab | 4 |
| `App.kt` route wiring | 5 |
| Manual verification flow | 6 |

Every spec section maps to at least one task.

## Risks recap

- **Build is broken between Tasks 4 and 5.** Adding the enum entry without the route branch makes `when (tab)` non-exhaustive. Atomic-per-file commits keep the diff readable; the build is healthy again after Task 5. If executing tasks via subagents in parallel, ensure 5 runs after 4.
- **Forecast simulation relies on `CutoffCalculator.computeDueRows` not changing semantics**. If the calculator's contract changes (e.g., new fields on `DueRow`), the forecast might break silently. Mitigated by the calculator tests covering the simulation behavior end-to-end.
- **Font scale at 1.30 + 4 tabs**. 1080px screen / 4 tabs = 270px per tab. "Settings" + "Insights" at labelSmall 10sp scaled 1.30 = ~13sp should fit comfortably. Verified by the font-scale step.
