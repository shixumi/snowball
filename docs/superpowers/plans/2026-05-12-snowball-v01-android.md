# Snowball v0.1 — Android MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a sideload-ready Android APK that shows the user's current debt-cutoff at a glance, lets them add scheduled debts, mark payments as paid, and set their per-cutoff income — replacing the spreadsheet for the core daily workflow.

**Architecture:** Compose Multiplatform project with the Android target only in v0.1. A single shared module (`composeApp`) keeps UI and logic in `commonMain` so future iOS/macOS/Windows targets drop in without rewrite. SQLDelight provides type-safe SQLite. State is hoisted into screen-level ViewModels backed by Kotlin coroutine `StateFlow`. No DI library — manual factories. No DRY abstraction work beyond what the screens demand.

**Tech Stack:**
- Kotlin 2.1.20
- Compose Multiplatform 1.8.0
- SQLDelight 2.0.2 (Android driver)
- Android Gradle Plugin 8.11.x, Gradle 8.13.x, JDK 21 (bundled with Android Studio Panda)
- kotlinx-datetime 0.6.1, kotlinx-coroutines 1.9.0
- Target: Android API 36 (Android 16) — user's daily driver is a Samsung S25
- Fonts: Fraunces (variable, OFL) and DM Sans (variable, OFL) — bundled

**Spec reference:** `docs/superpowers/specs/2026-05-12-snowball-design.md`

**Scope cuts from spec:** Categories management UI, MISC items, Debt Detail screen, Archive view, Snowball journey card, "Up next" card, overdue rollover, notifications, iOS/macOS/Windows targets — all deferred to follow-up plans.

---

## File Map

Project layout once v0.1 is complete:

```
Snowball/
├── settings.gradle.kts
├── build.gradle.kts                            # root build script
├── gradle.properties
├── gradle/libs.versions.toml                   # version catalog
├── composeApp/
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/
│       │   ├── kotlin/com/snowball/
│       │   │   ├── App.kt                      # root composable, nav host
│       │   │   ├── data/
│       │   │   │   ├── db/
│       │   │   │   │   ├── DatabaseFactory.kt  # expect declaration
│       │   │   │   │   └── Database.kt         # SqlDriver → SnowballDb wrapper
│       │   │   │   ├── model/
│       │   │   │   │   ├── Category.kt
│       │   │   │   │   ├── Debt.kt
│       │   │   │   │   ├── Payment.kt
│       │   │   │   │   └── Settings.kt
│       │   │   │   └── repo/
│       │   │   │       ├── CategoryRepository.kt
│       │   │   │       ├── DebtRepository.kt
│       │   │   │       ├── PaymentRepository.kt
│       │   │   │       └── SettingsRepository.kt
│       │   │   ├── domain/
│       │   │   │   ├── DateUtils.kt            # last-day-of-month, etc.
│       │   │   │   ├── Cutoff.kt               # window types and detection
│       │   │   │   └── CutoffCalculator.kt     # due-this-cutoff logic
│       │   │   └── ui/
│       │   │       ├── theme/
│       │   │       │   ├── Color.kt
│       │   │       │   ├── Type.kt
│       │   │       │   └── Theme.kt
│       │   │       ├── components/
│       │   │       │   ├── HeroAmount.kt
│       │   │       │   ├── ProgressArc.kt
│       │   │       │   ├── PesoText.kt
│       │   │       │   └── CutoffCard.kt
│       │   │       ├── nav/
│       │   │       │   └── Nav.kt              # routes + bottom nav
│       │   │       ├── home/
│       │   │       │   ├── HomeScreen.kt
│       │   │       │   └── HomeViewModel.kt
│       │   │       ├── debts/
│       │   │       │   ├── DebtsScreen.kt
│       │   │       │   └── DebtsViewModel.kt
│       │   │       ├── form/
│       │   │       │   ├── DebtFormScreen.kt
│       │   │       │   └── DebtFormViewModel.kt
│       │   │       └── settings/
│       │   │           ├── SettingsScreen.kt
│       │   │           └── SettingsViewModel.kt
│       │   ├── sqldelight/com/snowball/db/
│       │   │   ├── Category.sq
│       │   │   ├── Debt.sq
│       │   │   ├── Payment.sq
│       │   │   └── Settings.sq
│       │   └── composeResources/
│       │       └── font/                       # Fraunces*.ttf, DMSans*.ttf
│       ├── androidMain/
│       │   ├── AndroidManifest.xml
│       │   └── kotlin/com/snowball/
│       │       ├── MainActivity.kt
│       │       └── data/db/DatabaseFactory.android.kt  # AndroidSqliteDriver
│       ├── commonTest/
│       │   └── kotlin/com/snowball/domain/
│       │       ├── DateUtilsTest.kt
│       │       ├── CutoffTest.kt
│       │       └── CutoffCalculatorTest.kt
│       └── androidUnitTest/
│           └── kotlin/com/snowball/repo/
│               ├── CategoryRepositoryTest.kt
│               ├── DebtRepositoryTest.kt
│               ├── PaymentRepositoryTest.kt
│               └── SettingsRepositoryTest.kt
├── docs/superpowers/
│   ├── specs/2026-05-12-snowball-design.md
│   └── plans/2026-05-12-snowball-v01-android.md
└── .gitignore
```

---

## Phase 1 — Project Foundation (Tasks 1–4)

### Task 1: Gradle skeleton

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `composeApp/build.gradle.kts`

- [ ] **Step 1: Write `gradle.properties`**

Create `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx4096M -Dfile.encoding=UTF-8 -Dkotlin.daemon.jvm.options\="-Xmx2g"
org.gradle.parallel=true
org.gradle.caching=true
kotlin.code.style=official
android.useAndroidX=true
android.nonTransitiveRClass=true
```

- [ ] **Step 2: Write the version catalog**

Create `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.1.20"
agp = "8.11.0"
compose-multiplatform = "1.8.0"
androidx-activity-compose = "1.9.3"
sqldelight = "2.0.2"
kotlinx-datetime = "0.6.1"
kotlinx-coroutines = "1.9.0"
android-min-sdk = "26"
android-target-sdk = "36"
android-compile-sdk = "36"

[libraries]
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "androidx-activity-compose" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-sqlite-driver = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqldelight" }
sqldelight-coroutines-extensions = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
android-application = { id = "com.android.application", version.ref = "agp" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

- [ ] **Step 3: Write `settings.gradle.kts`**

Create `settings.gradle.kts`:

```kotlin
rootProject.name = "Snowball"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
```

- [ ] **Step 4: Write root `build.gradle.kts`**

Create `build.gradle.kts` (project root):

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.sqldelight) apply false
}
```

> Note: TOML aliases use hyphens (`kotlin-multiplatform`), but Gradle's generated Kotlin DSL accessors map hyphens to dots. Use `libs.plugins.kotlin.multiplatform` in Kotlin scripts.

- [ ] **Step 5: Write `composeApp/build.gradle.kts`**

Create `composeApp/build.gradle.kts`:

```kotlin
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.sqldelight.android.driver)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        getByName("androidUnitTest").dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.snowball.resources"
}

android {
    namespace = "com.snowball"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()

    defaultConfig {
        applicationId = "com.snowball"
        minSdk = libs.versions.android.min.sdk.get().toInt()
        targetSdk = libs.versions.android.target.sdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        getByName("release") { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

sqldelight {
    databases {
        create("SnowballDb") {
            packageName.set("com.snowball.db")
        }
    }
}
```

- [ ] **Step 6: Add Android manifest**

Create `composeApp/src/androidMain/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="Snowball"
        android:icon="@android:drawable/sym_def_app_icon"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 7: Verify Gradle sync**

Run: `./gradlew help`
Expected: build script evaluates with no errors. No tasks run; just config validation.

If `gradle-wrapper.jar` is missing, run `gradle wrapper` (with system Gradle) first.

- [ ] **Step 8: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/libs.versions.toml composeApp/build.gradle.kts composeApp/src/androidMain/AndroidManifest.xml
git commit -m "chore: gradle skeleton for compose multiplatform android target"
```

---

### Task 2: Hello-world Compose entry point

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/App.kt`
- Create: `composeApp/src/androidMain/kotlin/com/snowball/MainActivity.kt`

- [ ] **Step 1: Write `App.kt`**

Create `composeApp/src/commonMain/kotlin/com/snowball/App.kt`:

```kotlin
package com.snowball

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun App() {
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0B0F14)),
            contentAlignment = Alignment.Center
        ) {
            Text("Snowball", color = Color(0xFFF2F5F8))
        }
    }
}
```

- [ ] **Step 2: Write `MainActivity.kt`**

Create `composeApp/src/androidMain/kotlin/com/snowball/MainActivity.kt`:

```kotlin
package com.snowball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}
```

- [ ] **Step 3: Build and verify**

Run: `./gradlew :composeApp:assembleDebug`
Expected: BUILD SUCCESSFUL. APK written to `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.

- [ ] **Step 4: Sideload to a device or emulator**

Run: `adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk`
Expected: `Success`. Launch the "Snowball" app from the launcher and verify the dark screen with "Snowball" text appears.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/App.kt composeApp/src/androidMain/kotlin/com/snowball/MainActivity.kt
git commit -m "feat: hello-world compose entry point"
```

---

### Task 3: Bundle Fraunces and DM Sans fonts

**Files:**
- Add: `composeApp/src/commonMain/composeResources/font/Fraunces-Variable.ttf`
- Add: `composeApp/src/commonMain/composeResources/font/DMSans-Variable.ttf`

- [ ] **Step 1: Download Fraunces variable font**

```powershell
$dest = "composeApp\src\commonMain\composeResources\font"
New-Item -ItemType Directory -Path $dest -Force | Out-Null
Invoke-WebRequest `
  -Uri "https://raw.githubusercontent.com/googlefonts/fraunces/master/fonts/Fraunces%5BSOFT%2CWONK%2Copsz%2Cwght%5D.ttf" `
  -OutFile "$dest\Fraunces-Variable.ttf" -UseBasicParsing
```

File size should be ~350 KB. Note the repo's default branch is `master`, not `main`.

- [ ] **Step 2: Download DM Sans variable font**

```powershell
Invoke-WebRequest `
  -Uri "https://raw.githubusercontent.com/googlefonts/dm-fonts/main/Sans/fonts/variable/DMSans%5Bopsz%2Cwght%5D.ttf" `
  -OutFile "$dest\DMSans-Variable.ttf" -UseBasicParsing
```

File size should be ~247 KB. Both files' first 4 bytes must be `00 01 00 00` (TrueType signature) — confirm before proceeding.

- [ ] **Step 3: Verify resources compile**

Run: `./gradlew :composeApp:assembleDebug`
Expected: BUILD SUCCESSFUL. The generated `Res` object will expose `Res.font.Fraunces_Variable` and `Res.font.DMSans_Variable` (used in Task 18).

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/composeResources/font/
git commit -m "chore: bundle fraunces and dm sans variable fonts"
```

---

### Task 4: SQLDelight smoke schema

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/com/snowball/db/_Init.sq`

- [ ] **Step 1: Add a no-op schema file**

Create `composeApp/src/commonMain/sqldelight/com/snowball/db/_Init.sq`:

```sql
-- Sentinel file so the SqlDelight plugin generates the SnowballDb class.
-- Real tables live in Category.sq, Debt.sq, Payment.sq, Settings.sq.
```

- [ ] **Step 2: Generate database**

Run: `./gradlew :composeApp:generateCommonMainSnowballDbInterface`
Expected: BUILD SUCCESSFUL. `composeApp/build/generated/sqldelight/code/SnowballDb/commonMain/com/snowball/db/SnowballDb.kt` exists.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/sqldelight/com/snowball/db/_Init.sq
git commit -m "chore: scaffold sqldelight database"
```

---

## Phase 2 — Data Schema (Tasks 5–9)

### Task 5: Category schema with seed

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/com/snowball/db/Category.sq`

- [ ] **Step 1: Write schema**

Create `composeApp/src/commonMain/sqldelight/com/snowball/db/Category.sq`:

```sql
CREATE TABLE Category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    isSystem INTEGER NOT NULL DEFAULT 0,    -- 0=user, 1=system
    behavior TEXT NOT NULL,                  -- 'SCHEDULED' | 'LEDGER'
    createdAt INTEGER NOT NULL
);

-- Seed: system categories. These are inserted in DatabaseFactory after creation
-- via INSERT OR IGNORE so reruns are safe.
selectAll:
SELECT * FROM Category ORDER BY isSystem DESC, name ASC;

