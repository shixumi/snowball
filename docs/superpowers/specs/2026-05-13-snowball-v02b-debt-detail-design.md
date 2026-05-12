# Snowball v0.2 — Sub-project B: Debt Detail + Archive view + MISC items

**Date:** 2026-05-13
**Scope:** A new `DebtDetailScreen` that becomes the destination of every debt tap, an improved Archive view, and a new MISC-items entry path via a slim form reached through a FAB dropdown.
**Excluded (other v0.2 sub-projects):** Category management (sub-project C — separate cycle), cutoff rollover behaviors (sub-project D), notifications, platform expansion.

**Autonomous-design note:** This spec was written without the usual approve-each-section gate per user's overnight authorization. Design decisions live inline alongside their rationale. If a call doesn't match user preference, revert the commit and re-spec.

## Purpose

Three loosely-related v0.2 items, all touching the Debts-tab navigation surface:

1. The HANDOFF describes a **Debt Detail screen** with header / progress arc / stats / payment history / actions. Currently tapping a debt row goes directly to the Edit form. Detail decouples "look at this debt" from "modify this debt".
2. **Archive view** already exists structurally (a toggle on DebtsScreen) but the archived rows look identical to active ones. The HANDOFF describes a read-only ledger feel.
3. **MISC items** are conceptually one-time recorded payments (not scheduled monthly debts). They need a slimmer form and their own visual slot.

## In scope

| Item | Resolution |
|---|---|
| `Route.DebtDetail(id)` route | Add to existing sealed interface in `App.kt` |
| `DebtDetailScreen` composable | New file |
| `DebtDetailViewModel` | New file |
| Tap-to-detail navigation (Debts list AND Home payment rows) | Update `App.kt` wiring |
| Edit action from Detail | Reuses existing `DebtFormScreen` via existing `Route.Form` |
| Archive/Unarchive action from Detail | New `setArchived` calls on existing `DebtRepository` (already exists) |
| Delete action from Detail | Reuses AlertDialog pattern from v0.1.1 form |
| Payment history list with tap-to-undo + confirmation | Inline within Detail |
| Archive view: hide FAB when viewing archived | Modify `DebtsScreen` |
| Archive view: "Cleared {date}" subtitle | Modify `DebtsScreen` |
| FAB dropdown menu (Add debt / Add MISC item) | Replace single-tap with dropdown |
| `Route.MiscForm` route | Add to sealed interface |
| `MiscFormScreen` composable | New slim form |
| `MiscFormViewModel` | New file |
| MISC items render as separate section on Debts | Modify `DebtsScreen` |

## Out of scope

- Editing MISC items after creation (read-only ledger).
- Multi-action FABs / SpeedDial. Single FAB + dropdown menu is the chosen pattern.
- Bulk operations on archived debts.

## Design — section by section

### 1. Navigation routes

Modify `composeApp/src/commonMain/kotlin/com/snowball/App.kt` sealed interface:

```kotlin
sealed interface Route {
    data object Tabs : Route
    data class Form(val existingDebtId: Long?) : Route
    data class DebtDetail(val debtId: Long) : Route
    data object MiscForm : Route
}
```

`when (route) { ... }` in App.kt branches gain `is Route.DebtDetail` and `is Route.MiscForm`. The `BottomNav` is hidden for both new routes (same rule as `Route.Form`).

Tap flows:
- `DebtsScreen.onEdit(id)` → now navigates to `Route.DebtDetail(id)` (was `Route.Form(id)`). Rename the callback to `onOpen` for clarity.
- From inside Detail, the Edit action fires `onEdit(id)` which → `Route.Form(id)` (existing edit form). Back from form returns to `Route.Tabs` (not back to Detail — keeps navigation flat).
- `HomeScreen` payment rows still tap-to-toggle for paid/unpaid (existing behavior). Long-press-or-something to open detail? **Decision: no.** Home is a fast-actions screen; tapping a row is "I just paid this". Detail is reachable from Debts only.

### 2. DebtDetailScreen

**File:** `composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailScreen.kt`

**Layout** (Scaffold-based, top → bottom):

