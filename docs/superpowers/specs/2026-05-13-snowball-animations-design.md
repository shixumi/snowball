# Snowball — Motion / Animation Polish

**Date:** 2026-05-13
**Scope:** Five small animation passes across the existing app. No new screens, no new data. Everything runs on Compose's built-in animation APIs (`animateFloatAsState`, `AnimatedVisibility`, `AnimatedContent`).

**Autonomous-design note:** Design calls made without per-section approval per user direction.

## What gets animated

| # | Effect | Where | How |
|---|---|---|---|
| 1 | Number counter tween | `PesoText` (opt-in), Journey `% cleared` | `animateFloatAsState` on the value, format on every frame |
| 2 | Progress arc smooth fill | `ProgressArc` (always) | `animateFloatAsState` on `progress` param, internal |
| 3 | Up next chevron rotation | `UpNextCard` header chevron | `animateFloatAsState` 0°↔180° on `isExpanded` |
| 4 | List entry stagger | Home payment rows, Insights forecast rows | per-row `AnimatedVisibility` + `LaunchedEffect(delay(i * 40ms))` |
| 5 | Route transitions | `App.kt` `when (route)` | wrap in `AnimatedContent`, slide+fade |

## What stays static

- Bottom-nav tab switching — instant. Users tap nav rapidly to compare screens; an animation here would feel laggy.
- Form fields (OutlinedTextField focus, isError) — Material 3's built-in transitions are sufficient.
- AlertDialog open/close — Material 3 default scale-fade.
- FAB tap → ripple — Material default.

## Section 1 — PesoText animated counter

Add an opt-in `animate: Boolean = false` parameter to `PesoText`. When true, the internal display value uses `animateFloatAsState` keyed on `amount`. The auto-shrink logic continues to operate on the *target* string (full final amount) so the layout doesn't keep recomputing — the display value just smoothly counts up to it.

```kotlin
@Composable
fun PesoText(
    amount: Double,
    style: TextStyle,
    modifier: Modifier = Modifier,
    pesoColor: Color = SnowColors.FrostDim,
    numberColor: Color = MaterialTheme.colorScheme.onBackground,
    align: TextAlign = TextAlign.Start,
    animate: Boolean = false,
) {
    val displayAmount = if (animate) {
        val animated by animateFloatAsState(
            targetValue = amount.toFloat(),
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            label = "pesoCount",
        )
        animated.toDouble()
    } else amount
    // ... existing measurement & rendering using `displayAmount` for the displayed string
    // ... but feed `amount` (the target) to the auto-shrink measurer so width stays stable
}
```

**Opt-in callers:**
- `CutoffCard` DUE PesoText → `animate = true`
- `CutoffCard` ledger cells (INCOME, LEFT OVER / SHORT BY) → `animate = true`
- `InsightsScreen` SnapshotCard remaining → `animate = true`
- Journey card `%` line (separate Text, not PesoText) → see Section 1b

Payment rows on Home, UpNextCard rows, debt list amounts, MISC entries → stay non-animated. Those are static identifiers, not running totals.

### 1b — Journey `% cleared`

The Journey card renders `"${stats.percentCleared}%"` as a plain `Text`. Wrap the integer in an `animateIntAsState` and use that for display:

```kotlin
val animatedPct by animateIntAsState(
    targetValue = stats.percentCleared,
    animationSpec = tween(600, easing = FastOutSlowInEasing),
    label = "percentCleared",
)
Text("$animatedPct%", style = ..., color = SnowColors.Ice)
```

## Section 2 — ProgressArc smooth fill

`ProgressArc(progress: Float, modifier: Modifier)` currently draws an arc with the given progress immediately. Wrap internally:

```kotlin
@Composable
fun ProgressArc(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "progressArc",
    )
    // ... existing Canvas drawing using `animated` instead of `progress`
}
```

Always on — no opt-out. The Home payment row's arc fill flips smoothly when a row is marked paid/undone; the Detail screen's big 160dp arc fills from 0 on screen entry.

## Section 3 — Up next chevron rotation

In `UpNextCard.kt`, the chevron Icon currently swaps between `ExpandMore` and `ExpandLess` icons based on `isExpanded`. Replace with a single icon + rotation:

```kotlin
val chevronRotation by animateFloatAsState(
    targetValue = if (isExpanded) 180f else 0f,
    animationSpec = tween(200, easing = FastOutSlowInEasing),
    label = "chevron",
)
Icon(
    imageVector = Icons.Outlined.ExpandMore,
    contentDescription = null,
    tint = SnowColors.FrostMute,
    modifier = Modifier.size(20.dp).rotate(chevronRotation),
)
```

Tighter than the existing icon-swap because the rotation is continuous instead of swap-on-toggle.

## Section 4 — List entry stagger

Home payment rows and Insights forecast rows fade in + slide up on first composition. Index-based `LaunchedEffect` triggers visibility with a 40ms-per-row cascade, max 8 rows (so the last row appears at most 320ms after the first — feels brisk).