selectById:
SELECT * FROM Category WHERE id = ?;

insert:
INSERT INTO Category(name, isSystem, behavior, createdAt) VALUES (?, ?, ?, ?);

insertOrIgnore:
INSERT OR IGNORE INTO Category(name, isSystem, behavior, createdAt) VALUES (?, ?, ?, ?);

renameById:
UPDATE Category SET name = :name WHERE id = :id AND isSystem = 0;

deleteById:
DELETE FROM Category WHERE id = ? AND isSystem = 0;
```

- [ ] **Step 2: Regenerate database**

Run: `./gradlew :composeApp:generateCommonMainSnowballDbInterface`
Expected: BUILD SUCCESSFUL. `CategoryQueries` interface generated.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/sqldelight/com/snowball/db/Category.sq
git commit -m "feat(db): category schema with system flag and behavior"
```

---

### Task 6: Debt schema

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/com/snowball/db/Debt.sq`

- [ ] **Step 1: Write schema**

Create `composeApp/src/commonMain/sqldelight/com/snowball/db/Debt.sq`:

```sql
CREATE TABLE Debt (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    categoryId INTEGER NOT NULL REFERENCES Category(id),
    monthlyAmount REAL NOT NULL,
    totalPayments INTEGER NOT NULL,
    dueDay INTEGER NOT NULL,                   -- 1..30
    useLastDayOfMonth INTEGER NOT NULL DEFAULT 0,  -- 0|1
    startDate TEXT NOT NULL,                   -- ISO yyyy-MM-dd
    isArchived INTEGER NOT NULL DEFAULT 0,
    notes TEXT,
    createdAt INTEGER NOT NULL
);

CREATE INDEX idx_debt_archived ON Debt(isArchived);
CREATE INDEX idx_debt_category ON Debt(categoryId);

selectAll:
SELECT * FROM Debt ORDER BY createdAt DESC;

selectActive:
SELECT * FROM Debt WHERE isArchived = 0 ORDER BY dueDay ASC;

selectById:
SELECT * FROM Debt WHERE id = ?;

selectByCategory:
SELECT * FROM Debt WHERE categoryId = ? AND isArchived = 0;

insert:
INSERT INTO Debt(name, categoryId, monthlyAmount, totalPayments, dueDay, useLastDayOfMonth, startDate, isArchived, notes, createdAt)
VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?);

update:
UPDATE Debt SET
    name = :name,
    categoryId = :categoryId,
    monthlyAmount = :monthlyAmount,
    totalPayments = :totalPayments,
    dueDay = :dueDay,
    useLastDayOfMonth = :useLastDayOfMonth,
    startDate = :startDate,
    notes = :notes
WHERE id = :id;

setArchived:
UPDATE Debt SET isArchived = :isArchived WHERE id = :id;

deleteById:
DELETE FROM Debt WHERE id = ?;
```

- [ ] **Step 2: Regenerate**

Run: `./gradlew :composeApp:generateCommonMainSnowballDbInterface`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/sqldelight/com/snowball/db/Debt.sq
git commit -m "feat(db): debt schema"
```

---

### Task 7: Payment schema

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/com/snowball/db/Payment.sq`

- [ ] **Step 1: Write schema**

Create `composeApp/src/commonMain/sqldelight/com/snowball/db/Payment.sq`:

```sql
CREATE TABLE Payment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    debtId INTEGER NOT NULL REFERENCES Debt(id) ON DELETE CASCADE,
    paidDate TEXT NOT NULL,                    -- ISO yyyy-MM-dd
    amount REAL NOT NULL,
    createdAt INTEGER NOT NULL
);

CREATE INDEX idx_payment_debt ON Payment(debtId);
CREATE INDEX idx_payment_paiddate ON Payment(paidDate);

selectAllForDebt:
SELECT * FROM Payment WHERE debtId = ? ORDER BY paidDate DESC, id DESC;

countForDebt:
SELECT COUNT(*) FROM Payment WHERE debtId = ?;

selectLatestForDebt:
SELECT * FROM Payment WHERE debtId = ? ORDER BY paidDate DESC, id DESC LIMIT 1;

selectInDateRange:
SELECT * FROM Payment WHERE debtId = ? AND paidDate >= :from AND paidDate <= :to ORDER BY paidDate DESC;

insert:
INSERT INTO Payment(debtId, paidDate, amount, createdAt) VALUES (?, ?, ?, ?);

deleteById:
DELETE FROM Payment WHERE id = ?;
```

- [ ] **Step 2: Regenerate**

Run: `./gradlew :composeApp:generateCommonMainSnowballDbInterface`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/sqldelight/com/snowball/db/Payment.sq
git commit -m "feat(db): payment schema with debt cascade"
```

---

### Task 8: Settings schema

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/com/snowball/db/Settings.sq`

- [ ] **Step 1: Write schema**

Create `composeApp/src/commonMain/sqldelight/com/snowball/db/Settings.sq`:

```sql
-- Single-row settings: id is always 1.
CREATE TABLE Settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    incomePerCutoff REAL NOT NULL DEFAULT 0.0,
    currency TEXT NOT NULL DEFAULT 'PHP',
    notificationsEnabled INTEGER NOT NULL DEFAULT 1,
    notificationHour INTEGER NOT NULL DEFAULT 9,
    notificationMinute INTEGER NOT NULL DEFAULT 0
);

select:
SELECT * FROM Settings WHERE id = 1;

insertIfMissing:
INSERT OR IGNORE INTO Settings(id, incomePerCutoff, currency, notificationsEnabled, notificationHour, notificationMinute)
VALUES (1, 0.0, 'PHP', 1, 9, 0);

setIncome:
UPDATE Settings SET incomePerCutoff = :income WHERE id = 1;

setNotificationsEnabled:
UPDATE Settings SET notificationsEnabled = :enabled WHERE id = 1;

setNotificationTime:
UPDATE Settings SET notificationHour = :hour, notificationMinute = :minute WHERE id = 1;
```

- [ ] **Step 2: Regenerate**

Run: `./gradlew :composeApp:generateCommonMainSnowballDbInterface`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/sqldelight/com/snowball/db/Settings.sq
git commit -m "feat(db): single-row settings schema"
```

---

### Task 9: Database factory with seed

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/data/db/DatabaseFactory.kt`
- Create: `composeApp/src/androidMain/kotlin/com/snowball/data/db/DatabaseFactory.android.kt`

- [ ] **Step 1: Common factory contract**

Create `composeApp/src/commonMain/kotlin/com/snowball/data/db/DatabaseFactory.kt`:

```kotlin
package com.snowball.data.db

import app.cash.sqldelight.db.SqlDriver
import com.snowball.db.SnowballDb

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(factory: DatabaseDriverFactory): SnowballDb {
    val driver = factory.createDriver()
    val db = SnowballDb(driver)
    seedSystemCategories(db)
    db.settingsQueries.insertIfMissing()
    return db
}

private fun seedSystemCategories(db: SnowballDb) {
    val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    db.categoryQueries.insertOrIgnore(
        name = "Credit Card",
        isSystem = 1,
        behavior = "SCHEDULED",
        createdAt = now
    )
    db.categoryQueries.insertOrIgnore(
        name = "MISC",
        isSystem = 1,
        behavior = "LEDGER",
        createdAt = now
    )
}
```

- [ ] **Step 2: Android actual**

Create `composeApp/src/androidMain/kotlin/com/snowball/data/db/DatabaseFactory.android.kt`:

```kotlin
package com.snowball.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.snowball.db.SnowballDb

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(SnowballDb.Schema, context, "snowball.db")
}
```

- [ ] **Step 3: Wire factory into MainActivity**

Modify `composeApp/src/androidMain/kotlin/com/snowball/MainActivity.kt` to construct the database before showing the UI:

```kotlin
package com.snowball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.snowball.data.db.DatabaseDriverFactory
import com.snowball.data.db.createDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = createDatabase(DatabaseDriverFactory(applicationContext))
        setContent { App() }
    }
}
```

(We'll thread `db` into the UI properly in Task 14. For now, it just needs to construct without crashing.)

- [ ] **Step 4: Build and verify install**

Run: `./gradlew :composeApp:assembleDebug` then `adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk`
Expected: install succeeds. Open the app — black screen with "Snowball" text. Logcat should show no DB errors.

Run: `adb shell run-as com.snowball ls -la databases/`
Expected: `snowball.db` exists.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/data/db/DatabaseFactory.kt composeApp/src/androidMain/kotlin/com/snowball/data/db/DatabaseFactory.android.kt composeApp/src/androidMain/kotlin/com/snowball/MainActivity.kt
git commit -m "feat(db): database factory with seeded system categories"
```

---

## Phase 3 — Data Layer (Tasks 10–13)

### Task 10: Domain models

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/data/model/Category.kt`
- Create: `composeApp/src/commonMain/kotlin/com/snowball/data/model/Debt.kt`
- Create: `composeApp/src/commonMain/kotlin/com/snowball/data/model/Payment.kt`
- Create: `composeApp/src/commonMain/kotlin/com/snowball/data/model/Settings.kt`

- [ ] **Step 1: Category**

Create `composeApp/src/commonMain/kotlin/com/snowball/data/model/Category.kt`:

```kotlin
package com.snowball.data.model

enum class CategoryBehavior { SCHEDULED, LEDGER }

data class Category(
    val id: Long,
    val name: String,
    val isSystem: Boolean,
    val behavior: CategoryBehavior,
)
```

- [ ] **Step 2: Debt**

Create `composeApp/src/commonMain/kotlin/com/snowball/data/model/Debt.kt`:

```kotlin
package com.snowball.data.model

import kotlinx.datetime.LocalDate

data class Debt(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val monthlyAmount: Double,
    val totalPayments: Int,
    val dueDay: Int,
    val useLastDayOfMonth: Boolean,
    val startDate: LocalDate,
    val isArchived: Boolean,
    val notes: String?,
)
```

- [ ] **Step 3: Payment**

Create `composeApp/src/commonMain/kotlin/com/snowball/data/model/Payment.kt`:

```kotlin
package com.snowball.data.model

import kotlinx.datetime.LocalDate

data class Payment(
    val id: Long,
    val debtId: Long,
    val paidDate: LocalDate,
    val amount: Double,
)
```

- [ ] **Step 4: Settings**

Create `composeApp/src/commonMain/kotlin/com/snowball/data/model/Settings.kt`:

```kotlin
package com.snowball.data.model

data class Settings(
    val incomePerCutoff: Double,
    val currency: String,
    val notificationsEnabled: Boolean,
    val notificationHour: Int,
    val notificationMinute: Int,
)
```

- [ ] **Step 5: Build to verify compilation**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/data/model/
git commit -m "feat(model): plain kotlin domain types for entities"
```

---

### Task 11: SettingsRepository

> **Test source set note (applies to Tasks 11–14):** Repository tests use SQLDelight's `JdbcSqliteDriver`, which is JVM-only and not available in `commonTest`. They live under `androidUnitTest` and run as part of `testDebugUnitTest`. Domain-only tests (Tasks 15–18) stay in `commonTest`.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/data/repo/SettingsRepository.kt`
- Create: `composeApp/src/androidUnitTest/kotlin/com/snowball/repo/SettingsRepositoryTest.kt`

- [ ] **Step 1: Write failing test**

Create `composeApp/src/androidUnitTest/kotlin/com/snowball/repo/SettingsRepositoryTest.kt`:

```kotlin
package com.snowball.repo

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.snowball.data.repo.SettingsRepository
import com.snowball.db.SnowballDb
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryTest {
    private fun freshRepo(): SettingsRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SnowballDb.Schema.create(driver)
        val db = SnowballDb(driver)
        db.settingsQueries.insertIfMissing()
        return SettingsRepository(db)
    }

    @Test
    fun reads_default_settings() {
        val repo = freshRepo()
        val s = repo.get()
        assertEquals(0.0, s.incomePerCutoff)
        assertEquals("PHP", s.currency)
    }

    @Test
    fun setIncome_persists() {
        val repo = freshRepo()
        repo.setIncome(25000.0)
        assertEquals(25000.0, repo.get().incomePerCutoff)
    }
}
```

> The `sqldelight-sqlite-driver` dep is already wired into `androidUnitTest` in Task 1's build script. No further build edits needed here.

- [ ] **Step 2: Run the test to verify failure**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.snowball.repo.SettingsRepositoryTest"`
Expected: FAIL with `SettingsRepository` unresolved reference.