```
┌───────────────────────────────────────────┐
│  ←  Sloan                          ⋮      │   ← TopAppBar
│     [Credit Card] [Active]                │   ← header row 2
├───────────────────────────────────────────┤
│                                           │
│             ╭───────╮                     │   ← big ProgressArc 160dp
│            │ 3 of 12 │                    │
│             ╰───────╯                     │
│                                           │
│       ₱13,500 left                        │   ← derived: (totalPayments - paymentsMade) × monthlyAmount
│                                           │
│  ────────────────────────────────────     │   ← LineStrong divider
│                                           │
│  MONTHLY        ₱1,500                    │
│  DUE DAY        10                        │
│  STARTED        Jan 1, 2026               │
│  PROJECTED END  Dec 10, 2026              │
│                                           │
│  ────────────────────────────────────     │
│                                           │
│  PAYMENT HISTORY (3)                      │
│  ○ Mar 10, 2026   ₱1,500            ⋯    │   ← tap row → confirm undo
│  ○ Feb 10, 2026   ₱1,500            ⋯    │
│  ○ Jan 10, 2026   ₱1,500            ⋯    │
└───────────────────────────────────────────┘
```

**TopAppBar:**
- Back arrow → `onBack()` (returns to Tabs).
- Title: debt name (Fraunces titleLarge), truncated with ellipsis if long.
- Overflow `⋮`: opens DropdownMenu with three items based on archive state:
  - Active: **Edit** (→ `Route.Form(id)`), **Archive** (sets `isArchived = true`), **Delete** (confirm + execute).
  - Archived: **Unarchive** (sets `isArchived = false`), **Delete**. No Edit on archived (read-only).

