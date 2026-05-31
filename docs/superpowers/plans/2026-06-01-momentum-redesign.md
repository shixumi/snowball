# Snowball v0.4.0 — Momentum Redesign Implementation Plan

> Execute via superpowers:subagent-driven-development. Steps use `- [ ]`.

**Goal:** Adopt the "Momentum" identity (vivid palette, Space Grotesk + Inter, kinetic payoff reward, visible progress) and fold in the inspection fixes (unified header, formatting, cutoff rebalance, depth, nav separation).

**Spec:** `docs/superpowers/specs/2026-06-01-momentum-redesign-design.md`

**Tech:** Kotlin Multiplatform · Compose Multiplatform 1.8.0. Fonts already in `composeResources/font/` (`SpaceGrotesk-Variable.ttf`, `Inter-Variable.ttf`).

**Build preamble (every gradle call):**
```
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; $env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
```
Git at `C:\Program Files\Git\cmd`. Commit messages via `git commit -F <file>` (apostrophes break PS here-strings). Never `Read` a `.png`.

---

## Task 1 — Design tokens (color + type)

**Files:** `ui/theme/Color.kt`, `ui/theme/Type.kt`

- [ ] Replace `SnowColors` values with the Momentum palette from the spec (add `TopHighlight`, `Charge`, `ChargeSoft`).
- [ ] In `Type.kt`: add `spaceGrotesk()` = `FontFamily(Font(Res.font.SpaceGrotesk_Variable))` and `inter()` = `FontFamily(Font(Res.font.Inter_Variable))`. Set `display = spaceGrotesk()`, `body = inter()`. Update sizes/weights: displayLarge 64/W600, displayMedium 46/W600, displaySmall 30/W600, headlineLarge 26/W600, headlineMedium 22/W600, headlineSmall 18/W600; title/body/label use `inter()` with existing weights + letter-spacing. Remove Fraunces/DMSans accessors + `FraunsesItalic` (and delete the two old `.ttf` files).
- [ ] Build `:composeApp:compileDebugKotlinAndroid`. Commit.

## Task 2 — Unified `ScreenHeader`

**Files:** create `ui/components/ScreenHeader.kt`; modify HomeScreen, DebtsScreen, InsightsScreen, SettingsScreen.

- [ ] `ScreenHeader(title: String, modifier: Modifier = Modifier)` = Row(AcUnit 22dp Ice + Spacer(10dp) + Text(title, headlineLarge, Frost)).
- [ ] Replace each tab's bespoke header with `ScreenHeader(...)`. Insights: remove `TopAppBar`/`Scaffold` chrome, use a scrolling `Column` with `ScreenHeader("Insights")` at top like the others (keep its content). Debts keeps the "View archived" toggle to the right of the header row. Build. Commit.

## Task 3 — Formatting consistency

**Files:** HomeScreen (payment row), InsightsScreen (PayoffTimelineRow), grep for other leaks.

- [ ] Home row: `"Due ${formatLongDate(row.effectiveDueDate)}"` (+ the semantics contentDescription).
- [ ] PayoffTimelineRow: amount → `"₱${formatAmountWithSeparators(row.monthlyAmount)}/mo"`; sub-label → `formatLongDate(row.endDate)`.
- [ ] Grep `\.toString()` on LocalDate and `%.2f` in `ui/`; fix any other leaks. Build. Commit.

## Task 4 — CutoffCard rebalance + progress bar

**Files:** `ui/components/CutoffCard.kt`

- [ ] Drop `W300` override on the DUE PesoText (inherits Space Grotesk W600 displayLarge 64).
- [ ] Add an animated progress bar (height 8dp, RoundedCornerShape(4dp), track `ChargeSoft`, fill `Charge`, fraction = `paidTotal/dueTotal` via `animateFloatAsState`); caption `"₱{paid} of ₱{due} paid this cutoff"` (bodySmall, FrostMute). Hide when `dueTotal <= 0`.
- [ ] Replace the two stacked INCOME/LEFT OVER `LedgerCell`s with: a small caption `"Income ₱{incomePerCutoff}"` (bodySmall, FrostDim) + a single secondary figure for LEFT OVER/SHORT BY (`headlineMedium`, `Ice`/`Ember`). Build. Commit.

## Task 5 — Momentum reward + accents

**Files:** `components/CelebratePaid.kt`, `components/ProgressArc.kt`, `components/JourneyCard.kt`.

- [ ] `celebratePaid`: keep scale-pop; swap the frost glow to a `Charge` glow and make it a touch stronger (alpha ~0.22). (Optional: an expanding ring — keep simple if risky.)
- [ ] `ProgressArc`: filled/active color → `Charge` (was Ice).
- [ ] JourneyCard: `%` number color → `Charge`; keep "melted" + "Free by"/"All clear". (Truthful metric only — no fabricated streaks.) Build. Commit.

## Task 6 — Depth + nav separation

**Files:** `components/CutoffCard.kt`, `JourneyCard.kt`, `UpNextCard.kt`, `insights/InsightsScreen.kt` (SnapshotCard), `ui/nav/Nav.kt`.

- [ ] Add a subtle top-edge highlight to the key cards: overlay a `Brush.verticalGradient(0f to SnowColors.TopHighlight, 0.5f to Color.Transparent)` ~2dp tall at the top inside the clip, OR a 1px top border in `TopHighlight`. Keep subtle, don't double-stack borders.
- [ ] `BottomNav`: add a 1px `LineStrong` divider `Box` across the top edge before the row. Active tint already animates — point it at `Ice`. Build. Commit.

## Task 7 — Build APK, tag, push (controller)

- [ ] `:composeApp:assembleDebug`; run `:composeApp:testDebugUnitTest` (must stay green — no domain changes).
- [ ] Tag `v0.4.0`; push main + tag; report APK path.

## Notes for implementers
- Only touch listed files per task. Mirror existing patterns.
- Everything references `SnowColors` tokens, so Task 1 propagates automatically; later tasks only adjust structure/weights.
- If a build fails, fix + retry (≤3). Report DONE + SHA or BLOCKED.