- [ ] **Step 3: Implement repository**

Create `composeApp/src/commonMain/kotlin/com/snowball/data/repo/SettingsRepository.kt`:

```kotlin
package com.snowball.data.repo

import com.snowball.data.model.Settings
import com.snowball.db.SnowballDb

class SettingsRepository(private val db: SnowballDb) {

    fun get(): Settings {
        val row = db.settingsQueries.select().executeAsOne()
        return Settings(
            incomePerCutoff = row.incomePerCutoff,
            currency = row.currency,
            notificationsEnabled = row.notificationsEnabled == 1L,
            notificationHour = row.notificationHour.toInt(),
            notificationMinute = row.notificationMinute.toInt(),
        )
    }

    fun setIncome(amount: Double) {
        db.settingsQueries.setIncome(amount)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        db.settingsQueries.setNotificationsEnabled(if (enabled) 1L else 0L)
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        db.settingsQueries.setNotificationTime(hour.toLong(), minute.toLong())
    }
}
```

- [ ] **Step 4: Run test to verify pass**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.snowball.repo.SettingsRepositoryTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/data/repo/SettingsRepository.kt composeApp/src/androidUnitTest/kotlin/com/snowball/repo/SettingsRepositoryTest.kt
git commit -m "feat(repo): settings repository with tests"
```

---

### Task 12: CategoryRepository

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/data/repo/CategoryRepository.kt`
- Create: `composeApp/src/androidUnitTest/kotlin/com/snowball/repo/CategoryRepositoryTest.kt`

- [ ] **Step 1: Write failing test**

Create `composeApp/src/androidUnitTest/kotlin/com/snowball/repo/CategoryRepositoryTest.kt`:

```kotlin
package com.snowball.repo

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.snowball.data.db.createDatabase
import com.snowball.data.model.CategoryBehavior
import com.snowball.data.repo.CategoryRepository
import com.snowball.db.SnowballDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFails

class CategoryRepositoryTest {
    private fun freshRepo(): CategoryRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SnowballDb.Schema.create(driver)
        val db = SnowballDb(driver)
        // Seed system categories manually since createDatabase needs platform factory
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        db.categoryQueries.insertOrIgnore("Credit Card", 1, "SCHEDULED", now)
        db.categoryQueries.insertOrIgnore("MISC", 1, "LEDGER", now)
        return CategoryRepository(db)
    }

    @Test
    fun system_categories_seeded() {
        val all = freshRepo().all()
        assertEquals(2, all.size)
        assertTrue(all.any { it.name == "Credit Card" && it.isSystem })
        assertTrue(all.any { it.name == "MISC" && it.behavior == CategoryBehavior.LEDGER })
    }

    @Test
    fun add_user_category() {
        val repo = freshRepo()
        repo.add("Sloan", CategoryBehavior.SCHEDULED)
        val sloan = repo.all().find { it.name == "Sloan" }!!
        assertEquals(false, sloan.isSystem)
        assertEquals(CategoryBehavior.SCHEDULED, sloan.behavior)
    }

    @Test
    fun cannot_delete_system_category() {
        val repo = freshRepo()
        val misc = repo.all().first { it.name == "MISC" }
        repo.delete(misc.id)  // no-op for system
        assertTrue(repo.all().any { it.name == "MISC" })
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.snowball.repo.CategoryRepositoryTest"`
Expected: FAIL — unresolved `CategoryRepository`.

- [ ] **Step 3: Implement repository**

Create `composeApp/src/commonMain/kotlin/com/snowball/data/repo/CategoryRepository.kt`:

```kotlin
package com.snowball.data.repo

import com.snowball.data.model.Category
import com.snowball.data.model.CategoryBehavior
import com.snowball.db.SnowballDb
import kotlinx.datetime.Clock

class CategoryRepository(private val db: SnowballDb) {

    fun all(): List<Category> =
        db.categoryQueries.selectAll().executeAsList().map { row ->
            Category(
                id = row.id,
                name = row.name,
                isSystem = row.isSystem == 1L,
                behavior = CategoryBehavior.valueOf(row.behavior),
            )
        }

    fun byId(id: Long): Category? =
        db.categoryQueries.selectById(id).executeAsOneOrNull()?.let { row ->
            Category(row.id, row.name, row.isSystem == 1L, CategoryBehavior.valueOf(row.behavior))
        }

    fun add(name: String, behavior: CategoryBehavior) {
        db.categoryQueries.insert(
            name = name,
            isSystem = 0,
            behavior = behavior.name,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
    }

    fun rename(id: Long, newName: String) {
        db.categoryQueries.renameById(name = newName, id = id)
    }

    fun delete(id: Long) {
        db.categoryQueries.deleteById(id)  // SQL itself guards isSystem = 0
    }
}
```

- [ ] **Step 4: Run test to verify pass**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.snowball.repo.CategoryRepositoryTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/data/repo/CategoryRepository.kt composeApp/src/androidUnitTest/kotlin/com/snowball/repo/CategoryRepositoryTest.kt
git commit -m "feat(repo): category repository with tests"
```

---

### Task 13: DebtRepository

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/data/repo/DebtRepository.kt`
- Create: `composeApp/src/androidUnitTest/kotlin/com/snowball/repo/DebtRepositoryTest.kt`

- [ ] **Step 1: Write failing test**

Create `composeApp/src/androidUnitTest/kotlin/com/snowball/repo/DebtRepositoryTest.kt`:

```kotlin
package com.snowball.repo

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.snowball.data.repo.DebtRepository
import com.snowball.db.SnowballDb
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DebtRepositoryTest {
    private fun freshDb(): SnowballDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SnowballDb.Schema.create(driver)
        val db = SnowballDb(driver)
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        db.categoryQueries.insertOrIgnore("Credit Card", 1, "SCHEDULED", now)
        return db
    }

    private fun catId(db: SnowballDb): Long = db.categoryQueries.selectAll().executeAsList().first().id

    @Test
    fun add_and_read_debt() {
        val db = freshDb()
        val repo = DebtRepository(db)
        val id = repo.add(
            name = "Sloan 16,500",
            categoryId = catId(db),
            monthlyAmount = 3566.74,
            totalPayments = 6,
            dueDay = 17,
            useLastDayOfMonth = false,
            startDate = LocalDate(2026, 1, 17),
            notes = null,
        )
        val read = repo.byId(id)
        assertNotNull(read)
        assertEquals("Sloan 16,500", read.name)
        assertEquals(17, read.dueDay)
    }

    @Test
    fun selectActive_excludes_archived() {
        val db = freshDb()
        val repo = DebtRepository(db)
        val id = repo.add("a", catId(db), 100.0, 1, 1, false, LocalDate(2026, 1, 1), null)
        repo.setArchived(id, true)
        assertEquals(0, repo.allActive().size)
    }

    @Test
    fun update_persists_fields() {
        val db = freshDb()
        val repo = DebtRepository(db)
        val id = repo.add("a", catId(db), 100.0, 1, 1, false, LocalDate(2026, 1, 1), null)
        repo.update(
            id = id,
            name = "renamed",
            categoryId = catId(db),
            monthlyAmount = 200.0,
            totalPayments = 2,
            dueDay = 5,
            useLastDayOfMonth = true,
            startDate = LocalDate(2026, 2, 1),
            notes = "note",
        )
        val updated = repo.byId(id)!!
        assertEquals("renamed", updated.name)
        assertEquals(200.0, updated.monthlyAmount)
        assertEquals(true, updated.useLastDayOfMonth)
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.snowball.repo.DebtRepositoryTest"`
Expected: FAIL — unresolved `DebtRepository`.

- [ ] **Step 3: Implement repository**

Create `composeApp/src/commonMain/kotlin/com/snowball/data/repo/DebtRepository.kt`:

```kotlin
package com.snowball.data.repo

import com.snowball.data.model.Debt
import com.snowball.db.SnowballDb
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

class DebtRepository(private val db: SnowballDb) {

    fun add(
        name: String,
        categoryId: Long,
        monthlyAmount: Double,
        totalPayments: Int,
        dueDay: Int,
        useLastDayOfMonth: Boolean,
        startDate: LocalDate,
        notes: String?,
    ): Long {
        db.debtQueries.insert(
            name = name,
            categoryId = categoryId,
            monthlyAmount = monthlyAmount,
            totalPayments = totalPayments.toLong(),
            dueDay = dueDay.toLong(),
            useLastDayOfMonth = if (useLastDayOfMonth) 1 else 0,
            startDate = startDate.toString(),
            notes = notes,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        return db.debtQueries.selectAll().executeAsList().first().id
    }

    fun update(
        id: Long,
        name: String,
        categoryId: Long,
        monthlyAmount: Double,
        totalPayments: Int,
        dueDay: Int,
        useLastDayOfMonth: Boolean,
        startDate: LocalDate,
        notes: String?,
    ) {
        db.debtQueries.update(
            name = name,
            categoryId = categoryId,
            monthlyAmount = monthlyAmount,
            totalPayments = totalPayments.toLong(),
            dueDay = dueDay.toLong(),
            useLastDayOfMonth = if (useLastDayOfMonth) 1 else 0,
            startDate = startDate.toString(),
            notes = notes,
            id = id,
        )
    }

    fun setArchived(id: Long, archived: Boolean) {
        db.debtQueries.setArchived(if (archived) 1 else 0, id)
    }

    fun delete(id: Long) {
        db.debtQueries.deleteById(id)
    }

    fun byId(id: Long): Debt? =
        db.debtQueries.selectById(id).executeAsOneOrNull()?.toModel()

    fun allActive(): List<Debt> =
        db.debtQueries.selectActive().executeAsList().map { it.toModel() }

    fun all(): List<Debt> =
        db.debtQueries.selectAll().executeAsList().map { it.toModel() }

    private fun com.snowball.db.Debt.toModel(): Debt = Debt(
        id = id,
        name = name,
        categoryId = categoryId,
        monthlyAmount = monthlyAmount,
        totalPayments = totalPayments.toInt(),
        dueDay = dueDay.toInt(),
        useLastDayOfMonth = useLastDayOfMonth == 1L,
        startDate = LocalDate.parse(startDate),
        isArchived = isArchived == 1L,
        notes = notes,
    )
}
```

- [ ] **Step 4: Run test to verify pass**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.snowball.repo.DebtRepositoryTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/data/repo/DebtRepository.kt composeApp/src/androidUnitTest/kotlin/com/snowball/repo/DebtRepositoryTest.kt
git commit -m "feat(repo): debt repository with tests"
```

---

### Task 14: PaymentRepository

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/data/repo/PaymentRepository.kt`
- Create: `composeApp/src/androidUnitTest/kotlin/com/snowball/repo/PaymentRepositoryTest.kt`

- [ ] **Step 1: Write failing test**

Create `composeApp/src/androidUnitTest/kotlin/com/snowball/repo/PaymentRepositoryTest.kt`:

```kotlin
package com.snowball.repo

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.snowball.data.repo.DebtRepository
import com.snowball.data.repo.PaymentRepository
import com.snowball.db.SnowballDb
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaymentRepositoryTest {
    private fun fresh(): Triple<PaymentRepository, DebtRepository, Long> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SnowballDb.Schema.create(driver)
        val db = SnowballDb(driver)
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        db.categoryQueries.insertOrIgnore("Credit Card", 1, "SCHEDULED", now)
        val catId = db.categoryQueries.selectAll().executeAsList().first().id
        val debtRepo = DebtRepository(db)
        val debtId = debtRepo.add("d1", catId, 1000.0, 12, 15, false, LocalDate(2026, 1, 15), null)
        return Triple(PaymentRepository(db), debtRepo, debtId)
    }

    @Test
    fun marking_paid_creates_row() {
        val (pay, _, debtId) = fresh()
        pay.markPaid(debtId, LocalDate(2026, 1, 15), 1000.0)
        assertEquals(1, pay.countForDebt(debtId))
    }

    @Test
    fun multiple_payments_count() {
        val (pay, _, debtId) = fresh()
        pay.markPaid(debtId, LocalDate(2026, 1, 15), 1000.0)
        pay.markPaid(debtId, LocalDate(2026, 2, 15), 1000.0)
        assertEquals(2, pay.countForDebt(debtId))
    }

    @Test
    fun delete_payment_undoes_mark() {
        val (pay, _, debtId) = fresh()
        pay.markPaid(debtId, LocalDate(2026, 1, 15), 1000.0)
        val rows = pay.historyForDebt(debtId)
        pay.delete(rows.first().id)
        assertEquals(0, pay.countForDebt(debtId))
    }

    @Test
    fun history_sorted_recent_first() {
        val (pay, _, debtId) = fresh()
        pay.markPaid(debtId, LocalDate(2026, 1, 15), 1000.0)
        pay.markPaid(debtId, LocalDate(2026, 3, 15), 1000.0)
        pay.markPaid(debtId, LocalDate(2026, 2, 15), 1000.0)
        val history = pay.historyForDebt(debtId)
        assertTrue(history[0].paidDate > history[1].paidDate)
        assertTrue(history[1].paidDate > history[2].paidDate)
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.snowball.repo.PaymentRepositoryTest"`
Expected: FAIL — unresolved `PaymentRepository`.

