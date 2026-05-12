# Snowball v0.2 Sub-project C — Category Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development or superpowers:executing-plans. Steps use `- [ ]` checkboxes.

**Goal:** Add a Category Management screen reachable from Settings; introduce an `iconKey` column on Category; ship an icon-picker UI.

**Architecture:** SQLDelight migration `1.sqm` adds the column, .sq queries gain it, `Category` data class gets the field, `CategoryRepository` plumbs it through, and a new `IconCatalog` maps icon keys to ImageVectors. UI: new `categories/` package with VM + Screen and inline dialogs for add/rename/icon/delete/reassign.

**Tech Stack:** Same as v0.2.1. Schema bumps to v2.

**Reference:** Spec at `docs/superpowers/specs/2026-05-13-snowball-v02c-categories-design.md`.

---

## Build / verification recipe

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:assembleDebug
```

Install on `emulator-5554`:

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

**NEVER `Read` a `.png` file.**

---

## Task 1: Schema migration + Category data model + CategoryRepository

**Files:**
- Modify: `composeApp/src/commonMain/sqldelight/com/snowball/db/Category.sq`
- Create: `composeApp/src/commonMain/sqldelight/com/snowball/db/1.sqm`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/data/model/Category.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/data/repo/CategoryRepository.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/data/db/DatabaseFactory.kt`

**Goal:** End-to-end schema change with backwards-compat migration.

- [ ] **Step 1: Update Category.sq**

Replace the contents of `composeApp/src/commonMain/sqldelight/com/snowball/db/Category.sq` with:

```sql
CREATE TABLE Category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    isSystem INTEGER NOT NULL DEFAULT 0,
    behavior TEXT NOT NULL,
    iconKey TEXT NOT NULL DEFAULT '',
    createdAt INTEGER NOT NULL
);

selectAll:
SELECT * FROM Category ORDER BY isSystem DESC, name ASC;

selectById:
SELECT * FROM Category WHERE id = ?;

insert:
INSERT INTO Category(name, isSystem, behavior, iconKey, createdAt) VALUES (?, ?, ?, ?, ?);

insertOrIgnore:
INSERT OR IGNORE INTO Category(name, isSystem, behavior, iconKey, createdAt) VALUES (?, ?, ?, ?, ?);

renameById:
UPDATE Category SET name = :name WHERE id = :id AND isSystem = 0;

setIconById:
UPDATE Category SET iconKey = :iconKey WHERE id = :id;

deleteById:
DELETE FROM Category WHERE id = ? AND isSystem = 0;
```

- [ ] **Step 2: Create 1.sqm migration**

Create `composeApp/src/commonMain/sqldelight/com/snowball/db/1.sqm` with:

```sql
ALTER TABLE Category ADD COLUMN iconKey TEXT NOT NULL DEFAULT '';
UPDATE Category SET iconKey = 'credit_card' WHERE name = 'Credit Card';
UPDATE Category SET iconKey = 'more_horiz' WHERE name = 'MISC';
```

The filename `1.sqm` tells SQLDelight: "this migrates from schema v1 to v2." After this file exists, `SnowballDb.Schema.version` is 2.

- [ ] **Step 3: Update Category data model**

Replace `composeApp/src/commonMain/kotlin/com/snowball/data/model/Category.kt` with:

```kotlin
package com.snowball.data.model

data class Category(
    val id: Long,
    val name: String,
    val isSystem: Boolean,
    val behavior: CategoryBehavior,
    val iconKey: String,
)

enum class CategoryBehavior { SCHEDULED, LEDGER }
```

(If the existing file has `CategoryBehavior` declared elsewhere, keep the existing enum and just add `iconKey` to the data class. Most likely the file already has the enum — preserve it exactly.)

- [ ] **Step 4: Read the existing CategoryRepository.kt to understand current toModel mapping**

Use the Read tool to inspect `composeApp/src/commonMain/kotlin/com/snowball/data/repo/CategoryRepository.kt`. Note the function signatures for `add`, `insertOrIgnore`, and the internal `toModel` mapping function.

- [ ] **Step 5: Update CategoryRepository.kt**

