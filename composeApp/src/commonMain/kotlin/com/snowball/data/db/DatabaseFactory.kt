package com.snowball.data.db

import app.cash.sqldelight.db.SqlDriver
import com.snowball.db.SnowballDb

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(factory: DatabaseDriverFactory): SnowballDb {
    val driver = factory.createDriver()
    val db = SnowballDb(driver)
    seedSystemCategories(db)
    db.settingsQueries.insertIfMissing()
    return db
}

private fun seedSystemCategories(db: SnowballDb) {
    val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    db.categoryQueries.insertOrIgnore(
        name = "Credit Card",
        isSystem = 1,
        behavior = "SCHEDULED",
        createdAt = now
    )
    db.categoryQueries.insertOrIgnore(
        name = "MISC",
        isSystem = 1,
        behavior = "LEDGER",
        createdAt = now
    )
}
