# Snowball v0.2 — Sub-project C: Category Management

**Date:** 2026-05-13
**Scope:** Category management screen (list/add/rename/delete/reassign) + `iconKey` schema migration + icon picker UI.
**Excluded:** Cutoff rollover (sub-project D), notifications (E), platform expansion (F).

**Autonomous-design note:** Written without per-section approval per user's overnight authorization.

## Purpose

HANDOFF says: "Category Management screen — pushed from Settings; list categories with system-lock icons on Credit Card + MISC; inline rename (system not editable); '+ New category'; delete blocked for system, prompts to reassign for user categories with debts. Adds `iconKey` column + icon picker."

## In scope

| Item | Resolution |
|---|---|
| `iconKey TEXT` column on Category table | SQLDelight migration `1.sqm` + updated `Category.sq` |
| `Category` data model gains `iconKey: String` | Modify `data/model/Category.kt` + `CategoryRepository` |
| `CategoryIcon.icon()` switches from name-pattern to iconKey lookup | Modify `ui/components/CategoryIcon.kt` |
| 12 named icons in a predefined set | New `IconCatalog` object with key→ImageVector map |
| Settings → "Manage categories" entry point | Modify `SettingsScreen.kt` |
| `Route.CategoryManagement` route | Modify `App.kt` |
| `CategoryManagementScreen` (list + add/edit/delete affordances) | New file |
| `CategoryManagementViewModel` | New file |
| Inline rename dialog | AlertDialog with text field |
| Icon picker dialog | Grid of icon options |
| "+ New category" flow | Add bottom-anchored button → dialog with name + icon |
| Delete flow with reassignment | If category has debts, modal to pick replacement; else confirm + delete |
| System lock indicator on Credit Card + MISC rows | Lock icon, disabled tap |

## Schema migration

**`composeApp/src/commonMain/sqldelight/com/snowball/db/Category.sq`** updated:

```sql
CREATE TABLE Category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    isSystem INTEGER NOT NULL DEFAULT 0,
    behavior TEXT NOT NULL,
    iconKey TEXT NOT NULL DEFAULT '',
    createdAt INTEGER NOT NULL
);
```

**`composeApp/src/commonMain/sqldelight/com/snowball/db/migrations/1.sqm`** *(new)*:

```sql
ALTER TABLE Category ADD COLUMN iconKey TEXT NOT NULL DEFAULT '';
UPDATE Category SET iconKey = 'credit_card' WHERE name = 'Credit Card';
UPDATE Category SET iconKey = 'more_horiz' WHERE name = 'MISC';
```

Wait — SQLDelight 2.x expects `.sqm` files in the SAME directory as `.sq` files (not a `migrations/` subdirectory). Path is `composeApp/src/commonMain/sqldelight/com/snowball/db/1.sqm`. Filename `1.sqm` means "migration TO version 2 from version 1." SQLDelight reads the highest .sqm number and uses that + 1 as the schema version.

**Insert/update query changes** in `Category.sq`:

```sql
insert:
INSERT INTO Category(name, isSystem, behavior, iconKey, createdAt) VALUES (?, ?, ?, ?, ?);

insertOrIgnore:
INSERT OR IGNORE INTO Category(name, isSystem, behavior, iconKey, createdAt) VALUES (?, ?, ?, ?, ?);

updateIcon:
UPDATE Category SET iconKey = :iconKey WHERE id = :id AND isSystem = 0;
```

`renameById` stays unchanged (rename ignores iconKey). New `updateIcon` query for the icon picker.

## Icon catalog

**`composeApp/src/commonMain/kotlin/com/snowball/ui/components/IconCatalog.kt`** *(new)*:

12 keys → ImageVector. Material Outlined icons:
- `credit_card` → `Icons.Outlined.CreditCard`
- `more_horiz` → `Icons.Outlined.MoreHoriz`
- `account_balance` → `Icons.Outlined.AccountBalance`
- `shopping_bag` → `Icons.Outlined.ShoppingBag`
- `phone_iphone` → `Icons.Outlined.PhoneIphone`
- `home` → `Icons.Outlined.Home`
- `directions_car` → `Icons.Outlined.DirectionsCar`
- `local_hospital` → `Icons.Outlined.LocalHospital`
- `school` → `Icons.Outlined.School`
- `receipt` → `Icons.Outlined.Receipt`
- `inventory_2` → `Icons.Outlined.Inventory2` (default fallback)
- `wifi` → `Icons.Outlined.Wifi`

