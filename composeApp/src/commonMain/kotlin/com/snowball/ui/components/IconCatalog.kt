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
 * `inventory_2` is the default fallback when an iconKey is empty or unrecognized.
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
