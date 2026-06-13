# Insights — Per-Cutoff / Per-Month View Toggle

**Date:** 2026-06-01
**Status:** Approved (Option B; autonomous execution)
**Target tag:** v0.4.2

## Goal

Add a "slide bar" segmented toggle to the Insights screen that switches the **UPCOMING** forecast between **Per cutoff** (each payday window, the current behavior) and **Per month** (the two paydays of a calendar month combined into one row). Toggle visual = **Option B**: a sliding pill with an *outlined* thumb (Ice border, transparent fill), active label Ice, inactive FrostDim.

## Scope

- Affects the **UPCOMING forecast list only.** The Snapshot card (already monthly) and the Payoff Timeline (per-debt) are untouched.
- Default view = **Per cutoff** (no behavior change until the user toggles).
- Toggle state is in-memory UI state (resets to Per cutoff when re-entering Insights). Persistence is out of scope.

## Data — per-month aggregation (pure, testable)

The forecast already computes `List<CutoffForecast>` (12 per-cutoff entries) via `InsightsCalculator.forecastCutoffs(...)`. Add a pure aggregation over that list:

```kotlin
data class MonthForecast(
    val year: Int,
    val month: Int,           // 1..12
    val dueTotal: Double,
    val leftOver: Double,
    val isAllClear: Boolean,
)

/** Groups per-cutoff forecasts into calendar months. Income for a month scales with
 *  the number of that month's paydays present in the window (1 for a partial leading
 *  month, 2 for a full month), so leftOver stays accurate. */
fun aggregateByMonth(cutoffs: List<CutoffForecast>, incomePerCutoff: Double): List<MonthForecast>
```

Rules:
- Group by `(cutoff.year, cutoff.month)` — the FIFTEENTH and THIRTIETH cutoffs of the same calendar month share `month`, so they merge.
- `dueTotal` = sum of the group's `dueTotal`.
- `leftOver` = `incomePerCutoff * group.size - dueTotal` (group.size is 1 or 2).
- `isAllClear` = every cutoff in the group is all-clear.
- Result sorted ascending by `(year, month)`.

Lives in `InsightsCalculator.kt`.

## ViewModel

`InsightsState` gains `monthForecast: List<MonthForecast>`, computed in `load()` as `aggregateByMonth(forecast, incomePerCutoff)`. `forecast` (the existing per-cutoff list) stays.

## UI

### `SegmentedToggle` (new reusable component)

`composeApp/src/commonMain/kotlin/com/snowball/ui/components/SegmentedToggle.kt`:
- A rounded-full track (`Night` fill, 1dp `LineStrong` border, ~44dp tall, 4dp inset).
- An **outlined** thumb (1.5dp `Ice` border, transparent fill, rounded-full) sized to one segment, positioned by `animateFloatAsState` on the selected index (slides, ~280ms FastOutSlowInEasing).
- Two equal-weight clickable segments; active label `Ice`, inactive `FrostDim` (color animated). Uses `BoxWithConstraints` to size the thumb to `maxWidth / options.size`.
- API: `SegmentedToggle(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier)`.

### InsightsScreen

In the forecast section (currently the `"UPCOMING (NEXT 6 MONTHS)"` heading + list):
- Keep a small `"UPCOMING"` eyebrow label, then place the `SegmentedToggle(listOf("Per cutoff", "Per month"), ...)` directly beneath it, then the list.
- Hold `var forecastView by remember { mutableStateOf(0) }` (0 = cutoff, 1 = month).
- When `0`: render the existing `ForecastRow` per `state.forecast` (unchanged).
- When `1`: render a `MonthForecastRow` per `state.monthForecast` — same visual language as `ForecastRow` (rounded card, NightElev, short/leftover coloring), but the left label is the month name via `formatMonthYear(LocalDate(year, month, 1))`, and dueTotal/leftOver/allClear render exactly like ForecastRow.
- Empty-state (no forecast) handling stays as-is for both views.
- Wrap rows in `StaggeredItem` as today.

## Testing

Unit tests for `aggregateByMonth` in `InsightsCalculatorTest`:
1. Two same-month cutoffs merge → one row, `dueTotal` summed, `leftOver = income*2 - due`.
2. Partial leading month (single cutoff) → `leftOver = income*1 - due`.
3. `isAllClear` true only when all cutoffs in the group are clear; false if any has due.
4. Output sorted by `(year, month)`; a Dec→Jan span orders correctly across the year boundary.
5. Empty input → empty output.

These cover the bug-prone logic. The toggle/rows are presentational; verified by build + on-device (no Compose UI-test harness in this repo).

## Acceptance

- Insights shows the outlined sliding-pill toggle above UPCOMING; default Per cutoff (current behavior preserved).
- Toggling to Per month collapses the list to one row per calendar month with correct summed due and accurate left-over; the thumb slides.
- All unit tests green (including new `aggregateByMonth` tests).
- Code-reviewed; APK builds and installs.

## Tag

v0.4.2.
