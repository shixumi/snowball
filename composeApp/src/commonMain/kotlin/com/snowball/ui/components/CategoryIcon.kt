package com.snowball.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector
import com.snowball.data.model.Category

/**
 * Resolve a Material icon for a category. System categories ("Credit Card", "MISC")
 * have specific icons; user-managed categories fall back to a generic receipt icon
 * until v0.2 introduces a per-category icon picker stored in the DB.
 *
 * A few common Filipino-context category names get sensible icons by string match
 * so the seeded vendor-style categories users typically add ("Sloan", "Billease",
 * "CC Payments", etc.) look intentional without requiring schema changes.
 */
fun Category.icon(): ImageVector = when (name) {
    "Credit Card" -> Icons.Outlined.CreditCard
    "MISC" -> Icons.Outlined.MoreHoriz
    "Monthly Payments" -> Icons.Outlined.Receipt
    "SLOAN", "Sloan" -> Icons.Outlined.AccountBalance
    "BILLEASE", "Billease" -> Icons.Outlined.ShoppingBag
    "CC Payments", "Credit Card Installments" -> Icons.Outlined.PhoneIphone
    else -> Icons.Outlined.Inventory2
}
