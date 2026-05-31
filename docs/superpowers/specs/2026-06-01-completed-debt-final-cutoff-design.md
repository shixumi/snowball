# Keep a Completed Debt in Its Final Cutoff — Design

**Date:** 2026-06-01
**Status:** Approved (inline)
**Scope:** Home current + next cutoff display

## Problem

When a debt's last payment is recorded, `markPaid` auto-archives it. `HomeViewModel.load()` builds cutoff rows from `repos.debts.allActive()`, so the moment the debt archives it leaves that list: the row disappears from the current cutoff and the cutoff's `dueTotal`/`paidTotal` silently shrink. The cutoff then misrepresents the period — it acts as if the debt was never due, even though it was due and paid this cutoff.

## Desired behavior

A debt that is fully paid (and therefore archived) remains visible as a **paid row** in the single cutoff whose window contains its final scheduled due date (`projectedEndDate`). It counts toward that cutoff's due/paid totals. Once that cutoff rolls over it disappears, and it never appears in "Up Next" or any other cutoff. Swipe-to-undo continues to work: undo deletes the latest payment and un-archives the debt (existing `undoPayment` behavior), returning it to active/unpaid.

The "fully paid" condition (payment count ≥ total payments) distinguishes a *completed* debt from one a user archived manually mid-way, which should stay hidden.

## Approach (chosen: "only its final cutoff")

### Pure predicate (domain, testable)

Add a top-level function in `com.snowball.domain` (in `CutoffCalculator.kt`):

```kotlin
fun completedDebtDueInCutoff(debt: Debt, paymentCount: Int, cutoff: Cutoff): Boolean {
    if (!debt.isArchived) return false
    if (paymentCount < debt.totalPayments) return false
    val end = projectedEndDate(debt) ?: return false
    return end >= cutoff.windowStart && end <= cutoff.windowEnd
}
```

`projectedEndDate` (already in `JourneyCalculator.kt`) yields the final cycle's effective due date.

### Wiring (HomeViewModel)

`load()` builds each cutoff's debt list as `allActive()` plus archived debts satisfying the predicate for *that* cutoff's window, computed independently for the current and next cutoff. Because a debt completed this cutoff has its `projectedEndDate` in the current window (not next), it will not leak into "Up Next". The payments map is extended to cover the displayed debts.

`OverdueCalculator` keeps operating on the active set (a completed debt is never overdue). `JourneyCalculator` is unchanged (already reads all debts). `CutoffCalculator.computeDueRows` is unchanged — it simply receives a slightly larger list; the final-cycle row computes `isPaidThisCycle = true` because the payment exists in-window.

## Out of scope

- Insights forecast (future cutoffs over active debts) — does not exhibit the issue.
- Manually-archived incomplete debts — intentionally stay hidden (the payment-count gate).
- Paid-ahead debts (completed before schedule) — naturally show in the future cutoff their `projectedEndDate` lands in, consistent with the rule.

## Testing

Unit-test the pure predicate `completedDebtDueInCutoff`:
- completed debt whose `projectedEndDate` is in the cutoff window → true
- same debt, a different cutoff window → false
- active (non-archived) debt → false
- archived but not fully paid (manual archive) → false

HomeViewModel wiring is a thin filter over the tested predicate; verified on-device/emulator.

## Acceptance

- Marking the final payment of a debt leaves it visible as a paid row in the current cutoff; the cutoff's due/paid totals are unchanged by the archive.
- After the cutoff rolls over, the completed debt no longer appears.
- The completed debt never appears in "Up Next".
- Undo on the completed row restores it to active/unpaid.
- Full unit suite stays green.