- [ ] **Step 3: Implement repository**

Create `composeApp/src/commonMain/kotlin/com/snowball/data/repo/PaymentRepository.kt`:

```kotlin
package com.snowball.data.repo

import com.snowball.data.model.Payment
import com.snowball.db.SnowballDb
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

class PaymentRepository(private val db: SnowballDb) {

    fun markPaid(debtId: Long, paidDate: LocalDate, amount: Double) {
        db.paymentQueries.insert(
            debtId = debtId,
            paidDate = paidDate.toString(),
            amount = amount,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
    }

    fun delete(paymentId: Long) {
        db.paymentQueries.deleteById(paymentId)
    }

    fun countForDebt(debtId: Long): Int =
        db.paymentQueries.countForDebt(debtId).executeAsOne().toInt()

    fun historyForDebt(debtId: Long): List<Payment> =
        db.paymentQueries.selectAllForDebt(debtId).executeAsList().map { row ->
            Payment(
                id = row.id,
                debtId = row.debtId,
                paidDate = LocalDate.parse(row.paidDate),
                amount = row.amount,
            )
        }

    fun latestForDebt(debtId: Long): Payment? =
        db.paymentQueries.selectLatestForDebt(debtId).executeAsOneOrNull()?.let { row ->
            Payment(row.id, row.debtId, LocalDate.parse(row.paidDate), row.amount)
        }

    fun inDateRangeForDebt(debtId: Long, from: LocalDate, to: LocalDate): List<Payment> =
        db.paymentQueries
            .selectInDateRange(debtId = debtId, from = from.toString(), to = to.toString())
            .executeAsList()
            .map { row ->
                Payment(row.id, row.debtId, LocalDate.parse(row.paidDate), row.amount)
            }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.snowball.repo.PaymentRepositoryTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/data/repo/PaymentRepository.kt composeApp/src/androidUnitTest/kotlin/com/snowball/repo/PaymentRepositoryTest.kt
git commit -m "feat(repo): payment repository with tests"
```

---

## Phase 4 — Cutoff Logic (Tasks 15–18)

### Task 15: Date utilities

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/domain/DateUtils.kt`
- Create: `composeApp/src/commonTest/kotlin/com/snowball/domain/DateUtilsTest.kt`

- [ ] **Step 1: Write failing test**

Create `composeApp/src/commonTest/kotlin/com/snowball/domain/DateUtilsTest.kt`:

```kotlin
package com.snowball.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DateUtilsTest {
    @Test
    fun lastDayOfMonth_returns_31_for_january() {
        assertEquals(31, lastDayOfMonth(2026, 1))
    }

    @Test
    fun lastDayOfMonth_returns_28_for_february_2026() {
        assertEquals(28, lastDayOfMonth(2026, 2))
    }

    @Test
    fun lastDayOfMonth_returns_29_for_february_2028() {
        assertEquals(29, lastDayOfMonth(2028, 2))
    }

    @Test
    fun lastDayOfMonth_returns_30_for_april() {
        assertEquals(30, lastDayOfMonth(2026, 4))
    }

    @Test
    fun effectiveDueDate_clamps_to_last_day() {
        // dueDay 30 with useLastDay=true in Feb 2026 → 28
        val d = effectiveDueDate(year = 2026, month = 2, dueDay = 30, useLastDay = true)
        assertEquals(LocalDate(2026, 2, 28), d)
    }

    @Test
    fun effectiveDueDate_uses_literal_day_when_present() {
        val d = effectiveDueDate(year = 2026, month = 5, dueDay = 17, useLastDay = false)
        assertEquals(LocalDate(2026, 5, 17), d)
    }

    @Test
    fun effectiveDueDate_returns_null_when_day_invalid_and_not_floating() {
        // dueDay 31 in Feb 2026 with useLastDay=false → null
        val d = effectiveDueDate(year = 2026, month = 2, dueDay = 31, useLastDay = false)
        assertEquals(null, d)
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.snowball.domain.DateUtilsTest"`
Expected: FAIL — unresolved `lastDayOfMonth`.

- [ ] **Step 3: Implement**

Create `composeApp/src/commonMain/kotlin/com/snowball/domain/DateUtils.kt`:

```kotlin
package com.snowball.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

fun lastDayOfMonth(year: Int, month: Int): Int {
    val firstOfNext = if (month == 12) {
        LocalDate(year + 1, 1, 1)
    } else {
        LocalDate(year, month + 1, 1)
    }
    return firstOfNext.minus(DatePeriod(days = 1)).dayOfMonth
}

fun effectiveDueDate(year: Int, month: Int, dueDay: Int, useLastDay: Boolean): LocalDate? {
    val maxDay = lastDayOfMonth(year, month)
    val day = when {
        useLastDay && dueDay >= maxDay -> maxDay
        useLastDay -> dueDay.coerceAtMost(maxDay)
        dueDay > maxDay -> return null
        else -> dueDay
    }
    return LocalDate(year, month, day)
}

fun today(zone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    Clock.System.now().toLocalDateTime(zone).date

fun previousMonth(date: LocalDate): Pair<Int, Int> =
    if (date.monthNumber == 1) (date.year - 1) to 12 else date.year to (date.monthNumber - 1)

fun nextMonth(date: LocalDate): Pair<Int, Int> =
    if (date.monthNumber == 12) (date.year + 1) to 1 else date.year to (date.monthNumber + 1)
```

- [ ] **Step 4: Run test to verify pass**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.snowball.domain.DateUtilsTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/domain/DateUtils.kt composeApp/src/commonTest/kotlin/com/snowball/domain/DateUtilsTest.kt
git commit -m "feat(domain): date utilities with last-day-of-month logic"
```

---

### Task 16: Cutoff window types and detection

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/domain/Cutoff.kt`
- Create: `composeApp/src/commonTest/kotlin/com/snowball/domain/CutoffTest.kt`

Implements: spec §6 (Cutoff identity, current-cutoff detection).

- [ ] **Step 1: Write failing test**

Create `composeApp/src/commonTest/kotlin/com/snowball/domain/CutoffTest.kt`:

```kotlin
package com.snowball.domain

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class CutoffTest {
    @Test
    fun cutoff_15_covers_15_to_30_of_same_month() {
        val c = Cutoff(year = 2026, month = 5, payday = Payday.FIFTEENTH)
        assertEquals(LocalDate(2026, 5, 15), c.windowStart)
        assertEquals(LocalDate(2026, 5, 30), c.windowEnd)
    }

    @Test
    fun cutoff_30_covers_1_to_14_of_next_month() {
        val c = Cutoff(year = 2026, month = 5, payday = Payday.THIRTIETH)
        assertEquals(LocalDate(2026, 6, 1), c.windowStart)
        assertEquals(LocalDate(2026, 6, 14), c.windowEnd)
    }

    @Test
    fun cutoff_30_in_december_rolls_to_january() {
        val c = Cutoff(year = 2026, month = 12, payday = Payday.THIRTIETH)
        assertEquals(LocalDate(2027, 1, 1), c.windowStart)
        assertEquals(LocalDate(2027, 1, 14), c.windowEnd)
    }

    @Test
    fun currentCutoff_when_day_in_1_to_14_returns_previous_month_30() {
        val c = currentCutoff(today = LocalDate(2026, 5, 7))
        assertEquals(2026, c.year)
        assertEquals(4, c.month)
        assertEquals(Payday.THIRTIETH, c.payday)
    }

    @Test
    fun currentCutoff_when_day_in_15_to_30_returns_same_month_15() {
        val c = currentCutoff(today = LocalDate(2026, 5, 20))
        assertEquals(2026, c.year)
        assertEquals(5, c.month)
        assertEquals(Payday.FIFTEENTH, c.payday)
    }

    @Test
    fun currentCutoff_on_january_5_returns_december_30() {
        val c = currentCutoff(today = LocalDate(2026, 1, 5))
        assertEquals(2025, c.year)
        assertEquals(12, c.month)
        assertEquals(Payday.THIRTIETH, c.payday)
    }

    @Test
    fun next_cutoff_after_may_15_is_may_30() {
        val current = Cutoff(2026, 5, Payday.FIFTEENTH)
        val next = current.next()
        assertEquals(2026, next.year)
        assertEquals(5, next.month)
        assertEquals(Payday.THIRTIETH, next.payday)
    }

    @Test
    fun next_cutoff_after_may_30_is_june_15() {
        val current = Cutoff(2026, 5, Payday.THIRTIETH)
        val next = current.next()
        assertEquals(2026, next.year)
        assertEquals(6, next.month)
        assertEquals(Payday.FIFTEENTH, next.payday)
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.snowball.domain.CutoffTest"`
Expected: FAIL — unresolved `Cutoff`.

- [ ] **Step 3: Implement**

Create `composeApp/src/commonMain/kotlin/com/snowball/domain/Cutoff.kt`:

```kotlin
package com.snowball.domain

import kotlinx.datetime.LocalDate

enum class Payday { FIFTEENTH, THIRTIETH }

data class Cutoff(
    val year: Int,
    val month: Int,
    val payday: Payday,
) {
    val payDate: LocalDate
        get() = when (payday) {
            Payday.FIFTEENTH -> LocalDate(year, month, 15)
            Payday.THIRTIETH -> LocalDate(year, month, minOf(30, lastDayOfMonth(year, month)))
        }

    val windowStart: LocalDate
        get() = when (payday) {
            Payday.FIFTEENTH -> LocalDate(year, month, 15)
            Payday.THIRTIETH -> {
                val (ny, nm) = nextYearMonth()
                LocalDate(ny, nm, 1)
            }
        }

    val windowEnd: LocalDate
        get() = when (payday) {
            Payday.FIFTEENTH -> LocalDate(year, month, minOf(30, lastDayOfMonth(year, month)))
            Payday.THIRTIETH -> {
                val (ny, nm) = nextYearMonth()
                LocalDate(ny, nm, 14)
            }
        }

    fun next(): Cutoff = when (payday) {
        Payday.FIFTEENTH -> Cutoff(year, month, Payday.THIRTIETH)
        Payday.THIRTIETH -> {
            val (ny, nm) = nextYearMonth()
            Cutoff(ny, nm, Payday.FIFTEENTH)
        }
    }

    fun previous(): Cutoff = when (payday) {
        Payday.FIFTEENTH -> {
            val (py, pm) = if (month == 1) (year - 1) to 12 else year to (month - 1)
            Cutoff(py, pm, Payday.THIRTIETH)
        }
        Payday.THIRTIETH -> Cutoff(year, month, Payday.FIFTEENTH)
    }

    private fun nextYearMonth(): Pair<Int, Int> =
        if (month == 12) (year + 1) to 1 else year to (month + 1)
}

fun currentCutoff(today: LocalDate): Cutoff {
    val day = today.dayOfMonth
    return if (day in 1..14) {
        val (py, pm) = if (today.monthNumber == 1) (today.year - 1) to 12 else today.year to (today.monthNumber - 1)
        Cutoff(py, pm, Payday.THIRTIETH)
    } else {
        Cutoff(today.year, today.monthNumber, Payday.FIFTEENTH)
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.snowball.domain.CutoffTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/domain/Cutoff.kt composeApp/src/commonTest/kotlin/com/snowball/domain/CutoffTest.kt
git commit -m "feat(domain): cutoff window types and current-cutoff detection"
```

---

### Task 17: Cutoff calculator — "due this cutoff"

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/domain/CutoffCalculator.kt`
- Create: `composeApp/src/commonTest/kotlin/com/snowball/domain/CutoffCalculatorTest.kt`

Implements: spec §6 ("Due this cutoff" calculation).

- [ ] **Step 1: Write failing test**

Create `composeApp/src/commonTest/kotlin/com/snowball/domain/CutoffCalculatorTest.kt`:

```kotlin
package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CutoffCalculatorTest {
    private fun debt(
        id: Long = 1,
        dueDay: Int,
        useLastDay: Boolean = false,
        monthly: Double = 1000.0,
        total: Int = 12,
        start: LocalDate = LocalDate(2026, 1, 1),
        archived: Boolean = false,
    ) = Debt(
        id = id, name = "d$id", categoryId = 1, monthlyAmount = monthly,
        totalPayments = total, dueDay = dueDay, useLastDayOfMonth = useLastDay,
        startDate = start, isArchived = archived, notes = null,
    )

    @Test
    fun debt_due_day_17_in_may15_cutoff_appears() {
        val c = Cutoff(2026, 5, Payday.FIFTEENTH)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(debt(dueDay = 17)),
            paymentsByDebt = emptyMap(),
        )
        assertEquals(1, rows.size)
    }

    @Test
    fun debt_due_day_10_in_may30_cutoff_appears() {
        // May 30 cutoff covers June 1–14, so debts with dueDay 10 appear
        val c = Cutoff(2026, 5, Payday.THIRTIETH)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(debt(dueDay = 10)),
            paymentsByDebt = emptyMap(),
        )
        assertEquals(1, rows.size)
    }

    @Test
    fun debt_due_day_outside_window_excluded() {
        val c = Cutoff(2026, 5, Payday.FIFTEENTH) // covers 15–30
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(debt(dueDay = 5)),
            paymentsByDebt = emptyMap(),
        )
        assertEquals(0, rows.size)
    }

    @Test
    fun debt_starting_after_cutoff_excluded() {
        val c = Cutoff(2026, 5, Payday.FIFTEENTH)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(debt(dueDay = 17, start = LocalDate(2026, 6, 1))),
            paymentsByDebt = emptyMap(),
        )
        assertEquals(0, rows.size)
    }

    @Test
    fun archived_debt_excluded() {
        // The calculator only sees activeDebts (caller filters), so this is a
        // sanity check on the input contract: passing only active debts works.
        val c = Cutoff(2026, 5, Payday.FIFTEENTH)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(debt(dueDay = 17, archived = true)).filter { !it.isArchived },
            paymentsByDebt = emptyMap(),
        )
        assertEquals(0, rows.size)
    }

    @Test
    fun debt_paid_this_cycle_marked_as_paid() {
        val c = Cutoff(2026, 5, Payday.FIFTEENTH)
        val d = debt(dueDay = 17)
        val paid = Payment(id = 1, debtId = d.id, paidDate = LocalDate(2026, 5, 17), amount = 1000.0)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to listOf(paid)),
        )
        assertEquals(1, rows.size)
        assertTrue(rows.first().isPaidThisCycle)
    }

    @Test
    fun debt_paid_in_prior_cycle_remains_owed() {
        val c = Cutoff(2026, 5, Payday.FIFTEENTH)
        val d = debt(dueDay = 17)
        val priorPaid = Payment(id = 1, debtId = d.id, paidDate = LocalDate(2026, 4, 17), amount = 1000.0)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to listOf(priorPaid)),
        )
        assertEquals(1, rows.size)
        assertEquals(false, rows.first().isPaidThisCycle)
    }

    @Test
    fun totals_sum_monthly_amount() {
        val c = Cutoff(2026, 5, Payday.FIFTEENTH)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(
                debt(id = 1, dueDay = 17, monthly = 3000.0),
                debt(id = 2, dueDay = 19, monthly = 500.0),
            ),
            paymentsByDebt = emptyMap(),
        )
        val total = rows.sumOf { it.amount }
        assertEquals(3500.0, total)
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.snowball.domain.CutoffCalculatorTest"`
Expected: FAIL — unresolved `CutoffCalculator`.

