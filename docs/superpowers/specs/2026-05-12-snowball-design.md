# Snowball — Design Spec

**Date:** 2026-05-12
**Status:** Approved — ready for implementation planning
**Author:** Brainstormed with Claude

---

## 1. Summary

Snowball is a personal cross-platform app that replaces a debt-tracking spreadsheet. Its core job is to answer one question on every payday: *"What am I paying this cutoff, and how much do I have left?"*

The user gets paid on the **15th** and **30th** of every month. Each paycheck funds a specific window of upcoming debt payments. The app shows that math at a glance, lets the user mark payments as paid, sends payday reminders, and visualizes long-term progress as a growing "snowball" of cleared debt.

It is single-user, offline-first, with no cloud sync.

## 2. Goals & non-goals

### Goals (v1)
- Show, on each payday, the total amount due in the upcoming cutoff and the breathing room after debts.
- Track every payment as history (no merely-incremented counter).
- Send a notification on the 15th and 30th summarizing each cutoff.
- Auto-archive paid-off debts; auto-flag overdue ones and roll them into the current cutoff.
- Run natively on **Android, iOS, macOS, and Windows** from a single codebase.
- Have its own visual identity (Editorial Frost) — distinct from generic banking apps.

### Non-goals (v1)
- Cloud sync, multi-device backup, multi-user.
- Snowball/avalanche *strategy recommendations* (the visualization is in; the "attack this debt next" logic is out).
- "What-if" extra-payment simulations.
- Per-debt custom notification rules.
- Cutoff history browser (viewing past cutoffs).
- Spreadsheet import.
- Multiple incomes, variable cutoff income, multi-currency, light theme, localization.
- Search / sort / filter in the debts list beyond the active-vs-archived toggle.

## 3. Visual identity

**Theme:** Editorial Frost — single dark theme, midnight blue base, frost-white type, single cool accent (ice blue) and single warm accent (champagne).

**Typography:**
- **Display & numbers:** Fraunces (variable serif, SOFT and WONK axes tuned warm)
- **Body & labels:** DM Sans

**Motif:** A growing snowball — a soft white sphere with a donut progress ring around it, representing total debt cleared. Each debt card has a thin progress arc. Paid-off debts briefly transform to a snowflake glyph before archiving.

**Reference mockup:** `.superpowers/brainstorm/1189-1778573455/content/snowball-identity.html` (preserved in the project for design reference).

## 4. Tech stack

- **Language & UI:** Kotlin + Compose Multiplatform
- **Database:** SQLDelight (SQLite, type-safe Kotlin)
- **Platform-specific code:** `expect`/`actual` declarations — notifications and any OS-specific work isolated to per-platform modules.
- **Targets:** Android, iOS, macOS, Windows (single codebase)
- **Build & run:**
  - Android — Gradle, sideload APK
  - iOS — Xcode build from a Mac; free signing for personal use, Apple Developer Program ($99/yr) for TestFlight stability
  - macOS / Windows — native desktop binaries

## 5. Data model

Four core entities plus a settings singleton.

### Category
- `id` — unique
- `name` — display name
- `isSystem` — `true` for the two locked categories: **Credit Card** and **MISC**
- `behavior` — `SCHEDULED` for all categories except MISC; `LEDGER` for MISC
- `createdAt`

Two seeded categories ship with the app: **Credit Card** (`SCHEDULED`, system-locked) and **MISC** (`LEDGER`, system-locked). Users can create, rename, and delete any other categories. System categories cannot be renamed or deleted.

### Debt — scheduled debts; one row per debt
- `id`
- `name`
- `categoryId` (FK)
- `monthlyAmount` — what's paid each cycle
- `totalPayments` — duration in months
- `dueDay` — 1..30
- `useLastDayOfMonth` — boolean; if true, payment day floats to actual last day (handles Feb / 30-day months for `dueDay = 30+`)
- `startDate` — first cycle's effective start; gates the debt from appearing in earlier cutoffs
- `isArchived` — flipped automatically when fully paid (`paymentsMade == totalPayments`); can also be set manually
- `notes`

