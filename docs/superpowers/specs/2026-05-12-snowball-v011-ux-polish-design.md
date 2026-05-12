# Snowball v0.1.1 — UX/UI Polish Design

**Date:** 2026-05-12
**Scope:** Address findings from `docs/design/ux-review-2026-05-12.md`
**Excluded:** Blocker #1 (debt added today filtered from Home) and Blocker #2 (categories) — both reserved for v0.2

## Purpose

The independent UX inspector identified 21 issues in v0.1.0 across blockers, important polish gaps, nitpicks, and accessibility. This release closes everything except the two items already planned into v0.2 (the "Up next" mini-card and category management). The intent is a v0.1.1 that ships as the polished version of v0.1 rather than a feature increment.

## In scope

| # | Severity | Issue | Resolution |
|---|---|---|---|
| 3 | Blocker | Validation failures are silent on the form | Disabled Save + inline per-field errors |
| 4 | Blocker | Delete is one-tap destructive | AlertDialog confirmation |
| 5 | Important | `LEFT OVER` doesn't change for negative state | Ember color + label flips to "SHORT BY" |
| 6 | Important | Form has no top bar; Cancel buried | TopAppBar + bottom-pinned Save; Delete in overflow |
| 7 | Important | Numeric inputs show "1500.0" | Shared `formatAmount` helper |
| 8 | Important | Settings income pre-fills "0.0", auto-commits per keystroke | Empty placeholder, commit-on-blur, check ack, formatted display when unfocused |
| 9 | Important | "Payments already made" semantics drift in edit mode | Hide field in edit mode; show "X of Y" summary instead |
| 10 | Important | Long debt names break Debts row layout | Top-align amount, `maxLines = 2` + ellipsis on name |
| 11 | Important | Empty Home doesn't prompt income setup | Conditional empty-state copy by state |
| 12 | Important | Save button stays Ice when disabled | `disabledContainerColor = FrostMute` (folded into #3) |
| 13 | Nitpick | PAYMENTS section header label/caption gap | Spacer 2dp → 8dp |
| 14 | Nitpick | "+" glyph FAB | Replace with `Icons.Outlined.Add` |
| 15 | Nitpick | Missing contentDescriptions | Add to FAB, payment row, PesoText |
| 16 | Nitpick | "Use last day of month" Switch unlabeled | Semantics merge on Switch row |
| 17 | Nitpick | Bottom-nav active indicator weak | Ice-tinted 10%-alpha pill behind selected tab |
| 18 | Nitpick | "View archived" no affordance | Underline + ChevronRight icon |
| 19 | Nitpick | ALL-CAPS labels "verge on" hard to read | Keep — brand identity, addressed via contrast fixes |
| 20 | Nitpick | Cutoff card DUE label spacing asymmetric | 8/24 → 12/20 |
| 21 | Nitpick | Italic swipe caption decorative once learned | Keep, recolor for contrast; true coachmark deferred |
| 22 | Nitpick | Debts list shows no progress | Append `· n/total paid` to subtitle |
| A11Y | Contrast | FrostDeep #3C4452 (1.65:1) fails AA in 3 text usages | Retire FrostDeep from text; reroute usages to FrostMute/FrostDim |
| A11Y | Contrast | FrostDim #5E6874 (2.7:1) fails AA for body labels | Lift FrostDim to `#7A8696` (~4.6:1) |
| A11Y | Contrast | Line divider Alpha-12 white invisible | Bump to Alpha-24 |
| A11Y | Semantics | OutlinedTextFields all flagged NAF | Wrap each Field column with `semantics(mergeDescendants=true) { contentDescription = label }` |
| A11Y | Dynamic text | Last Home row touches nav at font_scale 1.30 | `contentPadding = PaddingValues(bottom = 16.dp)` on payment LazyColumn |

## Out of scope (deferred to v0.2)

- **Blocker #1** — Silent debt-drop from Home when `startDate > payDate`. Solved properly by the v0.2 "Up next" mini-card, which surfaces queued-for-next-cutoff debts.
- **Blocker #2** — Category management. The "+ New category" affordance and CategoryRepository UI hookup ship with the v0.2 Category Management screen.
- True first-launch **coachmark** for the swipe instruction (nitpick #21 partial). Lands with other v0.2 onboarding.
- **Rotation/landscape** verification. The emulator wouldn't actually rotate during the v0.1 inspection; defer to a v0.2 verification task.
- Full **TalkBack audit** beyond the inspector's identified flags.

## Design — section by section

### 1. Form blockers (validation + delete)

#### Validation feedback

`DebtFormViewModel` introduces a derived `isValid: StateFlow<Boolean>`:

```kotlin
val isValid: StateFlow<Boolean> = combine(
    nameField, categoryField, amountField, totalField, dueDayField, paymentsAlreadyMadeField
) { name, category, amount, total, dueDay, alreadyMade ->
    name.isNotBlank() &&
    category != null &&
    (amount.toDoubleOrNull() ?: 0.0) > 0.0 &&
    (total.toIntOrNull() ?: 0) in 1..600 &&
    (dueDay.toIntOrNull() ?: 0) in 1..31 &&
    (alreadyMade.toIntOrNull() ?: 0) in 0..(total.toIntOrNull() ?: 0)
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
```

Each form field gains a "touched" flag held in screen-level state (`var nameTouched by remember { mutableStateOf(false) }` etc.), flipped to true via `Modifier.onFocusChanged { if (!it.isFocused) touched = true }` on the OutlinedTextField. Errors only render after a field has been touched, so a blank form on first open is silent — the user discovers each rule only after interacting.

Each `OutlinedTextField` receives:
- `isError = touched && !fieldValid`
- `supportingText = { if (isError) Text(errorMsg, color = SnowColors.Ember) }`

Per-field error strings:
- Name: "Enter a name"
- Category: "Choose a category"
- Monthly amount: "Enter an amount greater than 0"
- Total payments: "Total must be between 1 and 600"
- Due day: "Day must be between 1 and 31"
- Payments already made: "Must be between 0 and total payments"

The Save button:
- `enabled = isValid` (Material handles ripple/click suppression — taps on a disabled Button are a no-op)
- Visual disabled state: `colors = ButtonDefaults.buttonColors(disabledContainerColor = SnowColors.FrostMute, disabledContentColor = SnowColors.Night)`

The user can't trigger a save while invalid, so the "tap Save → flash all errors" pattern isn't built. Errors emerge as the user interacts with each field. If a future UX iteration wants the explicit "tap to learn what's wrong" pattern, wrap the disabled Button in a tap-capturing Box that flips all touched flags to true.

`DebtFormViewModel.save()` continues to return `Boolean` — once `isValid` is wired the false branch never executes in practice.

#### Delete confirmation

State:
```kotlin
var showDeleteConfirm by remember { mutableStateOf(false) }
```

Triggered from the new TopAppBar overflow menu's "Delete" item (see Section 2).

Dialog:
```kotlin
if (showDeleteConfirm) {
    AlertDialog(
        onDismissRequest = { showDeleteConfirm = false },
        title = { Text("Delete ${state.name}?", style = MaterialTheme.typography.headlineSmall) },
        text = { Text("This removes the debt and all payment history.") },
        confirmButton = {
            TextButton(onClick = {
                showDeleteConfirm = false
                vm.delete()
                onSaved()
            }) { Text("Delete", color = SnowColors.Ember) }
        },
        dismissButton = {
            TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
        },
        containerColor = SnowColors.CardElev,
        titleContentColor = SnowColors.Frost,
        textContentColor = SnowColors.FrostMute
    )
}
```

The existing bottom Cancel/Save/Delete trio in `DebtFormScreen.kt:167-170` is removed entirely as part of Section 2.

### 2. Form chrome restructure

`DebtFormScreen` becomes:

```kotlin
Scaffold(
    containerColor = SnowColors.Night,
    topBar = {
        TopAppBar(
            title = { Text(if (vm.isEdit) "Edit debt" else "New debt", style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onSaved) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = SnowColors.Frost)
                }
            },
            actions = {
                if (vm.isEdit) {
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More options", tint = SnowColors.Frost)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = SnowColors.Ember) },
                            onClick = {
                                menuOpen = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SnowColors.Night,
                titleContentColor = SnowColors.Frost,
                navigationIconContentColor = SnowColors.Frost,
                actionIconContentColor = SnowColors.Frost
            )
        )
    },
    bottomBar = {
        Surface(color = SnowColors.Night, tonalElevation = 0.dp) {
            Button(
                onClick = { if (vm.save()) onSaved() },
                enabled = vm.isValid.collectAsState().value,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SnowColors.Ice,
                    contentColor = SnowColors.Night,
                    disabledContainerColor = SnowColors.FrostMute,
                    disabledContentColor = SnowColors.Night
                )
            ) { Text("Save") }
        }
    }
) { padding ->
    LazyColumn(
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
        modifier = Modifier.padding(padding).imeNestedScroll()
    ) {
        // form fields...
    }
}
```

The form's hidden bottom-nav rule in `App.kt:83` (`if (route != Route.Form) BottomNav(...)`) stays.

"Use last day of month" Switch row gets:
```kotlin
Row(
    Modifier.semantics(mergeDescendants = true) {
        contentDescription = "Use last day of month, Feb adjusts"
    }
) { Switch(...); Text("Use last day of month (Feb adjusts)") }
```

### 3. Home polish

#### Negative LEFT OVER

`CutoffCard.kt:79` becomes:

```kotlin
val isShort = summary.breathingRoom < 0
val label = if (isShort) "SHORT BY" else "LEFT OVER"
val amount = abs(summary.breathingRoom)
val color = if (isShort) SnowColors.Ember else SnowColors.Ice
LedgerCell(label = label, amount = amount, color = color)
```

`LedgerCell` already accepts `color` as a parameter — no signature change.

#### Empty-state copy

`HomeScreen.kt:240-244` becomes:

```kotlin
val msg = when {
    summary.incomePerCutoff == 0.0 -> "Start by setting your income in Settings."
    state.payments.isEmpty()       -> "Add debts from the Debts tab."
    else                           -> "No payments due this cutoff yet."
}
Text(msg, style = MaterialTheme.typography.bodyMedium, color = SnowColors.FrostDim)
```

The Italic "Swipe left to mark paid · swipe right to undo" caption stays (and recolors per Section 5).

#### Payments-already-made field

`DebtFormScreen` becomes conditional:

```kotlin
if (!vm.isEdit) {
    // existing editable OutlinedTextField for paymentsAlreadyMade
    // label: "PAYMENTS ALREADY MADE"
    // helper: "Use this to import a debt mid-way. Backfills payment history."
}
if (vm.isEdit) {
    Column {
        Text("PAYMENTS RECORDED", style = labelSmallTracked, color = SnowColors.FrostDim)
        Spacer(8.dp)
        Text(
            "${vm.paymentsMade} of ${vm.totalPayments}",
            style = MaterialTheme.typography.bodyLarge,
            color = SnowColors.Frost
        )
    }
}
```

`DebtFormViewModel` exposes `paymentsMade` (re-read from `repos.payments.countForDebt(existing.id)`) and `totalPayments` (parsed from the field) for the edit-mode static summary.

### 4. Nitpick + small-polish pass

| Change | Location | Code |
|---|---|---|
| `formatAmount(d: Double): String` helper | new `ui/util/AmountFormat.kt` | Drops `.0` when `d.rem(1.0) == 0.0`; else two decimals. Used by `DebtFormViewModel` for monthlyAmount initialization. |
| Top-aligned amount, name wrap | `DebtsScreen.kt:83-106` | Row → `verticalAlignment = Alignment.Top`. Name `Text` → `maxLines = 2, overflow = TextOverflow.Ellipsis`. |
| PAYMENTS header gap | `HomeScreen.kt:~61` | `Spacer(2.dp)` → `Spacer(8.dp)` |
| FAB icon | `DebtsScreen.kt:136` | `Text("+", ...)` → `Icon(Icons.Outlined.Add, contentDescription = "Add debt", tint = SnowColors.Night)` |
| Bottom-nav active pill | `Nav.kt:50-69` | Wrap selected `NavigationBarItem` content in `Box(Modifier.background(SnowColors.Ice.copy(alpha = 0.10f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 4.dp))` |
| "View archived" affordance | `DebtsScreen.kt:54-59` | Row with `Text(..., textDecoration = TextDecoration.Underline)` + `Icon(Icons.Outlined.ChevronRight, ..., Modifier.size(16.dp))` |
| Cutoff DUE spacing | `CutoffCard.kt:63-70` | `Spacer(8.dp)` above amount → `Spacer(12.dp)`; `Spacer(24.dp)` below → `Spacer(20.dp)` |
| Debts row progress | `DebtsScreen.kt` (subtitle Text) | Append `· ${paymentsMade}/${totalPayments} paid` to existing subtitle. `paymentsMade` exposed by `DebtsViewModel`. |

Form field labels stay ALL-CAPS labelSmall with 3sp tracking — brand identity.

### 5. Accessibility

#### Contrast

`Color.kt` changes:

```kotlin
// Before
val FrostDim = Color(0xFF5E6874)  // 2.7:1 — fails AA

// After
val FrostDim = Color(0xFF7A8696)  // ~4.6:1 — passes AA
```

The single value change cascades to PAYMENTS / INCOME / DUE section labels, inactive amount color, dropdown chevron, form helper text — every existing FrostDim reference.

```kotlin
// Line divider alpha
val Line = Color(0x24FFFFFF)  // bumped from 0x12FFFFFF
```

`FrostDeep #3C4452` retained in palette but no `Text(...)` uses it after this pass:
- Home swipe caption → `FrostMute` (covered in Section 4 via recolor, applied here)
- Form placeholder "0" on Payments-already-made → `FrostDim` (placeholder hint)
- Settings "Snowball v0.1" stamp → `FrostMute`

#### Screen-reader semantics

Each form `Field` column (`DebtFormScreen.kt`) gets:

```kotlin
Column(
    Modifier.semantics(mergeDescendants = true) {
        contentDescription = labelString  // e.g. "Name", "Monthly amount"
    }
) {
    Text(labelString, style = labelSmallTracked)
    OutlinedTextField(...)
}
```

Removes NAF flag for every field including the Notes textarea.

Payment row in `HomeScreen.kt:~115` (the SwipeToDismissBox content):
```kotlin
Modifier.semantics {
    role = Role.Checkbox
    stateDescription = if (row.isPaidThisCycle) "Paid" else "Not paid"
    contentDescription = "${row.debtName}, ${formatAmount(row.amount)} pesos, day ${row.dueDay}"
}
```

`PesoText.kt` outer Row gets:
```kotlin
Modifier.semantics(mergeDescendants = true) {
    contentDescription = "${amount.toLong()} pesos"
}
```

FAB content description handled by Section 4's icon swap.

#### Dynamic text scaling

Home payment list LazyColumn (`HomeScreen.kt`) gains:
```kotlin
LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), ...) { ... }
```

Form's LazyColumn gains `Modifier.imeNestedScroll()` (covered in Section 2).

## File-level change inventory

Compose UI (`composeApp/src/commonMain/kotlin/com/snowball/`):
- `ui/screens/HomeScreen.kt` — empty-state copy, swipe caption recolor, payment row semantics, LazyColumn bottom padding, PAYMENTS header spacer
- `ui/screens/DebtsScreen.kt` — top-aligned amount, name wrap, FAB icon, View archived affordance, subtitle progress, debt row no changes elsewhere
- `ui/screens/DebtFormScreen.kt` — Scaffold restructure (TopAppBar + bottom bar), AlertDialog, conditional paymentsAlreadyMade field, field semantics wrappers, removed bottom button trio
- `ui/screens/SettingsScreen.kt` — empty placeholder pattern, formatted display when unfocused, commit-on-blur with check ack, version stamp recolor
- `ui/components/CutoffCard.kt` — LEFT OVER / SHORT BY conditional, DUE spacing
- `ui/components/PesoText.kt` — mergeDescendants semantics
- `ui/nav/Nav.kt` — active-tab pill
- `ui/theme/Color.kt` — FrostDim value, Line alpha
- `ui/util/AmountFormat.kt` — new file, `formatAmount(d: Double): String`

ViewModels:
- `ui/viewmodel/DebtFormViewModel.kt` — `isValid: StateFlow<Boolean>`, per-field validity, `paymentsMade`/`totalPayments` exposure for edit-mode summary, `formatAmount` used in init
- `ui/viewmodel/DebtsViewModel.kt` — expose per-debt `paymentsMade` (already derivable, just needs surfacing)
- `ui/viewmodel/SettingsViewModel.kt` — commit-on-blur split from continuous edit state (edit state local to screen, commit fn on VM)

No domain or data layer changes. No schema migrations. No new dependencies.

## Testing strategy

- **Unit tests:** `DebtFormViewModel.isValid` combinations (each field individually invalid → false; all valid → true; boundary cases on numeric ranges). One new test class `DebtFormViewModelValidationTest` in `androidUnitTest`.
- **Unit tests:** `formatAmount` covering 0.0, integer doubles, fractional doubles, large numbers. New `AmountFormatTest` in `commonTest`.
- **Manual test on emulator:** Re-run the inspector's full flow checklist (form save with empty fields, delete confirmation, negative LEFT OVER by adding debts > income, font_scale 1.30 with 3+ debts, TalkBack enabled walkthrough of one new-debt creation).
- **No new repository or domain tests** — no data-layer changes.

## Open questions

None at design time.

## Risks

- The visual lift on `FrostDim` from `#5E6874` to `#7A8696` is a small but real shift in the brand's mid-gray. If it reads as "too light" against the Night background, the alternative is a per-usage palette: add a new `FrostDimAccessible` token and migrate text usages while leaving the icon/chevron uses on the old value. Lean toward the single-token change first; reassess on the emulator.
- The `OutlinedTextField` semantics-via-wrapper-Column approach preserves the visual identity but is non-standard. If a TalkBack edge case (e.g. error state announcement) misbehaves, fallback is to refactor to Material 3's built-in `label = { Text(...) }` slot, which would change the visual to a floating label. Document if this lands as a follow-up.
- AlertDialog uses `MaterialTheme.colorScheme.surface` for its default `containerColor` and we override to `SnowColors.CardElev`. Verify no other dialog usage in the app expects the Material default.

## Success criteria

- All in-scope items from `docs/design/ux-review-2026-05-12.md` (20 numbered items plus the 5-item accessibility set) resolved with code under HEAD and committed.
- Re-running uiautomator dumps shows zero NAF flags on form fields.
- WCAG AA contrast passes for every body text usage (FrostDim ≥ 4.5:1, no FrostDeep text).
- At `font_scale = 1.30` the bottom payment row maintains ≥ 16dp clearance from the bottom-nav.
- Delete confirmation dialog cannot be bypassed by a single tap; Save button is visually disabled until form valid.
- `:composeApp:test` passes (no regressions in existing 36 tests + new validation/format tests).
