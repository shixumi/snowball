# Snowball v0.2 — Sub-project A: Home (Up next + Journey)

**Date:** 2026-05-13
**Scope:** Two new cards on `HomeScreen` — "Up next" (preview of next cutoff's payments) and "Your journey" (cumulative progress stats). Pure UI additions plus two new domain helpers. No schema, no data-layer changes, no new dependencies.
**Excluded (other v0.2 sub-projects):** Debt Detail screen, Archive view, MISC items UI, Category management, cutoff rollover behaviors, notifications, platform expansion. Each will get its own brainstorm/spec/plan cycle.

## Purpose

Resolves the v0.1 blocker that was deferred to v0.2:

> A debt added with `startDate = today` doesn't appear in the current cutoff's payment list. The behavior is correct (`CutoffCalculator` drops debts whose `startDate > payDate`), but invisible — users add a debt and assume the app is broken.

The Up next card surfaces those queued-for-next-cutoff debts directly on Home, with an inline expandable list so the user gets the "oh THERE'S my Sloan" moment without leaving the screen.

The Journey card supplies a motivational reading of cumulative progress (% cleared, total melted, debt-free forecast date). All values derive from existing data; no extra inputs from the user.

## In scope

| Item | Resolution |
|---|---|
| Up next card on Home (placement, collapsed state, expandable list) | New `UpNextCard.kt` component |
| `nextCutoff(today)` domain helper | Added to `Cutoff.kt` |
| `HomeState` carries next-cutoff data | Extend `HomeViewModel.HomeState` |
| Journey card on Home (visibility rule, three stats) | New `JourneyCard.kt` component |
| Journey stat computation (% cleared, total melted, forecast end date) | New `JourneyCalculator.kt` pure object |
| HomeScreen layout: cutoff → up next → payments → journey | Inline edits in `HomeScreen.kt` |
| Unit tests for `nextCutoff` and `JourneyCalculator` | Two new `commonTest` files |

## Out of scope (deferred to later v0.2 sub-projects)

- **Debt Detail screen** (sub-project B) — tapping a row in the Up next expanded list does NOT navigate anywhere in this sub-project. The list is read-only.
- **Archive view, MISC items UI, Category management** — separate sub-projects.
- **Overdue rollover with red tag, auto-rollover at midnight** — sub-project D.
- **Notifications, iOS/macOS/Windows targets** — sub-projects E and F.

## Design — section by section

### 1. Up next card

**Placement.** In `HomeScreen.kt`, immediately after the existing `CutoffCard` and before the `PAYMENTS` section header. Same horizontal padding (the outer `Column` already applies 20dp). 24dp top margin (Spacer) from the cutoff card; 24dp bottom margin to the PAYMENTS label.

**Visual.** RoundedCornerShape(28dp), background `SnowColors.NightElev` (solid; the cutoff card uses a gradient so this reads as a quieter sibling), 1dp `LineStrong` border. Internal padding `horizontal = 24.dp, vertical = 16.dp`.

**Collapsed state.**

```
┌──────────────────────────────────────────┐
│  UP NEXT · May 15 → 30                   │   <- labelSmall 4sp tracking, FrostDim
│  ₱4,200 · 3 debts                ˅       │   <- bodyLarge Frost + chevron-down FrostMute
└──────────────────────────────────────────┘
```

- Header row: `"UP NEXT · ${rangeLabel(nextCutoff)}"` using the existing `cutoffRangeLabel(c: Cutoff)` helper from `CutoffCard.kt` (export it to package level if needed, or reuse via a shared util).
- Spacer 8dp.
- Content row: `Row(verticalAlignment = CenterVertically) { PesoText(...); Spacer(weight=1f); Icon(Icons.Outlined.ExpandMore, ...) }`. Total amount rendered via existing `PesoText` at `headlineMedium`; the `· 3 debts` text after as `bodyMedium FrostMute`. Chevron 20dp, FrostMute tint.

**Expanded state.**

```
┌──────────────────────────────────────────┐
│  UP NEXT · May 15 → 30                   │
│  ₱4,200 · 3 debts                ˄       │
│  ─────────────────────────────────────   │   <- 1dp Line divider, 12dp vertical margin
│  Sloan        Due 5/20         ₱1,500    │   <- compact rows; bodyMedium Frost
│  Globe        Due 5/25         ₱900      │
│  Pag-IBIG     Due 5/30         ₱1,800    │
└──────────────────────────────────────────┘
```

- Each row: `Row(verticalAlignment = CenterVertically) { Text(debt.name, weight=1f, maxLines=1, ellipsis); Text("Due ${dueDay}", FrostMute, bodySmall); Spacer(width=12dp); PesoText(amount, headlineSmall) }`.
- Row vertical padding 8dp. 1dp Line divider between rows.
- No swipe gestures. No progress arc. These rows are not actionable in v0.2 sub-project A — they're a read-only preview.

**Animation.** Wrap the expanded section in `AnimatedVisibility(visible = isExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut())`. Default `tween(200ms)`. No bounce, no overshoot — matches the app's restrained animation vocabulary.

**Interaction.** The whole card surface is `Modifier.clickable { onToggle() }`. No separate tappable region for the chevron. Haptic feedback on toggle (`HapticFeedbackType.LongPress` for consistency with the swipe gesture). State held in `HomeScreen` as `var upNextExpanded by remember { mutableStateOf(false) }` — resets to collapsed each time the user navigates away from Home and back.

**Visibility rule.** Card renders only when `state.nextRows.isNotEmpty()`. If no debts fall in next cutoff (or there are no debts at all), the slot is empty (no Spacer either — the existing 24dp before PAYMENTS does the job).

**Accessibility.**
- Whole card `Modifier.semantics(mergeDescendants = true) { contentDescription = "Up next, ${rangeLabel}, ${total} pesos, ${count} debts"; role = Role.Button; stateDescription = if (isExpanded) "Expanded" else "Collapsed" }`.
- Each expanded debt row gets its own `semantics { contentDescription = "${debt.name}, ${amount} pesos, due day ${dueDay}" }`.
- Chevron icon `contentDescription = null` (semantics rolled up to card).

### 2. Journey card

**Placement.** Below the PAYMENTS list, after the trailing `Spacer(Modifier.height(16.dp))` added in v0.1.1 Task 12. Add a 24dp top spacer to separate it from the last payment row. Card is the last item in the outer Column; the existing bottom-nav inset handling already covers safe-area concerns.

**Visual.** Same RoundedCornerShape(28dp), but background `SnowColors.CardElev` (slightly more elevated than NightElev — establishes it as a "rest" card rather than a "queue" card like Up next). 1dp `LineStrong` border. Internal padding `horizontal = 24.dp, vertical = 24.dp`.

**Layout — hero + subtitle pattern.**

```
┌──────────────────────────────────────────┐
│  YOUR JOURNEY                            │   <- labelSmall 4sp tracking, FrostDim
│                                          │
│  18%                                     │   <- displayMedium Fraunces W300, Ice
│  cleared                                 │   <- labelMedium DM Sans FrostMute
│                                          │
│  ₱8,500 melted  ·  Free by Aug 2027      │   <- bodyMedium FrostMute, middle dot separator
└──────────────────────────────────────────┘
```

- Header: `Text("YOUR JOURNEY", style = labelSmall.copy(letterSpacing = 4.sp), color = FrostDim)`.
- Spacer 12dp.
- Hero number: `Text("${stats.percentCleared}%", style = displayMedium.copy(fontWeight = FontWeight.W300), color = Ice)`.
- Spacer 2dp.
- Label: `Text("cleared", style = labelMedium, color = FrostMute)`.
- Spacer 16dp.
- Subtitle row: a Row with two Text composables and a `Text(" · ", FrostMute)` between them. Each Text uses `bodyMedium` FrostMute.
  - Left: `"₱${formatAmountWithSeparators(stats.totalMelted)} melted"`.
  - Right: `if (stats.forecastEndDate == null) "All clear ✓" else "Free by ${formatMonthYear(date)}"`.

**Format helpers.**

- `formatAmountWithSeparators(d: Double): String` — already exists as a private function inside `PesoText.kt`. For this sub-project, lift it to a top-level function in `composeApp/src/commonMain/kotlin/com/snowball/ui/util/AmountFormat.kt` (alongside the existing `toFormFieldString` from v0.1.1 Task 1). Both `PesoText` and `JourneyCard` then import it. This also matches the v0.1.1 final review's "Recommendations" note about unifying number formatters.
- `formatMonthYear(d: LocalDate): String` — new helper in `composeApp/src/commonMain/kotlin/com/snowball/ui/util/DateFormat.kt`. Uses the same month abbreviations already defined in `CutoffCard.kt:cutoffRangeLabel` (`"Jan", "Feb", ...`). Returns `"${months[d.monthNumber - 1]} ${d.year}"`. Three lines, one helper, fully testable.

**Visibility rule.** Card renders only when `state.journey != null`. The `JourneyCalculator.compute(...)` returns `null` when `totalMelted == 0.0`. So fresh installs and users with debts but no payments yet see no card. First swipe-to-paid causes the card to appear.

**Accessibility.**
- Outer Column `semantics(mergeDescendants = true) { contentDescription = "Your journey, ${percent} percent cleared, ${melted} pesos melted, ${forecastText}" }`.
- "All clear ✓" — the check glyph is decorative; the `mergeDescendants` description already says "all clear".

### 3. Supporting domain logic

#### 3.1 `nextCutoff(today)`

Added to `composeApp/src/commonMain/kotlin/com/snowball/domain/Cutoff.kt` (top-level function, parallel to existing `currentCutoff`):

```kotlin
fun nextCutoff(today: LocalDate = today()): Cutoff {
    val current = currentCutoff(today)
    val nextStart = current.windowEnd.plus(1, DateTimeUnit.DAY)
    return currentCutoff(nextStart)
}
```

Uses recursion through the existing builder: `currentCutoff(d)` always returns the half-month window containing `d`. Advancing one day past the current window's end lands us in the next window; calling `currentCutoff` on that date returns it. Simpler than re-implementing the half-month math, and guaranteed to stay in sync with the canonical cutoff rules.

#### 3.2 `JourneyCalculator`

New file `composeApp/src/commonMain/kotlin/com/snowball/domain/JourneyCalculator.kt`:

```kotlin
package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

data class JourneyStats(
    val percentCleared: Int,           // 0..100, integer
    val totalMelted: Double,           // sum of all Payment.amount
    val forecastEndDate: LocalDate?,   // null when no active debts → "All clear ✓"
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
        val lastDay = lastDayOfMonth(endMonth.year, endMonth.monthNumber)
        val day = if (debt.useLastDayOfMonth) lastDay
                  else debt.dueDay.coerceAtMost(lastDay)
        return LocalDate(endMonth.year, endMonth.monthNumber, day)
    }
}
```

`lastDayOfMonth(year, month)` already lives in `composeApp/src/commonMain/kotlin/com/snowball/domain/DateUtils.kt` and is used by the existing cutoff logic. Reuse it directly.

#### 3.3 `HomeViewModel` extensions

Extend `HomeState` in `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeViewModel.kt`:

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
)
```

Extend `HomeViewModel.load()`:

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

    return HomeState(cutoff, rows, summary, income, next, nextRows, nextTotal, journey)
}
```

