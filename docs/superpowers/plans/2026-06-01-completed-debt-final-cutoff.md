# Keep a Completed Debt in Its Final Cutoff — Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development or executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** A fully-paid (auto-archived) debt remains a paid row in the cutoff containing its final scheduled due date, so the cutoff's totals stay correct; it drops off once that cutoff passes and never appears in Up Next.

**Architecture:** A pure domain predicate `completedDebtDueInCutoff(debt, paymentCount, cutoff)` decides inclusion. `HomeViewModel.load()` adds archived debts matching the predicate (per cutoff) to the active set fed to `CutoffCalculator.computeDueRows`, which is otherwise unchanged.

**Tech Stack:** Kotlin Multiplatform, kotlinx-datetime, kotlin.test.

**Spec:** `docs/superpowers/specs/2026-06-01-completed-debt-final-cutoff-design.md`

---

## File Structure

- **Modify:** `composeApp/src/commonMain/kotlin/com/snowball/domain/CutoffCalculator.kt` — add top-level `completedDebtDueInCutoff`.
- **Modify:** `composeApp/src/commonTest/kotlin/com/snowball/domain/CutoffCalculatorTest.kt` — predicate tests.
- **Modify:** `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeViewModel.kt` — wire the predicate into `load()`.

## Task 1: Pure predicate `completedDebtDueInCutoff`

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/domain/CutoffCalculator.kt`
- Test: `composeApp/src/commonTest/kotlin/com/snowball/domain/CutoffCalculatorTest.kt`

- [ ] **Step 1: Write the failing tests** (append inside `class CutoffCalculatorTest`)

```kotlin
@Test
fun completed_debt_shows_in_cutoff_of_its_final_due_date() {
    // 6 payments, firstPayment Jan 10, dueDay 10 -> final due date Jun 10.
    // Jun 10 falls in the May 30 cutoff window (May 30 - Jun 14).
    val d = debt(dueDay = 10, total = 6, start = LocalDate(2026, 1, 10),
        firstPayment = LocalDate(2026, 1, 10), archived = true)
    val c = Cutoff(2026, 5, Payday.THIRTIETH)
    assertTrue(completedDebtDueInCutoff(d, paymentCount = 6, cutoff = c))
}

@Test
fun completed_debt_absent_from_other_cutoffs() {
    val d = debt(dueDay = 10, total = 6, start = LocalDate(2026, 1, 10),
        firstPayment = LocalDate(2026, 1, 10), archived = true)
    // Jun 15 cutoff window is Jun 15-29; final due date Jun 10 is not in it.
    val c = Cutoff(2026, 6, Payday.FIFTEENTH)
    assertEquals(false, completedDebtDueInCutoff(d, paymentCount = 6, cutoff = c))
}

@Test
fun active_debt_is_not_a_completed_row() {
    val d = debt(dueDay = 10, total = 6, start = LocalDate(2026, 1, 10),
        firstPayment = LocalDate(2026, 1, 10), archived = false)
    val c = Cutoff(2026, 5, Payday.THIRTIETH)
    assertEquals(false, completedDebtDueInCutoff(d, paymentCount = 6, cutoff = c))
}

@Test
fun manually_archived_incomplete_debt_excluded() {
    // Archived but only 5 of 6 paid -> not "completed", stays hidden.
    val d = debt(dueDay = 10, total = 6, start = LocalDate(2026, 1, 10),
        firstPayment = LocalDate(2026, 1, 10), archived = true)
    val c = Cutoff(2026, 5, Payday.THIRTIETH)
    assertEquals(false, completedDebtDueInCutoff(d, paymentCount = 5, cutoff = c))
}
```

- [ ] **Step 2: Run to verify they fail**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.domain.CutoffCalculatorTest"
```

Expected: FAIL — unresolved reference `completedDebtDueInCutoff`.

- [ ] **Step 3: Implement the predicate** (append to `CutoffCalculator.kt`, top-level, after the `object`)

```kotlin
/**
 * True when [debt] is a fully-paid, archived debt whose final scheduled due date
 * falls within [cutoff]'s window. Used to keep a just-completed debt visible as a
 * paid row in the single cutoff its last payment belongs to. The payment-count gate
 * excludes debts archived manually before completion.
 */
fun completedDebtDueInCutoff(debt: Debt, paymentCount: Int, cutoff: Cutoff): Boolean {
    if (!debt.isArchived) return false
    if (paymentCount < debt.totalPayments) return false
    val end = projectedEndDate(debt) ?: return false
    return end >= cutoff.windowStart && end <= cutoff.windowEnd
}
```

