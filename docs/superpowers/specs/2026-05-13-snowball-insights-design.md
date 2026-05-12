# Snowball — Insights Page Design

**Date:** 2026-05-13
**Scope:** A new bottom-nav tab presenting (1) an aggregate snapshot of total outstanding debt and (2) a forward forecast of the next 12 cutoffs.

## Purpose

User's current views show "right now" (Home cutoff card) and "next cutoff" (Up next card). There's no answer to "how much do I owe in total?" or "what do the next 6 months look like?" The Insights page fills that gap with two purely-derived sections that need no new tracking.

Quote from the user: "i can see what my up next is, thats good for quick glance. But what about upcoming ones? or how much debt do i have left?"

## In scope

| Item | Resolution |
|---|---|
| Snapshot: total debt remaining, debt count, monthly burden, income coverage % | New `InsightsCalculator.snapshot(...)` |
| Forecast: next 12 cutoffs (6 months) with due total + left over | New `InsightsCalculator.forecastCutoffs(today, count=12)` |
| New 4th bottom-nav tab | Add `Tab.Insights` to `Nav.kt` |
| Wire route + VM | `InsightsViewModel` + `App.kt` `Tab.Insights` branch |
| Insights screen UI (TopAppBar + snapshot card + forecast list) | New `InsightsScreen.kt` |
| Heavy cutoffs (negative left over) flagged Ember | Per-row border + label color |
| Auto-tapering "All clear ✓" markers after every debt is paid | Forecast walks virtual payments forward |

## Out of scope (parked for a future v0.3+)

- Per-debt payoff calendar (when each individual debt finishes)
- By-category breakdown bar chart
- Snowball ranking (smallest-balance-first list)
- Tap-to-drill from a forecast row into the debts that fall in that window
- Time-horizon switcher (3 / 6 / 12 / all)
- Historical stats beyond what's already in the Home Journey card

## Navigation

Bottom-nav order: **Home · Debts · Insights · Settings**. Adds a 4th tab between Debts and Settings. Material 3 bottom nav supports up to 5 tabs; the active-tab Ice pill (introduced in v0.1.1) carries over unchanged.

`Tab.Insights` enum entry uses `Icons.Outlined.Insights` from `compose.materialIconsExtended` (already on the classpath — no new dependency).

## Layout

```
┌─────────────────────────────────────────┐
│  Insights                          ▌    │   ← TopAppBar (no actions)
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────────────────────────┐    │
│  │  WHAT YOU OWE                   │    │   ← Snapshot card
│  │                                 │    │
│  │  ₱68,234                        │    │
│  │  across 5 debts                 │    │
│  │                                 │    │
│  │  ₱12,400/mo · 62% of monthly    │    │
│  └─────────────────────────────────┘    │
│                                         │
│  UPCOMING (NEXT 6 MONTHS)               │   ← labelSmall, 4sp tracking
│                                         │
│  ┌─────────────────────────────────┐    │
│  │  May 15 → 30        ₱4,200      │    │
│  │                     ₱20,800 left│    │
│  └─────────────────────────────────┘    │
│  ┌─────────────────────────────────┐    │
│  │  Jun 1 → 14         ₱7,600      │    │
│  │                     ₱17,400 left│    │
│  └─────────────────────────────────┘    │
│  ┌─────────────────────────────────┐    │
│  │  Jun 15 → 30        ₱29,000     │    │   ← Ember border
│  │                     SHORT BY    │    │
│  │                     ₱4,000      │    │
│  └─────────────────────────────────┘    │
│  ... (continues, 12 rows total)         │
│                                         │
│  ┌─────────────────────────────────┐    │
│  │  Aug 30 → Sep 14    All clear ✓ │    │   ← Ice color, no due/leftover
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

## Section 1 — Snapshot card

**File:** part of `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsScreen.kt`

**Visual:** `RoundedCornerShape(28.dp)`, `background SnowColors.CardElev`, 1dp `LineStrong` border, `padding(horizontal=24.dp, vertical=24.dp)`. Mirrors the existing Journey card style so the two cards read as siblings.

**Contents:**

```kotlin
Column {
    Text("WHAT YOU OWE", style = labelSmall(letterSpacing = 4.sp), color = FrostDim)
    Spacer(12.dp)
    PesoText(
        amount = stats.remaining,
        style = displayMedium.copy(fontWeight = FontWeight.W300),
        pesoColor = FrostMute,
        numberColor = Ice,
    )
    Spacer(2.dp)
    Text("across ${stats.debtCount} debt${if (stats.debtCount == 1) "" else "s"}",
         style = labelMedium, color = FrostMute)
    Spacer(16.dp)
    Row {
        Text("₱${formatAmountWithSeparators(stats.monthlyBurden)}/mo",
             style = bodyMedium, color = FrostMute)
        Text(" · ", style = bodyMedium, color = FrostMute)
        Text(
            if (stats.coveragePercent == null) "— of monthly"
            else "${stats.coveragePercent}% of monthly",
            style = bodyMedium, color = FrostMute,
        )
    }
}
```

PesoText already auto-shrinks to fit (v0.2.6), so the large hero number handles long amounts gracefully.

**Empty state:** When `stats.debtCount == 0`, swap the card body for a single muted line:

```
WHAT YOU OWE