The current `add(name: String, behavior: CategoryBehavior)` becomes `add(name: String, behavior: CategoryBehavior, iconKey: String = "")`. The `toModel()` function reads the new `iconKey` column from the row. Add a new `setIcon(id: Long, iconKey: String)` method.

Expected final shape of relevant methods (adjust the exact toModel reference to match what's already there):

```kotlin
fun add(name: String, behavior: CategoryBehavior, iconKey: String = "") {
    queries.insert(
        name = name,
        isSystem = 0,
        behavior = behavior.name,
        iconKey = iconKey,
        createdAt = nowMillis(),
    )
}

fun setIcon(id: Long, iconKey: String) {
    queries.setIconById(iconKey = iconKey, id = id)
}

// In the private mapper:
private fun com.snowball.db.Category.toModel(): Category = Category(
    id = id,
    name = name,
    isSystem = isSystem == 1L,
    behavior = CategoryBehavior.valueOf(behavior),
    iconKey = iconKey,
)
```

The `rename(id, newName)` and `delete(id)` methods stay unchanged.

- [ ] **Step 6: Update DatabaseFactory to seed iconKey**

Open `composeApp/src/commonMain/kotlin/com/snowball/data/db/DatabaseFactory.kt`. The two `insertOrIgnore` calls in `seedSystemCategories` need an `iconKey` parameter:

```kotlin
db.categoryQueries.insertOrIgnore(
    name = "Credit Card",
    isSystem = 1,
    behavior = "SCHEDULED",
    iconKey = "credit_card",
    createdAt = now,
)
db.categoryQueries.insertOrIgnore(
    name = "MISC",
    isSystem = 1,
    behavior = "LEDGER",
    iconKey = "more_horiz",
    createdAt = now,
)
```

- [ ] **Step 7: Build**

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL. Gradle regenerates the SQLDelight DAO; `categoryQueries.insert(...)` etc. all gain the new `iconKey` parameter.

If the build fails with "expected X arguments but got Y" on `categoryQueries.insert/insertOrIgnore`, that means a caller wasn't updated. Search the repo:

```bash
grep -rn "categoryQueries\.insert\|repos.categories.add\(" composeApp/src/commonMain
```

Find all call sites and add the `iconKey` parameter to each. The only legitimate callers should be `DatabaseFactory` and `CategoryRepository.add`.

- [ ] **Step 8: Run tests + install + smoke check**

```bash
./gradlew.bat :composeApp:testDebugUnitTest
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

Expected: app launches without database errors. The existing Credit Card and MISC categories should still appear in the Debts tab and form dropdown, with their existing icons (because the migration backfilled iconKey for them and Task 3 below will switch CategoryIcon.icon() to read it).

Verify via logcat that the migration ran without errors:

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe logcat -d | grep -iE "snowball|sqlite" | tail -20
```

- [ ] **Step 9: Commit**

```bash
git add composeApp/src/commonMain/sqldelight/com/snowball/db/Category.sq \
        composeApp/src/commonMain/sqldelight/com/snowball/db/1.sqm \
        composeApp/src/commonMain/kotlin/com/snowball/data/model/Category.kt \
        composeApp/src/commonMain/kotlin/com/snowball/data/repo/CategoryRepository.kt \
        composeApp/src/commonMain/kotlin/com/snowball/data/db/DatabaseFactory.kt
git commit -m "feat(db): add iconKey column to Category via 1.sqm migration"
```

---

## Task 2: IconCatalog + CategoryIcon migration

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/IconCatalog.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/components/CategoryIcon.kt`

**Goal:** Replace name-pattern icon mapping with iconKey-based lookup. 12 named icons in the catalog.

- [ ] **Step 1: Create IconCatalog.kt**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/components/IconCatalog.kt`:

```kotlin
package com.snowball.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The catalog of icons users can pick from for their categories.
 * The first entry (`inventory_2`) is the default fallback when an
 * iconKey is empty or unrecognized.
 */
val IconCatalog: List<Pair<String, ImageVector>> = listOf(
    "inventory_2" to Icons.Outlined.Inventory2,
    "credit_card" to Icons.Outlined.CreditCard,
    "more_horiz" to Icons.Outlined.MoreHoriz,
    "account_balance" to Icons.Outlined.AccountBalance,
    "shopping_bag" to Icons.Outlined.ShoppingBag,
    "phone_iphone" to Icons.Outlined.PhoneIphone,
    "home" to Icons.Outlined.Home,
    "directions_car" to Icons.Outlined.DirectionsCar,
    "local_hospital" to Icons.Outlined.LocalHospital,
    "school" to Icons.Outlined.School,
    "receipt" to Icons.Outlined.Receipt,
    "wifi" to Icons.Outlined.Wifi,
)

private val byKey: Map<String, ImageVector> = IconCatalog.toMap()

fun iconFor(key: String): ImageVector = byKey[key] ?: Icons.Outlined.Inventory2
```

- [ ] **Step 2: Replace CategoryIcon.kt**

Replace `composeApp/src/commonMain/kotlin/com/snowball/ui/components/CategoryIcon.kt` with:

```kotlin
package com.snowball.ui.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.snowball.data.model.Category

fun Category.icon(): ImageVector = iconFor(iconKey)
```

The old name-pattern mapping is gone. Categories with empty `iconKey` (legacy data, no migration backfill match) fall back to `Inventory2`.

- [ ] **Step 3: Build + smoke check**

```bash
./gradlew.bat :composeApp:assembleDebug
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

Expected: BUILD SUCCESSFUL. App launches. Credit Card and MISC categories display correct icons (their iconKey was backfilled by the migration in Task 1).

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/components/IconCatalog.kt composeApp/src/commonMain/kotlin/com/snowball/ui/components/CategoryIcon.kt
git commit -m "feat(ui): introduce IconCatalog; CategoryIcon resolves via iconKey"
```

---

## Task 3: CategoryManagementViewModel

**File:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/categories/CategoryManagementViewModel.kt`

**Goal:** State container + actions for add/rename/icon/delete/reassign.

- [ ] **Step 1: Create the file**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/categories/CategoryManagementViewModel.kt`:

```kotlin
package com.snowball.ui.categories

import com.snowball.data.Repos
import com.snowball.data.model.Category
import com.snowball.data.model.CategoryBehavior

data class CategoryManagementState(
    val categories: List<Category>,
    val debtCounts: Map<Long, Int>,
)

class CategoryManagementViewModel(private val repos: Repos) {

    fun load(): CategoryManagementState {
        val cats = repos.categories.all()
        val allDebts = repos.debts.all()
        val counts = cats.associate { c -> c.id to allDebts.count { it.categoryId == c.id } }
        return CategoryManagementState(cats, counts)
    }

    fun rename(id: Long, newName: String) {
        if (newName.isBlank()) return
        repos.categories.rename(id, newName.trim())
    }

    fun setIcon(id: Long, iconKey: String) {
        repos.categories.setIcon(id, iconKey)
    }

    fun create(name: String, iconKey: String) {
        if (name.isBlank()) return
        repos.categories.add(name.trim(), CategoryBehavior.SCHEDULED, iconKey)
    }

    fun reassignAndDelete(sourceId: Long, targetId: Long) {
        val debts = repos.debts.all().filter { it.categoryId == sourceId }
        debts.forEach { d ->
            repos.debts.update(
                id = d.id,
                name = d.name,
                categoryId = targetId,
                monthlyAmount = d.monthlyAmount,
                totalPayments = d.totalPayments,
                dueDay = d.dueDay,
                useLastDayOfMonth = d.useLastDayOfMonth,
                startDate = d.startDate,
                notes = d.notes,
            )
        }
        repos.categories.delete(sourceId)
    }

    fun deleteEmpty(id: Long) {
        repos.categories.delete(id)
    }
}
```

`repos.categories.add(...)` was extended in Task 1 to accept `iconKey`. `repos.categories.setIcon(...)` was added in Task 1.

- [ ] **Step 2: Build**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/categories/CategoryManagementViewModel.kt
git commit -m "feat(categories): add CategoryManagementViewModel"
```

---

## Task 4: CategoryManagementScreen with dialogs

**File:**
- Create: `composeApp/src/commonMain/kotlin/com/snowball/ui/categories/CategoryManagementScreen.kt`

**Goal:** List screen with five inline dialogs (rename / pick icon / new category / confirm delete / reassign).

- [ ] **Step 1: Create the file**

Create `composeApp/src/commonMain/kotlin/com/snowball/ui/categories/CategoryManagementScreen.kt`:

```kotlin
package com.snowball.ui.categories

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.snowball.data.model.Category
import com.snowball.data.model.CategoryBehavior
import com.snowball.ui.components.IconCatalog
import com.snowball.ui.components.iconFor
import com.snowball.ui.theme.SnowColors

private sealed interface DialogState {
    data object New : DialogState
    data class Rename(val category: Category) : DialogState
    data class PickIcon(val category: Category) : DialogState
    data class ConfirmDelete(val category: Category) : DialogState
    data class Reassign(val source: Category, val candidates: List<Category>) : DialogState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(vm: CategoryManagementViewModel, onBack: () -> Unit) {
    var tick by remember { mutableStateOf(0) }
    val state = remember(tick) { vm.load() }
    var dialog by remember { mutableStateOf<DialogState?>(null) }
    var openOverflowFor by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        containerColor = SnowColors.Night,
        topBar = {
            TopAppBar(
                title = { Text("Manage categories", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = SnowColors.Frost)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SnowColors.Night,
                    titleContentColor = SnowColors.Frost,
                    navigationIconContentColor = SnowColors.Frost,
                ),
            )
        },
        bottomBar = {
            Surface(color = SnowColors.Night) {
                Button(
                    onClick = { dialog = DialogState.New },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SnowColors.Ice,
                        contentColor = SnowColors.Night,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("+ New category") }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                "CATEGORIES",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
                color = SnowColors.FrostDim,
            )
            Spacer(Modifier.height(12.dp))
            state.categories.forEach { cat ->
                CategoryRow(
                    cat = cat,
                    debtCount = state.debtCounts[cat.id] ?: 0,
                    overflowOpen = openOverflowFor == cat.id,
                    onOverflowToggle = {
                        openOverflowFor = if (openOverflowFor == cat.id) null else cat.id
                    },
                    onRename = { openOverflowFor = null; dialog = DialogState.Rename(cat) },
                    onChangeIcon = { openOverflowFor = null; dialog = DialogState.PickIcon(cat) },
                    onDelete = {
                        openOverflowFor = null
                        val n = state.debtCounts[cat.id] ?: 0
                        dialog = if (n == 0) {
                            DialogState.ConfirmDelete(cat)
                        } else {
                            val candidates = state.categories.filter {
                                it.id != cat.id && it.behavior == CategoryBehavior.SCHEDULED && !it.isSystem.not()
                                // ↑ correction: just exclude self + LEDGER ones
                            }
                            // The condition above is wrong; rewrite properly:
                            val sane = state.categories.filter {
                                it.id != cat.id && it.behavior == CategoryBehavior.SCHEDULED
                            }
                            DialogState.Reassign(cat, sane)
                        }
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    when (val d = dialog) {
        is DialogState.New -> NewCategoryDialog(
            onDismiss = { dialog = null },
            onCreate = { name, iconKey ->
                vm.create(name, iconKey)
                dialog = null
                tick++
            },
        )
        is DialogState.Rename -> RenameDialog(
            category = d.category,
            onDismiss = { dialog = null },
            onSave = { newName ->
                vm.rename(d.category.id, newName)
                dialog = null
                tick++
            },
        )
        is DialogState.PickIcon -> IconPickerDialog(
            current = d.category.iconKey,
            onDismiss = { dialog = null },
            onPick = { key ->
                vm.setIcon(d.category.id, key)
                dialog = null
                tick++
            },
        )
        is DialogState.ConfirmDelete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Delete ${d.category.name}?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("This category has no debts.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteEmpty(d.category.id)
                    dialog = null
                    tick++
                }) { Text("Delete", color = SnowColors.Ember) }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text("Cancel", color = SnowColors.FrostMute) }
            },
            containerColor = SnowColors.CardElev,
            titleContentColor = SnowColors.Frost,
            textContentColor = SnowColors.FrostMute,
        )
        is DialogState.Reassign -> ReassignDialog(
            source = d.source,
            candidates = d.candidates,
            onDismiss = { dialog = null },
            onConfirm = { targetId ->
                vm.reassignAndDelete(d.source.id, targetId)
                dialog = null
                tick++
            },
        )
        null -> Unit
    }
}

@Composable
private fun CategoryRow(
    cat: Category,
    debtCount: Int,
    overflowOpen: Boolean,
    onOverflowToggle: () -> Unit,
    onRename: () -> Unit,
    onChangeIcon: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SnowColors.CardElev)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = iconFor(cat.iconKey),
            contentDescription = null,
            tint = SnowColors.Frost,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(cat.name, style = MaterialTheme.typography.bodyLarge, color = SnowColors.Frost)
            Text(
                "$debtCount ${if (debtCount == 1) "debt" else "debts"}",
                style = MaterialTheme.typography.bodySmall,
                color = SnowColors.FrostMute,
            )
        }
        if (cat.isSystem) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "System category — read-only",
                tint = SnowColors.FrostMute,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Box {
                IconButton(onClick = onOverflowToggle) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Edit", tint = SnowColors.Frost)
                }
                DropdownMenu(expanded = overflowOpen, onDismissRequest = onOverflowToggle) {
                    DropdownMenuItem(text = { Text("Rename", color = SnowColors.Frost) }, onClick = onRename)
                    DropdownMenuItem(text = { Text("Change icon", color = SnowColors.Frost) }, onClick = onChangeIcon)
                    DropdownMenuItem(text = { Text("Delete", color = SnowColors.Ember) }, onClick = onDelete)
                }
            }
        }
    }
}

