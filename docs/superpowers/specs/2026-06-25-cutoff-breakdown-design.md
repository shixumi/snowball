# Per-Cutoff Breakdown (tap to expand) — Design

**Date:** 2026-06-25
**Status:** Approved (full-auto build)

## Goal

In Insights → **Per cutoff**, let the user tap a cutoff row to expand an inline
breakdown of exactly what they'll pay that cutoff: each debt due, when, and how
much. Answers "what will I pay that time?" without leaving the screen.

## Scope

- **Per-cutoff rows only.** Per-month rows stay summary-only (a documented
  future extension).
- Read-only — no actions in the breakdown.
- Shared `commonMain` + Compose; no platform code, so it builds on iOS as-is.

## Key enabler

`InsightsCalculator.forecastCutoffs` already computes each cutoff's per-debt due
rows via `CutoffCalculator.computeDueRows` (returns `List<DueRow>` —
`{debt, effectiveDueDate, amount, isPaidThisCycle}`, sorted by due date) but
discards them, keeping only `dueTotal`. The feature **retains** those rows.

## Approach (chosen)

**A — retain rows on `CutoffForecast`.** Zero extra computation, single source
of truth, unit-testable.

Rejected:
- **B — recompute on tap.** The forecast simulates sequential on-time payments
  cutoff-by-cutoff (virtual payments), so re-deriving one cutoff in isolation
  means replaying all prior cutoffs. Fragile, more code, no benefit.
- **C — parallel `Cutoff → rows` map in the VM.** Redundant with A.

## Changes

### Domain — `InsightsCalculator.kt`
- `CutoffForecast` gains `val rows: List<DueRow>`.
- In `forecastCutoffs`, the `rows` already computed for `dueTotal` are passed
  into the `CutoffForecast`. (An all-clear cutoff has `rows = emptyList()`.)

### ViewModel — `InsightsViewModel.kt`
- `InsightsState` gains `val categoriesById: Map<Long, Category>`, populated from
  the `catById` the VM already builds, so the UI can resolve each row's category
  icon. (`forecast` now carries `rows` automatically.)

### Util — `ui/util/DateFormat.kt`
- Add `fun ordinalDay(date: LocalDate): String` → "1st", "2nd", "3rd", "4th",
  … "11th", "21st", "30th". Pure, multiplatform.

### UI — `InsightsScreen.kt` (per-cutoff branch only)
- Replace the static `ForecastSummaryRow` for per-cutoff items with an
  **expandable** variant:
  - Collapsed: existing summary (range label · `Due` PesoText · `Left`) plus a
    rotating chevron — but **only when the cutoff has rows**. All-clear cutoffs
    render exactly as today (no chevron, not clickable).
  - Tap toggles expansion; **accordion — one open at a time** (`expandedIndex:
    Int?`), with a haptic tick (matches Payoff Timeline).
  - Expanded: a divider, then for each `DueRow` (already sorted by due date): the
    category icon (`categoriesById[row.debt.categoryId]?.icon()`), the debt name,
    `"due ${ordinalDay(row.effectiveDueDate)}"`, and the amount
    (`₱${formatAmountWithSeparators(row.amount)}`).
- Per-month branch unchanged (still `ForecastSummaryRow`).

## Edge cases
- All-clear cutoff (`rows` empty): not expandable, no chevron.
- A 30th cutoff's due date can land in the next month; "due 14th" is unambiguous
  because the row header shows the date range.
- Switching the Per cutoff / Per month toggle collapses any open row
  (`expandedIndex` resets when the toggle changes / lives under the per-cutoff
  branch).

## Testing
- **`InsightsCalculatorTest`** (commonTest): assert `CutoffForecast.rows` for a
  forecast cutoff contains the expected debts/amounts and omits debts not yet
  started or fully paid; an all-clear cutoff has empty `rows`.
- **`DateFormatTest`** (commonTest): `ordinalDay` for 1/2/3/4/11/12/13/21/22/23/30.
- UI verified on device.

## Out of scope (follow-ups)
- Per-month row expansion (merging the month's two cutoffs).
- Tapping a breakdown line to open that debt's detail.