**Derived (not stored):**
- `paymentsMade` = `count(Payment where debtId = this.id)`
- `amountLeft` = `monthlyAmount × (totalPayments − paymentsMade)`
- `projectedEndDate` = `startDate + totalPayments` months (approx)

### Payment — full history; one row per payment event
- `id`
- `debtId` (FK)
- `paidDate` — when the user tapped paid
- `amount` — defaults to the debt's `monthlyAmount`; user can override for partial or extra payments

Deleting a Payment row undoes that payment (the count goes down, archive flag reconsidered).

### MiscItem — separate table, different shape
- `id`
- `name`
- `amount`
- `dueDate` — optional, free date (not tied to cutoff math)
- `isSettled` — manual toggle
- `notes`

MISC items are a read-only ledger. They do **not** participate in cutoff math.

### Settings — single row
- `incomePerCutoff` — single fixed amount used for both 15th and 30th
- `currency` — PHP (locked for v1)
- `notificationsEnabled`
- `notificationTimeOfDay` — when the payday notification fires
- `cutoffSummaryEnabled`

## 6. Cutoff logic

### Cutoff identity
A cutoff is named by its payday and covers the window of debts it funds:

| Cutoff payday | Covers |
|---|---|
| 15th | days **15–30** of the same month |
| 30th | days **1–14** of the *next* month |

### Current-cutoff detection
Given `today`:
- If `day(today) ∈ [1, 14]` → current cutoff is **last month's 30th**
- If `day(today) ∈ [15, 30]` → current cutoff is **this month's 15th**

### Last-day-of-month handling
For `dueDay = 30` paired with `useLastDayOfMonth = true`, the effective due day floats to the actual last day of each month (Feb 28/29, etc.). Without that flag, `dueDay = 30` is treated literally and skipped in months with fewer days (rare edge case; user can opt into the floating behavior).

### "Due this cutoff" calculation
For each non-archived `Debt`:
1. Does its effective `dueDay` fall in the cutoff's window?
2. Is `startDate ≤` the cutoff's payday? (skip debts not yet started)
3. Find the most recent `Payment` for this debt. Has it been paid *for this cycle*?
   - "This cycle" = the period between the previous expected dueDate and the next one
   - For the very first cycle (no prior dueDate), the previous bound is the debt's `startDate`
   - No payment in that range → **still owed**
4. If still owed → add `monthlyAmount` to "due this cutoff" total.

### "Money left" (breathing room)
`incomePerCutoff − dueThisCutoffTotal`

The total uses the full *owed* amount (paid + unpaid), so the breathing-room number stays stable as the user ticks off payments. Tapping "mark paid" updates the per-payment status but not the cutoff total.

### Overdue rollover
When a cutoff's payday passes, any unpaid debts from the closing cutoff get tagged `OVERDUE` and:
- Appear in the **new** current cutoff's list with a red badge
- **Add to the new cutoff's total** (your actual obligation reflects reality)
- Settle the same way (mark paid → tag clears)

### Mark paid
- Tap "mark paid" on a payment row → insert a `Payment` row with `paidDate = now`, `amount = debt.monthlyAmount` (override-able in a sheet).
- `paymentsMade` recounts; if it equals `totalPayments`, set `isArchived = true` and trigger a brief "snowflake" animation before the debt slides off the active list.
- Undo: open Debt Detail → Payment History → tap the row → confirm delete.

### Snowball journey metric
- **Total melted (cleared):** `sum(Payment.amount)` across all debts ever.
- **Total commitment:** `sum(monthlyAmount × totalPayments)` across all scheduled debts (active + archived).
- **% cleared:** `melted / commitment`.
- **Debt-free date:** the latest projected end date across all active scheduled debts.

