package com.snowball.repo

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.snowball.data.repo.DebtRepository
import com.snowball.db.SnowballDb
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DebtRepositoryTest {
    private fun freshDb(): SnowballDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SnowballDb.Schema.create(driver)
        val db = SnowballDb(driver)
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        db.categoryQueries.insertOrIgnore("Credit Card", 1, "SCHEDULED", "credit_card", now)
        return db
    }

    private fun catId(db: SnowballDb): Long = db.categoryQueries.selectAll().executeAsList().first().id

    @Test
    fun add_and_read_debt() {
        val db = freshDb()
        val repo = DebtRepository(db)
        val id = repo.add(
            name = "Sloan 16,500",
            categoryId = catId(db),
            monthlyAmount = 3566.74,
            totalPayments = 6,
            dueDay = 17,
            useLastDayOfMonth = false,
            startDate = LocalDate(2026, 1, 17),
            notes = null,
        )
        val read = repo.byId(id)
        assertNotNull(read)
        assertEquals("Sloan 16,500", read.name)
        assertEquals(17, read.dueDay)
    }

    @Test
    fun selectActive_excludes_archived() {
        val db = freshDb()
        val repo = DebtRepository(db)
        val id = repo.add("a", catId(db), 100.0, 1, 1, false, LocalDate(2026, 1, 1), null)
        repo.setArchived(id, true)
        assertEquals(0, repo.allActive().size)
    }

    @Test
    fun update_persists_fields() {
        val db = freshDb()
        val repo = DebtRepository(db)
        val id = repo.add("a", catId(db), 100.0, 1, 1, false, LocalDate(2026, 1, 1), null)
        repo.update(
            id = id,
            name = "renamed",
            categoryId = catId(db),
            monthlyAmount = 200.0,
            totalPayments = 2,
            dueDay = 5,
            useLastDayOfMonth = true,
            startDate = LocalDate(2026, 2, 1),
            notes = "note",
        )
        val updated = repo.byId(id)!!
        assertEquals("renamed", updated.name)
        assertEquals(200.0, updated.monthlyAmount)
        assertEquals(true, updated.useLastDayOfMonth)
    }
}
