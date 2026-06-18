# Data Export / Import Implementation Plan

> **For agentic workers:** execute task-by-task; steps use checkbox syntax.

**Goal:** Cross-platform JSON backup with replace-on-import, transported via clipboard/text.

**Architecture:** Pure `BackupCodec` (kotlinx.serialization) + DB-aware `BackupService` (transactional replace) + Settings UI. See `docs/superpowers/specs/2026-06-19-data-export-import-design.md`.

**Tech Stack:** Kotlin Multiplatform, kotlinx.serialization-json, SQLDelight, Compose Multiplatform.

---

### Task 1: Add kotlinx.serialization

**Files:** `gradle/libs.versions.toml`, `composeApp/build.gradle.kts`

- [ ] Add version `kotlinx-serialization = "1.8.0"`, library `kotlinx-serialization-json`, plugin `kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }`.
- [ ] Apply `alias(libs.plugins.kotlin.serialization)` in build.gradle.kts plugins; add `implementation(libs.kotlinx.serialization.json)` to `commonMain.dependencies`.

### Task 2: SQL queries

**Files:** `composeApp/src/commonMain/sqldelight/com/snowball/db/{Payment,Category,Debt,Settings}.sq`

- [ ] `Payment.sq`: `selectAll: SELECT * FROM Payment;`, `insertWithId: INSERT INTO Payment(id,debtId,paidDate,amount,createdAt) VALUES (?,?,?,?,?);`, `deleteAll: DELETE FROM Payment;`
- [ ] `Category.sq`: `insertWithId: INSERT INTO Category(id,name,isSystem,behavior,iconKey,createdAt) VALUES (?,?,?,?,?,?);`, `deleteAll: DELETE FROM Category;`
- [ ] `Debt.sq`: `insertWithId: INSERT INTO Debt(id,name,categoryId,monthlyAmount,totalPayments,dueDay,useLastDayOfMonth,startDate,firstPaymentDate,isArchived,notes,createdAt) VALUES (?,?,?,?,?,?,?,?,?,?,?,?);`, `deleteAll: DELETE FROM Debt;`
- [ ] `Settings.sq`: `replaceAll: UPDATE Settings SET incomePerCutoff=?, currency=?, notificationsEnabled=?, notificationHour=?, notificationMinute=?, firstLaunchSeen=?, swipeCoachmarkSeen=?, paidAheadKey=? WHERE id=1;`

### Task 3: DTOs + Codec (TDD, commonTest)

**Files:** Create `data/backup/BackupDto.kt`, `data/backup/BackupCodec.kt`; Test `commonTest/.../backup/BackupCodecTest.kt`

- [ ] `@Serializable` DTOs: `BackupFile`, `CategoryDto`, `DebtDto`, `PaymentDto`, `SettingsDto` (all primitive fields; dates as String).
- [ ] `BackupFormatException(message)`; `BackupCodec` object with `CURRENT_FORMAT_VERSION = 1`, `encode(BackupFile): String`, `decode(String): BackupFile` (wrap `SerializationException` → `BackupFormatException`; reject `formatVersion != 1`).
- [ ] Test: round-trip equality; decode rejects malformed text; decode rejects unsupported formatVersion.

### Task 4: BackupService (export + replace import) + Repos wiring

**Files:** Create `data/backup/BackupService.kt`; Modify `data/Repos.kt`; Test `androidUnitTest/.../backup/BackupServiceTest.kt`

- [ ] `ImportResult` sealed (`Success(categories,debts,payments)`, `Failure(message)`).
- [ ] `BackupService(db)`: `export(exportedAt): String` reads all tables → `BackupFile` → encode. `import(json): ImportResult` decodes, checks `dbVersion == SnowballDb.Schema.version` (else Failure), then `db.transaction {}` delete Payment→Debt→Category, insert Category→Debt→Payment with ids, `replaceAll` settings. Catch `BackupFormatException`/`Exception` → Failure (transaction rolls back).
- [ ] `Repos.backup = BackupService(db)`.
- [ ] Test: seed → export → import into fresh DB → assert tables + IDs equal; bad JSON → Failure and DB untouched.

### Task 5: Settings UI + App wiring

**Files:** Modify `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsViewModel.kt`, `App.kt`

- [ ] `SettingsViewModel`: `exportJson(): String`, `import(json): ImportResult` delegating to `repos.backup` (stamp `exportedAt` via `Clock.System.now()`).
- [ ] `SettingsScreen`: "BACKUP & RESTORE" section; Export dialog (selectable JSON + Copy via `LocalClipboardManager`); Import dialog (paste field + Paste button + Replace confirm). New param `onDataReplaced: () -> Unit`.
- [ ] `App.kt`: pass `onDataReplaced = { route = Route.Tabs; tab = Tab.Home; refreshKey++ }`.

### Task 6: Build, test, release

- [ ] `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest` green (fix failures).
- [ ] Bump versionCode 5→6 / versionName 0.5.0→0.6.0 + Settings string; commit; tag v0.6.0; push.