- [ ] **Step 3: Implement**

Create `composeApp/src/commonMain/kotlin/com/snowball/domain/CutoffCalculator.kt`:

```kotlin
package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.LocalDate

data class DueRow(
    val debt: Debt,
    val effectiveDueDate: LocalDate,
    val amount: Double,
    val isPaidThisCycle: Boolean,
)

object CutoffCalculator {
    /**
     * Returns the list of rows that appear in this cutoff. Callers should pass
     * only active (non-archived) debts. Each row reports the debt's effective
     * due date inside this cutoff's window, the amount, and whether a payment
     * has already been recorded for this cycle.
     */
    fun computeDueRows(
        cutoff: Cutoff,
        activeDebts: List<Debt>,
        paymentsByDebt: Map<Long, List<Payment>>,
    ): List<DueRow> {
        val rows = mutableListOf<DueRow>()
        for (debt in activeDebts) {
            val effective = effectiveDueDate(
                year = cutoff.windowStart.year,
                month = cutoff.windowStart.monthNumber,
                dueDay = debt.dueDay,
                useLastDay = debt.useLastDayOfMonth,
            ) ?: continue

            if (effective < cutoff.windowStart || effective > cutoff.windowEnd) continue
            if (debt.startDate > cutoff.payDate) continue

            val priorEffective = priorCycleDueDate(debt, effective)
            val payments = paymentsByDebt[debt.id].orEmpty()
            val paid = payments.any { it.paidDate > priorEffective && it.paidDate <= cutoff.windowEnd }

            rows.add(
                DueRow(
                    debt = debt,
                    effectiveDueDate = effective,
                    amount = debt.monthlyAmount,
                    isPaidThisCycle = paid,
                )
            )
        }
        return rows.sortedBy { it.effectiveDueDate }
    }

    private fun priorCycleDueDate(debt: Debt, current: LocalDate): LocalDate {
        // One month prior, clamped to last day if needed
        val (py, pm) = if (current.monthNumber == 1) (current.year - 1) to 12 else current.year to (current.monthNumber - 1)
        val prior = effectiveDueDate(
            year = py, month = pm,
            dueDay = debt.dueDay, useLastDay = debt.useLastDayOfMonth,
        )
        // If the debt didn't exist last month (started later), use startDate as the floor.
        return when {
            prior == null -> debt.startDate
            prior < debt.startDate -> debt.startDate
            else -> prior
        }
    }
}
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.snowball.domain.CutoffCalculatorTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/domain/CutoffCalculator.kt composeApp/src/commonTest/kotlin/com/snowball/domain/CutoffCalculatorTest.kt
git commit -m "feat(domain): cutoff calculator returning per-debt due rows"
```

---

### Task 18: Cutoff summary aggregation

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/domain/CutoffCalculator.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/snowball/domain/CutoffCalculatorTest.kt`

- [ ] **Step 1: Add failing test for summary**

Append to `composeApp/src/commonTest/kotlin/com/snowball/domain/CutoffCalculatorTest.kt` (inside the class):

```kotlin
    @Test
    fun summary_total_uses_all_owed_paid_or_not() {
        val c = Cutoff(2026, 5, Payday.FIFTEENTH)
        val d1 = debt(id = 1, dueDay = 17, monthly = 3000.0)
        val d2 = debt(id = 2, dueDay = 19, monthly = 500.0)
        val rows = CutoffCalculator.computeDueRows(
            cutoff = c,
            activeDebts = listOf(d1, d2),
            paymentsByDebt = mapOf(
                d1.id to listOf(Payment(1, d1.id, LocalDate(2026, 5, 17), 3000.0))
            ),
        )
        val summary = CutoffCalculator.summarize(rows = rows, incomePerCutoff = 25000.0)
        assertEquals(3500.0, summary.dueTotal)
        assertEquals(21500.0, summary.breathingRoom)
        assertEquals(3000.0, summary.paidTotal)
    }
```

- [ ] **Step 2: Run to verify failure**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.snowball.domain.CutoffCalculatorTest.summary_total_uses_all_owed_paid_or_not"`
Expected: FAIL — unresolved `summarize`.

- [ ] **Step 3: Implement summary**

Add to `composeApp/src/commonMain/kotlin/com/snowball/domain/CutoffCalculator.kt`, inside the `CutoffCalculator` object after `computeDueRows`:

```kotlin
    data class Summary(
        val dueTotal: Double,
        val paidTotal: Double,
        val breathingRoom: Double,
    )

    fun summarize(rows: List<DueRow>, incomePerCutoff: Double): Summary {
        val due = rows.sumOf { it.amount }
        val paid = rows.filter { it.isPaidThisCycle }.sumOf { it.amount }
        return Summary(
            dueTotal = due,
            paidTotal = paid,
            breathingRoom = incomePerCutoff - due,
        )
    }
```

- [ ] **Step 4: Run to verify pass**

Run: `./gradlew :composeApp:testDebugUnitTest --tests "com.snowball.domain.CutoffCalculatorTest"`
Expected: PASS for all `CutoffCalculatorTest` tests.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/domain/CutoffCalculator.kt composeApp/src/commonTest/kotlin/com/snowball/domain/CutoffCalculatorTest.kt
git commit -m "feat(domain): cutoff summary with due, paid, and breathing room"
```

---

## Phase 5 — UI Foundation (Tasks 19–22)

### Task 19: Theme — colors

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/theme/Color.kt`

- [ ] **Step 1: Define colors from spec §3**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/theme/Color.kt`:

```kotlin
package com.snowball.ui.theme

import androidx.compose.ui.graphics.Color

object SnowColors {
    val Night = Color(0xFF0B0F14)
    val NightElev = Color(0xFF11161D)
    val CardElev = Color(0xFF161C25)
    val Line = Color(0x12FFFFFF)         // rgba(255,255,255,0.07)
    val LineStrong = Color(0x21FFFFFF)   // rgba(255,255,255,0.13)
    val Frost = Color(0xFFF2F5F8)
    val FrostMute = Color(0xFFA8B2BF)
    val FrostDim = Color(0xFF5E6874)
    val FrostDeep = Color(0xFF3C4452)
    val Ice = Color(0xFF9FCEE3)
    val IceSoft = Color(0x299FCEE3)      // rgba(159,206,227,0.16)
    val Champagne = Color(0xFFE8C68A)
    val Ember = Color(0xFFE07856)
    val Green = Color(0xFF8FD9B2)
}
```

- [ ] **Step 2: Build to verify**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/theme/Color.kt
git commit -m "feat(theme): snow color palette"
```

---

### Task 20: Theme — typography

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/theme/Type.kt`

- [ ] **Step 1: Define typography**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/theme/Type.kt`:

```kotlin
package com.snowball.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.snowball.resources.Res
import org.jetbrains.compose.resources.Font

@Composable
fun fraunces(): FontFamily = FontFamily(Font(Res.font.Fraunces_Variable))

@Composable
fun dmSans(): FontFamily = FontFamily(Font(Res.font.DMSans_Variable))

// Note: `Res` is generated by Compose Resources from the `packageOfResClass`
// config set in composeApp/build.gradle.kts. Font properties are derived from
// file names: "Fraunces-Variable.ttf" → `Res.font.Fraunces_Variable`.

@Composable
fun snowballTypography(): Typography {
    val display = fraunces()
    val body = dmSans()
    return Typography(
        displayLarge = TextStyle(fontFamily = display, fontSize = 96.sp, fontWeight = FontWeight.W350),
        displayMedium = TextStyle(fontFamily = display, fontSize = 64.sp, fontWeight = FontWeight.W400),
        displaySmall = TextStyle(fontFamily = display, fontSize = 32.sp, fontWeight = FontWeight.W400),
        headlineLarge = TextStyle(fontFamily = display, fontSize = 28.sp, fontWeight = FontWeight.W500),
        headlineMedium = TextStyle(fontFamily = display, fontSize = 22.sp, fontWeight = FontWeight.W500),
        headlineSmall = TextStyle(fontFamily = display, fontSize = 18.sp, fontWeight = FontWeight.W500),
        titleLarge = TextStyle(fontFamily = body, fontSize = 16.sp, fontWeight = FontWeight.Medium),
        titleMedium = TextStyle(fontFamily = body, fontSize = 14.sp, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontFamily = body, fontSize = 16.sp, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontFamily = body, fontSize = 14.sp, fontWeight = FontWeight.Normal),
        bodySmall = TextStyle(fontFamily = body, fontSize = 12.sp, fontWeight = FontWeight.Normal),
        labelLarge = TextStyle(fontFamily = body, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        labelMedium = TextStyle(fontFamily = body, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.0.sp),
        labelSmall = TextStyle(fontFamily = body, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp),
    )
}

val FraunsesItalic: FontStyle = FontStyle.Italic
```

