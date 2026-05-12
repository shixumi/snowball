# HANDOFF — Snowball

Resume notes after a session died on `API Error: 400 Could not process image`.

Failed transcript: `C:\Users\Pika\.claude\projects\C--Users-Pika-projects-snowball\826c9af7-02fb-4d81-98a1-0f444df0d973.jsonl` (line 1213 dispatch → line 1217 first crash; main agent then poisoned and unresponsive from line 1224 onward).

## Goal

Build **Snowball** — a personal debt tracker for Android (Compose Multiplatform / Kotlin Multiplatform). Replaces user's Google Sheet for tracking debts and bi-monthly payments (15th and 30th cutoffs). v0.1 = working app on emulator + S25; v0.2 reserved for snowball-history visualization.

## What's been done (all committed and pushed to `origin/main`)

Repo: `https://github.com/shixumi/snowball` (private; user `shixumi`). Working tree clean. v0.1 metrics: 35/35 plan tasks complete, 36/36 unit tests passing (7 test classes), debug APK 10.1 MB, release APK 7.5 MB unsigned. Tag `v0.1.0` pushed.

Latest commits (newest first):
- `bc1e7c7` — App icon (adaptive icon + Gemini-generated snowball PNG) + fix swipe-right undo stale closure via `rememberUpdatedState`
- `b8935e6` — Swipe-to-toggle payments on Home (`SwipeToDismissBox`, haptic, directional gating, tap-fallback)
- `3b8af76` — "Payments already made" field in DebtFormScreen — backfills synthetic Payment rows so users can import partially-paid debts
- `3749cb3` / `656327e` — Form field editability fixes (observable state + start-date)
- `a1808ec` — Bottom nav background extends through gesture-nav area
- `f27bce7` — Icons in bottom nav + category badges
- `1efd305` / `c12f0c4` — Settings: income-per-cutoff input + ViewModel
- `be150ca` / `8419196` / `902a199` — Debts list (FAB + edit) + form + form VM
- `ff371b9` / `643a718` — Home cutoff dashboard + hero card

Design intent and full spec/plan live in:
- Spec: `docs/superpowers/specs/2026-05-12-snowball-design.md`
- v0.1 plan: `docs/superpowers/plans/2026-05-12-snowball-v01-android.md`
- App icon prompt: `docs/design/app-icon-prompt.md`
- Dev environment setup: `docs/setup/android-dev-environment.md`

Identity = "Editorial Frost" (navy `#0B0F14` bg + champagne pinpoint + ice-blue orbit arc). Fonts: Fraunces (display, with SOFT axis up) + DM Sans (body/labels). Snow*ball* wordmark italicizes "ball". Microcopy: "**LEFT OVER**" (renamed from "BREATHING ROOM" — that string wrapped at 4sp letter-spacing), "melted", "cleared". Tagline tested in mockups: "*A quieter way to owe.*" Currency locked to **PHP** for v1.

Decisions worth knowing:
- **Categories are user-configurable**, but **Credit Card** and **MISC** are locked-in defaults. MISC is read-only ledger — shown but excluded from cutoff totals (user pays manually).
- Cutoff math: **30th-payday cutoff covers days 1–14** of next month; **15th-payday cutoff covers days 15–30** of same month. (Day 15 belongs to the 15th cutoff — user clarified this at line 202 of the failed transcript.) End-of-month edge handled via `useLastDay` flag on `Debt.dueDay`.
- `paymentsMade` is derived from Payment row count; `totalPayments` is original duration. The backfill field inserts synthetic Payment rows dated at `startDate`.
- An already-paid-off debt (paymentsAlreadyMade == totalPayments) auto-archives on save.
- Snowball-history visualization deferred to v0.2 (intentional).
- Bug from "I added a debt for may 12 and due date is 5, not populating in home tab" is **deferred to v0.2** per user's call.

## What's left

### Immediate (the thing that crashed the session)

User's last request before the crash: **run an independent UX/UI inspector subagent** to do an unbiased review of the app on the emulator. The dispatched subagent (`aa0131d78fd4581ce`) captured one screenshot (`%TEMP%\snowball-home-empty.png`) then crashed reading it — so no `docs/design/ux-review-2026-05-12.md` was produced.

Next concrete steps:
1. Re-dispatch the UX inspector with a hard rule: **do not call the Read tool on PNGs**. Inspect via code under `composeApp/src/commonMain/kotlin/com/snowball/ui/` + adb behavioral signals (`dumpsys window`, `dumpsys input`, logcat, `dumpsys activity top`). The screenshots stay on disk for the user.
2. Output goes to `docs/design/ux-review-2026-05-12.md` per the format in the original prompt (TL;DR, Strengths, Issues by severity, Accessibility, Priority order, Methodology).
3. The full inspector prompt is at line 1213 of the failed transcript — reuse it but strip steps 2–3 (screenshot+Read) and replace with "code + adb-only".

### v0.2 — explicitly deferred during planning

A new plan needs to be written for v0.2 (no plan doc exists yet). Scope agreed during the spec/plan discussion (transcript lines 222, 230, 289, 356, 806, 995, 1004, 1040, 1104):

**UI screens / features**
- **Debt Detail screen** — header (name, category badge, status), big progress arc with `X of Y paid`, stats (amount left, monthly, due day, start date, projected end date), payment history list (tap row to undo that payment), actions (Edit · Archive · Delete)
- **Archive view** — accessed via Active/Archived filter on Debts tab; paid-off debts, read-only
- **MISC items UI** — separate FAB action ("Add MISC item"), rendered as own section at bottom of Debts list, read-only ledger (no progress arc, excluded from cutoff totals)
- **Snowball journey card** on Home — % cleared, total melted, debt-free forecast date (all derived from existing data, no extra inputs)
- **"Up next" mini-card** on Home — next cutoff's total + window. Resolves the "I added a debt for May 12 due day 5 and don't see it" confusion (correct behavior in v0.1, but invisible without this card).
- **Category Management screen** — pushed from Settings; list categories with system-lock icons on Credit Card + MISC; inline rename (system not editable); "+ New category"; delete blocked for system, prompts to reassign for user categories with debts. Adds `iconKey` column + icon picker. *(User said in the session: defer this even though only Credit Card + MISC ship as system.)*

