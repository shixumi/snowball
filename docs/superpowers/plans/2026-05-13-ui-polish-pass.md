# UI Polish Pass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the existing motion language broadly across the app — navbar, add-debt form, list screens, detail screen, empty states — and introduce one reusable `pressScale` modifier for tactile button/card feedback.

**Architecture:** Reuse the four animation APIs already established (`animateFloatAsState`, `animateColorAsState`, `AnimatedVisibility`, `StaggeredItem`). Add one new shared modifier file. No new dependencies.

**Tech Stack:** Kotlin Multiplatform · Compose Multiplatform 1.8.0 · Material 3 · commonMain only.

**Spec:** `docs/superpowers/specs/2026-05-13-ui-polish-pass-design.md`

---

## File Structure

- **Create:** `composeApp/src/commonMain/kotlin/com/snowball/ui/components/PressScale.kt`
- **Modify:** `composeApp/src/commonMain/kotlin/com/snowball/ui/nav/Nav.kt`
- **Modify:** `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt`
- **Modify:** `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsScreen.kt`
- **Modify:** `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtDetailScreen.kt` (path inferred — confirm at task start)
- **Modify:** `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt` (empty state only)
- **Modify:** `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsScreen.kt` (empty state + snapshot card entry)

## Task 1: Reusable PressScale modifier

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/PressScale.kt`

- [ ] **Step 1: Create the modifier**

```kotlin
package com.snowball.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

/**
 * Applies a brief scale-down (1.0 → [pressed]) when the user is pressing this element,
 * returning to 1.0 on release. Use on buttons, FABs, cards, and tab targets that
 * should feel tactile. The caller owns the InteractionSource so it can be the same
 * source the underlying component (Button, IconButton, clickable) already uses.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressed: Float = 0.96f,
): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressed = true
                is PressInteraction.Release -> isPressed = false
                is PressInteraction.Cancel -> isPressed = false
            }
        }
    }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "pressScale",
    )
    return this.scale(scale)
}
```

- [ ] **Step 2: Verify it compiles**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/components/PressScale.kt
git commit -m "feat(ui): pressScale modifier for tactile press feedback"
```

## Task 2: BottomNav animation

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/nav/Nav.kt`

- [ ] **Step 1: Add animated icon scale + animated pill color + tap pulse**

Replace the inner per-tab Column with this expanded structure. Each tab now owns its own `MutableInteractionSource` (so we can wire the pressScale modifier through the clickable). The active icon scales to 1.15; pill background fades via `animateColorAsState`.

Key changes inside the `Tab.entries.forEach` block:
- Add `val interactionSource = remember { MutableInteractionSource() }` per tab
- Add `val iconScale by animateFloatAsState(targetValue = if (active) 1.15f else 1f, animationSpec = tween(300, easing = FastOutSlowInEasing), label = "navIconScale")`
- Add `val pillColor by animateColorAsState(targetValue = if (active) SnowColors.Ice.copy(alpha = 0.10f) else androidx.compose.ui.graphics.Color.Transparent, animationSpec = tween(200), label = "navPillColor")`
- Change `clickable { onSelect(tab) }` to `clickable(interactionSource = interactionSource, indication = LocalIndication.current) { onSelect(tab) }`
- Add `.pressScale(interactionSource)` on the outer per-tab Column
- Change `Icon(... modifier = Modifier.size(22.dp))` to `Icon(... modifier = Modifier.size(22.dp).scale(iconScale))`
- Change the inner pill `.background(if (active) SnowColors.Ice.copy(alpha = 0.10f) else Color.Transparent)` to `.background(pillColor)`

Add imports:
```kotlin
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.LocalIndication
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import com.snowball.ui.components.pressScale
```

- [ ] **Step 2: Verify it compiles**

```powershell
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/nav/Nav.kt
git commit -m "feat(nav): animated tab pill, icon scale, and tap pulse"
```

## Task 3: DebtFormScreen polish

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt`

- [ ] **Step 1: Wrap Field blocks in StaggeredItem cascade**

In the main `Column` that scrolls the form (lines ~145–367), wrap each top-level `Field("...") {...}` block (and the surrounding Row for total/dueDay, the conditional new/edit block, the Switch row) in `StaggeredItem(index = N)` where N starts at 0 and increments by 1 per logical field/group.

Order:
- index 0: Name
- index 1: Category
- index 2: Monthly amount
- index 3: Row(total payments + due day)
- index 4: Payments already made / Payments recorded (the if/else)
- index 5: Switch row (Use last day of month)
- index 6: Start date
- index 7: First payment date
- index 8: Notes

The existing `Spacer(Modifier.height(16.dp))` after each block stays.

- [ ] **Step 2: AnimatedVisibility on the conditional new/edit branch**

Replace the `if (!vm.isEditing) {...} else {...}` block (lines ~249–295) so each branch is wrapped in `AnimatedVisibility` with `fadeIn() + expandVertically()` / `fadeOut() + shrinkVertically()`.

```kotlin
AnimatedVisibility(
    visible = !vm.isEditing,
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically(),
) {
    Column { /* "Payments already made" field + helper text + Spacer */ }
}
AnimatedVisibility(
    visible = vm.isEditing,
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically(),
) {
    Column { /* "PAYMENTS RECORDED" label + count + Spacer */ }
}
```

Note the two branches are mutually exclusive on `vm.isEditing`, so only one is visible at a time.

- [ ] **Step 3: Save button press scale**

Modify the Save button (lines ~125–142) to use a `MutableInteractionSource` and apply `.pressScale(interactionSource)` to its Modifier. Pass `interactionSource = interactionSource` to the Button.

```kotlin
val saveInteractionSource = remember { MutableInteractionSource() }
Button(
    onClick = { if (vm.save()) onSaved() },
    enabled = vm.isValid,
    interactionSource = saveInteractionSource,
    modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 16.dp)
        .height(56.dp)
        .pressScale(saveInteractionSource),
    /* ...colors, shape unchanged... */
) { Text("Save") }
```

Add imports:
```kotlin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.snowball.ui.components.StaggeredItem
import com.snowball.ui.components.pressScale
```

- [ ] **Step 4: Verify it compiles**

```powershell
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
```

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt
git commit -m "feat(form): staggered field cascade, animated conditional fields, press feedback on Save"
```