- [ ] **Step 2: Build to verify**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL. Note: the generated `Res.font.Fraunces_Variable` and `Res.font.DMSans_Variable` properties come from the bundled font files (Task 3).

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/theme/Type.kt
git commit -m "feat(theme): fraunces and dm sans typography scale"
```

---

### Task 21: Theme — wrapper

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/theme/Theme.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/App.kt`

- [ ] **Step 1: Theme composable**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/theme/Theme.kt`:

```kotlin
package com.snowball.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun SnowballTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        background = SnowColors.Night,
        surface = SnowColors.NightElev,
        surfaceVariant = SnowColors.CardElev,
        onBackground = SnowColors.Frost,
        onSurface = SnowColors.Frost,
        primary = SnowColors.Ice,
        onPrimary = SnowColors.Night,
        secondary = SnowColors.Champagne,
        tertiary = SnowColors.Ember,
        error = SnowColors.Ember,
        outline = SnowColors.LineStrong,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = snowballTypography(),
        content = content,
    )
}
```

- [ ] **Step 2: Apply theme in App.kt**

Replace `composeApp/src/commonMain/kotlin/com/snowball/App.kt`:

```kotlin
package com.snowball

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.snowball.ui.theme.SnowballTheme

@Composable
fun App() {
    SnowballTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Snowball",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
```

- [ ] **Step 3: Run on device and verify**

Run: `./gradlew :composeApp:assembleDebug && adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk`
Expected: install succeeds. The "Snowball" text now uses Fraunces (serif), on the dark background.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/theme/Theme.kt composeApp/src/commonMain/kotlin/com/snowball/App.kt
git commit -m "feat(theme): snowball theme wrapper applied to app"
```

---

### Task 22: Reusable components — PesoText and HeroAmount

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/PesoText.kt`
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/HeroAmount.kt`

- [ ] **Step 1: PesoText**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/components/PesoText.kt`:

```kotlin
package com.snowball.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.ui.theme.SnowColors

/**
 * Renders an amount with a small italic peso symbol followed by the number,
 * tabular figures aligned. Used everywhere money appears.
 */
@Composable
fun PesoText(
    amount: Double,
    style: TextStyle,
    modifier: Modifier = Modifier,
    pesoColor: Color = SnowColors.FrostDim,
    numberColor: Color = MaterialTheme.colorScheme.onBackground,
    align: TextAlign = TextAlign.Start,
) {
    val formatted = formatAmount(amount)
    val pesoStyle = style.copy(
        color = pesoColor,
        fontStyle = FontStyle.Italic,
        fontSize = style.fontSize * 0.55f,
    )
    val numStyle = style.copy(color = numberColor)
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text("₱", style = pesoStyle)
        Spacer(Modifier.width(2.dp))
        Text(formatted, style = numStyle)
    }
}

private fun formatAmount(amount: Double): String {
    val whole = amount.toLong()
    val fraction = ((amount - whole) * 100).toInt().let { if (it < 0) -it else it }
    val wholeStr = whole.toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
    return if (fraction == 0) wholeStr else "$wholeStr.${fraction.toString().padStart(2, '0')}"
}
```

- [ ] **Step 2: HeroAmount**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/components/HeroAmount.kt`:

```kotlin
package com.snowball.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.snowball.ui.theme.SnowColors

@Composable
fun HeroAmount(amount: Double, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        PesoText(
            amount = amount,
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.W350),
            numberColor = SnowColors.Frost,
            pesoColor = SnowColors.FrostMute,
        )
    }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/components/PesoText.kt composeApp/src/commonMain/kotlin/com/snowball/ui/components/HeroAmount.kt
git commit -m "feat(ui): peso text and hero amount components"
```

---

### Task 23: Reusable component — ProgressArc

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/ProgressArc.kt`

- [ ] **Step 1: Implement**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/components/ProgressArc.kt`:

```kotlin
package com.snowball.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.snowball.ui.theme.SnowColors

@Composable
fun ProgressArc(
    progress: Float,                 // 0f..1f
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    strokeDp: Dp = 2.dp,
    trackColor: Color = SnowColors.Line,
    arcColor: Color = SnowColors.Ice,
) {
    Canvas(modifier = modifier) {
        val stroke = strokeDp.toPx()
        val s = Size(size.toPx() - stroke, size.toPx() - stroke)
        val topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = s,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = arcColor,
            startAngle = -90f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = s,
            style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/components/ProgressArc.kt
git commit -m "feat(ui): progress arc component"
```

---

## Phase 6 — App Container, Nav, Repos Provider (Tasks 24–25)

### Task 24: Repository container

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/data/Repos.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/snowball/MainActivity.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/App.kt`

- [ ] **Step 1: Container class**

Create `composeApp/src/commonMain/kotlin/com/snowball/data/Repos.kt`:

```kotlin
package com.snowball.data

import com.snowball.data.repo.CategoryRepository
import com.snowball.data.repo.DebtRepository
import com.snowball.data.repo.PaymentRepository
import com.snowball.data.repo.SettingsRepository
import com.snowball.db.SnowballDb

class Repos(db: SnowballDb) {
    val categories: CategoryRepository = CategoryRepository(db)
    val debts: DebtRepository = DebtRepository(db)
    val payments: PaymentRepository = PaymentRepository(db)
    val settings: SettingsRepository = SettingsRepository(db)
}
```

- [ ] **Step 2: Wire into MainActivity**

Replace `composeApp/src/androidMain/kotlin/com/snowball/MainActivity.kt`:

```kotlin
package com.snowball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.snowball.data.Repos
import com.snowball.data.db.DatabaseDriverFactory
import com.snowball.data.db.createDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = createDatabase(DatabaseDriverFactory(applicationContext))
        val repos = Repos(db)
        setContent { App(repos) }
    }
}
```

- [ ] **Step 3: Update App to accept Repos**

Replace `composeApp/src/commonMain/kotlin/com/snowball/App.kt`:

```kotlin
package com.snowball

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.snowball.data.Repos
import com.snowball.ui.theme.SnowballTheme

@Composable
fun App(repos: Repos) {
    SnowballTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Snowball — ${repos.categories.all().size} categories",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
```

- [ ] **Step 4: Build, install, verify**

Run: `./gradlew :composeApp:assembleDebug && adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk`
Expected: App shows "Snowball — 2 categories" (Credit Card + MISC seeded).

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/data/Repos.kt composeApp/src/androidMain/kotlin/com/snowball/MainActivity.kt composeApp/src/commonMain/kotlin/com/snowball/App.kt
git commit -m "feat: wire repository container into compose root"
```

---

### Task 25: Navigation shell (3-tab bottom nav)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/nav/Nav.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/App.kt`

- [ ] **Step 1: Tab enum and bottom nav**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/nav/Nav.kt`:

```kotlin
package com.snowball.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snowball.ui.theme.SnowColors

enum class Tab(val label: String) { Home("Home"), Debts("Debts"), Settings("Settings") }

@Composable
fun BottomNav(
    selected: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SnowColors.NightElev)
            .padding(vertical = 14.dp),
    ) {
        Tab.entries.forEach { tab ->
            val active = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (active) SnowColors.Frost else SnowColors.FrostDim,
                    modifier = if (active) Modifier else Modifier.alpha(0.7f),
                )
            }
        }
    }
}
```

- [ ] **Step 2: Mount nav in App.kt**

Replace `composeApp/src/commonMain/kotlin/com/snowball/App.kt`:

```kotlin
package com.snowball

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.snowball.data.Repos
import com.snowball.ui.nav.BottomNav
import com.snowball.ui.nav.Tab
import com.snowball.ui.theme.SnowballTheme

@Composable
fun App(repos: Repos) {
    SnowballTheme {
        var tab by remember { mutableStateOf(Tab.Home) }
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            BottomNav(selected = tab, onSelect = { tab = it })
        }
    }
}
```

- [ ] **Step 3: Install and verify**

Run: `./gradlew :composeApp:assembleDebug && adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk`
Expected: app opens to "Home". Tap "Debts" → text changes to "Debts". Tap "Settings" → "Settings".

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/nav/Nav.kt composeApp/src/commonMain/kotlin/com/snowball/App.kt
git commit -m "feat(ui): 3-tab bottom navigation shell"
```

---

## Phase 7 — Home Screen (Tasks 26–28)

### Task 26: Home ViewModel

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeViewModel.kt`

- [ ] **Step 1: Write the ViewModel**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeViewModel.kt`:

```kotlin
package com.snowball.ui.home

import com.snowball.data.Repos
import com.snowball.domain.Cutoff
import com.snowball.domain.CutoffCalculator
import com.snowball.domain.DueRow
import com.snowball.domain.currentCutoff
import com.snowball.domain.today
import kotlinx.datetime.LocalDate

data class HomeState(
    val cutoff: Cutoff,
    val rows: List<DueRow>,
    val summary: CutoffCalculator.Summary,
    val income: Double,
)

class HomeViewModel(private val repos: Repos) {

    fun load(today: LocalDate = today()): HomeState {
        val cutoff = currentCutoff(today)
        val debts = repos.debts.allActive()
        val paymentsByDebt = debts.associate { it.id to repos.payments.historyForDebt(it.id) }
        val rows = CutoffCalculator.computeDueRows(cutoff, debts, paymentsByDebt)
        val income = repos.settings.get().incomePerCutoff
        val summary = CutoffCalculator.summarize(rows, income)
        return HomeState(cutoff, rows, summary, income)
    }

    fun markPaid(row: DueRow, todayDate: LocalDate = today()) {
        repos.payments.markPaid(row.debt.id, todayDate, row.amount)
        val totalPayments = repos.payments.countForDebt(row.debt.id)
        if (totalPayments >= row.debt.totalPayments) {
            repos.debts.setArchived(row.debt.id, true)
        }
    }

    fun undoPayment(row: DueRow) {
        val history = repos.payments.historyForDebt(row.debt.id)
        val latest = history.firstOrNull() ?: return
        repos.payments.delete(latest.id)
        if (row.debt.isArchived) {
            repos.debts.setArchived(row.debt.id, false)
        }
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeViewModel.kt
git commit -m "feat(home): home viewmodel computing cutoff state"
```

---

### Task 27: Cutoff card component

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/CutoffCard.kt`

- [ ] **Step 1: Implement**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/components/CutoffCard.kt`:

```kotlin
package com.snowball.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.domain.Cutoff
import com.snowball.domain.CutoffCalculator
import com.snowball.domain.Payday
import com.snowball.ui.theme.SnowColors

@Composable
fun CutoffCard(
    cutoff: Cutoff,
    summary: CutoffCalculator.Summary,
    incomePerCutoff: Double,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(SnowColors.CardElev, SnowColors.NightElev)
                )
            )
            .border(width = 1.dp, color = SnowColors.LineStrong, shape = RoundedCornerShape(28.dp))
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Text(
            "THIS CUTOFF",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            cutoffRangeLabel(cutoff),
            style = MaterialTheme.typography.headlineMedium,
            color = SnowColors.Frost,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "DUE",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(8.dp))
        PesoText(
            amount = summary.dueTotal,
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.W350),
            pesoColor = SnowColors.FrostMute,
            numberColor = SnowColors.Frost,
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            LedgerCell(label = "INCOME", amount = incomePerCutoff, color = SnowColors.Frost, modifier = Modifier.weight(1f))
            LedgerCell(label = "BREATHING ROOM", amount = summary.breathingRoom, color = SnowColors.Ice, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LedgerCell(label: String, amount: Double, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(SnowColors.Night.copy(alpha = 0.6f))
            .padding(20.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp), color = SnowColors.FrostDim)
        Spacer(Modifier.height(8.dp))
        PesoText(
            amount = amount,
            style = MaterialTheme.typography.headlineLarge,
            pesoColor = SnowColors.FrostDim,
            numberColor = color,
        )
    }
}

fun cutoffRangeLabel(c: Cutoff): String {
    val start = c.windowStart
    val end = c.windowEnd
    val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return if (start.monthNumber == end.monthNumber) {
        "${months[start.monthNumber - 1]} ${start.dayOfMonth} → ${end.dayOfMonth}"
    } else {
        "${months[start.monthNumber - 1]} ${start.dayOfMonth} → ${months[end.monthNumber - 1]} ${end.dayOfMonth}"
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/components/CutoffCard.kt
git commit -m "feat(ui): cutoff hero card matching identity spec"
```

---