Public API:
- `val IconCatalog: List<Pair<String, ImageVector>>` — ordered list for the picker.
- `fun iconFor(key: String): ImageVector` — lookup with fallback to `Inventory2`.

## CategoryIcon migration

`composeApp/src/commonMain/kotlin/com/snowball/ui/components/CategoryIcon.kt` becomes:

```kotlin
package com.snowball.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.snowball.data.model.Category

fun Category.icon(): ImageVector = iconFor(iconKey)
```

The name-based pattern matching goes away. All categories must have an `iconKey`. New user categories pick one via the icon-picker dialog; system categories' iconKey is set in the seed.

## Routes

`App.kt` sealed interface gains:

```kotlin
data object CategoryManagement : Route
```

(The add/edit/icon-picker UIs are dialogs inside `CategoryManagementScreen`, not separate routes.)

## CategoryManagementViewModel

```kotlin
data class CategoryManagementState(
    val categories: List<Category>,
    val debtCounts: Map<Long, Int>,  // categoryId → number of debts
)

sealed interface CategoryEditTarget {
    data class Rename(val category: Category) : CategoryEditTarget
    data class PickIcon(val category: Category) : CategoryEditTarget
    data object NewCategory : CategoryEditTarget
    data class Delete(val category: Category, val debtCount: Int) : CategoryEditTarget
    data class Reassign(val source: Category, val candidates: List<Category>) : CategoryEditTarget
}

class CategoryManagementViewModel(private val repos: Repos) {
    fun load(): CategoryManagementState {
        val cats = repos.categories.all()
        val counts = cats.associate { c -> c.id to repos.debts.all().count { it.categoryId == c.id } }
        return CategoryManagementState(cats, counts)
    }
    fun rename(id: Long, newName: String) { repos.categories.rename(id, newName) }
    fun setIcon(id: Long, iconKey: String) { repos.categories.setIcon(id, iconKey) }
    fun create(name: String, iconKey: String) { repos.categories.add(name, CategoryBehavior.SCHEDULED, iconKey) }
    fun reassignAndDelete(sourceId: Long, targetId: Long) {
        // Move all debts; delete the source category
        repos.debts.all().filter { it.categoryId == sourceId }.forEach { d ->
            repos.debts.update(
                id = d.id, name = d.name, categoryId = targetId,
                monthlyAmount = d.monthlyAmount, totalPayments = d.totalPayments,
                dueDay = d.dueDay, useLastDayOfMonth = d.useLastDayOfMonth,
                startDate = d.startDate, notes = d.notes,
            )
        }
        repos.categories.delete(sourceId)
    }
    fun delete(id: Long) { repos.categories.delete(id) }
}
```

`repos.categories.add` gains an `iconKey` parameter (the seed and CategoryRepository need updating).

## CategoryManagementScreen layout

Scaffold with TopAppBar (back arrow + "Manage categories" title). Body:

- Header "CATEGORIES" labelSmall FrostDim, 4sp tracking.
- Spacer.
- LazyColumn / Column of rows, each:
  - Icon (24dp) + name (bodyLarge Frost) + lock icon for system categories + count "N debts" FrostMute + (if user) overflow `⋮` button.
  - For system rows: tap is a no-op, lock icon is shown.
  - For user rows: tap opens overflow menu with Rename / Change icon / Delete items.
- Sticky bottom button: full-width "+ New category" Button (Ice background).

