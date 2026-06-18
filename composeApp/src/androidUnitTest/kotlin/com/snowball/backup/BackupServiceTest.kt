package com.snowball.backup

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.snowball.data.backup.BackupCodec
import com.snowball.data.backup.BackupFile
import com.snowball.data.backup.BackupService
import com.snowball.data.backup.CategoryDto
import com.snowball.data.backup.DebtDto
import com.snowball.data.backup.ImportResult
import com.snowball.data.backup.PaymentDto
import com.snowball.data.backup.SettingsDto
import com.snowball.db.SnowballDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupServiceTest {

    private fun freshDb(): SnowballDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SnowballDb.Schema.create(driver)
        val db = SnowballDb(driver)
        db.settingsQueries.insertIfMissing()
        return db
    }

    /** Seeds two categories, a debt, and two payments; tweaks settings. */
    private fun seed(db: SnowballDb) {
        db.categoryQueries.insertOrIgnore("Credit Card", 1, "SCHEDULED", "credit_card", 0)
        db.categoryQueries.insertOrIgnore("MISC", 1, "LEDGER", "more_horiz", 0)
        db.categoryQueries.insert("Gym", 0, "LEDGER", "fitness", 10)
        val catId = db.categoryQueries.selectAll().executeAsList().first { it.name == "Credit Card" }.id
        db.debtQueries.insert(
            "Laptop", catId, 2500.0, 12, 15, 0,
            "2026-01-15", "2026-02-15", "0% plan", 100,
        )
        val debtId = db.debtQueries.selectAll().executeAsList().first().id
        db.paymentQueries.insert(debtId, "2026-02-15", 2500.0, 200)
        db.paymentQueries.insert(debtId, "2026-03-15", 2500.0, 300)
        db.settingsQueries.setIncome(15000.0)
        db.settingsQueries.setPaidAhead("15:2026-06")
    }

    @Test
    fun export_then_import_into_fresh_db_reproduces_everything() {
        val source = freshDb()
        seed(source)
        val json = BackupService(source).export(exportedAt = 1_750_000_000_000)

        val target = freshDb()
        val result = BackupService(target).import(json)
        assertTrue(result is ImportResult.Success, "expected success, got $result")

        assertEquals(
            source.categoryQueries.selectAll().executeAsList(),
            target.categoryQueries.selectAll().executeAsList(),
        )
        assertEquals(
            source.debtQueries.selectAll().executeAsList(),
            target.debtQueries.selectAll().executeAsList(),
        )
        assertEquals(
            source.paymentQueries.selectAll().executeAsList().sortedBy { it.id },
            target.paymentQueries.selectAll().executeAsList().sortedBy { it.id },
        )
        assertEquals(
            source.settingsQueries.select().executeAsOne(),
            target.settingsQueries.select().executeAsOne(),
        )
    }

    @Test
    fun import_replaces_existing_data() {
        val source = freshDb()
        seed(source)
        val json = BackupService(source).export(exportedAt = 1)

        // Target already has DIFFERENT data; import must replace it wholesale.
        val target = freshDb()
        target.categoryQueries.insert("Old Cat", 0, "LEDGER", "x", 1)
        val oldCat = target.categoryQueries.selectAll().executeAsList().first().id
        target.debtQueries.insert("Old Debt", oldCat, 99.0, 3, 5, 0, "2025-01-01", "2025-02-01", null, 1)

        val result = BackupService(target).import(json)
        assertTrue(result is ImportResult.Success)
        result as ImportResult.Success
        assertEquals(3, result.categories)
        assertEquals(1, result.debts)
        assertEquals(2, result.payments)
        // No trace of the old data.
        assertTrue(target.debtQueries.selectAll().executeAsList().none { it.name == "Old Debt" })
        assertTrue(target.categoryQueries.selectAll().executeAsList().none { it.name == "Old Cat" })
    }

    @Test
    fun import_of_garbage_fails_and_leaves_data_untouched() {
        val target = freshDb()
        seed(target)
        val before = target.debtQueries.selectAll().executeAsList()

        val result = BackupService(target).import("this is not a backup")
        assertTrue(result is ImportResult.Failure)
        assertEquals(before, target.debtQueries.selectAll().executeAsList())
    }

    /** Orphaned payments (referencing a debt not in the file — e.g. a debt deleted
     *  before delete-cascade existed) are dropped, and the import still succeeds. */
    @Test
    fun import_drops_orphaned_payments_and_succeeds() {
        val target = freshDb()
        val withOrphan = BackupCodec.encode(
            BackupFile(
                formatVersion = BackupCodec.CURRENT_FORMAT_VERSION,
                dbVersion = SnowballDb.Schema.version,
                exportedAt = 1,
                categories = listOf(CategoryDto(1, "Credit Card", true, "SCHEDULED", "credit_card", 0)),
                debts = listOf(DebtDto(5, "Phone", 1, 999.0, 6, 10, false, "2026-01-10", "2026-02-10", false, null, 1)),
                payments = listOf(
                    PaymentDto(80, 5, "2026-02-10", 999.0, 2),    // valid → kept
                    PaymentDto(81, 999, "2026-02-10", 999.0, 3),  // orphan → dropped
                ),
                settings = SettingsDto(0.0, "PHP", true, 9, 0, true, false, ""),
            )
        )
        val result = BackupService(target).import(withOrphan)
        assertTrue(result is ImportResult.Success, "expected success, got $result")
        result as ImportResult.Success
        assertEquals(1, result.payments)

        val payments = target.paymentQueries.selectAll().executeAsList()
        assertEquals(1, payments.size)
        assertEquals(5L, payments.first().debtId)
    }

    /** A debt referencing a missing category is malformed → reject, data untouched. */
    @Test
    fun import_of_debt_with_missing_category_fails_and_leaves_data_untouched() {
        val target = freshDb()
        seed(target)
        val before = target.debtQueries.selectAll().executeAsList()

        val badCategory = BackupCodec.encode(
            BackupFile(
                formatVersion = BackupCodec.CURRENT_FORMAT_VERSION,
                dbVersion = SnowballDb.Schema.version,
                exportedAt = 1,
                categories = listOf(CategoryDto(1, "Credit Card", true, "SCHEDULED", "credit_card", 0)),
                debts = listOf(DebtDto(5, "Phone", 999, 999.0, 6, 10, false, "2026-01-10", "2026-02-10", false, null, 1)),
                payments = emptyList(),
                settings = SettingsDto(0.0, "PHP", true, 9, 0, true, false, ""),
            )
        )
        val result = BackupService(target).import(badCategory)
        assertTrue(result is ImportResult.Failure, "expected failure, got $result")
        assertEquals(before, target.debtQueries.selectAll().executeAsList())
    }

    @Test
    fun import_rejects_dbVersion_mismatch_and_keeps_data() {
        val target = freshDb()
        seed(target)
        val before = target.debtQueries.selectAll().executeAsList()

        val wrongVersion = BackupCodec.encode(
            BackupFile(
                formatVersion = BackupCodec.CURRENT_FORMAT_VERSION,
                dbVersion = SnowballDb.Schema.version + 1,
                exportedAt = 1,
                categories = emptyList(),
                debts = emptyList(),
                payments = emptyList(),
                settings = SettingsDto(0.0, "PHP", true, 9, 0, true, false, ""),
            )
        )
        val result = BackupService(target).import(wrongVersion)
        assertTrue(result is ImportResult.Failure)
        assertEquals(before, target.debtQueries.selectAll().executeAsList())
    }

    @Test
    fun import_of_empty_backup_clears_everything() {
        val target = freshDb()
        seed(target)
        val empty = BackupCodec.encode(
            BackupFile(
                formatVersion = BackupCodec.CURRENT_FORMAT_VERSION,
                dbVersion = SnowballDb.Schema.version,
                exportedAt = 1,
                categories = emptyList(),
                debts = emptyList(),
                payments = emptyList(),
                settings = SettingsDto(0.0, "PHP", true, 9, 0, true, false, ""),
            )
        )
        val result = BackupService(target).import(empty)
        assertTrue(result is ImportResult.Success)
        assertTrue(target.categoryQueries.selectAll().executeAsList().isEmpty())
        assertTrue(target.debtQueries.selectAll().executeAsList().isEmpty())
        assertTrue(target.paymentQueries.selectAll().executeAsList().isEmpty())
    }
}