### Task 28: Home screen with payment rows and mark-paid

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/App.kt`

- [ ] **Step 1: Home screen**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt`:

```kotlin
package com.snowball.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.domain.DueRow
import com.snowball.ui.components.CutoffCard
import com.snowball.ui.components.PesoText
import com.snowball.ui.components.ProgressArc
import com.snowball.ui.components.cutoffRangeLabel
import com.snowball.ui.theme.SnowColors

@Composable
fun HomeScreen(vm: HomeViewModel) {
    var tick by remember { mutableStateOf(0) }
    val state = remember(tick) { vm.load() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        CutoffCard(
            cutoff = state.cutoff,
            summary = state.summary,
            incomePerCutoff = state.income,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "PAYMENTS",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )

        Spacer(Modifier.height(8.dp))
        if (state.rows.isEmpty()) {
            EmptyHint()
        } else {
            state.rows.forEach { row ->
                PaymentRow(
                    row = row,
                    onMarkPaid = { vm.markPaid(row); tick++ },
                    onUndo = { vm.undoPayment(row); tick++ },
                )
            }
        }
    }
}

@Composable
private fun PaymentRow(row: DueRow, onMarkPaid: () -> Unit, onUndo: () -> Unit) {
    val progress = row.debt.totalPayments
        .takeIf { it > 0 }
        ?.let { (row.debt.totalPayments - (row.debt.totalPayments - paymentsMadeEstimate(row))).toFloat() / it }
        ?: 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .clickable { if (row.isPaidThisCycle) onUndo() else onMarkPaid() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressArc(
            progress = if (row.isPaidThisCycle) 1f else progress,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.debt.name,
                style = MaterialTheme.typography.headlineSmall,
                color = if (row.isPaidThisCycle) SnowColors.FrostDim else SnowColors.Frost,
            )
            Text(
                "Due ${row.effectiveDueDate}",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = SnowColors.FrostMute,
            )
        }
        PesoText(
            amount = row.amount,
            style = MaterialTheme.typography.headlineMedium,
            pesoColor = SnowColors.FrostDim,
            numberColor = if (row.isPaidThisCycle) SnowColors.FrostMute else SnowColors.Frost,
        )
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(SnowColors.Line))
}

@Composable
private fun EmptyHint() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "No payments due this cutoff yet.\nAdd debts from the Debts tab.",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = SnowColors.FrostDim,
        )
    }
}

private fun paymentsMadeEstimate(row: DueRow): Int =
    // The DueRow doesn't carry paymentsMade; cheap derivation acceptable for v0.1.
    // Caller of the row may pass a richer struct in a later refactor.
    if (row.isPaidThisCycle) row.debt.totalPayments / 2 else 0
```

Note on `paymentsMadeEstimate`: this is a placeholder approximation for the progress arc on Home — exact progress requires extra count lookup. For v0.1, the row primarily distinguishes paid/unpaid for this cycle; an accurate progress arc lives on Debt Detail in a later plan. (See Task 18.5 note in the spec — feel free to refine if it bothers you, but YAGNI.)

Also add the missing import for `width` in PaymentRow:

```kotlin
import androidx.compose.foundation.layout.width
```

- [ ] **Step 2: Wire HomeScreen into App**

Replace `composeApp/src/commonMain/kotlin/com/snowball/App.kt`:

```kotlin
package com.snowball

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.snowball.data.Repos
import com.snowball.ui.home.HomeScreen
import com.snowball.ui.home.HomeViewModel
import com.snowball.ui.nav.BottomNav
import com.snowball.ui.nav.Tab
import com.snowball.ui.theme.SnowballTheme

@Composable
fun App(repos: Repos) {
    SnowballTheme {
        var tab by remember { mutableStateOf(Tab.Home) }
        val homeVm = remember { HomeViewModel(repos) }
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (tab) {
                    Tab.Home -> HomeScreen(homeVm)
                    Tab.Debts -> PlaceholderScreen("Debts")
                    Tab.Settings -> PlaceholderScreen("Settings")
                }
            }
            BottomNav(selected = tab, onSelect = { tab = it })
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            name,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
```

- [ ] **Step 3: Build, install, smoke test**

Run: `./gradlew :composeApp:assembleDebug && adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk`
Expected: Home tab shows the cutoff card with ₱0 due (no debts yet) and the empty hint. Tapping Debts/Settings shows placeholder.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/home/HomeScreen.kt composeApp/src/commonMain/kotlin/com/snowball/App.kt
git commit -m "feat(home): cutoff dashboard with payment rows and mark-paid"
```

---

## Phase 8 — Debt Form & Debts List (Tasks 29–31)

### Task 29: Debt form ViewModel

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormViewModel.kt`

- [ ] **Step 1: Implement**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormViewModel.kt`:

```kotlin
package com.snowball.ui.form

import com.snowball.data.Repos
import com.snowball.data.model.Category
import com.snowball.data.model.Debt
import com.snowball.domain.today
import kotlinx.datetime.LocalDate

data class DebtFormState(
    val name: String = "",
    val categoryId: Long? = null,
    val monthlyAmount: String = "",
    val totalPayments: String = "",
    val dueDay: String = "",
    val useLastDayOfMonth: Boolean = false,
    val startDate: LocalDate = today(),
    val notes: String = "",
)

class DebtFormViewModel(private val repos: Repos, existing: Debt? = null) {
    var state: DebtFormState = if (existing == null) {
        DebtFormState()
    } else {
        DebtFormState(
            name = existing.name,
            categoryId = existing.categoryId,
            monthlyAmount = existing.monthlyAmount.toString(),
            totalPayments = existing.totalPayments.toString(),
            dueDay = existing.dueDay.toString(),
            useLastDayOfMonth = existing.useLastDayOfMonth,
            startDate = existing.startDate,
            notes = existing.notes.orEmpty(),
        )
    }
        private set

    private val existingId: Long? = existing?.id

    val categories: List<Category> = repos.categories.all().filter { it.behavior == com.snowball.data.model.CategoryBehavior.SCHEDULED }

    fun update(transform: (DebtFormState) -> DebtFormState) { state = transform(state) }

