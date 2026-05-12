# Snowball v0.2 Sub-project A — Home (Up next + Journey) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an "Up next" expandable card and a "Your journey" stats card to `HomeScreen`. Both are pure additions on top of v0.1.1 — no schema, no data-layer changes.

**Architecture:** Two new stateless Compose components consumed by `HomeScreen`. State extends `HomeViewModel.HomeState` with three new fields (`nextCutoff`, `nextRows`, `nextTotal`, plus a nullable `journey: JourneyStats?`). Two new pure functions in the domain layer (`nextCutoff(today)` and `JourneyCalculator.compute(...)`) plus a small `formatAmountWithSeparators` lift that unifies number formatters that diverged in v0.1.1.

**Tech Stack:** Kotlin Multiplatform (commonMain), Jetpack Compose Multiplatform, Material 3, kotlinx.datetime. JDK 21. Tests with `kotlin.test`. No new dependencies.

**Reference:** Spec at `docs/superpowers/specs/2026-05-13-snowball-v02a-home-design.md`.

---

## Build / verification recipe

Every task that touches UI ends with a build verification step. The recipe used throughout:

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:assembleDebug
```

For installing on the emulator (`emulator-5554`):

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

For uiautomator dumps:

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell uiautomator dump /sdcard/ui.xml
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe pull /sdcard/ui.xml /c/Users/Pika/AppData/Local/Temp/ui-<name>.xml
```

**NEVER call `Read` on a `.png` file.** Capture screenshots for the human user only — they will not be Read by any implementing agent.

---

## File-level change inventory

| File | Tasks | Purpose |
|---|---|---|
| `composeApp/src/commonMain/kotlin/com/snowball/domain/Cutoff.kt` | 1 | Add `nextCutoff(today)` top-level function |
| `composeApp/src/commonTest/kotlin/com/snowball/domain/NextCutoffTest.kt` *(new)* | 1 | Tests for above |
| `composeApp/src/commonMain/kotlin/com/snowball/domain/JourneyCalculator.kt` *(new)* | 2 | `JourneyStats` data class + `JourneyCalculator.compute(...)` |
| `composeApp/src/commonTest/kotlin/com/snowball/domain/JourneyCalculatorTest.kt` *(new)* | 2 | Tests for above |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/util/AmountFormat.kt` | 3 | Add `formatAmountWithSeparators` public function |
| `composeApp/src/commonTest/kotlin/com/snowball/ui/util/AmountFormatSeparatorsTest.kt` *(new)* | 3 | Tests for the separator formatter |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/components/PesoText.kt` | 3 | Use shared `formatAmountWithSeparators` |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsScreen.kt` | 3 | Use shared `formatAmountWithSeparators` (still adds ₱ prefix locally) |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/util/DateFormat.kt` *(new)* | 4 | `formatMonthYear(LocalDate)` helper |
| `composeApp/src/commonTest/kotlin/com/snowball/ui/util/DateFormatTest.kt` *(new)* | 4 | Tests for above |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeViewModel.kt` | 5 | Extend `HomeState`, extend `load()` |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/components/UpNextCard.kt` *(new)* | 6 | New stateless component |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/components/JourneyCard.kt` *(new)* | 7 | New stateless component |
| `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt` | 8 | Render both cards conditionally |

**Untouched (intentionally):**
- All data-layer code (`data/`, `db/`, `.sq` files)
- `CutoffCalculator.kt` — its existing functions are reused as-is for the next cutoff
- `DebtFormScreen.kt`, `DebtsScreen.kt`, `Nav.kt`, theme files

---

## Task 1: Add `nextCutoff(today)` to Cutoff.kt + tests

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/domain/Cutoff.kt`
- Create: `composeApp/src/commonTest/kotlin/com/snowball/domain/NextCutoffTest.kt`

