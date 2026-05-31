# Snowball v0.4.0 — "Momentum" Identity Redesign

**Date:** 2026-06-01
**Status:** Approved (identity chosen: Momentum; autonomous execution)
**Target tag:** v0.4.0

## Identity

**Momentum** — Snowball = the debt-snowball *method*: a ball rolling downhill, gathering mass and speed, flattening debts. The product's emotional job is to make the user feel **unstoppable** and to **reward progress**. The base stays a calm dark surface (so it's livable day-to-day), but it's energized with a vivid accent, confident type, and a signature "the snowball is rolling" reward on every payoff.

Folded in (from the v0.4 UI inspection, because they all serve "progress you can see"): a unified screen header, consistent date/money formatting, a rebalanced cutoff card with visible payment progress, sans-serif list names, card depth, and bottom-nav separation.

## Design tokens

### Color — revalue `SnowColors` (propagates app-wide)

```kotlin
val Night      = Color(0xFF0A0E16)
val NightElev  = Color(0xFF111A2B)
val CardElev   = Color(0xFF16223A)
val Line       = Color(0x1AFFFFFF)
val LineStrong = Color(0x2EFFFFFF)
val TopHighlight = Color(0x26FFFFFF)   // NEW — 1px top-edge highlight for glassy depth
val Frost      = Color(0xFFF3F6FA)
val FrostMute  = Color(0xFFA6B2C4)
val FrostDim   = Color(0xFF76839A)
val FrostDeep  = Color(0xFF3A4356)
val Ice        = Color(0xFF5B8DEF)     // primary "Velocity" (revalued from soft blue)
val IceSoft    = Color(0x335B8DEF)
val Charge     = Color(0xFF6FE3CE)     // NEW — momentum/streak/cleared accent (mint-cyan)
val ChargeSoft = Color(0x2E6FE3CE)     // NEW
val Champagne  = Color(0xFFE8C68A)
val Ember      = Color(0xFFE07856)
val Green      = Color(0xFF8FD9B2)
```

Theme mapping unchanged in structure: `primary = Ice`, `secondary = Charge`, `tertiary = Ember`, `error = Ember`.

### Type — replace fonts (`Type.kt`)

Fonts already added to `composeResources/font/`: `SpaceGrotesk-Variable.ttf` (→ `Res.font.SpaceGrotesk_Variable`) and `Inter-Variable.ttf` (→ `Res.font.Inter_Variable`).

- **Space Grotesk** = display family (display*/headline* + hero numbers + screen titles) — kinetic, distinctive digits.
- **Inter** = body family (title*/body*/label* + list-row names) — neutral, scannable. (Fixes the "serif for list names" issue.)
- Momentum favors **confident** numerals, not thin. Revalue weights and cap the hero:
  - displayLarge 64sp **W600** (was 96 W300), displayMedium 46sp W600, displaySmall 30sp W600
  - headlineLarge 26sp W600, headlineMedium 22sp W600, headlineSmall 18sp W600
  - title/body/label families → Inter, weights unchanged (Medium/Normal), keep letter-spacing on labels.
- Remove the `FraunsesItalic`/Fraunces/DM Sans accessors once unused.

Components that hardcoded `.copy(fontWeight = W300)` on the hero (CutoffCard, SnapshotCard, JourneyCard) must drop the W300 override so the new confident weight shows.

## Components & screens

### 1. `ScreenHeader` (NEW, unifies all four tabs)

`composeApp/src/commonMain/kotlin/com/snowball/ui/components/ScreenHeader.kt`:
A `Row` = snowflake (`Icons.Outlined.AcUnit`, 22dp, `Ice`) + `Spacer(10dp)` + title (`headlineLarge`, `Frost`). One consistent top treatment. Applied at the top of Home, Debts, Insights, Settings with identical leading padding. Insights drops its `TopAppBar` in favor of this inline header (matching the others). Title per tab: Home = "Snowball"; Debts = "Active"/"Archived"; Insights = "Insights"; Settings = "Settings".

### 2. CutoffCard rebalance + momentum progress

