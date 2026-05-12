package com.snowball.repo

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.snowball.data.model.CategoryBehavior
import com.snowball.data.repo.CategoryRepository
import com.snowball.db.SnowballDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryRepositoryTest {
    private fun freshRepo(): CategoryRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SnowballDb.Schema.create(driver)
        val db = SnowballDb(driver)
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        db.categoryQueries.insertOrIgnore("Credit Card", 1, "SCHEDULED", now)
        db.categoryQueries.insertOrIgnore("MISC", 1, "LEDGER", now)
        return CategoryRepository(db)
    }

    @Test
    fun system_categories_seeded() {
        val all = freshRepo().all()
        assertEquals(2, all.size)
        assertTrue(all.any { it.name == "Credit Card" && it.isSystem })
        assertTrue(all.any { it.name == "MISC" && it.behavior == CategoryBehavior.LEDGER })
    }

    @Test
    fun add_user_category() {
        val repo = freshRepo()
        repo.add("Sloan", CategoryBehavior.SCHEDULED)
        val sloan = repo.all().find { it.name == "Sloan" }!!
        assertEquals(false, sloan.isSystem)
        assertEquals(CategoryBehavior.SCHEDULED, sloan.behavior)
    }

    @Test
    fun cannot_delete_system_category() {
        val repo = freshRepo()
        val misc = repo.all().first { it.name == "MISC" }
        repo.delete(misc.id)  // no-op for system per SQL guard
        assertTrue(repo.all().any { it.name == "MISC" })
    }
}
