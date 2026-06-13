# Insights View Toggle — Implementation Plan

> Execute via superpowers:subagent-driven-development or inline. Steps use `- [ ]`.

**Goal:** Add a per-cutoff / per-month segmented toggle to the Insights UPCOMING forecast.

**Architecture:** A pure `aggregateByMonth(cutoffs, incomePerCutoff)` over the existing forecast (TDD'd) → `MonthForecast` list on `InsightsState`. A reusable outlined sliding-pill `SegmentedToggle` drives which list (`forecast` vs `monthForecast`) the UPCOMING section renders.

**Tech:** Kotlin Multiplatform · Compose Multiplatform 1.8.0 · kotlin.test.

**Spec:** `docs/superpowers/specs/2026-06-01-insights-view-toggle-design.md`

**Build preamble:** `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; $env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"`. Git at `C:\Program Files\Git\cmd`. Commit messages via `git commit -F <file>` (apostrophes break PS here-strings). Never Read a .png.

---

## File Structure

- **Modify** `domain/InsightsCalculator.kt` — add `MonthForecast` + `aggregateByMonth`.
- **Modify** `domain/InsightsCalculatorTest.kt` — tests for `aggregateByMonth`.
- **Modify** `ui/insights/InsightsViewModel.kt` — add `monthForecast` to state.
- **Create** `ui/components/SegmentedToggle.kt` — the toggle.
- **Modify** `ui/insights/InsightsScreen.kt` — toggle + conditional list + `MonthForecastRow`.

## Task 1: `aggregateByMonth` (TDD)

**Files:** `domain/InsightsCalculator.kt`, `domain/InsightsCalculatorTest.kt`

- [ ] **Step 1: Failing tests** — append to `InsightsCalculatorTest`:

```kotlin
private fun cf(year: Int, month: Int, payday: Payday, due: Double, allClear: Boolean = false) =
    CutoffForecast(Cutoff(year, month, payday), dueTotal = due, leftOver = 0.0, isAllClear = allClear)

@Test
fun aggregate_merges_two_paydays_of_a_month() {
    val rows = InsightsCalculator.aggregateByMonth(
        listOf(
            cf(2026, 6, Payday.FIFTEENTH, 3600.0),
            cf(2026, 6, Payday.THIRTIETH, 8200.0),
        ),
        incomePerCutoff = 10000.0,
    )
    assertEquals(1, rows.size)
    assertEquals(2026, rows[0].year); assertEquals(6, rows[0].month)
    assertEquals(11800.0, rows[0].dueTotal)
    assertEquals(20000.0 - 11800.0, rows[0].leftOver) // income*2 - due
}

@Test
fun aggregate_partial_month_scales_income_by_payday_count() {
    val rows = InsightsCalculator.aggregateByMonth(
        listOf(cf(2026, 6, Payday.THIRTIETH, 8200.0)),
        incomePerCutoff = 10000.0,
    )
    assertEquals(1, rows.size)
    assertEquals(10000.0 - 8200.0, rows[0].leftOver) // income*1 - due
}

@Test
fun aggregate_all_clear_only_when_every_payday_clear() {
    val mixed = InsightsCalculator.aggregateByMonth(
        listOf(
            cf(2026, 6, Payday.FIFTEENTH, 0.0, allClear = true),
            cf(2026, 6, Payday.THIRTIETH, 8200.0, allClear = false),
        ),
        incomePerCutoff = 10000.0,
    )
    assertEquals(false, mixed[0].isAllClear)

    val clear = InsightsCalculator.aggregateByMonth(
        listOf(
            cf(2026, 6, Payday.FIFTEENTH, 0.0, allClear = true),
            cf(2026, 6, Payday.THIRTIETH, 0.0, allClear = true),
        ),
        incomePerCutoff = 10000.0,
    )
    assertEquals(true, clear[0].isAllClear)
}

@Test
fun aggregate_sorts_across_year_boundary() {
    val rows = InsightsCalculator.aggregateByMonth(
        listOf(
            cf(2027, 1, Payday.FIFTEENTH, 100.0),
            cf(2026, 12, Payday.THIRTIETH, 200.0),
        ),
        incomePerCutoff = 10000.0,
    )
    assertEquals(listOf(2026 to 12, 2027 to 1), rows.map { it.year to it.month })
}

@Test
fun aggregate_empty_input_empty_output() {
    assertTrue(InsightsCalculator.aggregateByMonth(emptyList(), 10000.0).isEmpty())
}
```

- [ ] **Step 2: Run — expect red** (`aggregateByMonth` unresolved):
`.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.domain.InsightsCalculatorTest"`

- [ ] **Step 3: Implement** — in `InsightsCalculator.kt`, add the data class (top-level, next to `CutoffForecast`) and the function (inside the `InsightsCalculator` object):

```kotlin
data class MonthForecast(
    val year: Int,
    val month: Int,
    val dueTotal: Double,
    val leftOver: Double,
    val isAllClear: Boolean,
)
```

```kotlin
    fun aggregateByMonth(
        cutoffs: List<CutoffForecast>,
        incomePerCutoff: Double,
    ): List<MonthForecast> =
        cutoffs
            .groupBy { it.cutoff.year to it.cutoff.month }
            .map { (key, group) ->
                val due = group.sumOf { it.dueTotal }
                MonthForecast(
                    year = key.first,
                    month = key.second,
                    dueTotal = due,
                    leftOver = incomePerCutoff * group.size - due,
                    isAllClear = group.all { it.isAllClear },
                )
            }
            .sortedWith(compareBy({ it.year }, { it.month }))
```

- [ ] **Step 4: Run — expect green.** Commit:
`git add composeApp/src/commonMain/kotlin/com/snowball/domain/InsightsCalculator.kt composeApp/src/commonTest/kotlin/com/snowball/domain/InsightsCalculatorTest.kt`
message: `feat(insights): aggregateByMonth — per-month forecast rollup (TDD)`

## Task 2: ViewModel exposes monthForecast

**Files:** `ui/insights/InsightsViewModel.kt`

- [ ] **Step 1:** Add `val monthForecast: List<MonthForecast>` to `InsightsState` (import `com.snowball.domain.MonthForecast`).
- [ ] **Step 2:** In `load()`, after `forecast` is computed: `val monthForecast = InsightsCalculator.aggregateByMonth(forecast, income)`; include it in the returned `InsightsState`.
- [ ] **Step 3:** Compile (`:composeApp:compileDebugKotlinAndroid`). Commit: `feat(insights): expose monthForecast in InsightsState`.

## Task 3: `SegmentedToggle` component

**Files:** create `ui/components/SegmentedToggle.kt`

- [ ] **Step 1:** Create the file:

```kotlin
package com.snowball.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snowball.ui.theme.SnowColors

/** Outlined sliding-pill segmented control. Two-plus options; the thumb slides to
 *  the selected segment. Active label Ice, inactive FrostDim. */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "segThumb",
    )
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(SnowColors.Night)
            .border(1.dp, SnowColors.LineStrong, RoundedCornerShape(50))
            .padding(4.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val segWidth = maxWidth / options.size
            Box(
                Modifier
                    .offset(x = segWidth * fraction)
                    .width(segWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .border(1.5.dp, SnowColors.Ice, RoundedCornerShape(50)),
            )
            Row(Modifier.fillMaxSize()) {
                options.forEachIndexed { i, label ->
                    val active = i == selectedIndex
                    val color by animateColorAsState(
                        targetValue = if (active) SnowColors.Ice else SnowColors.FrostDim,
                        animationSpec = tween(200),
                        label = "segText$i",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelect(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                            color = color,
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2:** Compile. Commit: `feat(ui): SegmentedToggle (outlined sliding pill)`.

## Task 4: Wire toggle + per-month rows into InsightsScreen

**Files:** `ui/insights/InsightsScreen.kt`

- [ ] **Step 1: Read the file.** Locate the `"UPCOMING (NEXT 6 MONTHS)"` Text + the forecast `if (state.forecast.isEmpty()) {...} else {...}` block.

- [ ] **Step 2:** Replace the `"UPCOMING (NEXT 6 MONTHS)"` Text with an eyebrow + toggle, and add view state. Just before that section add:

```kotlin
var forecastView by remember { mutableStateOf(0) } // 0 = per cutoff, 1 = per month
```

Then:

```kotlin
Text(
    "UPCOMING",
    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
    color = SnowColors.FrostDim,
)
Spacer(Modifier.height(10.dp))
SegmentedToggle(
    options = listOf("Per cutoff", "Per month"),
    selectedIndex = forecastView,
    onSelect = { forecastView = it },
    modifier = Modifier.fillMaxWidth(),
)
Spacer(Modifier.height(12.dp))
```

- [ ] **Step 3:** Make the list conditional. The existing empty branch (`state.forecast.isEmpty()`) stays. Replace the non-empty `else { Column { state.forecast.forEachIndexed ... ForecastRow } }` with a `when (forecastView)`:

```kotlin
} else {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (forecastView == 0) {
            state.forecast.forEachIndexed { i, f ->
                StaggeredItem(index = i + 1) { ForecastRow(f) }
            }
        } else {
            state.monthForecast.forEachIndexed { i, m ->
                StaggeredItem(index = i + 1) { MonthForecastRow(m) }
            }
        }
    }
}
```

- [ ] **Step 4:** Add the `MonthForecastRow` composable (mirror `ForecastRow`'s visuals; label = month name). Place next to `ForecastRow` in the file:

```kotlin
@Composable
private fun MonthForecastRow(m: com.snowball.domain.MonthForecast) {
    val isShort = !m.isAllClear && m.leftOver < 0
    val borderColor = if (isShort) SnowColors.Ember.copy(alpha = 0.4f) else SnowColors.LineStrong
    val monthLabel = formatMonthYear(LocalDate(m.year, m.month, 1))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SnowColors.NightElev)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(monthLabel, style = MaterialTheme.typography.bodyLarge, color = SnowColors.Frost, modifier = Modifier.weight(1f))
        if (m.isAllClear) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AcUnit, contentDescription = null, tint = SnowColors.Charge, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("All clear", style = MaterialTheme.typography.bodyLarge, color = SnowColors.Charge)
            }
        } else {
            Column(horizontalAlignment = Alignment.End) {
                PesoText(
                    amount = m.dueTotal,
                    style = MaterialTheme.typography.headlineSmall,
                    pesoColor = SnowColors.FrostDim,
                    numberColor = SnowColors.Frost,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (isShort) "SHORT BY ₱${formatAmountWithSeparators(abs(m.leftOver))}"
                    else "₱${formatAmountWithSeparators(m.leftOver)} left",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isShort) SnowColors.Ember else SnowColors.FrostMute,
                )
            }
        }
    }
}
```

Add imports as needed: `com.snowball.ui.components.SegmentedToggle`, `kotlinx.datetime.LocalDate`, `com.snowball.ui.util.formatMonthYear`, `androidx.compose.foundation.layout.fillMaxWidth` (likely present). `formatAmountWithSeparators`, `abs`, `PesoText`, `Icons.Outlined.AcUnit`, `SnowColors.Charge` are already used in the file.

- [ ] **Step 5:** Compile, then run the full suite. Commit: `feat(insights): per-cutoff/per-month toggle on UPCOMING`.

## Task 5: Code review + ship (controller)

- [ ] Run `/code-review` (or a reviewer subagent) on the diff; address findings.
- [ ] `:composeApp:testDebugUnitTest` green; `:composeApp:assembleRelease` (signed) builds.
- [ ] Tag `v0.4.2`, push main + tag, deliver APK.

## Self-review

- **Spec coverage:** aggregateByMonth (T1) + tests; monthForecast on state (T2); SegmentedToggle Option-B outlined (T3); toggle placement + conditional list + MonthForecastRow (T4); review/ship (T5). All spec sections covered. ✓
- **Placeholders:** none. ✓
- **Type consistency:** `MonthForecast(year, month, dueTotal, leftOver, isAllClear)` and `aggregateByMonth(cutoffs, incomePerCutoff)` identical across T1/T2/T4; `SegmentedToggle(options, selectedIndex, onSelect, modifier)` identical T3/T4. ✓
