# v0.2.12 Polish Pass 2 — Identity, Spark & Voice

**Date:** 2026-05-13
**Status:** Approved (autonomous mode)
**Target tag:** v0.2.12
**Builds on:** v0.2.11 (`docs/superpowers/specs/2026-05-13-ui-polish-pass-design.md`)

## Why a second pass

v0.2.11 fixed the specific surfaces the user called out (navbar, add-debt form). A subsequent whole-app audit surfaced gaps the first pass didn't touch: no branded splash, no snowflake identity inside the app chrome, several state-change moments that snap (OVERDUE block, payment counts, settings save), inconsistent empty-state copy voice, and zero celebration on key milestones (marking paid, completing a debt).

This pass tackles **identity**, **motion gaps the first pass missed**, **spark moments**, and **copy continuity**. Out of scope for v0.2.12: loading shimmer (architectural), custom branded transitions (deferred), semantic color token refactor (refactor with low user-visible payoff).

## Surfaces & Treatments

### A. Identity

#### A1. Android splash screen

**Current:** `MainActivity` calls `enableEdgeToEdge()` then immediately `setContent { App() }`. Cold start shows a brief black/system-default frame.

**Treatment:** Use `androidx.core:core-splashscreen` 1.0.x. Splash background = `SnowColors.Night` (#0E1620 or similar), splash icon = the existing app launcher icon (snowflake). Manifest theme `Theme.App.Starting` extends `Theme.SplashScreen` with the snowflake foreground + Night background. `MainActivity.onCreate` calls `installSplashScreen()` before `super.onCreate` so the system shows the splash until the Compose tree's first frame is ready.

**Acceptance:** Cold-launching the app on Android shows the snowflake on a Night background for ~300–600ms before the home screen renders.

#### A2. Snowflake glyph in main-tab TopAppBars

**Current:** Home/Debts/Insights/Settings TopAppBars (or their equivalent header treatments) carry plain text titles.

**Treatment:** Add a small `Icons.Outlined.AcUnit` (already used for the Home tab icon) at 20.dp tinted `SnowColors.Frost` to the left of each main tab's title text or header. Settings keeps its existing header style — add the glyph only to Home, Debts, and Insights tab headers.

**Acceptance:** Each of the three primary tabs shows a small snowflake next to its title.

#### A3. "All clear ✓" → snowflake celebration

**Current:** `JourneyCard.kt` (or wherever the "All clear" text lives) renders as plain `bodyMedium` text.

**Treatment:** Replace the `✓` with `Icons.Outlined.AcUnit` tinted `SnowColors.Ice`. Wrap the row in an `AnimatedVisibility` (or apply an `animateFloatAsState` scale 0.8 → 1.0 + alpha 0 → 1, 400ms FastOutSlowInEasing) so it animates in when the state flips to "all clear". On Insights "All clear ✓" forecast row tail markers, apply the same treatment.

**Acceptance:** "All clear" moments visibly fade + scale in with a snowflake glyph instead of a checkmark.

### B. Motion gaps the first pass missed

#### B1. Animated payment count on DebtDetailScreen

**Current:** `Text("${state.paymentsMade} of ${state.debt.totalPayments}")` — plain Text. Jumps on mark-paid/undo.

**Treatment:** Use `animateIntAsState` to animate `paymentsMade` numerically (600ms FastOutSlowInEasing). Render the animated value.

#### B2. OVERDUE block fade-in

**Current:** OVERDUE card appears instantly when overdue debts exist.

**Treatment:** Wrap in `AnimatedVisibility(visible = overdueList.isNotEmpty(), enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically())`. Keep the existing card layout inside the wrapper.

#### B3. Settings save checkmark fade-in

**Current:** Save acknowledgement checkmark icon appears and disappears instantly when income is saved.

**Treatment:** Wrap the checkmark in `AnimatedVisibility(visible = showCheckmark, enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.6f, animationSpec = tween(200)), exit = fadeOut(tween(200)))`.

### C. Spark / micro-celebrations

#### C1. Payment mark-paid celebration

**Current:** Marking a payment paid (via swipe or arc tap) updates the row visually but produces no celebratory feedback.

**Treatment:** When a row's `paid` state flips from false to true, briefly scale that row to 1.04 (200ms out, 200ms back) AND glow its background with `SnowColors.Ice.copy(alpha = 0.18f)` that fades to transparent over 600ms. Implement as a `Modifier.celebratePaid(triggerKey: Any)` extension in a new file `composeApp/src/commonMain/kotlin/com/snowball/ui/components/Celebrate.kt`. The trigger key is `paid` state — when it flips true, the animation plays once. Reverting (undo) doesn't trigger.

**Acceptance:** Swiping a payment row to paid produces a brief scale pop + frost glow.

### D. Voice + tone unification

#### D1. Empty-state copy normalization

**Current (audit findings):**
- HomeScreen: "Start by setting your income in Settings."
- DebtsScreen (no debts): "No debts yet. Tap + to add your first."
- DebtsScreen (archived): "Nothing archived yet."
- InsightsScreen: "No upcoming debts in your forecast window."

**Treatment:** Unify to a "calm, action-cueing" tone:
- HomeScreen empty income: "Set your income in Settings to get started."
- DebtsScreen no debts: "No debts yet — tap ＋ to add your first."
- DebtsScreen archived: "No archived debts yet."
- InsightsScreen: "Nothing on the horizon — you're caught up."

(Keep en-dashes for the gentle "—" pause where it reads naturally.)

#### D2. Error-message tone normalization

**Current (audit findings):**
- "Enter an amount greater than 0" ← starts with "Enter"
- "1 to 600" ← bare range
- "1 to 31" ← bare range
- "Must be between 0 and total payments"

**Treatment:** All field-validation errors use the form "Enter ..." for input-prompts, or be a one-line plain assertion if not a prompt:
- "Enter an amount greater than 0" (keep)
- "1 to 600" → "Enter 1 to 600"
- "1 to 31" → "Enter 1 to 31"
- "Must be between 0 and total payments" (keep — it's an assertion, not an instruction)

## Non-goals

- Loading shimmer / skeleton (architectural — requires async state separation; deferred)
- Custom branded route transitions (the generic slide+fade is fine; snowflake-particle transitions are visually distracting and over-engineered)
- Semantic color token refactor (Color.kt is fine for now; not user-visible)
- Onboarding flow / welcome screen (deferred to v0.3)
- Coachmarks (deferred)
- Haptic feedback (needs expect/actual — deferred)

## Risks

- **Splash screen API.** `androidx.core:core-splashscreen` needs gradle dep added; manifest theme update required. Build risk if the dep doesn't resolve. Mitigation: implementer to confirm gradle catalog has it or add it; fall back to manual launch theme if needed.
- **Celebration over-fires.** If the celebrate animation triggers on every recomposition rather than the true state-flip, it'll feel buggy. Mitigation: gate on `LaunchedEffect(triggerKey)` with previous-value comparison.
- **Snowflake glyph clutter.** Adding `AcUnit` to every TopAppBar could feel busy. Mitigation: only Home / Debts / Insights main tabs, never on sub-screens.

## Acceptance

- Cold app launch on emulator shows snowflake-on-Night splash before home.
- Home, Debts, Insights tab headers show a small snowflake left of their titles.
- "All clear" moments use a snowflake glyph and animate in.
- DebtDetailScreen's "X of Y" payment count animates when a payment is recorded.
- OVERDUE block fades in/out instead of snapping.
- Settings save checkmark fades+scales in.
- Marking a payment paid produces a brief scale + frost glow on the row.
- Empty-state and error copy reads cleanly and consistently.
- Builds; installs to Pixel_7 emulator; sideload APK to user's S25 path printed.

## Tagging

v0.2.12. Push immediately after commit.
