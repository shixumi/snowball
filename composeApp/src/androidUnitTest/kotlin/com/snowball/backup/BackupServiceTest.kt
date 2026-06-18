package com.snowball.backup

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.snowball.data.backup.BackupService
import com.snowball.data.backup.ImportResult
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
}