Nothing right now.
Add a debt from the Debts tab to start tracking.
```

`labelMedium FrostMute`, no big number.

## Section 2 — Upcoming cutoffs forecast

**Section header:** `Text("UPCOMING (NEXT 6 MONTHS)", labelSmall.copy(letterSpacing = 4.sp), FrostDim)` with 24dp top padding from the snapshot card and 12dp bottom padding before the first row.

**Row layout:**

```kotlin
@Composable
private fun ForecastRow(forecast: CutoffForecast) {
    val isShort = !forecast.isAllClear && forecast.leftOver < 0
    val borderColor = if (isShort) SnowColors.Ember.copy(alpha = 0.4f) else SnowColors.LineStrong
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SnowColors.NightElev)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            cutoffRangeLabel(forecast.cutoff),
            style = bodyLarge,
            color = Frost,
            modifier = Modifier.weight(1f),
        )
        if (forecast.isAllClear) {
            Text("All clear ✓", style = bodyLarge, color = Ice)
        } else {
            Column(horizontalAlignment = Alignment.End) {
                PesoText(
                    amount = forecast.dueTotal,
                    style = headlineSmall,
                    pesoColor = FrostDim,
                    numberColor = Frost,
                )
                Spacer(2.dp)
                Text(
                    if (isShort) "SHORT BY ₱${formatAmountWithSeparators(abs(forecast.leftOver))}"
                    else "₱${formatAmountWithSeparators(forecast.leftOver)} left",
                    style = bodySmall,
                    color = if (isShort) Ember else FrostMute,
                )
            }
        }
    }
}
```

Rows are stacked in a `Column` with `verticalArrangement = Arrangement.spacedBy(8.dp)`.

**`cutoffRangeLabel(c: Cutoff): String`** — already a top-level public function in `ui/components/CutoffCard.kt`. Reused unchanged.

**Empty state:** When `forecast.isEmpty()`, render `Text("No upcoming debts in your forecast window.", bodyMedium.copy(fontStyle = FontStyle.Italic), FrostDim)` centered in a `Box(padding(40.dp))`. Triggered only when there are no active debts at all (otherwise the calculator always returns 12 rows, the trailing ones potentially "All clear").

## Domain layer

**New file:** `composeApp/src/commonMain/kotlin/com/snowball/domain/InsightsCalculator.kt`

```kotlin
package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.LocalDate

data class SnapshotStats(
    val remaining: Double,
    val debtCount: Int,
    val monthlyBurden: Double,
    val monthlyIncome: Double,
    val coveragePercent: Int?,   // null when monthlyIncome <= 0
)