**Goal:** A simple top-level function that returns the cutoff window following the one containing `today`. The existing `Cutoff` data class already has a `.next(): Cutoff` method on it, so the implementation is a one-liner.

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/snowball/domain/NextCutoffTest.kt`:

```kotlin
package com.snowball.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class NextCutoffTest {

    @Test
    fun todayInFirstHalfReturnsSecondHalfOfSameMonth() {
        // May 5 is in current cutoff = April 30 payday (covers May 1-14).
        // Next is May 15 payday (covers May 15-30).
        val next = nextCutoff(LocalDate(2026, 5, 5))
        assertEquals(2026, next.year)
        assertEquals(5, next.month)
        assertEquals(Payday.FIFTEENTH, next.payday)
        assertEquals(LocalDate(2026, 5, 15), next.windowStart)
        assertEquals(LocalDate(2026, 5, 30), next.windowEnd)
    }

    @Test
    fun todayInSecondHalfReturnsFirstHalfOfNextMonth() {
        // May 20 is in current cutoff = May 15 payday (covers May 15-30).
        // Next is May 30 payday (covers June 1-14).
        val next = nextCutoff(LocalDate(2026, 5, 20))
        assertEquals(2026, next.year)
        assertEquals(5, next.month)
        assertEquals(Payday.THIRTIETH, next.payday)
        assertEquals(LocalDate(2026, 6, 1), next.windowStart)
        assertEquals(LocalDate(2026, 6, 14), next.windowEnd)
    }

    @Test
    fun todayOnDay14ReturnsSecondHalfSameMonth() {
        // May 14 still belongs to current cutoff (April 30 payday, May 1-14).
        // Next is May 15 payday (May 15-30).
        val next = nextCutoff(LocalDate(2026, 5, 14))
        assertEquals(Payday.FIFTEENTH, next.payday)
        assertEquals(LocalDate(2026, 5, 15), next.windowStart)
    }

    @Test
    fun todayOnDay15ReturnsFirstHalfNextMonth() {
        // May 15 is in current cutoff = May 15 payday.
        // Next is May 30 payday (June 1-14).
        val next = nextCutoff(LocalDate(2026, 5, 15))
        assertEquals(Payday.THIRTIETH, next.payday)
        assertEquals(LocalDate(2026, 6, 1), next.windowStart)
    }

    @Test
    fun todayOnLastDayOfMonthReturnsFirstHalfNextMonth() {
        // May 31 belongs to current cutoff = May 15 payday.
        // Next is May 30 payday (June 1-14).
        val next = nextCutoff(LocalDate(2026, 5, 31))
        assertEquals(Payday.THIRTIETH, next.payday)
        assertEquals(2026, next.year)
        assertEquals(5, next.month)
        assertEquals(LocalDate(2026, 6, 1), next.windowStart)
        assertEquals(LocalDate(2026, 6, 14), next.windowEnd)
    }

    @Test
    fun yearBoundary() {
        // Dec 25 is in current cutoff = Dec 15 payday (Dec 15-30).
        // Next is Dec 30 payday (Jan 1-14 of next year).
        val next = nextCutoff(LocalDate(2026, 12, 25))
        assertEquals(Payday.THIRTIETH, next.payday)
        assertEquals(2026, next.year)
        assertEquals(12, next.month)
        assertEquals(LocalDate(2027, 1, 1), next.windowStart)
        assertEquals(LocalDate(2027, 1, 14), next.windowEnd)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.domain.NextCutoffTest"
```

Expected: FAIL — `nextCutoff` is unresolved.

- [ ] **Step 3: Add the function to Cutoff.kt**

Append to `composeApp/src/commonMain/kotlin/com/snowball/domain/Cutoff.kt` (after the existing `currentCutoff` function):

```kotlin
fun nextCutoff(today: LocalDate = today()): Cutoff = currentCutoff(today).next()
```

The existing `Cutoff.next()` method already computes the next half-month window. No new logic needed.

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.domain.NextCutoffTest"
```

Expected: PASS — all 6 tests green.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/domain/Cutoff.kt composeApp/src/commonTest/kotlin/com/snowball/domain/NextCutoffTest.kt
git commit -m "feat(domain): add nextCutoff helper for surfacing the next half-month window"
```

---

## Task 2: Add JourneyCalculator + tests

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/domain/JourneyCalculator.kt`
- Create: `composeApp/src/commonTest/kotlin/com/snowball/domain/JourneyCalculatorTest.kt`

**Goal:** Pure function that computes the three Your Journey stats from raw debt + payment lists, returning `null` when there's nothing melted yet. No I/O, no repository access.

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/snowball/domain/JourneyCalculatorTest.kt`:

```kotlin
package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JourneyCalculatorTest {

    private fun debt(
        id: Long = 1L,
        monthlyAmount: Double = 1500.0,
        totalPayments: Int = 12,
        startDate: LocalDate = LocalDate(2026, 1, 1),
        dueDay: Int = 10,
        useLastDayOfMonth: Boolean = false,
        isArchived: Boolean = false,
    ) = Debt(
        id = id,
        name = "Debt $id",
        categoryId = 1L,
        monthlyAmount = monthlyAmount,
        totalPayments = totalPayments,
        dueDay = dueDay,
        useLastDayOfMonth = useLastDayOfMonth,
        startDate = startDate,
        isArchived = isArchived,
        notes = null,
    )

    private fun payment(id: Long, debtId: Long, amount: Double, date: LocalDate = LocalDate(2026, 1, 10)) =
        Payment(id = id, debtId = debtId, paidDate = date, amount = amount)

    @Test
    fun noDebtsNoPaymentsReturnsNull() {
        assertNull(JourneyCalculator.compute(emptyList(), emptyList()))
    }

    @Test
    fun debtsButNoPaymentsReturnsNull() {
        assertNull(JourneyCalculator.compute(listOf(debt()), emptyList()))
    }

    @Test
    fun singlePaymentComputesPercentMeltedAndForecast() {
        // Debt: ₱1500/mo × 12 = ₱18,000 scheduled, starts 2026-01-01, dueDay 10.
        // One ₱1500 payment recorded.
        // Expected: 8% cleared (1500/18000 = 0.0833 → 8), ₱1500 melted,
        // forecast = 2026-12-10 (start + 11 months at dueDay).
        val stats = JourneyCalculator.compute(
            listOf(debt()),
            listOf(payment(id = 1L, debtId = 1L, amount = 1500.0)),
        )
        assertEquals(8, stats?.percentCleared)
        assertEquals(1500.0, stats?.totalMelted)
        assertEquals(LocalDate(2026, 12, 10), stats?.forecastEndDate)
    }

    @Test
    fun allDebtsArchivedForecastIsNull() {
        // Single fully-paid debt: 12 payments × ₱1500 = ₱18,000 melted, scheduled = ₱18,000.
        val payments = (1L..12L).map { payment(id = it, debtId = 1L, amount = 1500.0) }
        val stats = JourneyCalculator.compute(
            listOf(debt(isArchived = true)),
            payments,
        )
        assertEquals(100, stats?.percentCleared)
        assertEquals(18_000.0, stats?.totalMelted)
        assertNull(stats?.forecastEndDate)
    }

    @Test
    fun forecastPicksLatestEndDateAmongActive() {
        // Two active debts:
        //   debt 1: 12 months from 2026-01-01 day 10 → ends 2026-12-10
        //   debt 2: 24 months from 2026-03-01 day 5 → ends 2028-02-05
        // Expected forecast = 2028-02-05.
        val stats = JourneyCalculator.compute(
            listOf(
                debt(id = 1L, totalPayments = 12, startDate = LocalDate(2026, 1, 1), dueDay = 10),
                debt(id = 2L, totalPayments = 24, startDate = LocalDate(2026, 3, 1), dueDay = 5),
            ),
            listOf(payment(id = 1L, debtId = 1L, amount = 1500.0)),
        )
        assertEquals(LocalDate(2028, 2, 5), stats?.forecastEndDate)
    }

    @Test
    fun archivedDebtsExcludedFromForecast() {
        // Active debt ends 2026-06-10. Archived debt would end 2028-02-05 if active.
        // Forecast should be 2026-06-10.
        val stats = JourneyCalculator.compute(
            listOf(
                debt(id = 1L, totalPayments = 6, startDate = LocalDate(2026, 1, 1), dueDay = 10),
                debt(id = 2L, totalPayments = 24, startDate = LocalDate(2026, 3, 1), dueDay = 5, isArchived = true),
            ),
            listOf(payment(id = 1L, debtId = 1L, amount = 1500.0)),
        )
        assertEquals(LocalDate(2026, 6, 10), stats?.forecastEndDate)
    }

    @Test
    fun dueDay31ClampsToShorterMonth() {
        // Debt with dueDay = 31, totalPayments = 2, startDate = 2026-01-15 (no useLastDay).
        // End month = 2026-02. February has 28 days in 2026 → forecast = 2026-02-28 (clamped).
        val stats = JourneyCalculator.compute(
            listOf(debt(totalPayments = 2, startDate = LocalDate(2026, 1, 15), dueDay = 31)),
            listOf(payment(id = 1L, debtId = 1L, amount = 1500.0)),
        )
        assertEquals(LocalDate(2026, 2, 28), stats?.forecastEndDate)
    }

    @Test
    fun useLastDayOfMonthYieldsLastDay() {
        // Debt with useLastDayOfMonth = true, totalPayments = 3, start 2026-01-31, dueDay 31.
        // End month = 2026-03. Forecast = 2026-03-31.
        val stats = JourneyCalculator.compute(
            listOf(
                debt(
                    totalPayments = 3,
                    startDate = LocalDate(2026, 1, 31),
                    dueDay = 31,
                    useLastDayOfMonth = true,
                ),
            ),
            listOf(payment(id = 1L, debtId = 1L, amount = 1500.0)),
        )
        assertEquals(LocalDate(2026, 3, 31), stats?.forecastEndDate)
    }

    @Test
    fun percentClearedBoundedAt100() {
        // Pathological: melted > scheduled (shouldn't happen with real data, but the
        // calculator should still bound the percent at 100, not return 200%).
        val stats = JourneyCalculator.compute(
            listOf(debt(monthlyAmount = 1500.0, totalPayments = 1)),  // scheduled = 1500
            listOf(payment(id = 1L, debtId = 1L, amount = 3000.0)),
        )
        assertEquals(100, stats?.percentCleared)
        assertEquals(3000.0, stats?.totalMelted)
    }

    @Test
    fun percentClearedRoundsDown() {
        // 1499/18000 = 0.0832… → 8 (rounded down).
        val stats = JourneyCalculator.compute(
            listOf(debt()),
            listOf(payment(id = 1L, debtId = 1L, amount = 1499.0)),
        )
        assertEquals(8, stats?.percentCleared)
        assertTrue((stats?.totalMelted ?: 0.0) > 0.0)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.domain.JourneyCalculatorTest"
```

Expected: FAIL — `JourneyCalculator` and `JourneyStats` are unresolved.

- [ ] **Step 3: Create the implementation file**

Create `composeApp/src/commonMain/kotlin/com/snowball/domain/JourneyCalculator.kt`:

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

    private fun projectedEndDate(debt: Debt): LocalDate? {
        if (debt.totalPayments <= 0) return null
        val endMonth = debt.startDate.plus(debt.totalPayments - 1, DateTimeUnit.MONTH)
        return effectiveDueDate(
            year = endMonth.year,
            month = endMonth.monthNumber,
            dueDay = debt.dueDay,
            useLastDay = debt.useLastDayOfMonth,
        )
    }
}
```

The `effectiveDueDate(year, month, dueDay, useLastDay)` function already exists in `composeApp/src/commonMain/kotlin/com/snowball/domain/DateUtils.kt` — same package, no import needed. It handles dueDay clamping and the `useLastDayOfMonth` flag, returning `null` for impossible date combinations.

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.domain.JourneyCalculatorTest"
```

Expected: PASS — all 10 tests green.

- [ ] **Step 5: Build the full debug variant to confirm no regressions**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/domain/JourneyCalculator.kt composeApp/src/commonTest/kotlin/com/snowball/domain/JourneyCalculatorTest.kt
git commit -m "feat(domain): add JourneyCalculator for cumulative progress stats"
```

---

## Task 3: Lift `formatAmountWithSeparators` to AmountFormat.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/util/AmountFormat.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/PesoText.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsScreen.kt`
- Create: `composeApp/src/commonTest/kotlin/com/snowball/ui/util/AmountFormatSeparatorsTest.kt`

**Goal:** Unify the duplicated thousand-separator formatters. `PesoText` has a private `formatAmount` (no ₱ prefix, used in Row's body Text). `SettingsScreen` has a private `toFormattedPeso` (adds ₱ prefix). Both produce the same digits — extract the digit logic, keep the prefix concerns local.

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/snowball/ui/util/AmountFormatSeparatorsTest.kt`:

```kotlin
package com.snowball.ui.util

import kotlin.test.Test
import kotlin.test.assertEquals

class AmountFormatSeparatorsTest {

    @Test
    fun zeroRendersAsZero() {
        assertEquals("0", formatAmountWithSeparators(0.0))
    }

    @Test
    fun smallWholeNumberNoSeparator() {
        assertEquals("500", formatAmountWithSeparators(500.0))
    }

    @Test
    fun thousandsInsertSeparator() {
        assertEquals("1,500", formatAmountWithSeparators(1500.0))
    }

    @Test
    fun millionsInsertMultipleSeparators() {
        assertEquals("1,500,000", formatAmountWithSeparators(1_500_000.0))
    }

    @Test
    fun nonWholeAddsTwoDecimals() {
        assertEquals("1,500.50", formatAmountWithSeparators(1500.5))
    }

    @Test
    fun floatingPointEdgeCase() {
        // 1500.56 is stored as ~1500.5599999999999; the +0.5 rounding ensures we get 56.
        assertEquals("1,500.56", formatAmountWithSeparators(1500.56))
    }

    @Test
    fun negativeRendersWithLeadingSign() {
        assertEquals("-2,250.50", formatAmountWithSeparators(-2250.5))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.ui.util.AmountFormatSeparatorsTest"
```

Expected: FAIL — `formatAmountWithSeparators` is unresolved.

- [ ] **Step 3: Add the function to AmountFormat.kt**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/util/AmountFormat.kt`. The current file content (after v0.1.1):

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
    val fractionalPart = abs(this) - abs(whole)
    val fraction = (fractionalPart * 100 + 0.5).toInt()
    return if (fraction == 0) whole.toString()
    else "$whole.${fraction.toString().padStart(2, '0')}"
}
```

Append the new function:

```kotlin

/**
 * Renders a Double as a separator-grouped amount with at most 2 decimal places.
 * Whole values render with no decimals (1500.0 -> "1,500").
 * Non-whole values render with two decimals (1500.5 -> "1,500.50").
 * Zero renders as "0" (caller decides whether to prefix or hide).
 * Uses round-half-up at the hundredths place to absorb IEEE 754 representation errors
 * (1500.56 is stored as ~1500.5599999... ; without the +0.5 we'd truncate to "1,500.55").
 * Does NOT include the ₱ glyph — caller adds the currency prefix where needed.
 */
fun formatAmountWithSeparators(amount: Double): String {
    val whole = amount.toLong()
    val fraction = ((abs(amount) - abs(whole)) * 100 + 0.5).toInt()
    val absWholeStr = abs(whole).toString()
    val grouped = absWholeStr
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
    val signedGrouped = if (whole < 0) "-$grouped" else grouped
    return if (fraction == 0) signedGrouped
    else "$signedGrouped.${fraction.toString().padStart(2, '0')}"
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.ui.util.AmountFormatSeparatorsTest"
```

Expected: PASS — all 7 tests green.

- [ ] **Step 5: Migrate PesoText.kt to use the shared formatter**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/components/PesoText.kt`. Current relevant excerpt (the body Row uses `val formatted = formatAmount(amount)` near the top of the composable, and `formatAmount` is the private function at the bottom):

Add this import near the existing imports:

```kotlin
import com.snowball.ui.util.formatAmountWithSeparators
```

Replace the line inside the composable:

```kotlin
    val formatted = formatAmount(amount)
```

with:

```kotlin
    val formatted = formatAmountWithSeparators(amount)
```

Then delete the entire private `formatAmount` function at the bottom of the file (it had truncation behavior; the v0.1.1 cleanup already replaced its body with the +0.5 rounding pattern; now we delete the function entirely since the shared one supersedes it).

- [ ] **Step 6: Migrate SettingsScreen.kt to use the shared formatter**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsScreen.kt`. Current `toFormattedPeso` function:

```kotlin
private fun Double.toFormattedPeso(): String {
    if (this == 0.0) return ""
    val whole = this.toLong()
    val grouped = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    val frac = ((this - whole) * 100 + 0.5).toInt()
    return if (frac == 0) "₱$grouped" else "₱$grouped.${frac.toString().padStart(2, '0')}"
}
```

Add this import:

```kotlin
import com.snowball.ui.util.formatAmountWithSeparators
```

Replace the private function body:

```kotlin
private fun Double.toFormattedPeso(): String {
    if (this == 0.0) return ""
    return "₱${formatAmountWithSeparators(this)}"
}
```

The `if (this == 0.0) return ""` behavior is preserved — the empty-display rule belongs to Settings (placeholder logic), not the shared formatter.

- [ ] **Step 7: Build and run all tests to confirm no regressions**

```bash
./gradlew.bat :composeApp:assembleDebug :composeApp:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL and all tests pass (including the new 7 tests, plus all existing tests).

- [ ] **Step 8: Install + spot-check on emulator**

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

Open the app. Visit Home, Debts, Settings — amounts should display identically to before (this is a pure refactor; same digits, same separators).

- [ ] **Step 9: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/util/AmountFormat.kt composeApp/src/commonTest/kotlin/com/snowball/ui/util/AmountFormatSeparatorsTest.kt composeApp/src/commonMain/kotlin/com/snowball/ui/components/PesoText.kt composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsScreen.kt
git commit -m "refactor(util): unify amount-with-separators formatter; remove duplication"
```

---

## Task 4: Add DateFormat.kt with `formatMonthYear`

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/util/DateFormat.kt`
- Create: `composeApp/src/commonTest/kotlin/com/snowball/ui/util/DateFormatTest.kt`

**Goal:** A small helper for rendering "Aug 2027" style month-year strings, used by JourneyCard.

- [ ] **Step 1: Write the failing test**

Create `composeApp/src/commonTest/kotlin/com/snowball/ui/util/DateFormatTest.kt`:

```kotlin
package com.snowball.ui.util

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormatTest {

    @Test
    fun januaryFormatsCorrectly() {
        assertEquals("Jan 2026", formatMonthYear(LocalDate(2026, 1, 1)))
    }

    @Test
    fun augustFormatsCorrectly() {
        assertEquals("Aug 2027", formatMonthYear(LocalDate(2027, 8, 15)))
    }

    @Test
    fun decemberFormatsCorrectly() {
        assertEquals("Dec 2025", formatMonthYear(LocalDate(2025, 12, 31)))
    }

    @Test
    fun dayOfMonthIgnored() {
        // Day shouldn't affect the output.
        assertEquals(
            formatMonthYear(LocalDate(2026, 5, 1)),
            formatMonthYear(LocalDate(2026, 5, 31)),
        )
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.ui.util.DateFormatTest"
```

Expected: FAIL — `formatMonthYear` is unresolved.

- [ ] **Step 3: Create the implementation file**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/util/DateFormat.kt`:

```kotlin
package com.snowball.ui.util

import kotlinx.datetime.LocalDate

private val MONTHS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

/** Renders a date as "MMM YYYY" (e.g. "Aug 2027"). The day-of-month is ignored. */
fun formatMonthYear(date: LocalDate): String =
    "${MONTHS[date.monthNumber - 1]} ${date.year}"
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.ui.util.DateFormatTest"
```

Expected: PASS — all 4 tests green.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/util/DateFormat.kt composeApp/src/commonTest/kotlin/com/snowball/ui/util/DateFormatTest.kt
git commit -m "feat(util): add formatMonthYear helper for compact month-year display"
```

---

## Task 5: Extend HomeState and HomeViewModel.load()

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeViewModel.kt`

**Goal:** Add four new fields to `HomeState` and compute them in `load()`. `HomeState` is currently a data class with 4 fields; we add 4 more.

- [ ] **Step 1: Modify HomeViewModel.kt**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeViewModel.kt`. The current file content:

```kotlin
package com.snowball.ui.home

import com.snowball.data.Repos
import com.snowball.domain.Cutoff
import com.snowball.domain.CutoffCalculator
import com.snowball.domain.DueRow
import com.snowball.domain.currentCutoff
import com.snowball.domain.today
import kotlinx.datetime.LocalDate

data class HomeState(
    val cutoff: Cutoff,
    val rows: List<DueRow>,
    val summary: CutoffCalculator.Summary,
    val income: Double,
)

class HomeViewModel(private val repos: Repos) {

    fun load(today: LocalDate = today()): HomeState {
        val cutoff = currentCutoff(today)
        val debts = repos.debts.allActive()
        val paymentsByDebt = debts.associate { it.id to repos.payments.historyForDebt(it.id) }
        val rows = CutoffCalculator.computeDueRows(cutoff, debts, paymentsByDebt)
        val income = repos.settings.get().incomePerCutoff
        val summary = CutoffCalculator.summarize(rows, income)
        return HomeState(cutoff, rows, summary, income)
    }

    fun markPaid(row: DueRow, todayDate: LocalDate = today()) {
        repos.payments.markPaid(row.debt.id, todayDate, row.amount)
        val totalPayments = repos.payments.countForDebt(row.debt.id)
        if (totalPayments >= row.debt.totalPayments) {
            repos.debts.setArchived(row.debt.id, true)
        }
    }

    fun undoPayment(row: DueRow) {
        val history = repos.payments.historyForDebt(row.debt.id)
        val latest = history.firstOrNull() ?: return
        repos.payments.delete(latest.id)
        if (row.debt.isArchived) {
            repos.debts.setArchived(row.debt.id, false)
        }
    }
}
```

Replace the entire file contents with:

```kotlin
package com.snowball.ui.home

import com.snowball.data.Repos
import com.snowball.domain.Cutoff
import com.snowball.domain.CutoffCalculator
import com.snowball.domain.DueRow
import com.snowball.domain.JourneyCalculator
import com.snowball.domain.JourneyStats
import com.snowball.domain.currentCutoff
import com.snowball.domain.nextCutoff
import com.snowball.domain.today
import kotlinx.datetime.LocalDate

data class HomeState(
    val cutoff: Cutoff,
    val rows: List<DueRow>,
    val summary: CutoffCalculator.Summary,
    val income: Double,
    val nextCutoff: Cutoff,
    val nextRows: List<DueRow>,
    val nextTotal: Double,
    val journey: JourneyStats?,
)

class HomeViewModel(private val repos: Repos) {

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

        return HomeState(cutoff, rows, summary, income, next, nextRows, nextTotal, journey)
    }

    fun markPaid(row: DueRow, todayDate: LocalDate = today()) {
        repos.payments.markPaid(row.debt.id, todayDate, row.amount)
        val totalPayments = repos.payments.countForDebt(row.debt.id)
        if (totalPayments >= row.debt.totalPayments) {
            repos.debts.setArchived(row.debt.id, true)
        }
    }

    fun undoPayment(row: DueRow) {
        val history = repos.payments.historyForDebt(row.debt.id)
        val latest = history.firstOrNull() ?: return
        repos.payments.delete(latest.id)
        if (row.debt.isArchived) {
            repos.debts.setArchived(row.debt.id, false)
        }
    }
}
```

Note on the `next` local variable: the field is named `nextCutoff` to mirror the existing `cutoff` field name, but we can't use `nextCutoff(today)` as the property initializer because Kotlin resolves identifiers in scope first — we'd shadow the function. Using a `next` local variable + named-parameter passing in the constructor keeps both names available.

- [ ] **Step 2: Build to confirm compile**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL. (`HomeScreen.kt` still references the original `state.cutoff`, `state.rows`, etc. — those fields still exist. The new fields are additions, not replacements, so HomeScreen continues to compile.)

- [ ] **Step 3: Run all tests to confirm no regressions**

```bash
./gradlew.bat :composeApp:testDebugUnitTest
```

Expected: all tests pass. (No new tests for HomeViewModel — it's mechanical wiring of pre-tested domain functions.)

- [ ] **Step 4: Install + verify the app still runs**

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

The app should look identical to before this task — Home renders the cutoff card + payments + (if applicable) journey/up-next data is computed but not yet rendered. Tap through Home / Debts / Settings to verify nothing crashed.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeViewModel.kt
git commit -m "feat(home): compute nextCutoff rows and JourneyStats in HomeViewModel"
```

---

## Task 6: Implement UpNextCard.kt

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/UpNextCard.kt`

**Goal:** A stateless composable that renders the collapsed Up-next header (range, total, count, chevron) and animates an expanded list of debt rows underneath. State is owned by the caller.

- [ ] **Step 1: Create the file**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/components/UpNextCard.kt`:

```kotlin
package com.snowball.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.domain.Cutoff
import com.snowball.domain.DueRow
import com.snowball.ui.theme.SnowColors

@Composable
fun UpNextCard(
    cutoff: Cutoff,
    rows: List<DueRow>,
    total: Double,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val rangeLabel = cutoffRangeLabel(cutoff)
    val stateDesc = if (isExpanded) "Expanded" else "Collapsed"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(SnowColors.NightElev)
            .border(width = 1.dp, color = SnowColors.LineStrong, shape = RoundedCornerShape(28.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                stateDescription = stateDesc
                contentDescription = "Up next, $rangeLabel, ${total.toLong()} pesos, ${rows.size} debts"
            }
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            "UP NEXT · $rangeLabel",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PesoText(
                amount = total,
                style = MaterialTheme.typography.headlineMedium,
                pesoColor = SnowColors.FrostDim,
                numberColor = SnowColors.Frost,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "· ${rows.size} debts",
                style = MaterialTheme.typography.bodyMedium,
                color = SnowColors.FrostMute,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = SnowColors.FrostMute,
                modifier = Modifier.size(20.dp),
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SnowColors.Line),
                )
                Spacer(Modifier.height(4.dp))
                rows.forEach { row ->
                    UpNextRow(row = row)
                }
            }
        }
    }
}

@Composable
private fun UpNextRow(row: DueRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${row.debt.name}, ${row.amount.toLong()} pesos, due day ${row.debt.dueDay}"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            row.debt.name,
            style = MaterialTheme.typography.bodyMedium,
            color = SnowColors.Frost,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "Due ${row.debt.dueDay}",
            style = MaterialTheme.typography.bodySmall,
            color = SnowColors.FrostMute,
        )
        Spacer(Modifier.width(12.dp))
        PesoText(
            amount = row.amount,
            style = MaterialTheme.typography.bodyLarge,
            pesoColor = SnowColors.FrostDim,
            numberColor = SnowColors.Frost,
        )
    }
}
```

This composable:
- Reuses `cutoffRangeLabel(cutoff: Cutoff)` from `CutoffCard.kt` (same package — no import needed).
- Reuses `PesoText` for amounts.
- Uses `AnimatedVisibility` with `expandVertically + fadeIn` for the expanded list.
- Whole-card click triggers haptic + `onToggle`.
- Semantics merged so TalkBack reads the whole card as one button.

- [ ] **Step 2: Build to verify compile**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL. Lint may warn about an unused composable (`UpNextCard` isn't wired into HomeScreen yet — that's Task 8). Lint warnings don't fail the build.

- [ ] **Step 3: Run tests to confirm no regressions**

```bash
./gradlew.bat :composeApp:testDebugUnitTest
```

Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/components/UpNextCard.kt
git commit -m "feat(home): add UpNextCard component with collapsible debt list"
```

---

## Task 7: Implement JourneyCard.kt

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/JourneyCard.kt`

**Goal:** A stateless composable that renders the three Your Journey stats. Hero "% cleared" with "cleared" sublabel, then a subtitle row showing melted + forecast.

- [ ] **Step 1: Create the file**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/components/JourneyCard.kt`:

```kotlin
package com.snowball.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.domain.JourneyStats
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.formatAmountWithSeparators
import com.snowball.ui.util.formatMonthYear

@Composable
fun JourneyCard(stats: JourneyStats, modifier: Modifier = Modifier) {
    val meltedText = "₱${formatAmountWithSeparators(stats.totalMelted)} melted"
    val forecastText = if (stats.forecastEndDate == null) "All clear ✓"
                       else "Free by ${formatMonthYear(stats.forecastEndDate)}"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(SnowColors.CardElev)
            .border(width = 1.dp, color = SnowColors.LineStrong, shape = RoundedCornerShape(28.dp))
            .semantics(mergeDescendants = true) {
                contentDescription = "Your journey, ${stats.percentCleared} percent cleared, $meltedText, $forecastText"
            }
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(
            "YOUR JOURNEY",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "${stats.percentCleared}%",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.W300),
            color = SnowColors.Ice,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "cleared",
            style = MaterialTheme.typography.labelMedium,
            color = SnowColors.FrostMute,
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                meltedText,
                style = MaterialTheme.typography.bodyMedium,
                color = SnowColors.FrostMute,
            )
            Text(
                " · ",
                style = MaterialTheme.typography.bodyMedium,
                color = SnowColors.FrostMute,
            )
            Text(
                forecastText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (stats.forecastEndDate == null) SnowColors.Ice else SnowColors.FrostMute,
            )
        }
    }
}
```

- [ ] **Step 2: Build to verify compile**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run tests to confirm no regressions**

```bash
./gradlew.bat :composeApp:testDebugUnitTest
```

Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/components/JourneyCard.kt
git commit -m "feat(home): add JourneyCard component with cumulative progress stats"
```

---

## Task 8: Wire both cards into HomeScreen.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt`

**Goal:** Render `UpNextCard` between `CutoffCard` and the PAYMENTS section (only when `state.nextRows` is non-empty), and render `JourneyCard` below the payments list + trailing spacer (only when `state.journey != null`). Track the Up next expanded state locally with `remember`.

- [ ] **Step 1: Modify HomeScreen.kt**

Open `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt`. The file currently begins:

```kotlin
package com.snowball.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
... (lots of imports) ...
import com.snowball.domain.DueRow
import com.snowball.ui.components.CutoffCard
import com.snowball.ui.components.PesoText
import com.snowball.ui.components.ProgressArc
import com.snowball.ui.theme.SnowColors
```

Add these two imports (alphabetical) to the imports block:

```kotlin
import com.snowball.ui.components.JourneyCard
import com.snowball.ui.components.UpNextCard
```

The current main `HomeScreen` composable body (post-v0.1.1) renders:

```kotlin
@Composable
fun HomeScreen(vm: HomeViewModel) {
    var tick by remember { mutableStateOf(0) }
    val state = remember(tick) { vm.load() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        CutoffCard(
            cutoff = state.cutoff,
            summary = state.summary,
            incomePerCutoff = state.income,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "PAYMENTS",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Swipe left to mark paid · swipe right to undo",
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = SnowColors.FrostMute,
        )

        Spacer(Modifier.height(8.dp))
        if (state.rows.isEmpty()) {
            val message = when {
                state.income == 0.0 -> "Start by setting your income in Settings."
                else -> "No payments due this cutoff yet.\nAdd debts from the Debts tab."
            }
            EmptyHint(message)
        } else {
            state.rows.forEach { row ->
                key(row.debt.id) {
                    SwipeablePaymentRow(
                        row = row,
                        onMarkPaid = { vm.markPaid(row); tick++ },
                        onUndo = { vm.undoPayment(row); tick++ },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
```

Two insertion points:

**Insertion 1** — between `CutoffCard(...)` and the `Spacer(Modifier.height(24.dp))` before "PAYMENTS":

```kotlin
        CutoffCard(
            cutoff = state.cutoff,
            summary = state.summary,
            incomePerCutoff = state.income,
        )

        if (state.nextRows.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            var upNextExpanded by remember { mutableStateOf(false) }
            UpNextCard(
                cutoff = state.nextCutoff,
                rows = state.nextRows,
                total = state.nextTotal,
                isExpanded = upNextExpanded,
                onToggle = { upNextExpanded = !upNextExpanded },
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "PAYMENTS",
            ...
```

**Insertion 2** — inside the `else` branch of `if (state.rows.isEmpty())`, after the existing `Spacer(Modifier.height(16.dp))` but before the closing `}` of the else block. The Journey card also renders when `state.rows.isEmpty()` (a fully-paid user with all debts archived would have empty rows but still want to see their journey), so move it OUTSIDE the if/else and conditional on `state.journey != null`.

Modify the if/else block to:

```kotlin
        Spacer(Modifier.height(8.dp))
        if (state.rows.isEmpty()) {
            val message = when {
                state.income == 0.0 -> "Start by setting your income in Settings."
                else -> "No payments due this cutoff yet.\nAdd debts from the Debts tab."
            }
            EmptyHint(message)
        } else {
            state.rows.forEach { row ->
                key(row.debt.id) {
                    SwipeablePaymentRow(
                        row = row,
                        onMarkPaid = { vm.markPaid(row); tick++ },
                        onUndo = { vm.undoPayment(row); tick++ },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (state.journey != null) {
            Spacer(Modifier.height(24.dp))
            JourneyCard(stats = state.journey)
        }
    }
}
```

The final composable body, after edits, should look like:

```kotlin
@Composable
fun HomeScreen(vm: HomeViewModel) {
    var tick by remember { mutableStateOf(0) }
    val state = remember(tick) { vm.load() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        CutoffCard(
            cutoff = state.cutoff,
            summary = state.summary,
            incomePerCutoff = state.income,
        )

        if (state.nextRows.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            var upNextExpanded by remember { mutableStateOf(false) }
            UpNextCard(
                cutoff = state.nextCutoff,
                rows = state.nextRows,
                total = state.nextTotal,
                isExpanded = upNextExpanded,
                onToggle = { upNextExpanded = !upNextExpanded },
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "PAYMENTS",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Swipe left to mark paid · swipe right to undo",
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = SnowColors.FrostMute,
        )

        Spacer(Modifier.height(8.dp))
        if (state.rows.isEmpty()) {
            val message = when {
                state.income == 0.0 -> "Start by setting your income in Settings."
                else -> "No payments due this cutoff yet.\nAdd debts from the Debts tab."
            }
            EmptyHint(message)
        } else {
            state.rows.forEach { row ->
                key(row.debt.id) {
                    SwipeablePaymentRow(
                        row = row,
                        onMarkPaid = { vm.markPaid(row); tick++ },
                        onUndo = { vm.undoPayment(row); tick++ },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (state.journey != null) {
            Spacer(Modifier.height(24.dp))
            JourneyCard(stats = state.journey)
        }
    }
}
```

The SwipeablePaymentRow, PaymentRowContent, SwipeBackground, ActionBackground, and EmptyHint composables at the bottom of the file are unchanged.

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

- [ ] **Step 4: Install on emulator**

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell pm clear com.snowball
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

`pm clear` ensures a fresh state for verification.

- [ ] **Step 5: Verify cleared-install state**

The app should launch on Home with:
- Cutoff card visible (DUE = 0, since no debts)
- NO Up next card (no debts → no rows for next cutoff)
- NO Journey card (no payments yet)
- "Start by setting your income in Settings." empty hint

Capture:
```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell uiautomator dump /sdcard/ui.xml
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe pull /sdcard/ui.xml /c/Users/Pika/AppData/Local/Temp/ui-v02a-home-empty.xml
```

Confirm no `UP NEXT` or `YOUR JOURNEY` text appears in the dump.

- [ ] **Step 6: Add a debt and verify Up next card**

Add a debt via the FAB on Debts. Use:
- Name: "Sloan"
- Monthly amount: 1500
- Total payments: 12
- Due day: 20 (in the second half of the month so it falls in the next cutoff)
- Start date: 2026-04-01 (well before today's payDate)

Save. Return to Home.

Expected:
- Cutoff card unchanged.
- **UP NEXT · {next cutoff range}** card appears below the cutoff card with **₱1,500 · 1 debts** and a down-chevron.
- PAYMENTS list might or might not include Sloan depending on dueDay vs current cutoff window.

Tap the Up next card. The card should expand (smooth animation) to show "Sloan  Due 20  ₱1,500". Tap again to collapse.

Capture both states:
```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell uiautomator dump /sdcard/ui.xml
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe pull /sdcard/ui.xml /c/Users/Pika/AppData/Local/Temp/ui-v02a-home-upnext-collapsed.xml
# tap to expand, then:
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell uiautomator dump /sdcard/ui.xml
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe pull /sdcard/ui.xml /c/Users/Pika/AppData/Local/Temp/ui-v02a-home-upnext-expanded.xml
```

- [ ] **Step 7: Mark a payment paid and verify Journey card appears**

If the current PAYMENTS list contains an entry (it should if dueDay or starting conditions made it fall in current cutoff), swipe one left to mark paid. Otherwise, add a second debt with dueDay in the current cutoff window (e.g., dueDay = 5) and pay it.

After the swipe-paid:
- A YOUR JOURNEY card should appear at the bottom of Home.
- It should show `N%` cleared, `₱{amount} melted`, and `Free by {Month Year}`.

Capture:
```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell uiautomator dump /sdcard/ui.xml
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe pull /sdcard/ui.xml /c/Users/Pika/AppData/Local/Temp/ui-v02a-home-with-journey.xml
```

Confirm "YOUR JOURNEY" text appears in the dump.

- [ ] **Step 8: Verify font_scale = 1.30 doesn't break the layout**

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell settings put system font_scale 1.30
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am force-stop com.snowball
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

Scroll Home. Confirm:
- Up next card still legible and tappable.
- Journey card still legible.
- No clipping or overlap between cards.

Revert:
```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell settings put system font_scale 1.0
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am force-stop com.snowball
```

- [ ] **Step 9: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt
git commit -m "feat(home): wire UpNextCard and JourneyCard into HomeScreen"
```

---

## Task 9: End-to-end verification + tag v0.2.0

**Files:** No code changes.

**Goal:** Walk through the full Home flow, run all tests one more time, tag the release.

- [ ] **Step 1: Run the full test suite (debug + release variants)**

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:test
```

Expected: BUILD SUCCESSFUL. The new tests should be:
- `AmountFormatTest` — 7 tests (from v0.1.1)
- `AmountFormatSeparatorsTest` — 7 tests (this plan, Task 3)
- `DateFormatTest` — 4 tests (this plan, Task 4)
- `DebtFormStateValidationTest` — 9 tests (from v0.1.1 + post-review fix)
- `NextCutoffTest` — 6 tests (this plan, Task 1)
- `JourneyCalculatorTest` — 10 tests (this plan, Task 2)
- Plus all pre-existing domain tests (`CutoffCalculatorTest`, `CutoffTest`, `DateUtilsTest`) and repo tests

Total new tests this plan: 27 (7 + 4 + 6 + 10).

- [ ] **Step 2: Clear-install + full flow walkthrough**

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell pm clear com.snowball
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

Walk through:

| Step | Action | Expected on Home |
|---|---|---|
| 1 | Cleared install | Cutoff card only; no Up next; no Journey. Empty hint says "Start by setting your income". |
| 2 | Set income to 25000 in Settings, return to Home | Empty hint flips to "Add debts from the Debts tab". |
| 3 | Add a debt due in current cutoff (dueDay 5, startDate Apr 1) | Debt appears in PAYMENTS list. No Up next (no debts in next cutoff). |
| 4 | Add a second debt due in next cutoff (dueDay 20) | PAYMENTS may still show only the first debt. Up next card appears under cutoff with "₱X · 1 debts". |
| 5 | Tap Up next card | Expands inline, shows the dueDay-20 debt by name. |
| 6 | Tap again | Collapses. |
| 7 | Swipe the dueDay-5 debt paid on PAYMENTS | Journey card appears at bottom of Home: "X% cleared, ₱amount melted, Free by Month Year". |
| 8 | Swipe undo on the same payment | Journey card disappears. |

- [ ] **Step 3: Tag v0.2.0 locally**

```bash
git tag -a v0.2.0 -m "v0.2.0 — Home: Up next card + Your journey card (sub-project A)"
git log --oneline d3eaf87..HEAD
git tag --list 'v*'
```

The tag is local only. The user explicitly pushes with `git push --tags` when ready.

- [ ] **Step 4: Done**

No commit for this task — it's verification. Plan complete.

---

## Spec coverage check

| Spec section | Plan task(s) |
|---|---|
| 1. Up next card placement + visual | 6 (component), 8 (wiring) |
| 1. Up next collapsed state | 6 |
| 1. Up next expanded state with debt rows | 6 |
| 1. Up next animation + interaction (haptic + toggle) | 6 |
| 1. Up next visibility rule (hide when `nextRows.isEmpty()`) | 8 |
| 1. Up next accessibility (role, stateDescription) | 6 |
| 2. Journey card placement + visual | 7 (component), 8 (wiring) |
| 2. Journey hero + subtitle layout | 7 |
| 2. Journey stats semantics (percent, melted, forecast) | 2 (calculator) |
| 2. Journey visibility rule (hide when journey is null) | 8 |
| 2. Journey accessibility (merged content description) | 7 |
| 3.1 `nextCutoff(today)` helper | 1 |
| 3.2 `JourneyCalculator.compute(...)` + `JourneyStats` | 2 |
| 3.3 `HomeState` + `HomeViewModel.load()` extensions | 5 |
| 4. HomeScreen layout edits | 8 |
| 5.1 `formatAmountWithSeparators` lift + PesoText/Settings migration | 3 |
| 5.2 `DateFormat.formatMonthYear` helper | 4 |

Every numbered spec section maps to at least one task. No orphans.

## Risks recap

- The `next` local variable in `HomeViewModel.load()` shadows the function name `nextCutoff` if used directly as a property name. Task 5 uses a `next` intermediate variable to avoid the shadowing — verify the agent doesn't try to inline this.
- The Up next list and current cutoff PAYMENTS list both render the same debt name when its monthly payment cadence puts it in both windows. This is correct (two separate scheduled payments), but documented here so the implementer doesn't try to de-duplicate.
- `repos.debts.all()` returns all debts (active + archived). The journey calculation depends on this (archived debts contribute to scheduled total). `repos.debts.allActive()` (used for cutoff rows) does NOT include archived. Don't conflate the two in `HomeViewModel.load()`.