Pattern (single helper composable in `ui/components/StaggeredItem.kt`):

```kotlin
@Composable
fun StaggeredItem(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(40L * minOf(index, 8))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(250)) +
                slideInVertically(
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 4 },
                ),
        exit = fadeOut(),
    ) { content() }
}
```

Callers wrap each row:

```kotlin
state.rows.forEachIndexed { i, row ->
    StaggeredItem(index = i) {
        SwipeablePaymentRow(row = row, onMarkPaid = ..., onUndo = ...)
    }
}
```

Same on Insights:

```kotlin
state.forecast.forEachIndexed { i, f ->
    StaggeredItem(index = i) { ForecastRow(f) }
}
```

Existing `key(row.debt.id)` wrappers on Home stay — they preserve identity across recomposition.

Debts list rows stay non-staggered (the list can be long; a cascade through 20+ rows would feel slow).

## Section 5 — Route transitions

Wrap the `when (val r = route)` block in `App.kt` with `AnimatedContent`. Transition spec: forward navigation (Tabs → Form/Detail/MISC/Cat) slides in from right + fades in; back navigation (Form/etc → Tabs) slides out to right + fades out.

```kotlin
AnimatedContent(
    targetState = route,
    transitionSpec = {
        val forward = initialState is Route.Tabs
        val slideDir = if (forward) AnimatedContentTransitionScope.SlideDirection.Start
                       else AnimatedContentTransitionScope.SlideDirection.End
        slideIntoContainer(slideDir, tween(250)) + fadeIn(tween(250)) togetherWith
            slideOutOfContainer(slideDir, tween(250)) + fadeOut(tween(250))
    },
    label = "route",
) { r ->
    when (r) {
        is Route.Tabs -> { /* existing Tab.* dispatch */ }
        is Route.Form -> { /* ... */ }
        is Route.DebtDetail -> { /* ... */ }
        is Route.MiscForm -> { /* ... */ }
        is Route.CategoryManagement -> { /* ... */ }
    }
}
```

The `forward` heuristic uses `initialState is Route.Tabs` — when navigating away from Tabs, slide right; when returning to Tabs, slide left (reversed). Doesn't handle Form↔Detail navigation but that pair doesn't currently exist in the app (Detail's Edit goes Tabs→Form, never Detail→Form directly).

## File-level change inventory

**New file (1):**
- `composeApp/src/commonMain/kotlin/com/snowball/ui/components/StaggeredItem.kt`

**Modified files (5):**
- `composeApp/src/commonMain/kotlin/com/snowball/ui/components/PesoText.kt` — add `animate` param
- `composeApp/src/commonMain/kotlin/com/snowball/ui/components/ProgressArc.kt` — internal animation
- `composeApp/src/commonMain/kotlin/com/snowball/ui/components/UpNextCard.kt` — chevron rotation
- `composeApp/src/commonMain/kotlin/com/snowball/ui/components/CutoffCard.kt` — opt-in animate=true on DUE + ledger cells
- `composeApp/src/commonMain/kotlin/com/snowball/ui/components/JourneyCard.kt` — animated percentage
- `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsScreen.kt` — animate=true on snapshot, StaggeredItem on forecast
- `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt` — StaggeredItem on payment rows
- `composeApp/src/commonMain/kotlin/com/snowball/App.kt` — AnimatedContent on routes

That's 1 new + 8 modified.

## Testing

No new unit tests — animations are visual and Compose's animation APIs are already well-tested. Manual emulator verification:

1. Mark a payment paid on Home → the DUE number tweens down, the LEFT OVER tweens up, the row's ProgressArc fills smoothly.
2. Tap Insights tab → snapshot remaining tweens from 0 to its value; forecast rows cascade in.
3. Tap Up next card to expand → chevron rotates smoothly, list slides down.
4. Open a debt's Detail → screen slides in from the right.
5. Tap back → screen slides out to the right.

All existing unit tests must continue to pass (no behavior changes, only display transitions).

## Risks

- **Animation budget on slower devices.** Each `animateFloatAsState` registers a recomposition observer. At app scale (≤15 animating values on Home at once) this is trivial; on very old devices could matter. Out of scope for v0.2.x; user tests on Pixel 7 emulator + Samsung S25.
- **`AnimatedContent` on routes can flash content if `Route.Tabs` and a sub-route share a layout slot.** Mitigated by the slide+fade — content moves visibly even at frame 1. If a route ends up with a janky transition, easy fallback is to drop the route transition (Section 5) and keep the per-component animations.
- **Number counter formatting overhead.** `formatAmountWithSeparators` runs on every frame of the tween (~36 frames at 600ms / 60fps). Pure string ops; cheap.

## Success criteria

- All 5 animation sections visibly active on emulator.
- No regressions in existing tests.
- App still feels responsive (taps register immediately even during animations).
- Tag `v0.2.10` pushed.