## 7. Screens & navigation

Bottom-nav with 3 tabs: **Home · Debts · Settings**.

### Home (default tab)
- Cutoff hero card — current cutoff window, "Due" hero number, ledger (Income / Breathing room), payments list with progress arcs.
- "Up next" mini-card — next cutoff window and total.
- Snowball journey card — donut ring around a growing sphere, "X% cleared", "₱X melted", debt-free date forecast.
- Tap any payment row → Debt Detail.

### Debts
- Sections collapsed by category. MISC items render as their own section at the bottom in a read-only treatment (no progress arc).
- Active/Archived toggle at the top.
- FAB → sheet asks **Debt or MISC?** → opens the matching form.

### Debt Detail
- Header: name, category badge, archived/active state.
- Progress arc with `X of Y paid`.
- Stats grid: amount left, monthly, due day, start date, projected end date.
- Payment history list — date + amount; tap to delete (undo).
- Actions: Edit · Archive manually · Delete.

### Add/Edit form (modal sheet)
- **Debt form:** name, category (dropdown), monthly amount, total payments, due day + "last day of month" toggle, start date, notes.
- **MISC form:** name, amount, due date (optional), notes.
- The form switches shape based on whether the chosen category's `behavior` is `SCHEDULED` or `LEDGER`.

### Settings
- Income per cutoff (single number)
- Notifications toggle + time-of-day picker
- Categories → opens Category Management
- Data → export all data as JSON or CSV
- Currency display (locked PHP)
- About

### Category Management (pushed from Settings)
- List of categories, with a lock icon on Credit Card and MISC
- Rename inline; system ones not editable
- "+ New category" creates a `SCHEDULED` category
- Delete: blocked for system; if a user category has debts, prompt to reassign before deletion

### Archive (accessed from Debts tab's Active/Archived toggle)
- Paid-off debts, read-only Debt Detail when tapped

## 8. Notifications

- **Trigger:** local OS notification at `Settings.notificationTimeOfDay` on the **15th** and **30th** of every month.
- **Content:** *"Cutoff [date range] starts: ₱X due, ₱Y left over after debts."*
- **Platform implementation** (via `expect`/`actual`):
  - Android: `AlarmManager` + `NotificationCompat`
  - iOS: `UNUserNotificationCenter`
  - Desktop: native OS notification APIs
- Settings toggle disables all notifications.
- No per-debt notification rules in v1.

## 9. Non-functional requirements

- **Offline-first.** No network calls in v1. Nothing leaves the device.
- **Privacy.** All data local. Export is opt-in user action.
- **Performance.** SQLite trivially handles <100 debts; no optimization concerns for v1.
- **Accessibility.** Standard system text scaling respected. Color contrast meets AA on the dark theme.
- **Crash safety.** All writes inside transactions; no risk of partial-save corrupting state.

## 10. Build & release

- **Repository:** single Compose Multiplatform Gradle project; Android, iOS, JVM (macOS/Windows desktop) targets.
- **Daily-driver build (Android):** sideload-able APK, debug-signed initially, release-signed once stable.
- **iOS build:** Xcode framework via Kotlin Multiplatform; install via Xcode + free signing during development, TestFlight once a Dev cert is in place.
- **Desktop builds:** native binaries for macOS (dmg) and Windows (msi or exe).
- **Versioning:** semantic — v0.1 = MVP shipped on Android only; v0.2 = iOS verified; v1.0 = all four targets stable.

## 11. Open items deferred to plan

These are implementation choices that don't need product input — they'll be settled in the implementation plan, not here:

- Exact Compose Multiplatform navigation library (Decompose vs Voyager vs the stable nav library — depends on current ecosystem state when plan is written)
- DI library (Koin vs manual)
- Time-of-day picker UX detail
- Animation timing values for the snowflake-on-payoff moment
- Whether to use a single SQLDelight schema file or split per entity