Dialogs (all `AlertDialog`):
1. **Rename:** title "Rename category" + OutlinedTextField + Save/Cancel.
2. **Pick icon:** title "Choose an icon" + LazyVerticalGrid (4 columns × 3 rows of icons, 12 total). Tap an icon → commits + dismisses.
3. **New category:** title "New category" + name field + icon row (or "Choose icon" button → opens picker) + Create/Cancel.
4. **Confirm delete (no debts):** title "Delete {name}?" + body "This category has no debts." + Delete/Cancel.
5. **Reassign (has debts):** title "Reassign N debts" + body "Choose another category for the {N} debts currently in {sourceName}." + radio list of OTHER SCHEDULED categories (excluding MISC and source itself) + Move/Cancel. If no other categories exist, show "Add another category first." with a Cancel-only dialog.

## Settings entry point

Add a clickable row under the income field (or above the version stamp):

```
MANAGE CATEGORIES                       ›
```

LabelSmall tracked, Ice-tinted chevron right, tap → navigates to `Route.CategoryManagement`.

## File-level change inventory

**New files (3):**
- `composeApp/src/commonMain/sqldelight/com/snowball/db/1.sqm`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/components/IconCatalog.kt`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/categories/CategoryManagementScreen.kt`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/categories/CategoryManagementViewModel.kt`

(That's 4 — my count error. Updating inventory header.)

**New files (4):**
- `composeApp/src/commonMain/sqldelight/com/snowball/db/1.sqm`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/components/IconCatalog.kt`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/categories/CategoryManagementScreen.kt`
- `composeApp/src/commonMain/kotlin/com/snowball/ui/categories/CategoryManagementViewModel.kt`

**Modified files (6):**
- `composeApp/src/commonMain/sqldelight/com/snowball/db/Category.sq` — add iconKey column; updated queries
- `composeApp/src/commonMain/kotlin/com/snowball/data/model/Category.kt` — add iconKey field
- `composeApp/src/commonMain/kotlin/com/snowball/data/repo/CategoryRepository.kt` — pass iconKey through
- `composeApp/src/commonMain/kotlin/com/snowball/data/db/DatabaseFactory.kt` — seed iconKey
- `composeApp/src/commonMain/kotlin/com/snowball/ui/components/CategoryIcon.kt` — switch to iconKey lookup
- `composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsScreen.kt` — add entry-point row
- `composeApp/src/commonMain/kotlin/com/snowball/App.kt` — wire new route

That's 7 modified, plus 4 new = 11 file changes.

## Tests

- `CategoryManagementTest` (androidUnitTest, since it touches Repos): not required for v0.2.2 — VM is mechanical wiring of pre-tested repos. Schema migration is verified via build + runtime smoke test.
- No new commonTest files. Existing tests should still pass.

## Risks

- **Schema migration on existing emulator state.** If the migration fails, the user's existing debts/payments are at risk. Mitigation: the migration uses `ALTER TABLE ADD COLUMN` with a `DEFAULT ''` — safest type of migration. SQLite handles this online with no data movement. If it fails the SQLDelight schema verifier will refuse to launch — easy to detect.
- **SQLDelight regeneration.** After modifying `.sq` files, Gradle must regenerate the Kotlin DAO. Running `./gradlew :composeApp:generateDebugSnowballDbInterface` (or just `:composeApp:assembleDebug`) does this. The generated `categoryQueries.insert(...)` signature changes — `CategoryRepository.add` calls must update to match.
- **iconKey empty for legacy categories.** If a user runs an old build, then upgrades, their custom categories have `iconKey = ''`. `iconFor("")` returns the Inventory2 fallback. Not broken — just not pretty. Acceptable.
- **Reassignment when only MISC is available.** A user with one user-category and Credit Card + MISC system. If they delete the user-category with debts: only Credit Card is a valid SCHEDULED target. If no other SCHEDULED categories exist, the dialog says "Add another category first." (no reassignment possible).

## Success criteria

- Schema migrates cleanly on an existing emulator install (test via uninstall → reinstall old v0.2.1 → upgrade to v0.2.2 build).
- New users can add a category with an icon picker.
- Existing users keep their categories with default icons.
- Credit Card + MISC cannot be renamed or deleted (lock icon shown).
- Deleting a user category with debts forces reassignment.
- Settings has a clear path to category management.
- Tag `v0.2.2` pushed.