data class CutoffForecast(
    val cutoff: Cutoff,
    val dueTotal: Double,
    val leftOver: Double,
    val isAllClear: Boolean,
)

object InsightsCalculator {

    /**
     * Aggregate snapshot. Caller is responsible for passing only SCHEDULED debts
     * (LEDGER / MISC entries don't count as obligations).
     */
    fun snapshot(
        activeScheduledDebts: List<Debt>,
        paymentsByDebt: Map<Long, List<Payment>>,
        incomePerCutoff: Double,
    ): SnapshotStats {
        val remaining = activeScheduledDebts.sumOf { d ->
            val made = paymentsByDebt[d.id]?.size ?: 0
            (d.totalPayments - made).coerceAtLeast(0) * d.monthlyAmount
        }
        val monthlyBurden = activeScheduledDebts.sumOf { it.monthlyAmount }
        val monthlyIncome = incomePerCutoff * 2.0
        val coverage = if (monthlyIncome > 0.0) {
            ((monthlyBurden / monthlyIncome) * 100).toInt().coerceIn(0, 999)
        } else null
        return SnapshotStats(
            remaining = remaining,
            debtCount = activeScheduledDebts.size,
            monthlyBurden = monthlyBurden,
            monthlyIncome = monthlyIncome,
            coveragePercent = coverage,
        )
    }

    /**
     * Walks forward `count` cutoffs starting from `nextCutoff(today).next()`
     * — i.e. the cutoff *after* the one shown on Home's Up next card.
     * Simulates payments by appending a synthetic Payment to each debt's
     * history after each forecast iteration, so subsequent iterations
     * correctly see prior cycles as paid and debts naturally drop off
     * once they reach their totalPayments cap.
     */
    fun forecastCutoffs(
        today: LocalDate,
        activeScheduledDebts: List<Debt>,
        paymentsByDebt: Map<Long, List<Payment>>,
        incomePerCutoff: Double,
        count: Int = 12,
    ): List<CutoffForecast> {
        if (activeScheduledDebts.isEmpty()) return emptyList()

        val results = mutableListOf<CutoffForecast>()
        val virtual: MutableMap<Long, MutableList<Payment>> =
            paymentsByDebt.mapValues { it.value.toMutableList() }.toMutableMap()
        var c = nextCutoff(today).next()

        repeat(count) {
            // Filter to debts that haven't exhausted their totalPayments yet.
            // Without this, finished debts would keep being billed in the forecast.
            val stillOwed = activeScheduledDebts.filter { d ->
                (virtual[d.id]?.size ?: 0) < d.totalPayments
            }
            val rows = CutoffCalculator.computeDueRows(c, stillOwed, virtual)
            val dueTotal = rows.sumOf { it.amount }
            val leftOver = incomePerCutoff - dueTotal
            results.add(
                CutoffForecast(
                    cutoff = c,
                    dueTotal = dueTotal,
                    leftOver = leftOver,
                    isAllClear = rows.isEmpty(),
                )
            )
            // Mark each row's cycle as paid for the next iteration.
            rows.forEach { row ->
                val list = virtual.getOrPut(row.debt.id) { mutableListOf() }
                list.add(
                    Payment(
                        id = -1L,
                        debtId = row.debt.id,
                        paidDate = row.effectiveDueDate,
                        amount = row.amount,
                    )
                )
            }
            c = c.next()
        }
        return results
    }
}
```

**Why simulate virtually paid:** `CutoffCalculator.computeDueRows` uses payment count to decide which cycles are still active for a debt. Without injecting virtual payments after each iteration, debts that should finish (e.g. a 6-month debt with 4 already paid would finish in 2 more cycles) would keep showing up forever in the forecast. With the simulation, debts drop off naturally and the tail of the list shows "All clear ✓."

**Why filter `stillOwed` each iteration:** `CutoffCalculator.computeDueRows` doesn't natively check `paymentsMade < totalPayments` — it emits a row whenever a cycle's effective due date lands in the cutoff window and `firstPaymentDate ≤ payDate`. Without filtering finished debts out of each forecast iteration, a 6-cycle loan would keep generating bills forever. The `stillOwed` filter (`virtual[id].size < totalPayments`) drops a debt as soon as the simulation has accumulated enough virtual payments to satisfy its total — so the bottom of the forecast list naturally tapers to "All clear" rows.

## ViewModel

**New file:** `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsViewModel.kt`

```kotlin
package com.snowball.ui.insights

