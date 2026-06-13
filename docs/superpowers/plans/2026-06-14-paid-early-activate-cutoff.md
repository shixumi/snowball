# "Got Paid Early" — Activate Next Cutoff — Implementation Plan

> Execute inline / subagent-driven. Steps use `- [ ]`. TDD the pure core.

**Goal:** Let the user activate the next cutoff early so its bills become Home's markable list; auto-clears when the calendar catches up.

**Architecture:** A pure `resolveEffectiveCutoff(today, override)` + a `Cutoff` storage-key codec drive everything (TDD'd). A persisted `Settings.paidAheadKey` holds the override; `HomeViewModel` resolves the effective cutoff and exposes `isActivatedEarly`; `HomeScreen` adds an activate button + an activated banner with Undo.

**Spec:** `docs/superpowers/specs/2026-06-14-paid-early-activate-cutoff-design.md`

**Build preamble:** `$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; $env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"`. Git at `C:\Program Files\Git\cmd`. Commit via `git commit -F <file>`. Never Read .png.

---

## File Structure
- **Modify** `domain/Cutoff.kt` — `EffectiveCutoff`, `resolveEffectiveCutoff`, `Cutoff.storageKey()`, `cutoffFromKey()`.
- **Modify** `domain/CutoffTest.kt` — tests for the above.
- **Modify** `sqldelight/.../Settings.sq` + new `4.sqm` — `paidAheadKey` column + queries.
- **Modify** `data/model/Settings.kt`, `data/repo/SettingsRepository.kt` — field + setters.
- **Modify** `ui/home/HomeViewModel.kt` — effective-cutoff wiring + `isActivatedEarly` + activate/undo.
- **Modify** `ui/home/HomeScreen.kt` — activate button + activated banner.

## Task 1: Pure domain (TDD)

**Files:** `domain/Cutoff.kt`, `domain/CutoffTest.kt`

- [ ] **Step 1: failing tests** (append to `CutoffTest`):

```kotlin
@Test
fun storageKey_roundtrips() {
    listOf(
        Cutoff(2026, 6, Payday.FIFTEENTH),
        Cutoff(2026, 6, Payday.THIRTIETH),
        Cutoff(2026, 12, Payday.THIRTIETH),
    ).forEach { c -> assertEquals(c, cutoffFromKey(c.storageKey())) }
}

@Test
fun cutoffFromKey_malformed_is_null() {
    assertEquals(null, cutoffFromKey(""))
    assertEquals(null, cutoffFromKey("2026-6"))
    assertEquals(null, cutoffFromKey("2026-6-NOPE"))
}

@Test
fun resolveEffective_null_override_uses_date_current() {
    val today = LocalDate(2026, 5, 20) // May 15 cutoff
    val e = resolveEffectiveCutoff(today, null)
    assertEquals(currentCutoff(today), e.cutoff)
    assertEquals(false, e.isActivatedEarly)
    assertEquals(false, e.clearOverride)
}

@Test
fun resolveEffective_override_ahead_is_activated() {
    val today = LocalDate(2026, 5, 13) // date-current = Apr 30 cutoff
    val override = nextCutoff(today)    // May 15 cutoff (ahead)
    val e = resolveEffectiveCutoff(today, override)
    assertEquals(override, e.cutoff)
    assertEquals(true, e.isActivatedEarly)
    assertEquals(false, e.clearOverride)
}

@Test
fun resolveEffective_override_caught_up_clears() {
    val today = LocalDate(2026, 5, 20)               // date-current = May 15 cutoff
    val override = Cutoff(2026, 5, Payday.FIFTEENTH) // same as date-current now
    val e = resolveEffectiveCutoff(today, override)
    assertEquals(currentCutoff(today), e.cutoff)
    assertEquals(false, e.isActivatedEarly)
    assertEquals(true, e.clearOverride)
}

@Test
fun resolveEffective_override_behind_clears() {
    val today = LocalDate(2026, 5, 20)              // date-current = May 15 cutoff
    val override = Cutoff(2026, 4, Payday.THIRTIETH) // stale/behind
    val e = resolveEffectiveCutoff(today, override)
    assertEquals(currentCutoff(today), e.cutoff)
    assertEquals(true, e.clearOverride)
}
```

- [ ] **Step 2: run → red** (`.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.domain.CutoffTest"`).

- [ ] **Step 3: implement** in `Cutoff.kt` (top-level, after `nextCutoff`):

```kotlin
data class EffectiveCutoff(
    val cutoff: Cutoff,
    val isActivatedEarly: Boolean,
    val clearOverride: Boolean,
)

fun resolveEffectiveCutoff(today: LocalDate, override: Cutoff?): EffectiveCutoff {
    val dateCurrent = currentCutoff(today)
    if (override == null) return EffectiveCutoff(dateCurrent, isActivatedEarly = false, clearOverride = false)
    return if (override.payDate > dateCurrent.payDate) {
        EffectiveCutoff(override, isActivatedEarly = true, clearOverride = false)
    } else {
        EffectiveCutoff(dateCurrent, isActivatedEarly = false, clearOverride = true)
    }
}

fun Cutoff.storageKey(): String = "$year-$month-${payday.name}"

fun cutoffFromKey(key: String): Cutoff? {
    val parts = key.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    if (month !in 1..12) return null
    val payday = Payday.entries.firstOrNull { it.name == parts[2] } ?: return null
    return Cutoff(year, month, payday)
}
```

- [ ] **Step 4: run → green.** Commit `feat(cutoff): effective-cutoff resolver + storage-key codec (TDD)`.

## Task 2: Persistence (DB v5 + repo)

**Files:** `sqldelight/com/snowball/db/Settings.sq`, new `sqldelight/com/snowball/db/4.sqm`, `data/model/Settings.kt`, `data/repo/SettingsRepository.kt`

- [ ] **Step 1:** Read `Settings.sq`, the existing `.sqm` files, `Settings.kt`, `SettingsRepository.kt`.
- [ ] **Step 2:** Create `4.sqm`: `ALTER TABLE Settings ADD COLUMN paidAheadKey TEXT NOT NULL DEFAULT '';`
- [ ] **Step 3:** In `Settings.sq`: add `paidAheadKey TEXT NOT NULL DEFAULT ''` to CREATE TABLE; add queries:
```sql
setPaidAhead:
UPDATE Settings SET paidAheadKey = :paidAheadKey WHERE id = 1;

clearPaidAhead:
UPDATE Settings SET paidAheadKey = '' WHERE id = 1;
```
- [ ] **Step 4:** `Settings.kt` data class gains `val paidAheadKey: String`. `SettingsRepository`: map it in the row→model conversion (default `''`); add `fun setPaidAhead(key: String) { db.settingsQueries.setPaidAhead(key) }` and `fun clearPaidAhead() { db.settingsQueries.clearPaidAhead() }` (match the repo's actual query-accessor style).
- [ ] **Step 5:** Compile (`:composeApp:compileDebugKotlinAndroid`). Commit `feat(db): migration v5 — Settings.paidAheadKey`.

## Task 3: ViewModel

**Files:** `ui/home/HomeViewModel.kt`

- [ ] **Step 1:** Add `isActivatedEarly: Boolean` to `HomeState`.
- [ ] **Step 2:** Top of `load(today)` — resolve effective cutoff and replace the cutoff/next derivation:
```kotlin
val override = repos.settings.get().paidAheadKey.takeIf { it.isNotBlank() }?.let { cutoffFromKey(it) }
val eff = resolveEffectiveCutoff(today, override)
if (eff.clearOverride) repos.settings.clearPaidAhead()
val cutoff = eff.cutoff
val next = cutoff.next()
```
(Remove the old `val cutoff = currentCutoff(today)` and `val next = nextCutoff(today)`.) Everything downstream that used `cutoff`/`next` is unchanged. Add `eff.isActivatedEarly` to the returned `HomeState`.
- [ ] **Step 3:** Add methods:
```kotlin
fun activateNextEarly(today: LocalDate = today()) {
    repos.settings.setPaidAhead(nextCutoff(today).storageKey())
}
fun undoActivateEarly() {
    repos.settings.clearPaidAhead()
}
```
Imports: `com.snowball.domain.resolveEffectiveCutoff`, `com.snowball.domain.cutoffFromKey`, `com.snowball.domain.storageKey`.
- [ ] **Step 4:** Compile. Commit `feat(home): effective-cutoff wiring + activate/undo early`.

## Task 4: HomeScreen UI

**Files:** `ui/home/HomeScreen.kt`

- [ ] **Step 1: Read the file** to confirm current header/CutoffCard/UpNext layout and the `tick`/`vm` wiring.
- [ ] **Step 2: Activated banner** — directly above `CutoffCard(...)`, when `state.isActivatedEarly`:
```kotlin
if (state.isActivatedEarly) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SnowColors.IceSoft)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Bolt, contentDescription = null, tint = SnowColors.Ice, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "Next payday — activated early",
            style = MaterialTheme.typography.bodySmall,
            color = SnowColors.Frost,
            modifier = Modifier.weight(1f),
        )
        Text(
            "Undo",
            style = MaterialTheme.typography.labelLarge,
            color = SnowColors.Ice,
            modifier = Modifier.clickable { vm.undoActivateEarly(); tick++ },
        )
    }
    Spacer(Modifier.height(12.dp))
}
```
(Imports: `androidx.compose.material.icons.outlined.Bolt`, plus existing `clip`/`background`/`clickable`/`Row`/`Icon`.)
- [ ] **Step 3: Activate action** — immediately after the `if (state.nextRows.isNotEmpty()) { ... UpNextCard ... }` block, add:
```kotlin
if (!state.isActivatedEarly && state.nextRows.isNotEmpty()) {
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { vm.activateNextEarly(); tick++ }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Bolt, contentDescription = null, tint = SnowColors.Ice, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("Got paid early? Activate", style = MaterialTheme.typography.labelLarge, color = SnowColors.Ice)
    }
}
```
(`Arrangement` is already imported in the file; confirm.)
- [ ] **Step 4:** Compile, then full suite. Commit `feat(home): "got paid early" activate button + activated banner`.

## Task 5: Review + ship (controller)

- [ ] `/code-review high` over the diff; address findings.
- [ ] `:composeApp:testDebugUnitTest` green; `:composeApp:assembleRelease` builds (signed).
- [ ] Bump `versionName` 0.4.3 / `versionCode` 4 + Settings footer; tag `v0.4.3`; push; deliver APK.

## Self-review
- **Spec coverage:** resolver+codec (T1)+tests; persistence (T2); VM wiring + activate/undo + isActivatedEarly (T3); banner + activate button + one-ahead cap via button-hidden-when-activated (T4); review/ship (T5). ✓
- **Placeholders:** none. ✓
- **Type consistency:** `resolveEffectiveCutoff(today, override): EffectiveCutoff(cutoff, isActivatedEarly, clearOverride)`, `Cutoff.storageKey()`, `cutoffFromKey(String): Cutoff?`, `Settings.paidAheadKey`, `setPaidAhead(key)/clearPaidAhead()`, `HomeState.isActivatedEarly`, `activateNextEarly()/undoActivateEarly()` — consistent across tasks. ✓
