# UI Polish Pass — Spark & Surprise Design

**Date:** 2026-05-13
**Status:** Approved (autonomous mode, skip-verification standing instruction)
**Target tag:** v0.2.11

## Problem

v0.2.10 ships with selective motion polish (PesoText auto-count, ProgressArc fill, route transitions, StaggeredItem cascade on Home), but several high-traffic surfaces still feel static — the navbar is the worst offender, and the add-debt form is utilitarian. The user wants "spark" and "surprise elements" across small everyday interactions.

This pass is breadth-over-depth: apply the existing motion language (FastOutSlowInEasing tween 200–600ms, StaggeredItem 40ms cascade) to surfaces that don't have it yet, plus add a reusable press-scale interaction for buttons and cards.

## Non-goals

- New animation primitives or third-party libraries — reuse the four animation APIs already in use (`animateFloatAsState`, `animateIntAsState`, `AnimatedVisibility`, `AnimatedContent`).
- Cross-platform deltas — everything must stay in `commonMain` and compile on Android.
- Performance regressions — keep all animations short (≤ 400ms) and avoid running them on offscreen lists.

## Surfaces & Treatments

### 1. BottomNav (Nav.kt)

**Current:** Active tab gets a static pill background + tint change. Icon does not scale. No indicator slides.

**Treatment:**
- **Animated active-icon scale.** `animateFloatAsState` from 1.0 → 1.15 on the active icon (300ms FastOutSlowInEasing). Inactive snaps back to 1.0.
- **Animated pill background alpha + color.** `animateColorAsState` on the background color (200ms) so the pill fades on/off rather than snapping.
- **Tap pulse.** Brief scale 1.0 → 0.92 → 1.0 (150ms total) when the user taps any tab — uses InteractionSource + animateFloatAsState. Gives the bar tactile feel.

### 2. DebtFormScreen (DebtFormScreen.kt)

**Current:** Fields appear instantly as a static column. Save button is plain. Conditional "Payments already made" vs "Payments recorded" branch snaps.

**Treatment:**
- **Staggered field entry.** Wrap each Field block in `StaggeredItem(index = N)` so the form cascades in (~40ms × index, capped at 8 — same as Home). Indexes assigned in render order.
- **AnimatedVisibility on the conditional branch** (`!vm.isEditing` block vs `else` block). `fadeIn() + expandVertically()` / `fadeOut() + shrinkVertically()`.
- **Save button press scale.** Reuse the new `pressScale` modifier (see below) to scale 1.0 → 0.96 → 1.0 on tap.

### 3. DebtsScreen (DebtsScreen.kt)

**Current:** Debt rows render instantly. FAB has default ripple only. Empty state is static.

**Treatment:**
- **Row cascade.** Wrap each debt-row in `StaggeredItem` so rows fan in.
- **FAB press scale.** `pressScale` on the FAB.
- **Empty state.** `AnimatedVisibility(fadeIn + slideInVertically)` wrapping the "no debts yet" placeholder.

### 4. DebtDetailScreen

**Current:** Cards render instantly.

**Treatment:**
- **Staggered card entry** on the main cards (summary card, payments card, notes card, etc.) via `StaggeredItem`.

### 5. Empty states — HomeScreen, InsightsScreen

**Current:** Centered static text.

**Treatment:**
- Wrap empty-state Box in `AnimatedVisibility` with `fadeIn() + slideInVertically(initialOffsetY = { it / 4 })`. 350ms tween.

### 6. CutoffCard + SnapshotCard

**Current:** Render instantly within their parent column.

**Treatment:**
- Wrap in `StaggeredItem(index = 0)` so they slide in with the cascade. If already inside one on Home, leave that single index and increment subsequent items.

### 7. Reusable `pressScale` modifier (NEW)

**File:** `composeApp/src/commonMain/kotlin/com/snowball/ui/components/PressScale.kt`

A composable `Modifier` extension that observes a passed `MutableInteractionSource`, animates a scale state between 1.0 and 0.96 on press, and applies the result via `Modifier.scale(...)`. Used on Save button, FAB, primary cards, and tab targets.

## Risks

- **Over-animation fatigue.** Mitigated by keeping all motion short (< 400ms) and consistent with the existing FastOutSlowInEasing language. No bouncy/playful overshoot.
- **List perf.** Mitigated by `StaggeredItem` already capping delay at `min(index, 8)` — large lists don't pile up delay.
- **Cross-platform breakage.** Mitigated by using only the four APIs we've verified work in commonMain CMP 1.8.0.

## Acceptance

- BottomNav: active icon visibly scales up when selected; pill fades; tap produces visible press-pulse.
- DebtFormScreen: fields cascade in on screen open; switching between "new debt" / "edit debt" conditional fields animates; Save button scales on tap.
- DebtsScreen: debt rows cascade in; FAB scales on tap; empty state fades+slides.
- DebtDetailScreen: cards cascade in.
- Empty states animate in across Home/Debts/Insights.
- No regression in existing animations (PesoText, ProgressArc, route transitions, JourneyCard counter).
- Builds and installs on Pixel_7 emulator + S25 sideload APK.

## Out of scope (future v0.3 polish if user asks)

- Custom haptic feedback (needs `expect/actual` per platform)
- Coachmark for swipe-to-pay (still on backlog)
- Animated empty-state illustrations (would need vector assets)
- Long-press quick-edit
- Light mode

## Tagging

v0.2.11. Push immediately after commit.