import com.snowball.data.Repos
import com.snowball.data.model.CategoryBehavior
import com.snowball.domain.CutoffForecast
import com.snowball.domain.InsightsCalculator
import com.snowball.domain.SnapshotStats
import com.snowball.domain.today
import kotlinx.datetime.LocalDate

data class InsightsState(
    val snapshot: SnapshotStats,
    val forecast: List<CutoffForecast>,
)

class InsightsViewModel(private val repos: Repos) {
    fun load(today: LocalDate = today()): InsightsState {
        val cats = repos.categories.all()
        val scheduledCatIds = cats
            .filter { it.behavior == CategoryBehavior.SCHEDULED }
            .map { it.id }
            .toSet()
        val active = repos.debts.allActive()
            .filter { it.categoryId in scheduledCatIds }
        val paymentsByDebt = active.associate { it.id to repos.payments.historyForDebt(it.id) }
        val income = repos.settings.get().incomePerCutoff
        val snapshot = InsightsCalculator.snapshot(active, paymentsByDebt, income)
        val forecast = InsightsCalculator.forecastCutoffs(today, active, paymentsByDebt, income, count = 12)
        return InsightsState(snapshot, forecast)
    }
}
```

LEDGER (MISC) debts are filtered out at the VM layer — they appear in Debts list but never in obligation totals or forecast.

## Screen

**New file:** `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsScreen.kt`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(vm: InsightsViewModel) {
    val state = remember { vm.load() }

    Scaffold(
        containerColor = SnowColors.Night,
        topBar = {
            TopAppBar(
                title = { Text("Insights", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SnowColors.Night,
                    titleContentColor = SnowColors.Frost,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            SnapshotCard(stats = state.snapshot)
            Spacer(Modifier.height(24.dp))
            Text(
                "UPCOMING (NEXT 6 MONTHS)",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                color = SnowColors.FrostDim,
            )
            Spacer(Modifier.height(12.dp))
            if (state.forecast.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No upcoming debts in your forecast window.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = SnowColors.FrostDim,
                    )
                }
            } else {
                state.forecast.forEach { f ->
                    ForecastRow(f)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
```

Pure consumer of state, no data fetching beyond the single `vm.load()` call. Reload pattern matches existing screens (would require a `refreshKey` from App.kt; since Insights is read-only it can be a `remember(refreshKey)`).

## Wiring (App.kt + Nav.kt)

**`Nav.kt`:**

```kotlin
import androidx.compose.material.icons.outlined.Insights

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Outlined.AcUnit),
    Debts("Debts", Icons.Outlined.ReceiptLong),
    Insights("Insights", Icons.Outlined.Insights),
    Settings("Settings", Icons.Outlined.Tune),
}
```

The bottom-nav rendering is data-driven (`Tab.entries.forEach`), so no further changes needed there. The pill-tinted active state from v0.1.1 applies automatically.

**`App.kt`:**

```kotlin
import com.snowball.ui.insights.InsightsScreen
import com.snowball.ui.insights.InsightsViewModel

// ... inside the Tabs branch:
Tab.Insights -> {
    val insightsVm = remember(refreshKey) { InsightsViewModel(repos) }
    InsightsScreen(insightsVm)
}
```

Same pattern as `Tab.Settings`.

## File-level change inventory

