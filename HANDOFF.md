# HANDOFF — Snowball

Resume notes for picking up the project in a new session.

## What is this

**Snowball** — a personal debt tracker for Android (Compose Multiplatform / Kotlin Multiplatform). Replaces a Google Sheet for tracking debts paid bi-monthly on 15th and 30th paydays. Currency is PHP (locked for v1). Theme is "Editorial Frost" — navy `#0B0F14` background, champagne and ice-blue accents, Fraunces (display) + DM Sans (body).

- **Repo:** `https://github.com/shixumi/snowball` (private; user `shixumi`)
- **Working tree:** keep clean unless mid-task
- **Branch:** `main`
- **Current tag:** `v0.2.10`

## Where things are

```
docs/
├── design/
│   └── ux-review-2026-05-12.md             ← independent UX/UI review from v0.1.0
├── superpowers/
│   ├── specs/                              ← brainstorming output, one per feature/sub-project
│   │   ├── 2026-05-12-snowball-design.md          (v0.1 design)
│   │   ├── 2026-05-12-snowball-v011-ux-polish-design.md
│   │   ├── 2026-05-13-snowball-v02a-home-design.md   (Up next + Journey)
│   │   ├── 2026-05-13-snowball-v02b-debt-detail-design.md
│   │   ├── 2026-05-13-snowball-v02c-categories-design.md
│   │   ├── 2026-05-13-snowball-v02d-rollover-design.md
│   │   ├── 2026-05-13-snowball-insights-design.md
│   │   └── 2026-05-13-snowball-animations-design.md
│   └── plans/                              ← writing-plans output, one per spec
│       └── (same dates, "-design" → -<feature>")
└── setup/
    └── android-dev-environment.md
```

Source: `composeApp/src/commonMain/kotlin/com/snowball/`
- `data/` — model + repos + db factory
- `domain/` — pure calculators (Cutoff, Journey, Overdue, Insights)
- `ui/components/` — shared composables (CutoffCard, PesoText, ProgressArc, UpNextCard, JourneyCard, IconCatalog, StaggeredItem)
- `ui/home/` — HomeScreen + VM
- `ui/debts/` — list + VM (now shows scheduled + MISC sections)
- `ui/form/` — DebtFormScreen + VM (with validation, TopAppBar, dropdown)
- `ui/detail/` — DebtDetailScreen + VM (Detail screen with progress arc + payment history)
- `ui/misc/` — MISC item slim form
- `ui/categories/` — Category Management (rename/icon/delete/reassign)
- `ui/insights/` — Insights tab (snapshot + 12-cutoff forecast)
- `ui/settings/` — SettingsScreen + VM
- `ui/nav/` — BottomNav + SystemBackHandler expect/actual
- `ui/util/` — AmountFormat, DateFormat
- `ui/theme/` — Color, Type, Theme
- `App.kt` — sealed `Route` + AnimatedContent route transitions

SQLDelight schema: `composeApp/src/commonMain/sqldelight/com/snowball/db/`
- `Category.sq`, `Debt.sq`, `Payment.sq`, `Settings.sq`, `_Init.sq`
- Migrations: `1.sqm` (adds iconKey to Category), `2.sqm` (adds firstPaymentDate to Debt)
- Current schema version: 3 (auto-detected from highest .sqm)

## Tag history

| Tag | What |
|---|---|
| v0.1.0 | Initial release: Home/Debts/Settings + cutoff math + payments swipe |
| v0.1.1 | UX polish (validation, delete confirm, accessibility, contrast, semantics) |
| v0.2.0 | Home: Up next card + Your Journey card |
| v0.2.1 | DebtDetailScreen + Archive view + MISC items + FAB dropdown |
| v0.2.2 | Category Management + iconKey schema migration |
| v0.2.3 | Cutoff rollover (OVERDUE section + 60s tick) |
| v0.2.4 | (Superseded by v0.2.5 — was a grace-period workaround) |
| v0.2.5 | Separate `firstPaymentDate` column; strict overdue semantics; backfill distribution |
| v0.2.6 | PesoText auto-shrink to fit available width |
| v0.2.7 | System back navigation + stacked INCOME/LEFT OVER cells |
| v0.2.8 | Distribute backfilled payment dates across cycle months |
| v0.2.9 | Insights page (snapshot + 12-cutoff forecast) |
| v0.2.10 | Animation polish (counters, progress arc, chevron, list stagger, route transitions) |

## Architecture decisions / nuances