    fun save(): Boolean {
        val name = state.name.trim()
        val catId = state.categoryId ?: return false
        val monthly = state.monthlyAmount.toDoubleOrNull() ?: return false
        val total = state.totalPayments.toIntOrNull() ?: return false
        val due = state.dueDay.toIntOrNull() ?: return false
        if (name.isBlank() || monthly <= 0.0 || total <= 0 || due !in 1..31) return false

        if (existingId == null) {
            repos.debts.add(
                name = name,
                categoryId = catId,
                monthlyAmount = monthly,
                totalPayments = total,
                dueDay = due,
                useLastDayOfMonth = state.useLastDayOfMonth,
                startDate = state.startDate,
                notes = state.notes.ifBlank { null },
            )
        } else {
            repos.debts.update(
                id = existingId,
                name = name,
                categoryId = catId,
                monthlyAmount = monthly,
                totalPayments = total,
                dueDay = due,
                useLastDayOfMonth = state.useLastDayOfMonth,
                startDate = state.startDate,
                notes = state.notes.ifBlank { null },
            )
        }
        return true
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormViewModel.kt
git commit -m "feat(form): debt form viewmodel with validation"
```

---

### Task 30: Debt form screen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt`

- [ ] **Step 1: Implement**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt`:

```kotlin
package com.snowball.ui.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.ui.theme.SnowColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtFormScreen(vm: DebtFormViewModel, onCancel: () -> Unit, onSaved: () -> Unit) {
    val state = vm.state

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("New debt", style = MaterialTheme.typography.headlineLarge, color = SnowColors.Frost)
        Spacer(Modifier.height(20.dp))

        Field("Name") {
            OutlinedTextField(
                value = state.name,
                onValueChange = { v -> vm.update { it.copy(name = v) } },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors(),
                shape = RoundedCornerShape(12.dp),
            )
        }
        Spacer(Modifier.height(16.dp))

        Field("Category") { CategoryDropdown(vm) }
        Spacer(Modifier.height(16.dp))

        Field("Monthly amount") {
            OutlinedTextField(
                value = state.monthlyAmount,
                onValueChange = { v -> vm.update { it.copy(monthlyAmount = v.filter { c -> c.isDigit() || c == '.' }) } },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors(),
                shape = RoundedCornerShape(12.dp),
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Field("Total payments", modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = state.totalPayments,
                    onValueChange = { v -> vm.update { it.copy(totalPayments = v.filter { c -> c.isDigit() }) } },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
            Field("Due day (1–31)", modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = state.dueDay,
                    onValueChange = { v -> vm.update { it.copy(dueDay = v.filter { c -> c.isDigit() }) } },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = state.useLastDayOfMonth,
                onCheckedChange = { v -> vm.update { it.copy(useLastDayOfMonth = v) } },
                colors = SwitchDefaults.colors(checkedTrackColor = SnowColors.Ice),
            )
            Text(
                "  Use last day of month (Feb adjusts)",
                color = SnowColors.FrostMute,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(16.dp))

        Field("Start date (YYYY-MM-DD)") {
            OutlinedTextField(
                value = state.startDate.toString(),
                onValueChange = { v ->
                    runCatching { kotlinx.datetime.LocalDate.parse(v) }
                        .onSuccess { d -> vm.update { it.copy(startDate = d) } }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors(),
                shape = RoundedCornerShape(12.dp),
            )
        }
        Spacer(Modifier.height(16.dp))

        Field("Notes (optional)") {
            OutlinedTextField(
                value = state.notes,
                onValueChange = { v -> vm.update { it.copy(notes = v) } },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors(),
                shape = RoundedCornerShape(12.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel", color = SnowColors.FrostMute)
            }
            Button(
                onClick = { if (vm.save()) onSaved() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SnowColors.Ice, contentColor = SnowColors.Night),
                shape = RoundedCornerShape(12.dp),
            ) { Text("Save") }
        }
    }
}

@Composable
private fun Field(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 3.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(vm: DebtFormViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val current = vm.categories.firstOrNull { it.id == vm.state.categoryId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = current?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = textFieldColors(),
            shape = RoundedCornerShape(12.dp),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            vm.categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.name, color = SnowColors.Frost) },
                    onClick = {
                        vm.update { it.copy(categoryId = cat.id) }
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SnowColors.Frost,
    unfocusedTextColor = SnowColors.Frost,
    focusedBorderColor = SnowColors.Ice,
    unfocusedBorderColor = SnowColors.LineStrong,
    cursorColor = SnowColors.Ice,
)
```

- [ ] **Step 2: Build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt
git commit -m "feat(form): debt creation form screen"
```

---

### Task 31: Debts list screen with FAB

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsViewModel.kt`
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/App.kt`

- [ ] **Step 1: ViewModel**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsViewModel.kt`:

```kotlin
package com.snowball.ui.debts

import com.snowball.data.Repos
import com.snowball.data.model.Category
import com.snowball.data.model.Debt

data class DebtsState(
    val categories: List<Category>,
    val debtsByCategory: Map<Long, List<Debt>>,
    val showArchived: Boolean,
)

class DebtsViewModel(private val repos: Repos) {
    var showArchived: Boolean = false
        private set

    fun load(): DebtsState {
        val cats = repos.categories.all()
        val all = if (showArchived) repos.debts.all().filter { it.isArchived } else repos.debts.allActive()
        val grouped = all.groupBy { it.categoryId }
        return DebtsState(cats, grouped, showArchived)
    }

    fun toggleArchive() { showArchived = !showArchived }

    fun delete(id: Long) { repos.debts.delete(id) }
}
```

- [ ] **Step 2: Screen**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsScreen.kt`:

```kotlin
package com.snowball.ui.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.ui.components.PesoText
import com.snowball.ui.theme.SnowColors

@Composable
fun DebtsScreen(
    vm: DebtsViewModel,
    onAddDebt: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    var tick by remember { mutableStateOf(0) }
    val state = remember(tick) { vm.load() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.showArchived) "Archived" else "Active",
                    style = MaterialTheme.typography.headlineLarge,
                    color = SnowColors.Frost,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (state.showArchived) "View active" else "View archived",
                    style = MaterialTheme.typography.labelMedium,
                    color = SnowColors.Ice,
                    modifier = Modifier.clickable { vm.toggleArchive(); tick++ },
                )
            }

            Spacer(Modifier.height(16.dp))

            state.categories.forEach { cat ->
                val debts = state.debtsByCategory[cat.id].orEmpty()
                if (debts.isEmpty()) return@forEach
                Text(
                    cat.name.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                    color = SnowColors.FrostDim,
                )
                Spacer(Modifier.height(8.dp))
                debts.forEach { d ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SnowColors.CardElev)
                            .clickable { onEdit(d.id) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(d.name, style = MaterialTheme.typography.headlineSmall, color = SnowColors.Frost)
                            Text(
                                "Day ${d.dueDay} · ${d.totalPayments} months",
                                style = MaterialTheme.typography.bodySmall,
                                color = SnowColors.FrostMute,
                            )
                        }
                        PesoText(
                            amount = d.monthlyAmount,
                            style = MaterialTheme.typography.headlineSmall,
                            pesoColor = SnowColors.FrostDim,
                            numberColor = SnowColors.Frost,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(16.dp))
            }

            if (state.debtsByCategory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No debts yet. Tap + to add your first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SnowColors.FrostDim,
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddDebt,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(56.dp)
                .clip(CircleShape),
            containerColor = SnowColors.Ice,
            contentColor = SnowColors.Night,
        ) {
            Text("+", style = MaterialTheme.typography.headlineLarge)
        }
    }
}
```

- [ ] **Step 3: Wire form route into App**

Replace `composeApp/src/commonMain/kotlin/com/snowball/App.kt`:

```kotlin
package com.snowball

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.snowball.data.Repos
import com.snowball.ui.debts.DebtsScreen
import com.snowball.ui.debts.DebtsViewModel
import com.snowball.ui.form.DebtFormScreen
import com.snowball.ui.form.DebtFormViewModel
import com.snowball.ui.home.HomeScreen
import com.snowball.ui.home.HomeViewModel
import com.snowball.ui.nav.BottomNav
import com.snowball.ui.nav.Tab
import com.snowball.ui.theme.SnowballTheme

sealed interface Route {
    data object Tabs : Route
    data class Form(val existingDebtId: Long?) : Route
}

@Composable
fun App(repos: Repos) {
    SnowballTheme {
        var route by remember { mutableStateOf<Route>(Route.Tabs) }
        var tab by remember { mutableStateOf(Tab.Home) }
        var refreshKey by remember { mutableStateOf(0) }

        val homeVm = remember(refreshKey) { HomeViewModel(repos) }
        val debtsVm = remember(refreshKey) { DebtsViewModel(repos) }

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                when (val r = route) {
                    is Route.Tabs -> {
                        when (tab) {
                            Tab.Home -> HomeScreen(homeVm)
                            Tab.Debts -> DebtsScreen(
                                vm = debtsVm,
                                onAddDebt = { route = Route.Form(null) },
                                onEdit = { id -> route = Route.Form(id) },
                            )
                            Tab.Settings -> PlaceholderScreen("Settings")
                        }
                    }
                    is Route.Form -> {
                        val existing = r.existingDebtId?.let { repos.debts.byId(it) }
                        val formVm = remember(r.existingDebtId) { DebtFormViewModel(repos, existing) }
                        DebtFormScreen(
                            vm = formVm,
                            onCancel = { route = Route.Tabs },
                            onSaved = { route = Route.Tabs; refreshKey++ },
                        )
                    }
                }
            }
            if (route is Route.Tabs) {
                BottomNav(selected = tab, onSelect = { tab = it })
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(name, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
    }
}
```

- [ ] **Step 4: Build, install, smoke test**

Run: `./gradlew :composeApp:assembleDebug && adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk`
Expected:
1. Open app → Home shows empty cutoff
2. Tap Debts → list is empty with "Tap + to add"
3. Tap FAB → form opens
4. Fill in a sample debt (name "Sloan", category "Credit Card", monthly 3567, total 6, due day 17, start 2026-01-17) → tap Save
5. Returns to Debts list → "Sloan" appears under "CREDIT CARD"
6. Tap Home → debt now shows in cutoff if today's date puts it in window

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsViewModel.kt composeApp/src/commonMain/kotlin/com/snowball/ui/debts/DebtsScreen.kt composeApp/src/commonMain/kotlin/com/snowball/App.kt
git commit -m "feat(debts): debts list with FAB and edit route"
```

---

## Phase 9 — Settings (Tasks 32–33)

### Task 32: Settings ViewModel

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsViewModel.kt`

- [ ] **Step 1: Implement**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsViewModel.kt`:

```kotlin
package com.snowball.ui.settings

import com.snowball.data.Repos
import com.snowball.data.model.Settings

class SettingsViewModel(private val repos: Repos) {
    fun load(): Settings = repos.settings.get()
    fun setIncome(amount: Double) { repos.settings.setIncome(amount) }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :composeApp:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsViewModel.kt
git commit -m "feat(settings): settings viewmodel"
```

---

### Task 33: Settings screen with income input

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/App.kt`

- [ ] **Step 1: Settings screen**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsScreen.kt`:

```kotlin
package com.snowball.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snowball.ui.theme.SnowColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val initial = remember { vm.load() }
    var income by remember { mutableStateOf(initial.incomePerCutoff.toString()) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, color = SnowColors.Frost)
        Spacer(Modifier.height(24.dp))

        Text(
            "INCOME PER CUTOFF",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.FrostDim,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = income,
            onValueChange = { v ->
                income = v.filter { c -> c.isDigit() || c == '.' }
                income.toDoubleOrNull()?.let { vm.setIncome(it) }
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SnowColors.Frost,
                unfocusedTextColor = SnowColors.Frost,
                focusedBorderColor = SnowColors.Ice,
                unfocusedBorderColor = SnowColors.LineStrong,
                cursorColor = SnowColors.Ice,
            ),
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Same amount used for both 15th and 30th cutoffs.",
            style = MaterialTheme.typography.bodySmall,
            color = SnowColors.FrostMute,
        )

        Spacer(Modifier.height(40.dp))
        Text(
            "Snowball v0.1",
            style = MaterialTheme.typography.labelSmall,
            color = SnowColors.FrostDeep,
        )
    }
}
```

- [ ] **Step 2: Replace placeholder in App.kt**

In `composeApp/src/commonMain/kotlin/com/snowball/App.kt`, replace the `Tab.Settings -> PlaceholderScreen("Settings")` line:

Add import:
```kotlin
import com.snowball.ui.settings.SettingsScreen
import com.snowball.ui.settings.SettingsViewModel
```

Inside the tabs `when` block, replace:
```kotlin
Tab.Settings -> PlaceholderScreen("Settings")
```
with:
```kotlin
Tab.Settings -> {
    val settingsVm = remember(refreshKey) { SettingsViewModel(repos) }
    SettingsScreen(settingsVm)
}
```

- [ ] **Step 3: Install and verify**

Run: `./gradlew :composeApp:assembleDebug && adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk`
Expected: Settings tab shows the income field. Enter 25000 → switch to Home → breathing room reflects 25000 - dueTotal.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsScreen.kt composeApp/src/commonMain/kotlin/com/snowball/App.kt
git commit -m "feat(settings): income-per-cutoff input"
```

---

## Phase 10 — Delete & Final Smoke Test (Tasks 34–35)

### Task 34: Delete debt from edit form

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormViewModel.kt`

- [ ] **Step 1: Add delete to ViewModel**

In `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormViewModel.kt`, add:

```kotlin
    val isEditing: Boolean = existingId != null

    fun delete(): Boolean {
        val id = existingId ?: return false
        repos.debts.delete(id)
        return true
    }
```

- [ ] **Step 2: Add delete button to form when editing**

In `composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt`, modify the action row at the bottom. Replace the existing `Row` that holds Cancel + Save with:

```kotlin
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (vm.isEditing) {
                TextButton(onClick = { if (vm.delete()) onSaved() }, modifier = Modifier.weight(1f)) {
                    Text("Delete", color = SnowColors.Ember)
                }
            }
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel", color = SnowColors.FrostMute)
            }
            Button(
                onClick = { if (vm.save()) onSaved() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = SnowColors.Ice, contentColor = SnowColors.Night),
                shape = RoundedCornerShape(12.dp),
            ) { Text("Save") }
        }
```

Also change the header `Text("New debt", ...)` to:

```kotlin
        Text(
            if (vm.isEditing) "Edit debt" else "New debt",
            style = MaterialTheme.typography.headlineLarge,
            color = SnowColors.Frost,
        )
```

- [ ] **Step 3: Install and verify**

Run: `./gradlew :composeApp:assembleDebug && adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk`
Expected: Tap an existing debt from Debts list → form shows "Edit debt" with Delete/Cancel/Save row. Delete works.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormScreen.kt composeApp/src/commonMain/kotlin/com/snowball/ui/form/DebtFormViewModel.kt
git commit -m "feat(form): delete action when editing"
```

---

### Task 35: End-to-end smoke test and release APK

**Files:** none

- [ ] **Step 1: Wipe app data on the device**

Run: `adb shell pm clear com.snowball`
Expected: `Success`. App starts fresh.

- [ ] **Step 2: Run the full user journey**

On the device, complete the following journey end-to-end and confirm each step works:

1. Open app → land on Home → empty cutoff (₱0 due, ₱0 breathing room).
2. Go to Settings → set income to 25000 → return to Home → breathing room shows ₱25,000.
3. Go to Debts → tap FAB → add a debt:
   - Name: "Sloan 16,500"
   - Category: Credit Card
   - Monthly: 3567
   - Total payments: 6
   - Due day: 17
   - Start date: 2026-01-17
   - Save.
4. Add a second debt: "Loan 27,300" / Credit Card / 3080 / 12 / 19 / 2025-06-19.
5. Add a third: "Sloan 10,700" / Credit Card / 1421 / 12 / 30 / 2025-08-30.
6. Return to Home — verify that today's cutoff includes the debts whose dueDay falls in the current window. (Today is 2026-05-12 so the current cutoff is April 30, covering May 1–14 — none of these debts fall in. Change a debt's dueDay to 5 to verify a debt does appear.)
7. Tap a payment row → progress arc fills + name dims to indicate paid.
8. Tap again → undo: progress arc empties.
9. Edit a debt → change monthly → save → verify Home reflects new amount.
10. Delete a debt → it disappears from list.

- [ ] **Step 3: Build release APK**

Run: `./gradlew :composeApp:assembleRelease`
Expected: BUILD SUCCESSFUL. APK at `composeApp/build/outputs/apk/release/composeApp-release-unsigned.apk`.

- [ ] **Step 4: Sign with debug key for sideload**

(For personal sideload we don't need a real release key. The debug build is fine to use day-to-day. Optionally generate a personal upload key later when iOS/TestFlight comes online.)

Run: `./gradlew :composeApp:assembleDebug`
Expected: BUILD SUCCESSFUL. Use this APK as the daily-driver build.

- [ ] **Step 5: Tag v0.1**

```bash
git tag v0.1.0 -a -m "Snowball v0.1 — Android MVP shipped"
```

- [ ] **Step 6: Commit any remaining changes**

```bash
git add -A
git status
# if there are tracked changes:
git commit -m "chore: v0.1 release prep" || true
```

---

## Self-Review Notes (for the engineer following this plan)

- All `.sq` queries are referenced by their generated names — if a query name changes, the calling code in repositories must update.
- The `paymentsMadeEstimate` in `HomeScreen` is intentionally approximate for v0.1. A more accurate progress arc requires also threading `paymentsMade` into `DueRow` — fine as a v0.2 polish.
- The form accepts text input for date as `YYYY-MM-DD`. A picker is v0.2 polish; the text input is honest and works.
- `assembleRelease` produces an unsigned APK; the debug APK is the daily driver until a real signing key exists.

## What's done

When this plan is executed end-to-end, you have:
- A Compose Multiplatform Android app named **Snowball**
- Editorial Frost theme (Fraunces + DM Sans, dark midnight, ice accent)
- Add/Edit/Delete scheduled debts in pre-seeded categories
- Set income per cutoff
- See current cutoff with payments, mark them paid (and undo), see breathing room
- All data local, no network, no permissions beyond app launch
- All business logic unit-tested

## What comes next (deferred plans)

- **v0.2 — Polish & MISC:** Debt Detail screen, MISC items, Category management UI, Archive view, accurate progress arcs.
- **v0.3 — Snowball journey:** Snowball motif card on Home, "Up next" card, overdue rollover, total-cleared and debt-free-date computation.
- **v0.4 — Notifications:** Android `AlarmManager` for payday-summary local notifications.
- **v0.5 — Multi-platform:** iOS target (via Mac+Xcode), macOS desktop, Windows desktop.