**Behaviors**
- **Overdue rollover with red tag** — unpaid debts from a past cutoff carry forward and surface visually
- **Auto-rollover at midnight** — cutoff dashboard advances automatically at midnight of the cutoff boundary
- **Per-debt payment history view** — implied by the Debt Detail screen above; also unblocks the v0.1 caveat that backfilled payments all share `startDate` as `paidDate` (looks weird only here)

**Platform expansion**
- **Notifications** — payday-style on 15th and 30th. Platform-specific via `expect/actual`: Android `AlarmManager` + `NotificationCompat`; iOS `UNUserNotificationCenter`; desktop native OS notifications. Settings already has the toggle + time-of-day picker stub.
- **iOS target** — Compose Multiplatform; build on Mac via Xcode; free signing for daily personal use until $99/yr cert for TestFlight
- **macOS desktop target** — fast iteration during development; usable client
- **Windows desktop target** — user's work machine

**Out of scope even for v0.2** (parked further out, per spec): cross-device sync / cloud backup, snowball-method *recommendations* ("attack this next"), avalanche method, what-if extra-payment simulations, per-debt custom notification timing, cutoff history view, spreadsheet import, multiple income sources / variable cutoff income, multi-currency, light mode, localization, search/sort in Debts list.

## Gotchas

- **`Read` tool crashes on certain PNGs from this session with `API Error: 400 Could not process image`.** Affected files include `C:\Users\Pika\AppData\Local\Temp\snowball-home-empty.png` (UX inspector) and the user's icon `C:\Users\Pika\Downloads\Gemini_Generated_Image_fas1xhfas1xhfas1.png`. This bug recurred **three times during this session** — subagents `a2fbb9b49b6c9aaf6` (Task 2), `ad5389522fe3460ec` (Phase 5+6), and `aa0131d78fd4581ce` (UX inspector). For the first two, the main agent worked around it by verifying repo state directly and committing on the subagent's behalf. For the third, the main agent itself ran Read on a PNG to test and **stayed poisoned** — every reply after that returned the same error until session restart. Rule: never call Read on PNGs. PowerShell `System.Drawing` works fine for resize/dimensions (that's how the launcher icons were built).
- Emulator is `emulator-5554` (AVD name `Pixel_7`, API 36, Android 16), screen 1080×2400. **Emulator-only dev path** — user explicitly chose option B (no USB debugging to real phone). Sideload to S25 by copying `composeApp\build\outputs\apk\debug\composeApp-debug.apk` via Drive or USB-MTP when ready.
- Phone target: **Samsung S25**, One UI 8.5, Android 16, API 36.1.
- Env vars set persistently but session inherits stale env from before they were set. Every shell command must explicitly set `$env:JAVA_HOME` and `$env:ANDROID_HOME`, and call `adb` via full path. `adb` not on PATH — use `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"`.
- Toolchain: **JDK 21** (bundled with Android Studio Panda 2025.3.4), not JDK 17. Gradle wrapper takes over after one-time bootstrap from `$env:LOCALAPPDATA\Temp\gradle-bootstrap\gradle-8.13`. AGP 8.7 supports both.
- Git LF→CRLF warnings on every commit are harmless; don't try to fix.
- Build command pattern that works:
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
  $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
  .\gradlew.bat :composeApp:assembleDebug
  ```
- App package is `com.snowball`; launcher activity `com.snowball/.MainActivity`. Tag `v0.1.0` exists locally and on remote.
- Finsky errors in logcat about `com.snowball` are Play Store complaining about a sideloaded package — ignore.
- Subagents default to bash shell on Windows. The first UX inspector dispatch crashed on `&` / `$env:` syntax before the screenshot crash. Any new subagent on Windows should be told explicitly to use PowerShell.
- SQLDelight JDBC driver is JVM-only — repository tests live in `androidUnitTest`, not `commonTest`. Don't move them.
- Brainstorm scratch dir `.superpowers/brainstorm/` is gitignored. A frontend-design preview server may still be running on `http://localhost:55195` from the brainstorming phase (low priority; can be killed).
- Two form input bugs already fixed (`656327e` for text fields, `3749cb3` for start date) had the same root cause: `var state` instead of `mutableStateOf`, plus binding `value` to a parsed type that can't represent in-progress input. Watch for this pattern in any new fields.

## How to resume

Paste this into a fresh session:

> Read `HANDOFF.md` in `C:\Users\Pika\projects\snowball`. The previous session got poisoned by `API Error: 400 Could not process image` after dispatching a UX inspector subagent that tried to Read a screenshot PNG. Pick up at the "What's left" section: re-dispatch the UX/UI inspector, but this time forbid Read on PNGs — the inspector must work from `composeApp/src/commonMain/kotlin/com/snowball/ui/` source + adb behavioral signals (`dumpsys`, logcat) only. Reuse the original prompt at line 1213 of `C:\Users\Pika\.claude\projects\C--Users-Pika-projects-snowball\826c9af7-02fb-4d81-98a1-0f444df0d973.jsonl`, deleting the screencap+Read steps. Output to `docs/design/ux-review-2026-05-12.md`. App is built and installed on `emulator-5554` at HEAD `bc1e7c7`.
