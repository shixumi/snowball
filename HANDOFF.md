# HANDOFF — Snowball

Resume notes for picking up the project in a new session.

## What is this

**Snowball** — a personal debt tracker for Android (Compose Multiplatform / Kotlin Multiplatform). Replaces a Google Sheet for tracking debts paid bi-monthly on 15th and 30th paydays. Currency is PHP (locked for v1). Theme is "Editorial Frost" — navy `#0B0F14` background, champagne and ice-blue accents, Fraunces (display) + DM Sans (body).

- **Repo:** `https://github.com/shixumi/snowball` (private; user `shixumi`)
- **Working tree:** keep clean unless mid-task
- **Branch:** `main`
- **Current tag:** `v0.3.0`

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
│   │   ├── 2026-05-13-snowball-animations-design.md
│   │   ├── 2026-05-13-ui-polish-pass-design.md           (v0.2.11)
│   │   ├── 2026-05-13-v0212-polish-pass2-design.md        (v0.2.12)
│   │   └── 2026-05-13-v030-design.md                      (v0.3.0)
│   └── plans/                              ← writing-plans output, one per spec
│       └── (same dates, "-design" → -<feature>")
└── setup/
    └── android-dev-environment.md
```

Source: `composeApp/src/commonMain/kotlin/com/snowball/`
- `data/` — model + repos + db factory
- `domain/` — pure calculators (Cutoff, Journey, Overdue, Insights)
- `platform/` — `expect` classes (Haptics, NotificationScheduler, Permissions) — actuals in `androidMain/`
- `ui/components/` — shared composables (CutoffCard, PesoText, ProgressArc, UpNextCard, JourneyCard, IconCatalog, StaggeredItem, PressScale, CelebratePaid, SwipeCoachmark)
- `ui/home/` — HomeScreen + VM
- `ui/debts/` — list + detail + VM (now shows scheduled + MISC sections)
- `ui/form/` — DebtFormScreen + VM (with validation, TopAppBar, dropdown)
- `ui/misc/` — MISC item slim form
- `ui/categories/` — Category Management (rename/icon/delete/reassign)
- `ui/insights/` — Insights tab (snapshot + payoff timeline + 12-cutoff forecast)
- `ui/onboarding/` — 4-slide welcome flow shown on first launch only
- `ui/settings/` — SettingsScreen + VM (income + notifications)
- `ui/nav/` — BottomNav + SystemBackHandler expect/actual
- `ui/util/` — AmountFormat, DateFormat
- `ui/theme/` — Color, Type, Theme
- `App.kt` — sealed `Route` (Tabs, DebtForm, DebtDetail, Onboarding) + AnimatedContent route transitions

Android-only sources: `composeApp/src/androidMain/kotlin/com/snowball/`
- `MainActivity.kt` — splash + channel init + scheduler bootstrap
- `notifications/` — `PaydayWorker`, `NotificationChannelInit`
- `platform/` — `actual` Haptics, NotificationScheduler, Permissions (via WorkManager + Compose APIs)
- `ui/nav/SystemBackHandler.android.kt`

SQLDelight schema: `composeApp/src/commonMain/sqldelight/com/snowball/db/`
- `Category.sq`, `Debt.sq`, `Payment.sq`, `Settings.sq`, `_Init.sq`
- Migrations: `1.sqm` (Category.iconKey), `2.sqm` (Debt.firstPaymentDate), `3.sqm` (Settings.firstLaunchSeen + swipeCoachmarkSeen)
- Current schema version: **4** (auto-detected from highest .sqm)

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
| v0.2.11 | UI polish pass 1: animated navbar, form cascade, press feedback, empty-state motion |
| v0.2.12 | Identity (splash + snowflake glyph), mark-paid celebration, motion gaps, copy unification |
| v0.3.0 | Notifications, haptics, onboarding flow, swipe coachmark, payoff timeline, db v4 |

## Architecture decisions / nuances

- **Cutoff windows:** 15th payday covers days 15–30 of same month; 30th payday covers days 1–14 of NEXT month. Day 15 belongs to 15th cutoff.
- **`startDate` vs `firstPaymentDate`:** `startDate` = loan origination (informational). `firstPaymentDate` = when cycle 1 falls due (drives all schedule math). User's default convention: `firstPaymentDate = startDate + 1 month`.
- **Backfill distribution:** When user enters `paymentsAlreadyMade = N`, the form creates N synthetic payments at `firstPaymentDate + i months` (i = 0..N-1). `reconcilePayments` also detects legacy uniform-date backfills and redistributes on save.
- **MISC items:** `Debt` rows with `categoryId` in a LEDGER category, `totalPayments = 1`, auto-archived. Excluded from cutoff totals (filtered via `category.behavior == SCHEDULED` in calculators). Visible in their own section on Debts tab regardless of archive state.
- **OverdueCalculator:** Strict "past due" semantics — a cycle is expected as soon as its due date passes. No grace period.
- **Insights forecast:** Walks `nextCutoff(today).next()` forward 12 cutoffs, simulating virtual payments per iteration so debts roll off the list as their cycles exhaust.
- **Insights payoff timeline:** Active SCHEDULED debts sorted by `projectedEndDate(debt)` ascending, rendered between SnapshotCard and forecast list.
- **PesoText:** Always wraps in `BoxWithConstraints` and auto-shrinks via `TextMeasurer` so long amounts never break layout. Opt-in `animate = true` tweens the value (used on DUE, INCOME, LEFT OVER, Insights remaining).
- **Route transitions:** `AnimatedContent` with `slideInHorizontally`/`slideOutHorizontally` (NOT `slideIntoContainer` — that's Android-only). Forward navigation slides Start→End; back slides End→Start.
- **System back:** `SystemBackHandler` `expect/actual` in `ui/nav/`. Android impl uses `androidx.activity.compose.BackHandler`. Enabled whenever route is not Tabs and not Onboarding.
- **First-launch routing:** On App start, read `repos.settings.get().firstLaunchSeen`. If false → `Route.Onboarding`. Existing users (upgrading from v0.2.x) are backfilled to `firstLaunchSeen = 1` by migration `3.sqm` so they don't see onboarding.
- **Swipe coachmark:** Shown on Home only when `swipeCoachmarkSeen == 0` AND there's an unpaid row. Auto-dismisses on swipe / tap / 5s. **Note:** swipe-LEFT marks paid (EndToStart); swipe-RIGHT is undo. The coachmark text and arrow direction reflect this.
- **Haptics:** `Haptics` expect class with `tick()` + `thump()`. Android actual wraps Compose's `HapticFeedback`. Applied on: tab tap, mark-paid (thump), undo (tick), Save button.
- **Notifications:** WorkManager `PeriodicWorkRequest` (daily) with `PaydayWorker` that fires a `NotificationCompat` post on the 15th and last day of the month. `NotificationScheduler` expect class; Android actual uses `WorkManager.enqueueUniquePeriodicWork`. `POST_NOTIFICATIONS` runtime permission requested via `expect/actual rememberRequestNotificationPermission` (uses `ActivityResultContracts.RequestPermission` on Android). Channel `"payday"` created in `MainActivity.onCreate`.
- **Splash screen:** AndroidX `core-splashscreen` 1.0.1. Theme `Theme.App.Starting` extends `Theme.SplashScreen`, background `@color/Night` (#0E1620), foreground `@mipmap/ic_launcher_foreground`. `MainActivity` calls `installSplashScreen()` before `super.onCreate`.

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

## v0.3+ backlog

**Sub-project E — Notifications: SHIPPED in v0.3.0.** Verify on a real device (emulator can't fake wall-clock progression cleanly):
- Toggle the Settings switch (grants `POST_NOTIFICATIONS` on Android 13+)
- Set time to ~2 minutes from now, leave app, wait for the worker to fire on the next 15th / last day of month
- On non-payday dates, the worker still runs daily but doesn't post — confirm no spurious notifications

**Sub-project F — Multiplatform expansion** (hardware-gated, deferred):
- iOS target (needs Mac + Xcode + Apple ID; free signing OK for personal use until $99/yr cert for TestFlight)
- macOS desktop target (fast iteration during development)
- Windows desktop target (work machine)
- Mostly toolchain/build-config work. Existing commonMain code is already mostly platform-agnostic.

**Insights v2 (per-debt payoff calendar shipped in v0.3.0). Remaining items:**
- By-category breakdown chart (monthly burden by category)
- Snowball ranking (smallest balance first)
- Tap-to-drill on Insights forecast rows (sheet showing which debts hit that cutoff)
- Time-horizon switcher on Insights (3/6/12/all)

**Polish items not yet shipped:**
- Loading shimmer / skeleton on app cold start (needs async state separation)
- Custom branded route transitions (snowflake wipe or particle effect)
- Long-press on payment row → quick edit
- Form auto-focus + tab order
- Settings sliders / textfield focus pop

**Parked further out:**
- Light mode
- Localization
- Cross-device sync / cloud backup
- Snowball-method recommendations
- Avalanche method (would need interest rate field)
- What-if extra-payment simulations
- Cutoff history view
- Spreadsheet import
- Multiple income sources / variable cutoff income
- Multi-currency

## Known gotchas

- **`Read` tool on `.png` files crashes the agent.** Confirmed on multiple PNGs from `%TEMP%`. Capture screenshots for the human user, never Read them. uiautomator XML dumps work fine.
- **Subagents default to bash shell on Windows.** Tell them explicitly to use the Gradle wrapper path or set `$env:JAVA_HOME` before gradle commands.
- **Git LF→CRLF warnings on every commit are harmless.** Don't try to fix.
- **`slideIntoContainer` / `slideOutOfContainer` are Android-only.** Use `slideInHorizontally` / `slideOutHorizontally` for cross-platform compatibility.
- **`LocalContext` and `Manifest`/`Build` aren't accessible in commonMain.** Wrap any Android-specific UI hooks in expect/actual (see `Permissions.kt` / `Permissions.android.kt` for the pattern).
- **WorkManager daily worker drift.** Periodic jobs can drift by several minutes; acceptable for payday notifications.
- **Subagent self-check may flag Co-Authored-By trailers as "impersonation."** False positive — the project's commit pattern is `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>`. Use that trailer.

## How to resume

> Read `HANDOFF.md` in `C:\Users\Pika\projects\snowball`. Current state is `v0.3.0`. The full feature backlog and architecture decisions are documented above. For next steps, common ones are: (a) verify notifications on the real S25 device (sideload v0.3.0 APK), (b) sub-project F (iOS/macOS/Windows targets — hardware-gated), (c) Insights v2 remaining items (by-category chart, snowball ranking, tap-to-drill, time-horizon switcher), (d) deferred polish (loading shimmer, custom branded transitions). Pick up from the user's first message.