Two new repository reads (`repos.debts.all()` and the `flatMap`), both already-existing methods on existing repositories. No new SQL.

### 4. HomeScreen layout edits

Diff vs current `HomeScreen.kt`:

1. Add imports: `androidx.compose.animation.AnimatedVisibility`, `androidx.compose.animation.expandVertically`, `androidx.compose.animation.shrinkVertically`, `androidx.compose.animation.fadeIn`, `androidx.compose.animation.fadeOut`, `com.snowball.ui.components.UpNextCard`, `com.snowball.ui.components.JourneyCard`.

2. Inside the outer Column, after the `CutoffCard(...)` call:

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
Text("PAYMENTS", ...)   // existing
```

3. After the existing `state.rows.forEach { ... }` block + trailing `Spacer(16.dp)` from v0.1.1, render the journey card when present:

```kotlin
state.rows.forEach { row -> ... }
Spacer(Modifier.height(16.dp))   // existing

if (state.journey != null) {
    Spacer(Modifier.height(24.dp))
    JourneyCard(stats = state.journey)
}
```

Both inserts are guarded by `null`/`isEmpty()` checks — no layout shift when the user has no next-cutoff debts or no payments yet.

### 5. New utility files

#### 5.1 `AmountFormat.kt` lift

The v0.1.1 final review noted that `PesoText`'s private `formatAmount` and `SettingsScreen`'s `toFormattedPeso` duplicate logic. This sub-project unifies them.

Edit `composeApp/src/commonMain/kotlin/com/snowball/ui/util/AmountFormat.kt` (already exists from v0.1.1 Task 1) to add a public function:

```kotlin
/**
 * Renders a Double as a peso-formatted amount with thousand separators and 2 decimals,
 * with round-half-up at the hundredths place (handles IEEE 754 representation errors).
 * Does NOT include the ₱ prefix — caller adds it where needed.
 */
