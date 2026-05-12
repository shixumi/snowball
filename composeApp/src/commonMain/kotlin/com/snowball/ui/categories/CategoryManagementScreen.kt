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
                        if (n == 0) {
                            dialog = DialogState.ConfirmDelete(cat)
                        } else {
                            val candidates = state.categories.filter {
                                it.id != cat.id && it.behavior == CategoryBehavior.SCHEDULED
                            }
                            dialog = DialogState.Reassign(cat, candidates)
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
            ) {
                Text(
                    "Create",
                    color = if (name.trim().isNotEmpty()) SnowColors.Ice else SnowColors.FrostMute,
                )
            }
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
    var selected by remember { mutableStateOf(current.ifEmpty { "inventory_2" }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose an icon", style = MaterialTheme.typography.headlineSmall) },
        text = { IconGrid(selected = selected, onSelect = { selected = it }) },
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
                            RadioButton(selected = target == c.id, onClick = { target = c.id })
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
            ) {
                Text(
                    "Move",
                    color = if (target != null) SnowColors.Ice else SnowColors.FrostMute,
                )
            }
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
