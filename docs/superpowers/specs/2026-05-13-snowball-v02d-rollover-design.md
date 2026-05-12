# Snowball v0.2 — Sub-project D: Cutoff Rollover

**Date:** 2026-05-13
**Scope:** Surface unpaid debts from prior cutoffs ("Overdue" rollover with red tag), and recompute Home state periodically so the dashboard advances near midnight without an app restart.
**Excluded:** Notifications (E), platform expansion (F).

**Autonomous-design note:** Written without per-section approval per user's overnight authorization.

## Purpose

HANDOFF lists three Behaviors for v0.2 — two are this sub-project (third is already shipped via Debt Detail in v0.2.1):

- **Overdue rollover with red tag** — unpaid debts from a past cutoff carry forward and surface visually.
- **Auto-rollover at midnight** — cutoff dashboard advances automatically at midnight of the cutoff boundary.

## In scope

| Item | Resolution |
|---|---|
| Determine which debts are overdue | New `OverdueCalculator` pure object in `domain/` |
| Surface overdue debts on Home with Ember tag | New "OVERDUE" section above PAYMENTS list |
| Surface overdue debts on Debt Detail screen | Add an "Overdue: N cycles" stat to the detail screen when applicable |
| Mark overdue debts paid (catch-up action) | Tap the row → confirm dialog → records N missed payments |
| Auto-recompute Home periodically | `LaunchedEffect` with `delay(60_000)` loop polling current cutoff; tick state on cutoff change |

## Out of scope

- Per-cycle granularity for overdue ("you missed Mar 5, Apr 5..."). v0.2.3 shows a count + total only.
- Notification when a debt becomes overdue (sub-project E).
- Rollover history view (the Debt Detail payment history already covers individual undo, sufficient for v0.2.3).

## Design

### 1. OverdueCalculator

**File:** `composeApp/src/commonMain/kotlin/com/snowball/domain/OverdueCalculator.kt` *(new)*