**New files (4):**
- `composeApp/src/commonMain/kotlin/com/snowball/domain/InsightsCalculator.kt`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsViewModel.kt`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/insights/InsightsScreen.kt`
- `composeApp/src/commonTest/kotlin/com/snowball/domain/InsightsCalculatorTest.kt`

**Modified files (2):**
- `composeApp/src/commonMain/kotlin/com/snowball/ui/nav/Nav.kt` — add `Tab.Insights`
- `composeApp/src/commonMain/kotlin/com/snowball/App.kt` — wire the route

**Untouched:** All data-layer code, all other screens, no schema changes, no new dependencies.

## Testing strategy

**Unit tests** (`InsightsCalculatorTest.kt`, ~8 cases):

Snapshot tests:
1. No active debts → `remaining = 0, debtCount = 0, monthlyBurden = 0, coverage = null` (when income = 0) or `0` (when income > 0).
2. One debt with 2 of 6 paid → `remaining = 4 × monthlyAmount`.
3. Income = 0 → `coveragePercent = null`.
4. Income > 0, monthly burden > 0 → `coveragePercent` computed correctly.

Forecast tests:
5. No active debts → empty list.
6. One debt with 6 cycles starting Feb 17 (firstPaymentDate Feb 17), 2 paid, today May 13 → forecast starts from Jul 1-14 cutoff (`nextCutoff(today).next() = Jun 1-14? wait, let me re-think.`)
   - Actually: today May 13 → currentCutoff = Apr 30 payday (windows May 1-14). nextCutoff = May 15 payday (May 15-30). `nextCutoff(today).next()` = Jun 1-14 (Jun 30 payday from May? No — May 15 payday → next is May 30 payday which covers Jun 1-14).
   - So forecast starts at Jun 1-14, Jun 15-30, Jul 1-14, etc.
7. Debt finishes mid-forecast → forecast row for the cutoff where it finishes shows the final due; subsequent rows omit it.
8. All debts finish before forecast ends → trailing rows are `isAllClear = true`.

Manual verification on emulator:
- Open Insights tab from a fresh build: snapshot shows current state, forecast shows next 12 cutoffs with correct dates.
- Add a debt → return to Insights → totals + forecast update.
- Mark a payment paid on Home → return to Insights → snapshot's "remaining" decreases.
- Heavy cutoff (where total due > income): row has Ember border and "SHORT BY ₱X" label.

## Risks

- **Forecast simulation correctness.** The forecast walks virtual payments forward. If `CutoffCalculator.computeDueRows` has any behavior that depends on full payment history beyond just "is this cycle paid" — e.g., the `paymentsMade` count — the simulation might diverge from reality. Mitigation: the calculator currently only checks the *current cycle's paidness* (via `paidDate > priorEffective && paidDate <= windowEnd`), not historical totals. The injection of one synthetic payment per cycle correctly increments the count for the `paymentsMade >= totalPayments` filter I'm adding. Reviewed inline above.
- **Floating-point in `coveragePercent`.** Standard truncation via `.toInt()`. Already the pattern used by JourneyCalculator.percentCleared.
- **Bottom-nav crowding at small font scales.** 4 tabs × 1080px wide screen at `font_scale = 1.30` → each tab gets ~270px. Should still fit "Settings" label without truncation. Verify on emulator with font_scale 1.30.

## Success criteria

- New Insights tab visible in bottom nav, opens to the Insights screen.
- Snapshot card shows correct remaining / debt count / monthly burden / coverage% based on the user's current debts and income.
- Forecast list shows 12 cutoffs starting two cutoffs after current.
- Debts roll off the forecast when their cycles are exhausted; trailing rows show "All clear ✓."
- Heavy cutoffs (`leftOver < 0`) render with Ember border and "SHORT BY ₱X" label.
- All 8 new `InsightsCalculatorTest` cases pass; existing 60+ tests still pass.
- Tag `v0.2.9` (or `v0.3.0` if user prefers a minor bump).
