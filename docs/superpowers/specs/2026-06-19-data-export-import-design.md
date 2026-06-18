# Data Export / Import (Backup & Restore) — Design

**Date:** 2026-06-19
**Status:** Approved (full-auto build)

## Goal

Let a user export all their Snowball data to a portable JSON document and import
it back, **replacing** whatever is currently stored. Primary motivating use case:
move real data from the Android phone onto the freshly-installed iOS app (iOS
starts with an empty, separate database).

## Why now

v0.5.0 made Snowball multiplatform. The iOS install has its own empty database,
so there's no built-in way to carry existing debts/payments across. A
cross-platform JSON backup is the migration bridge, and doubles as a manual
backup mechanism on either platform.

## Scope decisions

- **Replace, not merge.** Import wipes all existing data and restores the
  snapshot exactly (IDs preserved). Merge (dedup/conflict resolution) is
  explicitly out of scope — it doesn't serve the "move my data to the new phone"
  use case and adds significant complexity. Import is gated behind an explicit
  "this overwrites everything" confirmation.
- **Transport = clipboard / text, not file pickers (v1).** The JSON is shown in
  a selectable text box with **Copy to clipboard** on export, and pasted into a
  text field on import. Rationale: clipboard + text fields are pure Compose
  Multiplatform (`LocalClipboardManager`, `TextField`) with **zero platform
  interop**, so the feature ships reliably on both platforms today without the
  untestable iOS `UIDocumentPicker`/share-sheet code that the rest of the port
  showed is risky to write blind. Snowball's dataset is small (a handful of
  debts, dozens of payments → a few KB of JSON), so copy/paste via
  notes/email/messaging is entirely practical. File-based share/open is a
  documented follow-up (v2), not a blocker.

## Data captured

Everything needed to reconstruct app state, read straight from the DB rows
(including columns the repositories don't normally expose, e.g. `createdAt`):

- **Categories** — id, name, isSystem, behavior, iconKey, createdAt
- **Debts** — id, name, categoryId, monthlyAmount, totalPayments, dueDay,
  useLastDayOfMonth, startDate, firstPaymentDate, isArchived, notes, createdAt
- **Payments** — id, debtId, paidDate, amount, createdAt
- **Settings** (single row) — incomePerCutoff, currency, notificationsEnabled,
  notificationHour, notificationMinute, firstLaunchSeen, swipeCoachmarkSeen,
  paidAheadKey

## JSON format

```json
{
  "formatVersion": 1,
  "dbVersion": 5,
  "exportedAt": 1750000000000,
  "categories": [ { "id": 1, "name": "Credit Card", "isSystem": true, "behavior": "SCHEDULED", "iconKey": "credit_card", "createdAt": 0 } ],
  "debts": [ ... ],
  "payments": [ ... ],
  "settings": { ... }
}
```

- `formatVersion` — version of *this backup envelope* (currently 1). Import
  rejects anything it doesn't recognise.
- `dbVersion` — `SnowballDb.Schema.version` at export time. Import requires it to
  equal the running app's schema version; v1 does **not** migrate older/newer
  backups (rejected with a clear message). Recorded so future versions can.
- Dates are stored as ISO `yyyy-MM-dd` strings (exactly as in the DB), so no
  custom date serializers are needed.

## Architecture

Decoupled, each piece independently testable:

- **`BackupDto.kt`** (commonMain) — `@Serializable` DTOs mirroring DB rows
  (all primitive fields). Decoupled from the domain models so the wire format is
  stable and date handling stays trivial.
- **`BackupCodec.kt`** (commonMain) — pure `encode(BackupFile): String` /
  `decode(String): BackupFile` over `kotlinx.serialization.json`. No DB access →
  unit-testable in commonTest. `decode` throws a typed `BackupFormatException`
  on malformed JSON or unsupported `formatVersion`.
- **`BackupService.kt`** (commonMain) — owns the `SnowballDb`.
  - `export(exportedAt): String` — reads every table, builds `BackupFile`,
    encodes.
  - `import(json): ImportResult` — decodes, validates versions, then in a single
    `db.transaction { }`: delete children→parents (Payment, Debt, Category),
    re-insert parents→children with **preserved IDs**, overwrite the Settings
    row. Returns `ImportResult.Success(counts)` or `ImportResult.Failure(reason)`.
    Any exception rolls the transaction back, leaving existing data intact.
- Exposed as `Repos.backup`.

### Referential integrity on import

IDs are preserved verbatim. Deletes run children-first
(Payment → Debt → Category); inserts run parents-first
(Category → Debt → Payment), so foreign keys hold whether or not SQLite FK
enforcement is enabled. Because the wipe removes system categories too and they
are restored from the snapshot, the `Category.name` UNIQUE constraint never
conflicts.

### New SQL queries

- `Payment.sq`: `selectAll`, `insertWithId`, `deleteAll`
- `Category.sq`: `insertWithId`, `deleteAll`
- `Debt.sq`: `insertWithId`, `deleteAll`
- `Settings.sq`: `replaceAll` (update every column where id = 1)

## UI

A **Backup & Restore** section at the bottom of Settings:

- **Export data** → dialog showing the JSON in a read-only, selectable,
  scrollable box + **Copy** button (writes to clipboard, haptic tick + "Copied"
  ack). User pastes it wherever they like to transfer it.
- **Import data** → dialog with a multiline paste field + **Paste from
  clipboard** helper. The destructive **Replace my data** button requires a
  second confirmation ("This erases your current debts, payments and settings.").
  On success: haptic, dismiss, and the app refreshes (bumps `refreshKey`) and
  returns to Home so the restored data shows immediately. On failure: inline
  error message from `ImportResult.Failure`.

`App` passes an `onDataReplaced` callback into the Settings screen that performs
`route = Tabs; tab = Home; refreshKey++`.

## Error handling

- Malformed/empty JSON → "Couldn't read that backup — the text isn't a valid
  Snowball export."
- Wrong `formatVersion` / `dbVersion` → "This backup was made by an incompatible
  version of Snowball."
- Transactional import → on any failure the DB is unchanged.

## Testing

- **commonTest `BackupCodecTest`** — encode→decode round-trip equality; decode
  rejects malformed JSON and unsupported `formatVersion`.
- **androidUnitTest `BackupServiceTest`** (JVM, `JdbcSqliteDriver` in-memory,
  mirrors existing repo tests) — seed data, export, wipe-and-import into a second
  DB, assert all tables match incl. preserved IDs; assert import is atomic
  (bad payload leaves data intact).

## Out of scope (follow-ups)

- File-based export/import via `UIDocumentPicker` (iOS) and SAF/`ACTION_SEND`
  (Android).
- Backup migration across differing `dbVersion`s.
- Encryption / cloud sync.