(`projectedEndDate` is a top-level fun already in `JourneyCalculator.kt`, same package — no import needed.)

- [ ] **Step 4: Run to verify they pass**

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --tests "com.snowball.domain.CutoffCalculatorTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/domain/CutoffCalculator.kt composeApp/src/commonTest/kotlin/com/snowball/domain/CutoffCalculatorTest.kt
git commit -F <message-file>
# message: "feat(cutoff): predicate to keep a completed debt in its final cutoff"
```

## Task 2: Wire predicate into HomeViewModel.load()

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeViewModel.kt`

- [ ] **Step 1: Replace the debt/payments assembly in `load()`**

Current `load()` body (lines ~31–49) builds `debts = allActive()` and one `paymentsByDebt`. Replace the assembly so current and next cutoffs each include completed debts whose final due date lands in their window. New body:

```kotlin
fun load(today: LocalDate = today()): HomeState {
    val cutoff = currentCutoff(today)
    val next = nextCutoff(today)

    val active = repos.debts.allActive()
    val allDebts = repos.debts.all()
    val countById = allDebts.associate { it.id to repos.payments.countForDebt(it.id) }

    fun debtsFor(c: Cutoff): List<Debt> =
        active + allDebts.filter { completedDebtDueInCutoff(it, countById[it.id] ?: 0, c) }

    val currentDebts = debtsFor(cutoff)
    val nextDebts = debtsFor(next)

    val displayIds = (currentDebts + nextDebts).map { it.id }.toSet()
    val paymentsByDebt = displayIds.associateWith { repos.payments.historyForDebt(it) }

    val rows = CutoffCalculator.computeDueRows(cutoff, currentDebts, paymentsByDebt)
    val income = repos.settings.get().incomePerCutoff
    val summary = CutoffCalculator.summarize(rows, income)

    val nextRows = CutoffCalculator.computeDueRows(next, nextDebts, paymentsByDebt)
    val nextTotal = nextRows.sumOf { it.amount }

    val allPayments = allDebts.flatMap { repos.payments.historyForDebt(it.id) }
    val journey = JourneyCalculator.compute(allDebts, allPayments)

    val overdue = OverdueCalculator.computeOverdue(active, paymentsByDebt, today)
    val swipeCoachmarkSeen = repos.settings.get().swipeCoachmarkSeen
    return HomeState(cutoff, rows, summary, income, next, nextRows, nextTotal, journey, overdue, swipeCoachmarkSeen)
}
```

Add the import at the top of the file:
```kotlin
import com.snowball.domain.completedDebtDueInCutoff
```

Notes:
- `OverdueCalculator` runs over `active` only (a completed debt is never overdue); `paymentsByDebt` is a superset of active ids, so lookups still resolve.
- `JourneyCalculator` is unchanged (reads all debts + all payments).

- [ ] **Step 2: Compile**

```powershell
.\gradlew.bat :composeApp:compileDebugKotlinAndroid
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run full unit suite (regression sweep)**

```powershell
.\gradlew.bat :composeApp:testDebugUnitTest --rerun-tasks
```

Expected: BUILD SUCCESSFUL (no failures).

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeViewModel.kt
git commit -F <message-file>
# message: "fix(home): keep a completed debt visible in its final cutoff"
```

## Task 3: Build APK

- [ ] **Step 1:** `.\gradlew.bat :composeApp:assembleDebug` → confirm `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.

## Notes

- Windows + PowerShell. Always export `JAVA_HOME`/`ANDROID_HOME` before gradle. Git lives at `C:\Program Files\Git\cmd` (append to `$env:Path` per shell).
- Commit messages via `-F <file>` (apostrophes break PowerShell here-strings).
- Never `Read` a `.png`.

## Self-review

- **Spec coverage:** predicate (Task 1) covers all four test cases in the spec's Testing section; wiring (Task 2) covers current+next cutoff inclusion, payments map, overdue/journey untouched. ✓
- **Placeholders:** none. ✓
- **Type consistency:** `completedDebtDueInCutoff(debt, paymentCount, cutoff)` signature identical in Task 1 definition and Task 2 call. `HomeState` constructor args unchanged from current. ✓