**Header row 2 (under title in TopAppBar's subtitle slot is unusual; just put it as the first item in the scroll body):**
- Two chips side by side: category badge (existing `CategoryIcon` + name), and a status chip ("Active" Ice or "Archived" FrostMute).

**Progress arc block:**
- Reuse existing `ProgressArc` component, 160dp. Centered.
- `progress = paymentsMade / totalPayments`.
- Text overlay inside arc: `${paymentsMade} of ${totalPayments}` in Fraunces displaySmall.
- Below arc: "₱X left" computed as `(totalPayments - paymentsMade) × monthlyAmount`. Renders 0 when paid off.

**Stats grid** (vertical, label/value rows with 16dp gap):
- MONTHLY → `PesoText(monthlyAmount)`
- DUE DAY → `${dueDay}${if (useLastDayOfMonth) " (or last day)" else ""}`
- STARTED → `formatLongDate(startDate)` — new util "Jan 1, 2026" style
- PROJECTED END → reuses `JourneyCalculator.projectedEndDate(debt)` from sub-project A, formatted "Dec 10, 2026". If null → "—".

**Payment history:**
- Section header "PAYMENT HISTORY ({count})" — label small, FrostDim, 4sp tracking.
- One row per `Payment` from `repos.payments.historyForDebt(id)`, sorted by `paidDate` descending.
- Each row: filled small circle ○, formatted date, PesoText amount, ⋯ chevron at right.
- Tap row → AlertDialog "Undo this payment? Amount X recorded on Y will be removed." Cancel / Undo.
- Confirm → `repos.payments.delete(payment.id)`, refresh.

**MISC items in Detail:** A MISC `Debt` row also reaches this screen (it's still a `Debt`). For MISC: hide the ProgressArc + "₱X left" block (a one-payment ledger has no progress to show); show only the stats grid (just MONTHLY / DUE DAY irrelevant for MISC → adjust) and a single-line payment history. Rather than special-case the screen, detect `category.behavior == LEDGER` and render a thinner variant:

```
┌───────────────────────────────────────────┐
│  ←  Snack run                      ⋮      │
│     [MISC] [Archived]                     │
├───────────────────────────────────────────┤
│                                           │
│       ₱350                                │   ← big amount, no progress arc
│       Paid May 13, 2026                   │
│                                           │
│  Notes                                    │
│  "Cebuana to Mom"                         │
└───────────────────────────────────────────┘
```

Same TopAppBar (Edit hidden for MISC; only Delete makes sense).

### 3. DebtDetailViewModel

**File:** `composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailViewModel.kt`

```kotlin
data class DebtDetailState(
    val debt: Debt,
    val category: Category,
    val paymentsMade: Int,
    val payments: List<Payment>,         // sorted desc by paidDate
    val projectedEndDate: LocalDate?,
    val amountLeft: Double,               // (totalPayments - paymentsMade) × monthlyAmount
)

class DebtDetailViewModel(private val repos: Repos, private val debtId: Long) {
    fun load(): DebtDetailState? {
        val debt = repos.debts.byId(debtId) ?: return null
        val category = repos.categories.byId(debt.categoryId) ?: return null
        val payments = repos.payments.historyForDebt(debtId)
        val made = payments.size
        val left = (debt.totalPayments - made).coerceAtLeast(0) * debt.monthlyAmount
        // Reuse the projection function from JourneyCalculator
        val projected = projectedEndDateFor(debt)
        return DebtDetailState(debt, category, made, payments, projected, left)
    }

    fun setArchived(archived: Boolean) { repos.debts.setArchived(debtId, archived) }
    fun delete(): Boolean { repos.debts.delete(debtId); return true }
    fun undoPayment(paymentId: Long) {
        repos.payments.delete(paymentId)
        // If the debt was auto-archived after this payment, undo the archive too
        val debt = repos.debts.byId(debtId) ?: return
        val remaining = repos.payments.countForDebt(debtId)
        if (debt.isArchived && remaining < debt.totalPayments) {
            repos.debts.setArchived(debtId, false)
        }
    }
}
```

**`projectedEndDateFor(debt: Debt): LocalDate?`** is currently a private function in `JourneyCalculator`. Promote it to a top-level function in the same `JourneyCalculator.kt` file (rename to `projectedEndDate(debt)`) and have both `JourneyCalculator` and `DebtDetailViewModel` use it. Single source of truth.

### 4. DebtsScreen edits

#### Tap-to-detail

Rename `onEdit: (Long) -> Unit` parameter to `onOpenDebt: (Long) -> Unit`. The `App.kt` wiring changes:

```kotlin
DebtsScreen(
    vm = debtsVm,
    onAddDebt = { /* see FAB dropdown section */ },
    onAddMisc = { route = Route.MiscForm },
    onOpenDebt = { id -> route = Route.DebtDetail(id) },
)
```

#### FAB dropdown menu

Replace the existing single-`FloatingActionButton` block with a FAB + popup. State is a local `var fabExpanded by remember { mutableStateOf(false) }`. Tap FAB → toggle. The DropdownMenu anchors above the FAB:

```kotlin
Box(modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
    FloatingActionButton(
        onClick = { fabExpanded = true },
        modifier = Modifier.size(56.dp).clip(CircleShape),
        containerColor = SnowColors.Ice,
        contentColor = SnowColors.Night,
    ) {
        Icon(Icons.Outlined.Add, contentDescription = "Add", tint = SnowColors.Night)
    }
    DropdownMenu(
        expanded = fabExpanded,
        onDismissRequest = { fabExpanded = false },
        offset = DpOffset(x = (-160).dp, y = 0.dp),  // align right edge to FAB
    ) {
        DropdownMenuItem(
            text = { Text("Add debt", color = SnowColors.Frost) },
            onClick = { fabExpanded = false; onAddDebt() },
        )
        DropdownMenuItem(
            text = { Text("Add MISC item", color = SnowColors.Frost) },
            onClick = { fabExpanded = false; onAddMisc() },
        )
    }
}
```

When `state.showArchived == true`, the FAB and its DropdownMenu are not rendered (archived view is read-only).

#### Archive view subtitle

When viewing archived debts, replace the existing `"Day {dueDay} · {totalPayments} months · {paymentsMade}/{totalPayments} paid"` subtitle with:

```
Cleared {endDate} · ₱{totalPaidAmount}
```

Where `endDate` is the `paidDate` of the most recent payment (newest in history), and `totalPaidAmount` is `sum(payments.map { it.amount })`. Compute these in `DebtsViewModel.load()` and attach to `DebtRow`.

#### MISC items section

`DebtsViewModel.load()` currently groups by `categoryId` and produces a `Map<Long, List<DebtRow>>`. Categories are iterated in their natural order. To render MISC items last:

- Compute two groupings in `DebtsState`: `scheduledByCategory: Map<Long, List<DebtRow>>` (only SCHEDULED categories) and `miscRows: List<DebtRow>` (only LEDGER category, currently just MISC).
- DebtsScreen iterates `scheduledByCategory` first (existing rendering), then renders a MISC section at the bottom with its own header and simpler row layout (no progress, just date + amount).

MISC row layout:
```
┌───────────────────────────────────────────┐
│  Snack run                       ₱350     │   ← name, amount
│  Paid May 13                              │   ← single subtitle line
└───────────────────────────────────────────┘
```

(No "5/350 paid", no projection — these are one-shot entries.)

### 5. MiscFormScreen

**File:** `composeApp/src/commonMain/kotlin/com/snowball/ui/misc/MiscFormScreen.kt`

A slim form scaffolded the same way as `DebtFormScreen` (TopAppBar + bottom Save). Four fields:

- **Name** (required, text)
- **Amount** (required, decimal keyboard, > 0)
- **Date paid** (required, YYYY-MM-DD text, defaults to today)
- **Notes** (optional, text)

Save action behavior:
1. Look up the MISC category id (`repos.categories.all().first { it.behavior == LEDGER && it.name == "MISC" }`).
2. Call `repos.debts.add(name, categoryId = miscId, monthlyAmount = amount, totalPayments = 1, dueDay = 1, useLastDayOfMonth = false, startDate = datePaid, notes = notes.ifBlank { null })`.
3. Get the new debt's id via `repos.debts.all().first().id` (same pattern as `DebtFormViewModel`).
4. `repos.payments.markPaid(newId, datePaid, amount)` — records the single payment.
5. `repos.debts.setArchived(newId, true)` — MISC entries auto-archive so they don't clutter Active view.
6. `onSaved()`.

The existing auto-archive logic in `DebtFormViewModel` doesn't quite fit MISC's flow (it backfills then archives via the SCHEDULED form's path). Cleaner to have the MISC form do its own three-step save explicitly.

### 6. MiscFormViewModel

**File:** `composeApp/src/commonMain/kotlin/com/snowball/ui/misc/MiscFormViewModel.kt`

```kotlin
data class MiscFormState(
    val name: String = "",
    val amount: String = "",
    val datePaid: LocalDate = today(),
    val datePaidText: String = datePaid.toString(),
    val notes: String = "",
)

fun MiscFormState.isValid(): Boolean =
    name.trim().isNotEmpty() &&
        (amount.toDoubleOrNull() ?: 0.0) > 0.0 &&
        runCatching { LocalDate.parse(datePaidText) }.isSuccess

class MiscFormViewModel(private val repos: Repos) {
    var state by mutableStateOf(MiscFormState())
        private set
    val isValid: Boolean get() = state.isValid()

    fun update(transform: (MiscFormState) -> MiscFormState) { state = transform(state) }

    fun save(): Boolean {
        if (!isValid) return false
        val miscCategory = repos.categories.all().firstOrNull { it.behavior == CategoryBehavior.LEDGER }
            ?: return false
        val amount = state.amount.toDouble()
        val date = LocalDate.parse(state.datePaidText)
        val name = state.name.trim()

        repos.debts.add(
            name = name,
            categoryId = miscCategory.id,
            monthlyAmount = amount,
            totalPayments = 1,
            dueDay = 1,
            useLastDayOfMonth = false,
            startDate = date,
            notes = state.notes.ifBlank { null },
        )
        val newId = repos.debts.all().first().id
        repos.payments.markPaid(newId, date, amount)
        repos.debts.setArchived(newId, true)
        return true
    }
}
```

The `repos.debts.all().first()` pattern matches the existing `DebtFormViewModel.save()` approach — newest debts are first.

## File-level change inventory

**New files (5):**
- `composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailScreen.kt`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/detail/DebtDetailViewModel.kt`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/misc/MiscFormScreen.kt`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/misc/MiscFormViewModel.kt`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/util/DateFormat.kt` *— extends existing file with `formatLongDate`*
- `composeApp/src/commonTest/kotlin/com/snowball/ui/misc/MiscFormStateValidationTest.kt`

**Modified files (5):**
- `composeApp/src/commonMain/kotlin/com/snowball/App.kt` — new routes + wiring
- `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsScreen.kt` — onAddMisc, onOpenDebt rename, FAB dropdown, archived FAB hide, MISC section
- `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsViewModel.kt` — split into scheduledByCategory + miscRows, compute archive summary
- `composeApp/src/commonMain/kotlin/com/snowball/domain/JourneyCalculator.kt` — promote `projectedEndDate` to top-level public function
- `composeApp/src/commonMain/kotlin/com/snowball/ui/util/DateFormat.kt` — add `formatLongDate(LocalDate): String`

**Untouched:**
- All data-layer code (no schema changes — MISC is just a Debt with categoryId=MISC, totalPayments=1).
- `HomeScreen`, `HomeViewModel`, `CutoffCard`, `UpNextCard`, `JourneyCard`.
- `DebtFormScreen`, `DebtFormViewModel`, `SettingsScreen`, `Nav`.

## Testing strategy

**Unit tests:**
- `MiscFormStateValidationTest` (5 tests): blank name, empty amount, zero amount, unparseable date, all valid.
- Promote `JourneyCalculator.projectedEndDate` test cases to cover the new top-level function name (rename existing test methods if needed, no functional change).
- No new tests for `DebtDetailViewModel.load()` — it's mechanical wiring of pre-tested repository/domain functions.

**Manual emulator verification:**
- Tap a debt on Debts → opens DebtDetailScreen (not form). Header, arc, stats, history list all visible.
- Tap a payment history row → confirm dialog → undo → row disappears, stats update.
- Overflow → Edit → existing form opens (titled "Edit debt").
- Overflow → Archive → debt moves to Archived view.
- Switch to Archived view → FAB hidden, archived debt shows "Cleared ... · ₱..." subtitle.
- Tap archived debt → Detail opens with Unarchive in overflow (not Edit/Archive).
- FAB on Active view → tap → dropdown shows "Add debt" / "Add MISC item".
- Add a MISC item → appears in its own section at bottom of Active Debts. Wait, MISC items are auto-archived — should they appear in Active or Archived view?

**Decision on MISC visibility:** MISC items are auto-archived (so they're skipped by `repos.debts.allActive()`), but the user clearly wants them visible somewhere as a ledger. Render them in the **Active** view's MISC section regardless of `isArchived` flag. Modify `DebtsViewModel.load()` to fetch MISC items separately via `repos.debts.all().filter { category.behavior == LEDGER }`. In the Archived view, also show recently archived non-MISC debts; MISC stays only on Active.

Same `font_scale 1.30` and `pm clear` checks as v0.1.1 / v0.2a.

## Risks

- **Promoting `projectedEndDate` to public.** It's currently `private` inside `JourneyCalculator`. Two consumers now (JourneyCalculator + DebtDetailViewModel). One file, one source of truth. The signature is `(debt: Debt) -> LocalDate?`. Make sure JourneyCalculator's internal `mapNotNull(::projectedEndDate)` reference still works after the rename — it should, since the function moves from `private` to top-level in the same file.
- **MISC visibility on Active vs Archived.** Documented above. If user disagrees, the fix is to flip the filter in `DebtsViewModel.load()`.
- **FAB dropdown alignment.** `DropdownMenu(offset = DpOffset(-160.dp, 0.dp))` is hand-tuned. May look slightly off on different screen densities. Test on emulator and adjust.
- **Tap-to-detail on Debts might surprise users who expected tap-to-edit.** Mitigation: Edit is the first overflow item on Detail, one extra tap away. The information density of Detail makes this worth the friction.
- **Payment history undo with auto-unarchive.** If a fully-paid debt was auto-archived and the user undoes a payment from the now-archived debt's Detail screen, the debt needs to un-archive. Logic added to `DebtDetailViewModel.undoPayment`.

## Success criteria

- Tap any debt anywhere → DebtDetailScreen with full info.
- Payment history can be edited via tap-to-undo with confirmation.
- Archive/Unarchive/Delete work from overflow.
- MISC items can be created via FAB dropdown, render in their own section, don't affect cutoff totals (already guaranteed by domain logic).
- FAB hidden when viewing archived list.
- All existing tests pass; 5 new MiscForm validation tests pass.
- Build + uiautomator dumps confirm.
- Tag `v0.2.1` pushed.