fun formatAmountWithSeparators(amount: Double): String {
    val whole = amount.toLong()
    val fraction = ((kotlin.math.abs(amount) - kotlin.math.abs(whole)) * 100 + 0.5).toInt()
    val grouped = kotlin.math.abs(whole).toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
    val signedGrouped = if (whole < 0) "-$grouped" else grouped
    return if (fraction == 0) signedGrouped else "$signedGrouped.${fraction.toString().padStart(2, '0')}"
}
```

Migrate:
- `PesoText.kt`'s private `formatAmount` → call `formatAmountWithSeparators` instead. Delete the private helper.
- `SettingsScreen.kt`'s private `toFormattedPeso` → keep the `₱` prefix logic, but inner call becomes `formatAmountWithSeparators(this)`.

This cleans up the v0.1.1 final review's Minor issue #3 (PesoText vs SettingsScreen rounding inconsistency) as a side effect.

#### 5.2 `DateFormat.kt`

New file `composeApp/src/commonMain/kotlin/com/snowball/ui/util/DateFormat.kt`:

```kotlin
package com.snowball.ui.util

import kotlinx.datetime.LocalDate

private val MONTHS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

fun formatMonthYear(date: LocalDate): String =
    "${MONTHS[date.monthNumber - 1]} ${date.year}"
