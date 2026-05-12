# Snowball Animation Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development. Steps use `- [ ]` checkboxes.

**Goal:** Add five animation passes (number counter tweens, ProgressArc smooth fill, chevron rotation, list entry stagger, route transitions) using Compose's built-in animation APIs.

**Architecture:** Pure UI polish on top of v0.2.9. No data, ViewModel, or schema changes. One new component (`StaggeredItem`) plus opt-in `animate` flags on existing components.

**Tech Stack:** Same as v0.2.9. Compose Multiplatform animation APIs (`animateFloatAsState`, `animateIntAsState`, `AnimatedVisibility`, `AnimatedContent`).

**Reference:** Spec at `docs/superpowers/specs/2026-05-13-snowball-animations-design.md`.

---

## Build / verification recipe

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:assembleDebug
```

---

## Task 1: Small motion primitives (PesoText, ProgressArc, UpNextCard chevron, Journey %)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/PesoText.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/ProgressArc.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/UpNextCard.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/JourneyCard.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/CutoffCard.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsScreen.kt`

Bundled because each is 1–5 lines per file. All complete-and-build together.

- [ ] **Step 1: Update PesoText to support opt-in animated counter**

Read `composeApp/src/commonMain/kotlin/com/snowball/ui/components/PesoText.kt`. Add `animate: Boolean = false` parameter. Wrap the value used for the displayed string in `animateFloatAsState` when animate=true. Keep the auto-shrink measurement based on the target string (so width doesn't recompute every frame). Final shape:

```kotlin
package com.snowball.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.snowball.ui.theme.SnowColors
import com.snowball.ui.util.formatAmountWithSeparators

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
    val targetFormatted = formatAmountWithSeparators(amount)

    val displayAmount = if (animate) {
        val animated by animateFloatAsState(
            targetValue = amount.toFloat(),
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            label = "pesoCount",
        )
        animated.toDouble()
    } else amount

    val displayFormatted = if (animate) formatAmountWithSeparators(displayAmount) else targetFormatted

    BoxWithConstraints(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "₱$targetFormatted"
        },
    ) {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val basePesoFontSize = style.fontSize * 0.55f
        val pesoStyle = style.copy(
            color = pesoColor,
            fontStyle = FontStyle.Italic,
            fontSize = basePesoFontSize,
        )
        val numStyle = style.copy(color = numberColor)

        // Measure against the TARGET so width stays stable during the count-up.
        val pesoWidthPx = measurer.measure("₱", style = pesoStyle).size.width
        val numberWidthPx = measurer.measure(targetFormatted, style = numStyle).size.width
        val gapPx = with(density) { 2.dp.toPx() }
        val totalWidthPx = pesoWidthPx + gapPx + numberWidthPx
        val availablePx = constraints.maxWidth.toFloat()

        val scale = if (availablePx > 0f && totalWidthPx > availablePx) {
            availablePx / totalWidthPx
        } else 1f

        val finalNumStyle = numStyle.copy(fontSize = style.fontSize * scale)
        val finalPesoStyle = pesoStyle.copy(fontSize = basePesoFontSize * scale)

        Row(verticalAlignment = Alignment.Bottom) {
            Text("₱", style = finalPesoStyle, maxLines = 1, softWrap = false)
            Spacer(Modifier.width(2.dp))
            Text(displayFormatted, style = finalNumStyle, maxLines = 1, softWrap = false)
        }
    }
}
```

Key detail: the measurer measures `targetFormatted` (the final string), not `displayFormatted` (the intermediate animated string). This keeps the auto-shrink scale stable across frames so the text doesn't visibly resize during the tween.

- [ ] **Step 2: Update ProgressArc to animate internally**

Read `composeApp/src/commonMain/kotlin/com/snowball/ui/components/ProgressArc.kt`. The current signature is `ProgressArc(progress: Float, modifier: Modifier)`. Internally, wrap `progress` in `animateFloatAsState` and pass the animated value to the existing Canvas drawing logic.

Add these imports at the top:

```kotlin
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
```

In the composable body, replace direct `progress` usage. Currently the file looks something like:

```kotlin
@Composable
fun ProgressArc(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        // ... uses `progress` to compute sweep angle
    }
}
```

Wrap it:

```kotlin
@Composable
fun ProgressArc(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "progressArc",
    )
    Canvas(modifier) {
        // ... uses `animated` to compute sweep angle (replace every `progress` reference in the Canvas body)
    }
}
```

Use the Read tool first to see the exact Canvas body so you can substitute correctly.

- [ ] **Step 3: Update UpNextCard chevron to rotate**

Read `composeApp/src/commonMain/kotlin/com/snowball/ui/components/UpNextCard.kt`. Currently the chevron toggles between `Icons.Outlined.ExpandMore` and `Icons.Outlined.ExpandLess` via:

```kotlin
imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
```

Change to a single icon + rotation. Add imports:

```kotlin
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.rotate
```

Remove the `Icons.Outlined.ExpandLess` import (no longer used). Replace the chevron Icon block:

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

Place `val chevronRotation by ...` at the top of the composable scope (just after the existing `val rangeLabel = ...` etc).

- [ ] **Step 4: Update JourneyCard to animate the percent**

Read `composeApp/src/commonMain/kotlin/com/snowball/ui/components/JourneyCard.kt`. Find the line that renders `"${stats.percentCleared}%"`. Add an `animateIntAsState` wrapper.

Add imports:

```kotlin
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
```

Just before the Text that renders the percent:

```kotlin
val animatedPct by animateIntAsState(
    targetValue = stats.percentCleared,
    animationSpec = tween(600, easing = FastOutSlowInEasing),
    label = "percentCleared",
)
```

Update the Text to use `animatedPct` instead of `stats.percentCleared`:

```kotlin
Text(
    "$animatedPct%",
    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.W300),
    color = SnowColors.Ice,
)
```

- [ ] **Step 5: Opt CutoffCard ledger cells + DUE into animate=true**

Read `composeApp/src/commonMain/kotlin/com/snowball/ui/components/CutoffCard.kt`. Three PesoText call sites: the DUE hero (around the `MaterialTheme.typography.displayLarge` style) and the two `LedgerCell` (`INCOME`, `LEFT OVER` / `SHORT BY`).

The DUE PesoText call:

```kotlin
PesoText(
    amount = summary.dueTotal,
    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.W300),
    pesoColor = SnowColors.FrostMute,
    numberColor = SnowColors.Frost,
)
```

Add `animate = true`:

```kotlin
PesoText(
    amount = summary.dueTotal,
    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.W300),
    pesoColor = SnowColors.FrostMute,
    numberColor = SnowColors.Frost,
    animate = true,
)
```

Then find the `LedgerCell` composable definition (private to this file). It calls `PesoText` internally:

```kotlin
@Composable
private fun LedgerCell(label: String, amount: Double, color: Color, modifier: Modifier) {
    Column(...) {
        Text(label, ...)
        Spacer(...)
        PesoText(
            amount = amount,
            style = MaterialTheme.typography.headlineLarge,
            pesoColor = SnowColors.FrostDim,
            numberColor = color,
        )
    }
}
```

Add `animate = true` to that PesoText call too:

```kotlin
PesoText(
    amount = amount,
    style = MaterialTheme.typography.headlineLarge,
    pesoColor = SnowColors.FrostDim,
    numberColor = color,
    animate = true,
)
```

- [ ] **Step 6: Opt InsightsScreen snapshot remaining into animate=true**

Read `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsScreen.kt`. In `SnapshotCard`, find:

```kotlin
PesoText(
    amount = stats.remaining,
    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.W300),
    pesoColor = SnowColors.FrostMute,
    numberColor = SnowColors.Ice,
)
```

Add `animate = true`:

```kotlin
PesoText(
    amount = stats.remaining,
    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.W300),
    pesoColor = SnowColors.FrostMute,
    numberColor = SnowColors.Ice,
    animate = true,
)
```

Forecast row PesoTexts stay `animate = false` (default) — they aren't running totals.

- [ ] **Step 7: Build + run tests**

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:assembleDebug :composeApp:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 8: Install + spot-check on emulator**

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

Quick checks (visual only): swipe a payment on Home → DUE tweens. Tap Up next → chevron rotates. Open Insights → snapshot remaining tweens up. Open a debt detail → ProgressArc fills smoothly from 0.

- [ ] **Step 9: Report (controller will commit)**

Report:
- **Status:** DONE | BLOCKED
- Files touched
- Build + test results

---

## Task 2: StaggeredItem + apply on Home and Insights lists

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/StaggeredItem.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsScreen.kt`

- [ ] **Step 1: Create StaggeredItem**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/components/StaggeredItem.kt`:

```kotlin
package com.snowball.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Wraps content with a fade-in + slide-up entry animation delayed by index.
 * The cascade caps at index=8 so long lists don't feel slow.
 */
@Composable
fun StaggeredItem(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(40L * minOf(index, 8))
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
    ) {
        content()
    }
}
```

- [ ] **Step 2: Apply on HomeScreen payment rows**

Read `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt`. Find the payment rows loop:

```kotlin
state.rows.forEach { row ->
    key(row.debt.id) {
        SwipeablePaymentRow(...)
    }
}
```

Add the import:

```kotlin
import com.snowball.ui.components.StaggeredItem
```

Change the loop to `forEachIndexed` and wrap in StaggeredItem:

```kotlin
state.rows.forEachIndexed { i, row ->
    key(row.debt.id) {
        StaggeredItem(index = i) {
            SwipeablePaymentRow(
                row = row,
                onMarkPaid = { vm.markPaid(row); tick++ },
                onUndo = { vm.undoPayment(row); tick++ },
            )
        }
    }
}
```

- [ ] **Step 3: Apply on InsightsScreen forecast**

Read `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsScreen.kt`. Find the forecast loop:

```kotlin
Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    state.forecast.forEach { f ->
        ForecastRow(f)
    }
}
```

Add the import:

```kotlin
import com.snowball.ui.components.StaggeredItem
```

Change to `forEachIndexed` and wrap:

```kotlin
Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    state.forecast.forEachIndexed { i, f ->
        StaggeredItem(index = i) {
            ForecastRow(f)
        }
    }
}
```

- [ ] **Step 4: Build**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run tests**

```bash
./gradlew.bat :composeApp:testDebugUnitTest
```

Expected: all pass.

- [ ] **Step 6: Report**

Report status to controller.

---

## Task 3: Route transitions in App.kt

**File:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/App.kt`

- [ ] **Step 1: Read App.kt**

Use Read to inspect the current `when (val r = route) { ... }` block structure.

- [ ] **Step 2: Wrap in AnimatedContent**

Add imports:

```kotlin
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIntoContainer
import androidx.compose.animation.slideOutOfContainer
import androidx.compose.animation.togetherWith
```

Find the existing `when (val r = route) { ... }` block inside the `Box`. Wrap the entire `when` expression in `AnimatedContent`. The current structure:

```kotlin
Box(modifier = Modifier.fillMaxSize().weight(1f).then(if (isTabs) Modifier else Modifier.navigationBarsPadding())) {
    when (val r = route) {
        is Route.Tabs -> { ... }
        is Route.Form -> { ... }
        is Route.DebtDetail -> { ... }
        is Route.MiscForm -> { ... }
        is Route.CategoryManagement -> { ... }
    }
}
```

Replace with:

```kotlin
Box(modifier = Modifier.fillMaxSize().weight(1f).then(if (isTabs) Modifier else Modifier.navigationBarsPadding())) {
    AnimatedContent(
        targetState = route,
        transitionSpec = {
            val forward = initialState is Route.Tabs
            val slideDir = if (forward) AnimatedContentTransitionScope.SlideDirection.Start
                           else AnimatedContentTransitionScope.SlideDirection.End
            (slideIntoContainer(slideDir, tween(250)) + fadeIn(tween(250))) togetherWith
                (slideOutOfContainer(slideDir, tween(250)) + fadeOut(tween(250)))
        },
        label = "route",
    ) { r ->
        when (r) {
            is Route.Tabs -> {
                // existing Tabs branch — inner `when (tab) { ... }`
            }
            is Route.Form -> {
                val existing = r.existingDebtId?.let { repos.debts.byId(it) }
                val formVm = remember(r.existingDebtId) { DebtFormViewModel(repos, existing) }
                DebtFormScreen(
                    vm = formVm,
                    onCancel = { route = Route.Tabs },
                    onSaved = { route = Route.Tabs; refreshKey++ },
                )
            }
            is Route.DebtDetail -> {
                val detailVm = remember(r.debtId, refreshKey) { DebtDetailViewModel(repos, r.debtId) }
                DebtDetailScreen(
                    vm = detailVm,
                    onBack = { route = Route.Tabs; refreshKey++ },
                    onEdit = { id -> route = Route.Form(id) },
                )
            }
            is Route.MiscForm -> {
                val miscVm = remember { MiscFormViewModel(repos) }
                MiscFormScreen(
                    vm = miscVm,
                    onCancel = { route = Route.Tabs },
                    onSaved = { route = Route.Tabs; refreshKey++ },
                )
            }
            is Route.CategoryManagement -> {
                val catVm = remember(refreshKey) { CategoryManagementViewModel(repos) }
                CategoryManagementScreen(
                    vm = catVm,
                    onBack = { route = Route.Tabs; refreshKey++ },
                )
            }
        }
    }
}
```

For the `is Route.Tabs` branch inside the AnimatedContent, paste the existing `when (tab) { Tab.Home -> ...; Tab.Debts -> ...; Tab.Insights -> ...; Tab.Settings -> ... }` block unchanged.

- [ ] **Step 3: Build**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run tests**

```bash
./gradlew.bat :composeApp:testDebugUnitTest
```

Expected: all pass.

- [ ] **Step 5: Install + verify**

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

Navigate from Debts tab → tap a debt → DebtDetail slides in from the right. Tap back → slides out to the right. Tap FAB → form slides in. All transitions ~250ms.

- [ ] **Step 6: Report**

Report status to controller.

---

## Task 4: Verify + tag v0.2.10 + push + build APK

- [ ] **Step 1: Full test suite**

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:test
```

Expected: BUILD SUCCESSFUL. All existing tests still pass.

- [ ] **Step 2: Build final APK**

```bash
./gradlew.bat :composeApp:assembleDebug
```

- [ ] **Step 3: Tag and push**

```bash
git tag -a v0.2.10 -m "v0.2.10 — animation polish (number counters, progress arc, route transitions, list stagger, chevron rotation)"
git push origin main
git push origin v0.2.10
```

- [ ] **Step 4: Done**

---

## Risks recap

- **`animate = true` in PesoText with rapid amount changes** (e.g., user spams swipe-paid → swipe-undo): each new target value cancels the in-flight tween and starts a new one. Compose handles this gracefully — no flicker. Verified by Compose animation API contract.
- **`AnimatedContent`'s `forward` heuristic**: when navigating Detail → Form (via overflow Edit), `initialState` is `Route.DebtDetail` (not Tabs), so `forward = false` and it slides as a back-nav. Visually that's a Detail → Form transition that slides right instead of left. Minor — most users won't see this path frequently. Easy to fix later by tracking nav direction explicitly.
- **StaggeredItem's `LaunchedEffect(Unit)` re-fires when the parent recomposes with a new key.** Since each row already has `key(row.debt.id) { ... }`, the LaunchedEffect identity is keyed correctly and won't re-cascade on every recomposition (only on first mount of each row).
