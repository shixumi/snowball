package com.snowball.repo

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.snowball.data.repo.SettingsRepository
import com.snowball.db.SnowballDb
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryTest {
    private fun freshRepo(): SettingsRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SnowballDb.Schema.create(driver)
        val db = SnowballDb(driver)
        db.settingsQueries.insertIfMissing()
        return SettingsRepository(db)
    }

    @Test
    fun reads_default_settings() {
        val repo = freshRepo()
        val s = repo.get()
        assertEquals(0.0, s.incomePerCutoff)
        assertEquals("PHP", s.currency)
    }

    @Test
    fun setIncome_persists() {
        val repo = freshRepo()
        repo.setIncome(25000.0)
        assertEquals(25000.0, repo.get().incomePerCutoff)
    }
}