```

`CutoffCard.kt:cutoffRangeLabel` already has its own copy of the `MONTHS` array. Out of scope for this sub-project (we'd touch existing tested code for no functional gain) — leave the duplication and address it in a future cleanup pass if it becomes painful. Documented as a known minor duplication.

## File-level change inventory

**New files (6):**
- `composeApp/src/commonMain/kotlin/com/snowball/ui/components/UpNextCard.kt`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/components/JourneyCard.kt`
- `composeApp/src/commonMain/kotlin/com/snowball/domain/JourneyCalculator.kt`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/util/DateFormat.kt`
- `composeApp/src/commonTest/kotlin/com/snowball/domain/NextCutoffTest.kt`
- `composeApp/src/commonTest/kotlin/com/snowball/domain/JourneyCalculatorTest.kt`

**Modified files (6):**
- `composeApp/src/commonMain/kotlin/com/snowball/domain/Cutoff.kt` — add `nextCutoff(today)` function
- `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeViewModel.kt` — extend `HomeState`, extend `load()`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt` — render Up next + Journey cards conditionally
- `composeApp/src/commonMain/kotlin/com/snowball/ui/util/AmountFormat.kt` — add `formatAmountWithSeparators`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/components/PesoText.kt` — use shared formatter
- `composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsScreen.kt` — use shared formatter

**Untouched (intentionally):**
- All data-layer code (`data/`, `db/`)
- `CutoffCalculator.kt` — its existing `computeDueRows` and `summarize` work for both cutoff windows; reused as-is
- `DebtFormScreen.kt`, `DebtsScreen.kt`, `SettingsViewModel.kt`, `Nav.kt`, etc.

## Testing strategy

**Unit tests (commonTest):**
- `NextCutoffTest.kt` — 4–6 tests covering:
  - Today in first half of month (1–14) → next is second half (15–EOM)
  - Today in second half (15+) → next is first half of next month
  - Today = May 31 → next is June 1–14
  - Year boundary: today Dec 25 → next is Jan 1–14 of next year
  - Edge: today = May 14 (last day of first cutoff) → next is May 15–30
  - Edge: today = May 15 (first day of second cutoff) → next is June 1–14
- `JourneyCalculatorTest.kt` — 7–10 tests covering:
  - Empty state (no debts, no payments) → null
  - Debt exists but no payments → null (totalMelted is 0)
  - One debt with one payment → percentCleared computed correctly, forecast = startDate + (total-1) months at dueDay
  - 100% paid + archived → percentCleared = 100, forecast = null (all clear)
  - Multi-active, picks the latest end date
  - useLastDayOfMonth = true with February start → end date clamps correctly
  - dueDay = 31 in a month with 30 days → clamps to last day
  - percentCleared bounded 0..100 even when totalMelted > scheduled (paranoia case)

**Manual verification on emulator:**
- Fresh install: Home shows only the cutoff card (no Up next, no Journey).
- Add a debt with `startDate = today` and `dueDay` past the current cutoff's payDate → Up next card appears under cutoff card with `1 debts`. Tap card → expands to show that debt.
- Add 2 more debts queued for next cutoff → Up next total updates, expanded list shows all 3.
- Swipe a current-cutoff debt paid → Journey card appears below payments with `X% cleared, ₱Y melted`.
- Swipe undo on that payment → Journey card disappears (totalMelted back to 0).
- Verify `font_scale = 1.30` — both new cards stay legible, no clipping, last card has the existing 16dp bottom inset.

No new repository or domain integration tests beyond the two new unit files. `HomeViewModel.load()` is mechanical wiring and is exercised by the manual flow above.

## Risks

- **Re-render frequency.** `HomeViewModel.load()` now does two `repos.debts.all()` reads and a `flatMap` over payments per `tick++`. At realistic scale (10s of debts, 100s of payments) this is sub-millisecond. If a user accumulates years of payments, the journey card stats could become expensive — at that point, cache `totalMelted` in `SettingsRepository` or a separate stats table. Not v0.2 work.
- **Forecast accuracy.** `projectedEndDate` assumes on-time future payments. If the user falls behind, the forecast doesn't extend. For v0.2 sub-project A, this is fine: the spec says "all derived from existing data, no extra inputs." A more sophisticated forecast (factor in payment rate trend) is parked further out.
- **Up next list overlap with current cutoff.** A debt whose `startDate ≤ currentCutoff.payDate` AND falls in the next cutoff window appears in both the current cutoff (today's PAYMENTS list) and next cutoff (Up next list). This is correct — the same debt has two scheduled payments in the next 4 weeks. UI shows it in both places. Worth flagging in the plan so the implementer doesn't try to de-dupe.

## Success criteria

- v0.1 silent-debt-drop blocker is no longer silent: a debt with `startDate = today` is now discoverable on Home via Up next within 2 taps (tap card → see name).
- Journey card materializes the moment the user records their first payment, with accurate `%`, `melted`, and `forecast` values.
- All in-scope unit tests pass (`NextCutoffTest`, `JourneyCalculatorTest`).
- Existing 52 tests from v0.1.1 still pass.
- Build (debug + release) succeeds.
- Manual emulator walkthrough confirms layout at `font_scale = 1.0` and `1.30`.
- Card visibility rules behave correctly: Up next hidden when next is empty; Journey hidden when totalMelted = 0.