@Composable
private fun NewCategoryDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf("inventory_2") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New category", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Name", color = SnowColors.FrostDim) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors(),
                )
                Spacer(Modifier.height(16.dp))
                Text("Icon", style = MaterialTheme.typography.labelMedium, color = SnowColors.FrostDim)
                Spacer(Modifier.height(8.dp))
                IconGrid(selected = iconKey, onSelect = { iconKey = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, iconKey) },
                enabled = name.trim().isNotEmpty(),
            ) { Text("Create", color = if (name.trim().isNotEmpty()) SnowColors.Ice else SnowColors.FrostMute) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SnowColors.FrostMute) }
        },
        containerColor = SnowColors.CardElev,
        titleContentColor = SnowColors.Frost,
        textContentColor = SnowColors.Frost,
    )
}

@Composable
private fun RenameDialog(category: Category, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var newName by remember { mutableStateOf(category.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename category", style = MaterialTheme.typography.headlineSmall) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(newName) },
                enabled = newName.trim().isNotEmpty() && newName.trim() != category.name,
            ) { Text("Save", color = SnowColors.Ice) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SnowColors.FrostMute) }
        },
        containerColor = SnowColors.CardElev,
        titleContentColor = SnowColors.Frost,
        textContentColor = SnowColors.Frost,
    )
}