- **Cutoff windows:** 15th payday covers days 15–30 of same month; 30th payday covers days 1–14 of NEXT month. Day 15 belongs to 15th cutoff.
- **`startDate` vs `firstPaymentDate`:** `startDate` = loan origination (informational). `firstPaymentDate` = when cycle 1 falls due (drives all schedule math). User's default convention: `firstPaymentDate = startDate + 1 month`.
- **Backfill distribution:** When user enters `paymentsAlreadyMade = N`, the form creates N synthetic payments at `firstPaymentDate + i months` (i = 0..N-1). This ensures each fall in a distinct cutoff window. `reconcilePayments` also detects uniform-date legacy backfills (multiple rows same paidDate) and redistributes on save.
- **MISC items:** `Debt` rows with `categoryId` in a LEDGER category, `totalPayments = 1`, auto-archived. Excluded from cutoff totals (filtered via `category.behavior == SCHEDULED` in calculators). Visible in their own section on Debts tab regardless of archive state.
- **OverdueCalculator:** Strict "past due" semantics — a cycle is expected as soon as its due date passes. No grace period (was tried in v0.2.4, reverted in v0.2.5 once firstPaymentDate provided the right anchor).
- **Insights forecast:** Walks `nextCutoff(today).next()` forward 12 cutoffs, simulating virtual payments per iteration so debts roll off the list as their cycles exhaust.
- **PesoText:** Always wraps in `BoxWithConstraints` and auto-shrinks via `TextMeasurer` so long amounts never break layout. Opt-in `animate = true` tweens the value (used on DUE, INCOME, LEFT OVER, Insights remaining).
- **Route transitions:** `AnimatedContent` with `slideInHorizontally`/`slideOutHorizontally` (NOT `slideIntoContainer` — that's Android-only). Forward navigation slides Start→End; back slides End→Start. Heuristic: `forward = initialState is Route.Tabs`.
- **System back:** `SystemBackHandler` `expect/actual` in `ui/nav/`. Android impl uses `androidx.activity.compose.BackHandler`. Enabled whenever route is not Tabs.

## Build / run

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :composeApp:assembleDebug
```

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r composeApp\build\outputs\apk\debug\composeApp-debug.apk
& $adb shell am start -n com.snowball/.MainActivity
```

- Emulator: `emulator-5554` (Pixel_7, API 36, Android 16), 1080×2400
- Sideload target: Samsung S25, One UI 8.5, Android 16
- `adb` not on PATH — use full path above
- App package: `com.snowball`, launcher: `com.snowball/.MainActivity`
- JDK 21 from Android Studio's bundled JBR. AGP 8.7. Compose Multiplatform 1.8.0.

## v0.3 backlog (deferred from v0.2)

**E — Notifications**
- Payday-style notifications on 15th and 30th. Platform-specific via `expect/actual`: Android `AlarmManager` + `NotificationCompat`. Settings already has the toggle + time-of-day picker stub.
- Needs real device verification (emulator can't fake wall-clock progression cleanly).

**F — Multiplatform expansion**
- iOS target (needs Mac + Xcode + Apple ID; free signing OK for personal use until $99/yr cert for TestFlight)
- macOS desktop target (fast iteration during development)
- Windows desktop target (work machine)
- Mostly toolchain/build-config work. Existing commonMain code is already mostly platform-agnostic.

**v0.2-style features that didn't make the cut**
- Per-debt payoff calendar (Insights v2)
- By-category breakdown chart (Insights v2)
- Snowball ranking (smallest balance first)
- Tap-to-drill on Insights forecast rows
- Time-horizon switcher on Insights (3/6/12/all)
- Proper coachmark for first-launch swipe instruction
- Cross-device sync / cloud backup
- Snowball-method recommendations
- Avalanche method (would need interest rate field)
- What-if extra-payment simulations
- Cutoff history view
- Spreadsheet import

## Known gotchas

- **`Read` tool on `.png` files crashes the agent.** Confirmed on multiple PNGs from `%TEMP%`. Capture screenshots for the human user, never Read them. uiautomator XML dumps work fine.
- **Subagents default to bash shell on Windows.** Tell them explicitly to use the Gradle wrapper path or set `$env:JAVA_HOME` before gradle commands.
- **Git LF→CRLF warnings on every commit are harmless.** Don't try to fix.
- **`HANDOFF.md` and `build.log` were accidentally committed at v0.2.5.** Not a real problem but worth a future cleanup.
- **`slideIntoContainer` / `slideOutOfContainer` are Android-only.** Used `slideInHorizontally` / `slideOutHorizontally` for cross-platform compatibility (works once iOS/desktop targets are added).

## How to resume

> Read `HANDOFF.md` in `C:\Users\Pika\projects\snowball`. Current state is `v0.2.10`. The full feature backlog and architecture decisions are documented above. For next steps, common ones are: (a) sub-project E (notifications), (b) sub-project F (iOS/macOS/Windows targets), (c) iterating on the Insights page (per-debt payoff calendar, by-category chart), (d) any user-facing polish the user requests. Pick up from the user's first message.
