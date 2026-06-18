# Snowball — iOS Port (Compose Multiplatform)

**Date:** 2026-06-14
**Status:** Approved (autonomous; build/sign on user's Mac)
**Target:** running on the user's iPhone via free Apple ID signing

## Goal

Run Snowball on iOS. The app is already Kotlin/Compose Multiplatform, so all of `commonMain` (UI, domain, data, ViewModels) runs unchanged. The work is: add iOS Gradle targets + framework, write iOS `actual`s for the platform seams, and add a thin Xcode/SwiftUI shell that hosts the Compose UI.

## Division of labor

- **Windows (Claude):** write all Kotlin/Gradle/Swift/config, commit, push. Cannot compile iOS here (Kotlin/Native iOS needs macOS), so this is build-ready-but-unverified code.
- **Mac (user):** pull, generate the Xcode project, build/sign with a free Apple ID, run to the iPhone — following the runbook. Compile errors get pasted back here and fixed from Windows. Expect 1–2 iteration rounds.

## Architecture

**Gradle (`composeApp/build.gradle.kts`):**
- Add `iosArm64()` and `iosSimulatorArm64()` targets.
- Each target exposes a static framework named `ComposeApp` (the canonical CMP iOS setup):
  ```kotlin
  listOf(iosArm64(), iosSimulatorArm64()).forEach { t ->
      t.binaries.framework { baseName = "ComposeApp"; isStatic = true }
  }
  ```
- New `iosMain` source set (depends on `commonMain`), with `iosArm64Main`/`iosSimulatorArm64Main` depending on it.
- `iosMain.dependencies { implementation(libs.sqldelight.native.driver) }` (new catalog entry, version ref = existing `sqldelight`).

**iOS source (`composeApp/src/iosMain/kotlin/...`):**
- `MainViewController.kt` — the entry point hosting Compose:
  ```kotlin
  fun MainViewController(): UIViewController = ComposeUIViewController {
      val db = remember { createDatabase(DatabaseDriverFactory()) }
      val repos = remember { Repos(db) }
      val scheduler = remember { NotificationScheduler() }
      App(repos = repos, notificationScheduler = scheduler)
  }
  ```
- iOS `actual`s:
  - **`DatabaseFactory.ios.kt`** — `NativeSqliteDriver(SnowballDb.Schema, "snowball.db")`. (Essential, low risk.)
  - **`Haptics.ios.kt`** — `UIImpactFeedbackGenerator`: `tick()` = light, `thump()` = medium. (Real impl, low risk.)
  - **`SystemBackHandler.ios.kt`** — no-op (iOS has no system back button; in-app nav handles it). (Trivial.)
  - **`NotificationScheduler.ios.kt`** + **`Permissions.ios.kt`** — **stubbed for v1** (`schedule`/`cancel` no-op, `hasPermission()`/permission request return `true`). The Settings toggle still persists state; iOS scheduling lands in a follow-up. This deliberately keeps the first iOS build free of risky `UNUserNotificationCenter` interop while we get the app onto the phone. Documented as a known gap.

**Xcode shell (`iosApp/`):**
- Authored on Windows as an **xcodegen `project.yml`** + Swift sources + `Info.plist` (hand-writing a `.pbxproj` blind is too fragile). The Mac runs `xcodegen generate` to produce the real `.xcodeproj`.
- `iOSApp.swift` (SwiftUI `@main App`) → `ContentView.swift` wraps `MainViewControllerKt.MainViewController()` via `UIViewControllerRepresentable`, ignoring safe-area so Compose draws full-bleed (our screens already handle insets).
- A Run Script build phase runs `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode` (the standard CMP framework-embedding task), with `FRAMEWORK_SEARCH_PATHS` wired to the Gradle output. `project.yml` encodes this build phase + settings (bundle id `com.snowball`, deployment target iOS 15, `LaunchScreen`).

## Out of scope (v1 port)
- iOS local notifications (stubbed; follow-up).
- App icons / polished launch screen (a plain launch screen; icons later).
- Data export (separate pending feature).
- No change to Android behavior.

## Risks (honest)
- **Cannot compile iOS from Windows** — first Mac build will surface Kotlin/Native + Xcode issues; we iterate.
- **Framework embedding build phase** and **xcodegen settings** are the most likely to need adjustment on the Mac.
- Free Apple ID: 7-day expiry; must re-run from Xcode weekly. Bundle id may need to be unique per Apple ID (note in runbook).

## Testing
- Existing unit suite (commonTest) is unaffected and must stay green on the Android/JVM test target.
- iOS correctness is verified by building + running on the device (manual) — no iOS test target added in v1.

## Deliverable
- All iOS code/config committed + pushed.
- A `docs/ios-build-runbook.md` with exact Mac steps (toolchain install, xcodegen, signing with free Apple ID, run to iPhone, weekly re-sign).

## Tag
v0.5.0 (first multi-platform target) once it runs on device.
