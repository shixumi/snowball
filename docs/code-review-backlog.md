# Code Review Backlog — low-priority findings

From a full-codebase review on 2026-06-19. All verified against the code; none
can corrupt data or crash under normal (single-threaded UI) use. Listed for
future hardening, not urgent.

1. **iOS export ignores write failure** — `iosMain/.../platform/BackupFiles.ios.kt`
   `(content as NSString).writeToURL(..., error = null)` discards the result. On
   a failed write (e.g. disk full) the share sheet still presents a stale/empty
   temp file. Fix: check the returned `Boolean`; skip presenting the share sheet
   (and surface an error) if the write failed.

2. **No double-submit guard on Save/confirm buttons** — `DebtFormScreen`,
   `MiscFormScreen`, category dialogs. A rapid double-tap could fire `save()`
   twice before navigation tears the screen down, creating a duplicate. Fix:
   disable the button after first click, or guard with a `saving` flag.

3. **New-row id via `selectAll().first()`** — `DebtRepository.add` (and the form
   ViewModels) find the just-inserted debt by re-querying ordered `createdAt DESC`
   and taking the first row. Correct under the single-threaded UI, but fragile to
   same-millisecond inserts / clock skew. Prefer `last_insert_rowid()`.

4. **`remember { vm.load() }` without key** — `InsightsScreen:67`. Works today
   because the screen is disposed/recomposed on every tab entry (and `refreshKey`
   only bumps while it's off-screen), so it reloads fresh. Use `remember(vm)` to
   make that intent explicit and refactor-safe.

## Verified NOT bugs (investigated, dismissed)
- Deleting an in-use category cannot orphan debts — the UI forces reassignment
  first (`CategoryManagementScreen` Reassign dialog); plain delete only when the
  debt count is 0; system categories aren't deletable.
- `InsightsCalculator.snapshot`/`forecastCutoffs` measure progress by payment
  count × `monthlyAmount`; mark-paid always records `monthlyAmount`, so this
  equals the summed amounts — no discrepancy in practice.
- Cutoff / date math (windows, leap years, month-end clamping) traced and correct.
- Concurrency races: the UI is single-threaded; no concurrent inserts/deletes.
- Haptics run from Compose callbacks (main thread); `applicationContext` is not
  leaked; `settingsQueries.select().executeAsOne()` is safe (row guaranteed by
  `insertIfMissing`).

## Known intentional gaps (separate backlog)
- iOS notifications are stubbed (toggle persists but nothing schedules).
- iOS app icon (being addressed).
