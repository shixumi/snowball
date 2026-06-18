# Snowball iOS — Mac Build Runbook

Everything Kotlin/Gradle/Swift is already written and pushed. These are the steps **on your Mac** to build, sign (free Apple ID), and run on your iPhone. If anything errors, copy the message back to the Windows session and it'll be fixed there.

## One-time setup on the Mac

1. **Xcode** from the App Store, then command-line tools:
   ```
   xcode-select --install
   ```
2. **A JDK 17** (Gradle/AGP build the Kotlin framework with it). The build-phase script calls `/usr/libexec/java_home -v 17` directly — so as long as a JDK 17 is installed it'll be picked up, regardless of your shell's `JAVA_HOME` or whatever newer JDK is the system default. Verify one is present: `/usr/libexec/java_home -v 17` should print a path. If not, install one (`brew install --cask temurin@17`).
3. **Homebrew** (if missing) + **xcodegen** (generates the Xcode project from the checked-in `project.yml`):
   ```
   brew install xcodegen
   ```
4. Kotlin/Native's iOS toolchain downloads automatically on the first Gradle build — no action needed, just expect the first build to take several minutes.

## Build & run

```bash
git clone https://github.com/shixumi/snowball.git   # or: git pull
cd snowball/iosApp
xcodegen generate
open Snowball.xcodeproj
```

In Xcode:
1. **Signing:** select the **Snowball** target → *Signing & Capabilities* → tick **Automatically manage signing** → **Team** = your personal Apple ID (add it under Xcode ▸ Settings ▸ Accounts if it's not listed).
2. **Bundle id:** it's `com.snowball.app`. If Xcode complains it's unavailable for a free account, change it to something unique (e.g. `com.<yourname>.snowball`) — in `iosApp/project.yml` (`PRODUCT_BUNDLE_IDENTIFIER`) then re-run `xcodegen generate`, or just edit it inline in Xcode.
3. **Device:** plug in the iPhone, unlock it, "Trust This Computer", then pick it as the run destination (top bar).
4. **Run (⌘R).** The first build invokes Gradle to produce the `ComposeApp` framework (slow the first time).
5. **On the iPhone (first run only):** Settings ▸ General ▸ VPN & Device Management ▸ trust your developer certificate, then launch the app.

## Free Apple ID reality

- The app **stops working after 7 days**. To refresh: plug in, open the project, press Run again. (No code changes needed.)
- 3-app limit per free account; fine for one app.

## If it errors — likely spots (send the message back to Windows)

- **Gradle/JDK:** "invalid source release" / a bare version number like `25.0.2` as the failure → no JDK 17 installed, so `java_home -v 17` fell through. Install Temurin 17.
- **Script sandbox:** "Sandbox: bash deny..." → `ENABLE_USER_SCRIPT_SANDBOXING=NO` is already set in `project.yml`; make sure you re-ran `xcodegen generate` after any edit.
- **Framework not found / `import ComposeApp` fails:** the pre-build "Build Compose (Kotlin) framework" script didn't run or `FRAMEWORK_SEARCH_PATHS` is off — grab the Xcode build log for that phase.
- **Kotlin/Native compile errors in `iosMain`:** these only surface here (couldn't be compiled on Windows). Most likely candidates: the `UIImpactFeedbackGenerator`/`UIImpactFeedbackStyle` API names in `Haptics.ios.kt`, or the `ComposeUIViewController` import. Paste the exact error.
- **Signing/bundle-id conflicts:** change the bundle id as in step 2.

## Known v1 gaps (intentional, follow-ups)

- **Notifications are stubbed on iOS** — the Settings toggle persists but doesn't schedule yet (Android still works fully). Implementing `UNUserNotificationCenter` is a planned follow-up.
- **App icon** is the iOS default; a real icon is a follow-up.
- Once it runs on your phone, we tag **v0.5.0** (first multi-platform release).