```kotlin
package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

data class OverdueInfo(
    val debt: Debt,
    val missedCycles: Int,   // 1+ if overdue
    val missedAmount: Double, // missedCycles * monthlyAmount
    val firstMissedDueDate: LocalDate,
)

object OverdueCalculator {
    /**
     * A debt is overdue if its expected payment count by `today` exceeds the
     * actual payment count. Expected by today = number of monthly cycles whose
     * effective due date falls on or before today, starting from startDate.
     *
     * Only ACTIVE, SCHEDULED debts are considered (archived debts are done;
     * MISC debts have totalPayments=1 and a date-paid; not subject to schedule).
     */
    fun computeOverdue(
        debts: List<Debt>,
        paymentsByDebt: Map<Long, List<Payment>>,
        today: LocalDate,
    ): List<OverdueInfo> {
        return debts.mapNotNull { debt ->
            if (debt.isArchived) return@mapNotNull null
            val expectedSoFar = expectedPaymentsByDate(debt, today)
            val actual = paymentsByDebt[debt.id]?.size ?: 0
            val missed = expectedSoFar - actual
            if (missed <= 0) return@mapNotNull null

            val firstMissed = nthDueDate(debt, actual + 1) ?: return@mapNotNull null
            OverdueInfo(
                debt = debt,
                missedCycles = missed.coerceAtMost(debt.totalPayments - actual),
                missedAmount = missed * debt.monthlyAmount,
                firstMissedDueDate = firstMissed,
            )
        }
    }

    /** How many payment cycles should have been paid by `asOf`, capped at totalPayments. */
    private fun expectedPaymentsByDate(debt: Debt, asOf: LocalDate): Int {
        var count = 0
        for (n in 1..debt.totalPayments) {
            val due = nthDueDate(debt, n) ?: continue
            if (due <= asOf) count++ else break
        }
        return count
    }

    /** Effective due date of the Nth payment (1-indexed). */
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

Returns one entry per overdue debt. Active debts only. Performance: O(debts × totalPayments) worst-case — fine at app scale.

### 2. HomeViewModel + HomeState extension

`HomeState` gains:

```kotlin
val overdue: List<OverdueInfo>,
```

`HomeViewModel.load()` calls `OverdueCalculator.computeOverdue(debts, paymentsByDebt, today)`.

A new `catchUpOverdue(info: OverdueInfo, today: LocalDate)` VM method records `info.missedCycles` payments backdated to `info.firstMissedDueDate`, `info.firstMissedDueDate + 1 month`, etc. (Simplification: record them all at today's date but with the missed amount per cycle. Backdate is "nice to have"; today-date is acceptable.)

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

### 3. HomeScreen UI

Between the `CutoffCard` + `UpNextCard` block and the existing PAYMENTS section, render an "OVERDUE" block conditional on `state.overdue.isNotEmpty()`:

```
┌──────────────────────────────────────┐
│  OVERDUE                             │   ← labelSmall Ember, 4sp tracking
│  Tap to mark caught up               │   ← bodySmall italic FrostMute
│  ─── divider ───                     │
│  ⚠ Sloan         2 cycles    ₱3,000  │   ← Ember Row, tap → catch-up dialog
│  ⚠ BillEase      1 cycle     ₱1,500  │
└──────────────────────────────────────┘
```

Each overdue row: 24dp Ember warning icon (`Icons.Outlined.WarningAmber`), debt name (Frost), missedCycles label ("1 cycle" / "N cycles"), PesoText with Ember number color. Tap any row → AlertDialog confirms catch-up.

Catch-up dialog:
- Title: "Catch up on {debt.name}?"
- Body: "Records {N} missed payment(s) totaling ₱{missedAmount}. Each missed cycle starts from {firstMissedDueDate}."
- Buttons: Cancel / Catch up (Ember)

On confirm: `vm.catchUpOverdue(info)` then `tick++`.

### 4. Periodic auto-recompute on Home

In `HomeScreen` body, add:

```kotlin
LaunchedEffect(Unit) {
    while (true) {
        kotlinx.coroutines.delay(60_000)
        tick++
    }
}
```

Triggers a re-fetch of `vm.load()` every minute while Home is composed. When the device clock crosses the cutoff boundary, the next tick picks up the new `currentCutoff(today())`. Sufficient for the user's expected use ("I open the app the next morning and the cutoff has advanced").

A 60-second poll is gentle (zero network, in-memory recompute). Stops automatically when the user navigates away from Home (LaunchedEffect cancels on composable removal).

### 5. DebtDetailScreen update

When the debt is active and `OverdueCalculator.computeOverdue(listOf(debt), payments, today)` returns a non-empty result, add an additional stat row after PROJECTED END:

```
OVERDUE         2 cycles · ₱3,000
```

In Ember. Otherwise omit.

This requires `DebtDetailViewModel` to compute its own overdue info (single debt). Same calculator, just called with a single-element list.

## File-level change inventory

**New files (1):**
- `composeApp/src/commonMain/kotlin/com/snowball/domain/OverdueCalculator.kt`
- `composeApp/src/commonTest/kotlin/com/snowball/domain/OverdueCalculatorTest.kt`

*(That's 2 new files. Count corrected.)*

**Modified files (4):**
- `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeViewModel.kt` — add `overdue` to state, add `catchUpOverdue`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt` — OVERDUE block + LaunchedEffect
- `composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailViewModel.kt` — add `overdueInfo` to state
- `composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailScreen.kt` — render OVERDUE stat

## Tests

- `OverdueCalculatorTest` (commonTest): 5-6 cases — debt up to date (returns empty), one missed cycle, multiple missed cycles, archived debts skipped, edge cases (start date in future, debt with totalPayments exhausted).

## Risks

- **`expectedPaymentsByDate` capped at totalPayments**: A debt at month N with totalPayments = N expects exactly N payments. If user is `N+1` months in, the expected count is N (not N+1) — the debt should auto-archive at payment N, so `missed = N - N = 0`. Verified by the loop bound.
- **Catch-up records today's date as paidDate** not the missed dates. The journey calc and cutoff math don't care about exact paidDate beyond cycle assignment (covered by `priorCycleDueDate` logic in CutoffCalculator). Acceptable simplification.
- **`LaunchedEffect(Unit) { while (true) ... }` runs forever while Home is composed.** Cancels automatically on navigation; OK.
- **Time zone** — `today()` returns the device's local date. If the user changes timezone, the cutoff may shift mid-session — out of scope.

## Success criteria

- Add a debt with `startDate = 6 months ago`, `totalPayments = 12`, no payments → Home shows OVERDUE section with "6 cycles · ₱X".
- Tap the row → catch-up dialog → confirm → 6 payments recorded; OVERDUE section disappears; Journey card updates; debt subtitle shows "6/12 paid" on Debts.
- Open Home, leave it open across midnight (or change device date manually) → within ~60s of the cutoff boundary, the cutoff card label updates to the new window.
- DebtDetail of an overdue debt shows the OVERDUE stat.
- Tag `v0.2.3` pushed.