- Hero DUE number: drop the `W300` override (now Space Grotesk W600), and it inherits the capped `displayLarge` 64sp.
- **NEW — cutoff progress bar:** below the DUE number, a slim rounded bar (height 8dp, `ChargeSoft` track, `Charge` fill) showing `summary.paidTotal / summary.dueTotal`, with a caption `"₱{paid} of ₱{due} paid this cutoff"`. Animate the fill with `animateFloatAsState`. When `dueTotal == 0`, hide the bar. This is the Momentum signature on the home hero.
- **Demote INCOME:** remove the big INCOME ledger cell; show income as a small caption (`"Income ₱{x}"`, `bodySmall`, `FrostDim`).
- **LEFT OVER / SHORT BY:** keep as a single secondary number (`headlineMedium`), colored `Ice` (left over) or `Ember` (short).
- Card gains a top-edge highlight (see Depth).

### 3. Momentum signature — Journey → "Momentum" treatment + payoff reward

- **JourneyCard** becomes momentum-forward: keep `%` cleared (animated, now `Charge`), add a **streak line** — count of consecutive cleared payment rows in the current + prior cutoff is out of scope to compute precisely; instead show `"{n} cleared"` where `n = totalMelted / typical` is not reliable, so use a simpler, correct metric: **number of debts fully paid off** (archived scheduled debts) as `"{k} knocked out"`, plus the existing "Free by {date}". Keep it truthful and simple.
- **`celebratePaid` upgrade** (`components/CelebratePaid.kt`): on false→true, in addition to the existing scale-pop, add a brief **`Charge` glow ring** that expands and fades (a "the snowball just rolled" pulse). Heavier, more kinetic than the current frost glow, using `Charge`.
- **`ProgressArc`** filled state uses `Charge` (was Ice) so "paid" reads as momentum-green-cyan and the primary `Ice` stays reserved for interactive/primary.

### 4. Formatting consistency

- Home payment row subtitle: `"Due ${row.effectiveDueDate}"` (raw ISO) → `"Due ${formatLongDate(row.effectiveDueDate)}"`.
- Payoff timeline (`InsightsScreen.PayoffTimelineRow`): monthly amount `"%.2f".format(...)/mo` → `"₱${formatAmountWithSeparators(row.monthlyAmount)}/mo"`; the ISO `row.endDate.toString()` sub-label → `formatLongDate(row.endDate)`.
- Audit other `LocalDate.toString()` / bare `%.2f` leaks in UI and route through the formatters.

### 5. Depth + nav separation

- **Card top highlight:** key cards (CutoffCard, JourneyCard, SnapshotCard, UpNextCard) get a subtle 1px top highlight. Implement as a thin `TopHighlight`→transparent vertical gradient overlay at the top edge, or a top border. Keep subtle.
- **Bottom nav:** add a 1px `LineStrong` hairline along the top edge of `BottomNav` so it separates from scrolling content. Active tab tint → `Ice`; keep pill + scale.

## Non-goals

- No new screens or features; this is a visual/identity pass over existing surfaces.
- No light mode.
- No data/domain changes (cutoff math etc. untouched).
- Onboarding copy stays; only colors/type inherit.

## Risks

- **Palette revalue touches everything** but is centralized in `SnowColors`, so it propagates; risk is purely aesthetic (verified on-device, not by screenshots here).
- **Variable-font weight rendering:** mirror the existing single-`Font` family pattern; if a weight doesn't visibly vary, it still renders (no crash).
- **Contrast:** `Ice` #5B8DEF on `Night` is ~5:1; `Charge` #6FE3CE is bright (high contrast). Body text colors unchanged.

## Acceptance

- App builds; all unit tests still green (no domain changes).
- Every tab uses the same header treatment.
- Cutoff card shows an animated paid/due progress bar; income is a caption; left-over is the secondary figure.
- Marking a payment fires the kinetic Charge "roll" reward.
- No raw ISO dates or `%.2f` amounts anywhere in the UI.
- Bottom nav is visually separated; cards have a subtle top highlight.
- Fonts are Space Grotesk (display) + Inter (body) throughout; Fraunces/DM Sans removed.
- APK builds and installs.

## Tag

v0.4.0 — first identity-level release.