## Task 4: DebtsScreen + DebtDetailScreen polish

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtDetailScreen.kt` (find with Glob if path differs)

- [ ] **Step 1: Read both files to confirm structure**

Start by reading both files. Note row composables, FAB location, empty state location, and the card structure on detail screen.

- [ ] **Step 2: Wrap debt-row items in StaggeredItem**

In DebtsScreen's list rendering, wrap each row in `StaggeredItem(index = idx)` where `idx` is the row's index (use `itemsIndexed` if it's a LazyColumn, or pass the loop counter if it's a regular Column).

- [ ] **Step 3: FAB press scale**

Add a `MutableInteractionSource` for the FAB, pass it to the FloatingActionButton, and add `.pressScale(...)` to its Modifier.

- [ ] **Step 4: Empty state animation**

Wrap DebtsScreen's "no debts yet" empty-state Box (around line 112) in `AnimatedVisibility(visible = isEmpty, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }))`. Since the placeholder only renders when there are no debts, we can use `AnimatedVisibility` with `visible = true` and let the enter transition play once on composition — or wrap the entire `if (debts.isEmpty())` branch in an `AnimatedVisibility`.

Simplest: wrap the existing static Box in `Box { AnimatedVisibility(visible = true, enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 }) { /* existing content */ } }`. Make sure `visible = remember { mutableStateOf(false) }` is flipped to true via `LaunchedEffect(Unit)` so the animation actually triggers on first composition (otherwise `visible = true` initially means no transition plays).

- [ ] **Step 5: DebtDetailScreen card cascade**

Wrap each top-level card composable in `StaggeredItem(index = N)` where N starts at 0.

- [ ] **Step 6: Verify both files compile**

```powershell
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
```

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/debts/
git commit -m "feat(debts): row cascade, FAB press scale, empty-state and detail card animations"
```

## Task 5: Home + Insights empty-state and card entry polish

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt` (empty state on line ~352)
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsScreen.kt` (SnapshotCard + empty state on line ~74)

- [ ] **Step 1: Animate Home empty state**

Find the empty-state branch in HomeScreen and wrap its content in the same first-composition `AnimatedVisibility` pattern from Task 4 (fadeIn + slideInVertically, 350ms).

- [ ] **Step 2: Animate Insights SnapshotCard + empty state**

In InsightsScreen, wrap the SnapshotCard in `StaggeredItem(index = 0)` (and shift any subsequent forecast row index accordingly — confirm forecast already uses indices starting from 1, otherwise leave indices in their current sequence and prepend an explicit index 0 wrapper). Animate the empty state with the same pattern.

- [ ] **Step 3: Verify it compiles**

```powershell
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
```

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/home/ composeApp/src/commonMain/kotlin/com/snowball/ui/insights/
git commit -m "feat(ui): empty-state fade-in on Home/Insights, snapshot card cascade"
```

## Task 6: Build APK + tag + push

**This task is run by the controller (not a subagent).** All implementation commits land on main.

- [ ] **Step 1: Build debug APK**

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL; APK at `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.

- [ ] **Step 2: Tag v0.2.11**

```bash
git tag -a v0.2.11 -m "v0.2.11: UI polish pass — navbar animation, form cascade, press feedback, empty-state motion"
```

- [ ] **Step 3: Push commits + tag**

```bash
git push origin main
git push origin v0.2.11
```

- [ ] **Step 4: Report APK path to user**

Tell user where the APK is, ready for sideload.

## Notes for implementer subagents

- **Always export env vars before gradle.** Use this preamble in every gradle call:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
  ```
  Or in bash:
  ```bash
  export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"; export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
  ```
- **Never call Read on .png files.** Agent-poisoning bug. If a screenshot is needed, capture but do not read it.
- **commonMain only.** Don't add anything to `androidMain` for animation primitives — the existing four APIs all work cross-platform.
- **Use `LaunchedEffect(Unit)` + flipping a state from false to true** to trigger an enter animation on first composition. `AnimatedVisibility(visible = true)` alone won't play the enter transition.
- **If a file path in this plan doesn't exist (DebtDetailScreen.kt), Glob for the actual filename** and proceed.
