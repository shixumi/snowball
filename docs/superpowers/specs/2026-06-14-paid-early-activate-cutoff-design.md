# "Got Paid Early" — Activate the Next Cutoff Ahead of the Calendar

**Date:** 2026-06-14
**Status:** Approved (autonomous execution)
**Target tag:** v0.4.3

## Problem

Marking a payment paid only exists on Home for the **current** cutoff, and "current" is derived purely from today's date (`currentCutoff(today)`). "Up Next" is a read-only preview, and Debt Detail has no add-payment path. So when payday lands early (the 15th falls on a weekend, payroll runs early, etc.), the bills the user wants to pay sit in the *next* cutoff — visible but un-markable.

## Solution

Let the user **activate the next cutoff early**. When activated, that cutoff becomes Home's active cutoff (hero card + swipeable PAYMENTS list, markable normally); Up Next shifts to preview the cutoff after it. The user never marks a "preview" — they flip the cutoff to live. It auto-clears once the calendar catches up.

Scope is **Home only**: Overdue, Insights, and the cutoff math are untouched.

## Domain (pure, testable) — `Cutoff.kt`

```kotlin
data class EffectiveCutoff(
    val cutoff: Cutoff,
    val isActivatedEarly: Boolean,
    val clearOverride: Boolean,   // tells the caller to drop a now-stale override
)

/** The cutoff Home should treat as current. An override is honored only while it is
 *  still ahead of the calendar-derived current cutoff; once today reaches/passes it
 *  (or it is stale/behind), fall back to the date-current and signal clearOverride. */
fun resolveEffectiveCutoff(today: LocalDate, override: Cutoff?): EffectiveCutoff
```

Rules:
- `override == null` → `(currentCutoff(today), false, false)`.
- `override.payDate > currentCutoff(today).payDate` → `(override, true, false)` (still ahead → activated early).
- otherwise → `(currentCutoff(today), false, true)` (caught up or stale → use date-current, clear).

Persistence of the override is a stored cutoff key, with a round-trippable encoding (also in `Cutoff.kt`):
```kotlin
fun Cutoff.storageKey(): String          // "2026-6-FIFTEENTH"
fun cutoffFromKey(key: String): Cutoff?   // parse; null if malformed
```

## Data / persistence

- DB migration **v5** (`4.sqm`): `ALTER TABLE Settings ADD COLUMN paidAheadKey TEXT NOT NULL DEFAULT '';`
- `Settings.sq`: add the column to CREATE TABLE; add `setPaidAhead` and `clearPaidAhead` queries.
- `Settings` model gains `paidAheadKey: String`. `SettingsRepository` gains `setPaidAhead(key: String)` and `clearPaidAhead()`.

## ViewModel — `HomeViewModel`

In `load(today)`:
1. Read override: `repos.settings.get().paidAheadKey.takeIf { it.isNotBlank() }?.let { cutoffFromKey(it) }`.
2. `val eff = resolveEffectiveCutoff(today, override)`; if `eff.clearOverride` → `repos.settings.clearPaidAhead()`.
3. Use `eff.cutoff` everywhere the old code used `currentCutoff(today)`, and `eff.cutoff.next()` where it used `nextCutoff(today)` (identical when not activated, since `eff.cutoff == currentCutoff(today)` then).
4. `HomeState` gains `isActivatedEarly: Boolean = eff.isActivatedEarly`.

New methods:
- `activateNextEarly(today)` → `repos.settings.setPaidAhead(nextCutoff(today).storageKey())` (one cutoff ahead of the calendar).
- `undoActivateEarly()` → `repos.settings.clearPaidAhead()`.

`markPaid` is unchanged — it records today's date; the activated cutoff's `isPaidThisCycle` attributes it correctly (verified: today falls after the prior cycle's due date and on/before the activated window's end; it cannot leak into the previous cutoff because due-day windows differ).

## UI — `HomeScreen`

- **Activated banner:** when `state.isActivatedEarly`, render a small row just above the CutoffCard — "Next payday · activated early" + an **Undo** text button (`vm.undoActivateEarly(); tick++`).
- **Activate action:** when `!state.isActivatedEarly` **and** `state.nextRows.isNotEmpty()`, render a subtle full-width button directly under the UpNextCard — "Got paid early? Activate →" (`vm.activateNextEarly(); tick++`). Hidden once activated, which enforces the **one-cutoff-ahead cap**.
- Everything else (CutoffCard, PAYMENTS swipe list, Up Next, Overdue, Journey) already keys off `state.cutoff` / `state.nextRows`, so it follows automatically.

## Testing

Unit tests (commonTest), the bug-prone core:
- `resolveEffectiveCutoff`: null override; override ahead (today before its window) → activated; override equal to date-current (today entered it) → not activated + clearOverride; override behind/stale → clearOverride.
- `storageKey`/`cutoffFromKey` round-trip for FIFTEENTH and THIRTIETH, across a year boundary; `cutoffFromKey` returns null on malformed input.
- A small integration check: `resolveEffectiveCutoff(today, nextCutoff(today))` reports the next cutoff, `isActivatedEarly = true`.

Marking-in-activated-cutoff is covered by the existing, tested `CutoffCalculator` behavior. Full unit suite must stay green. Code review before ship.

## Out of scope

- No change to Insights, Overdue, Journey, or cutoff windows.
- No "advance multiple cutoffs" (hard cap one ahead).
- No auto-detection of payday (manual action only).

## Tag

v0.4.3.