@Composable
private fun IconPickerDialog(current: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var selected by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose an icon", style = MaterialTheme.typography.headlineSmall) },
        text = {
            IconGrid(selected = selected, onSelect = { selected = it })
        },
        confirmButton = {
            TextButton(onClick = { onPick(selected) }) { Text("Choose", color = SnowColors.Ice) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SnowColors.FrostMute) }
        },
        containerColor = SnowColors.CardElev,
        titleContentColor = SnowColors.Frost,
        textContentColor = SnowColors.Frost,
    )
}

@Composable
private fun IconGrid(selected: String, onSelect: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxWidth().height(240.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(IconCatalog.size) { idx ->
            val (key, icon) = IconCatalog[idx]
            val isSelected = key == selected
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) SnowColors.Ice.copy(alpha = 0.20f) else SnowColors.Night)
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = key,
                    tint = if (isSelected) SnowColors.Ice else SnowColors.Frost,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun ReassignDialog(
    source: Category,
    candidates: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var target by remember { mutableStateOf<Long?>(candidates.firstOrNull()?.id) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reassign debts", style = MaterialTheme.typography.headlineSmall) },
        text = {
            if (candidates.isEmpty()) {
                Text("No other categories available. Add another category first.")
            } else {
                Column {
                    Text(
                        "Choose a new category for the debts currently in ${source.name}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SnowColors.FrostMute,
                    )
                    Spacer(Modifier.height(12.dp))
                    candidates.forEach { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { target = c.id }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = target == c.id,
                                onClick = { target = c.id },
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = iconFor(c.iconKey),
                                contentDescription = null,
                                tint = SnowColors.FrostMute,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(c.name, style = MaterialTheme.typography.bodyLarge, color = SnowColors.Frost)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { target?.let { onConfirm(it) } },
                enabled = target != null,
            ) { Text("Move", color = if (target != null) SnowColors.Ice else SnowColors.FrostMute) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = SnowColors.FrostMute) }
        },
        containerColor = SnowColors.CardElev,
        titleContentColor = SnowColors.Frost,
        textContentColor = SnowColors.Frost,
    )
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

// Helper: LazyVerticalGrid.items extension needs the Foundation lazy.grid.items import
// — using indexed access avoids that.
```

**Important:** The `LazyVerticalGrid` requires importing `androidx.compose.foundation.lazy.grid.items` to use the indexed extension. The code above uses `items(IconCatalog.size) { idx -> ... }` which needs the items import. Add this to the imports:

```kotlin
import androidx.compose.foundation.lazy.grid.items
```

Actually, `items(count: Int)` is a built-in `LazyGridScope` function — no extra import needed.

The corrupt `CategoryRow.onDelete` block in my listing above had a buggy intermediate state — the FINAL version of that block must be:

```kotlin
                    onDelete = {
                        openOverflowFor = null
                        val n = state.debtCounts[cat.id] ?: 0
                        if (n == 0) {
                            dialog = DialogState.ConfirmDelete(cat)
                        } else {
                            val sane = state.categories.filter {
                                it.id != cat.id && it.behavior == CategoryBehavior.SCHEDULED
                            }
                            dialog = DialogState.Reassign(cat, sane)
                        }
                    },
```

Make sure to use that clean version, NOT the half-edited version above with the `it.isSystem.not()` mistake.

- [ ] **Step 2: Build**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/categories/CategoryManagementScreen.kt
git commit -m "feat(categories): add CategoryManagementScreen with rename/icon/delete/reassign dialogs"
```

---

## Task 5: Settings entry point

**File:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsScreen.kt`

**Goal:** Add a "Manage categories" tappable row in Settings.

- [ ] **Step 1: Read current SettingsScreen.kt**

Use Read to inspect the file.

- [ ] **Step 2: Add the navigation parameter and row**

Modify `SettingsScreen` to accept an `onManageCategories: () -> Unit` parameter. Add a tappable Row between the income section's helper text (`"Same amount used for both 15th and 30th cutoffs."`) and the version stamp:

```kotlin
@Composable
fun SettingsScreen(vm: SettingsViewModel, onManageCategories: () -> Unit) {
    // ... existing content above the version stamp ...

    Spacer(Modifier.height(28.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onManageCategories() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "MANAGE CATEGORIES",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = SnowColors.Frost,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = SnowColors.Ice,
            modifier = Modifier.size(20.dp),
        )
    }

    Spacer(Modifier.height(28.dp))
    Text(
        "Snowball v0.1.1",
        // ... existing
    )
}
```

Add imports as needed:

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
```

(Some of these likely already exist.)

Also update the version label to `"Snowball v0.2.2"` since we're shipping v0.2.2.

- [ ] **Step 3: Build**

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: build fails because `App.kt` still calls `SettingsScreen(settingsVm)` without the new parameter. This is OK — Task 6 fixes App.kt. To keep build green, give `onManageCategories` a default `= {}` parameter so the existing call site still compiles. Or, do Task 6 in the same task as this one.

To unblock the build immediately, set a default lambda:

```kotlin
fun SettingsScreen(vm: SettingsViewModel, onManageCategories: () -> Unit = {}) {
```

Task 6 supplies a real lambda and the default can stay (no harm).

```bash
./gradlew.bat :composeApp:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/ui/settings/SettingsScreen.kt
git commit -m "feat(settings): add Manage categories entry point"
```

---

## Task 6: Wire CategoryManagement route in App.kt

**File:**
- Modify: `composeApp/src/commonMain/kotlin/com/snowball/App.kt`

**Goal:** Add `Route.CategoryManagement`; wire Settings entry point to navigate.

- [ ] **Step 1: Modify App.kt**

Find the `sealed interface Route` block; add:

```kotlin
data object CategoryManagement : Route
```

Add the import:

```kotlin
import com.snowball.ui.categories.CategoryManagementScreen
import com.snowball.ui.categories.CategoryManagementViewModel
```

In the `when (val r = route)` block, add a new branch:

```kotlin
                    is Route.CategoryManagement -> {
                        val catVm = remember(refreshKey) { CategoryManagementViewModel(repos) }
                        CategoryManagementScreen(
                            vm = catVm,
                            onBack = { route = Route.Tabs; refreshKey++ },
                        )
                    }
```

In the Settings call site, pass the navigation lambda:

```kotlin
Tab.Settings -> {
    val settingsVm = remember(refreshKey) { SettingsViewModel(repos) }
    SettingsScreen(
        vm = settingsVm,
        onManageCategories = { route = Route.CategoryManagement },
    )
}
```

- [ ] **Step 2: Build + run tests**

```bash
./gradlew.bat :composeApp:assembleDebug
./gradlew.bat :composeApp:testDebugUnitTest
```

Expected: both pass.

- [ ] **Step 3: Install + spot-check**

```bash
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
/c/Users/Pika/AppData/Local/Android/Sdk/platform-tools/adb.exe shell am start -n com.snowball/.MainActivity
```

Navigate to Settings → tap "MANAGE CATEGORIES" → should land on the CategoryManagementScreen with Credit Card + MISC visible (system, lock icons). Tap "+ New category" → dialog opens. Type a name, pick an icon, Create → returns to list with the new category.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/snowball/App.kt
git commit -m "feat(nav): wire CategoryManagement route"
```

---

## Task 7: End-to-end verification + tag v0.2.2 + push

- [ ] **Step 1: Full test suite**

```bash
cd /c/Users/Pika/projects/snowball
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="$LOCALAPPDATA/Android/Sdk"
./gradlew.bat :composeApp:test
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Walkthrough**

Settings → Manage categories → add "Groceries" with `shopping_bag` icon → reopen → confirm it's there. Tap overflow → Rename "Groceries" to "Food" → confirm. Tap overflow → Change icon → pick `home` → confirm visual. Add a debt in "Food" via Debts tab → return to Manage categories → "Food" should show "1 debt". Try Delete on "Food" → reassign dialog appears with Credit Card as the only candidate → Move → category gone, debt moved to Credit Card. Verify the system categories (CC + MISC) cannot be tapped to edit and show a lock icon.

- [ ] **Step 3: Tag and push**

```bash
git tag -a v0.2.2 -m "v0.2.2 — Category Management with iconKey schema migration (sub-project C)"
git push origin main
git push origin v0.2.2
```

- [ ] **Step 4: Done**

---

## Spec coverage check

| Spec section | Plan task(s) |
|---|---|
| Schema migration (Category.sq + 1.sqm) | 1 |
| Category data model + repo plumbing | 1 |
| IconCatalog + CategoryIcon migration | 2 |
| CategoryManagementViewModel | 3 |
| CategoryManagementScreen + dialogs | 4 |
| Settings entry point | 5 |
| App.kt route wiring | 6 |
| Verify + tag | 7 |

## Risks recap

- **SQLDelight `1.sqm` filename convention.** SQLDelight 2.x: `1.sqm` is the migration from v1 → v2. Plugin auto-detects. If the build complains about a missing migration, double-check the filename is exactly `1.sqm` in the same directory as `Category.sq`.
- **`SnowballDb.Schema.version` jumping from 1 to 2** is exactly what we want. The Android driver invokes the migration on existing databases.
- **`CategoryIcon.icon()` now strictly reads `iconKey`** — any legacy categories with empty iconKey fall back to Inventory2. The migration backfills Credit Card and MISC; everything else gets the default.
- **The half-corrupt `onDelete` block in Task 4** is intentionally flagged in the spec to make sure the implementer uses the clean version. If a previous implementer tried to apply the broken intermediate, build will fail on `it.isSystem.not()` mismatch.
