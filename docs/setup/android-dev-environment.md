# Android Dev Environment Setup (Windows)

One-time setup so the Snowball v0.1 plan can be executed.

## What you'll have when done

- **JDK 17** (bundled inside Android Studio as JBR — JetBrains Runtime)
- **Android SDK** (API 34 platform, build tools, command-line tools, platform-tools)
- **Android Emulator** with a Pixel-class AVD
- **Gradle** (used by Android Studio internally and via the project's `gradlew` wrapper)
- `java`, `adb`, `emulator` on PATH; `JAVA_HOME` and `ANDROID_HOME` env vars set

---

## Step 1 — Install Android Studio

1. Go to https://developer.android.com/studio
2. Click **Download Android Studio**
3. Run the installer — accept the defaults. Default install path is `C:\Program Files\Android\Android Studio\`.
4. Launch Android Studio. The first-run wizard appears.

## Step 2 — Run the Setup Wizard

In the wizard:
1. **Install Type:** Standard
2. **UI Theme:** your call
3. **Verify Settings:** confirm SDK location is `C:\Users\Pika\AppData\Local\Android\Sdk` (default — note it down)
4. **License Agreement:** accept all licenses
5. Click **Finish** — components download (~1–2 GB)

## Step 3 — Add API 34 platform

From the welcome screen:
1. **More Actions → SDK Manager** (or in the IDE: **Settings → Languages & Frameworks → Android SDK**)
2. **SDK Platforms** tab:
   - Check **Android 14.0 ("UpsideDownCake") — API Level 34**
   - Click **Apply**, accept the license
3. **SDK Tools** tab — confirm these are checked:
   - ✅ Android SDK Build-Tools 34
   - ✅ Android Emulator
   - ✅ Android SDK Platform-Tools
   - ✅ Android SDK Command-line Tools (latest)
   - Click **Apply** if any need install

## Step 4 — Create an emulator

From the welcome screen:
1. **More Actions → Virtual Device Manager** (or in IDE: **Tools → Device Manager**)
2. Click **Create Device**
3. Pick **Pixel 7** (or any modern phone)
4. **System Image:** pick **API 34 (Tiramisu)** — download if needed
5. **Finish** → click ▶ to boot it once and confirm it lands at the Android home screen

## Step 5 — Set environment variables

Open PowerShell (as your user, not admin) and run these. They persist permanently for your user.

```powershell
setx ANDROID_HOME "$env:LOCALAPPDATA\Android\Sdk"
setx JAVA_HOME "C:\Program Files\Android\Android Studio\jbr"
$existing = [System.Environment]::GetEnvironmentVariable("Path", "User")
$additions = "$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:LOCALAPPDATA\Android\Sdk\emulator;$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin;C:\Program Files\Android\Android Studio\jbr\bin"
[System.Environment]::SetEnvironmentVariable("Path", "$existing;$additions", "User")
```

> `setx` writes to the user environment but **does not update the current shell**. Close and reopen any terminal after running these.

## Step 6 — Verify

Open a **new** terminal (PowerShell or bash) and run:

```bash
java -version      # expect: openjdk 17.x (JetBrains Runtime)
adb version        # expect: Android Debug Bridge version 1.0.x
emulator -list-avds  # expect: at least one AVD listed
```

If all three succeed, you're done.

## Troubleshooting

**`java -version` shows nothing or wrong version:**
- Confirm `C:\Program Files\Android\Android Studio\jbr\bin\java.exe` exists
- Open a fresh terminal (env vars only apply to new shells)
- Run `echo $env:JAVA_HOME` (PowerShell) — should print the jbr path

**`adb` not found:**
- Confirm `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe` exists
- Confirm PATH includes that directory (`echo $env:Path`)

**Emulator won't boot:**
- Likely virtualization is disabled in BIOS or Hyper-V conflict. Android Studio shows a hardware-acceleration check on launch — follow its guidance.
- On Windows 11, the Android Emulator uses Windows Hypervisor Platform (WHPX). Enable it in **Windows Features**.

**SDK install runs out of space:**
- Default location uses ~5 GB. Free up space or change SDK location during the wizard.
