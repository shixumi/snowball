package com.snowball.data.backup

import com.snowball.db.SnowballDb

/** Outcome of an import attempt. */
sealed interface ImportResult {
    data class Success(val categories: Int, val debts: Int, val payments: Int) : ImportResult
    data class Failure(val message: String) : ImportResult
}

/**
 * Reads/writes the whole database as a portable JSON backup.
 *
 * Import is **replace**: it wipes every table and restores the snapshot exactly,
 * preserving row IDs so foreign keys stay valid. The wipe + restore runs in a
 * single transaction, so a failure mid-import rolls back and leaves the existing
 * data untouched. See docs/superpowers/specs/2026-06-19-data-export-import-design.md
 */
class BackupService(private val db: SnowballDb) {

    fun export(exportedAt: Long): String {
        val categories = db.categoryQueries.selectAll().executeAsList().map { row ->
            CategoryDto(
                id = row.id,
                name = row.name,
                isSystem = row.isSystem == 1L,
                behavior = row.behavior,
                iconKey = row.iconKey,
                createdAt = row.createdAt,
            )
        }
        val debts = db.debtQueries.selectAll().executeAsList().map { row ->
            DebtDto(
                id = row.id,
                name = row.name,
                categoryId = row.categoryId,
                monthlyAmount = row.monthlyAmount,
                totalPayments = row.totalPayments,
                dueDay = row.dueDay,
                useLastDayOfMonth = row.useLastDayOfMonth == 1L,
                startDate = row.startDate,
                firstPaymentDate = row.firstPaymentDate,
                isArchived = row.isArchived == 1L,
                notes = row.notes,
                createdAt = row.createdAt,
            )
        }
        val payments = db.paymentQueries.selectAll().executeAsList().map { row ->
            PaymentDto(
                id = row.id,
                debtId = row.debtId,
                paidDate = row.paidDate,
                amount = row.amount,
                createdAt = row.createdAt,
            )
        }
        val s = db.settingsQueries.select().executeAsOne()
        val settings = SettingsDto(
            incomePerCutoff = s.incomePerCutoff,
            currency = s.currency,
            notificationsEnabled = s.notificationsEnabled == 1L,
            notificationHour = s.notificationHour,
            notificationMinute = s.notificationMinute,
            firstLaunchSeen = s.firstLaunchSeen == 1L,
            swipeCoachmarkSeen = s.swipeCoachmarkSeen == 1L,
            paidAheadKey = s.paidAheadKey,
        )

        return BackupCodec.encode(
            BackupFile(
                formatVersion = BackupCodec.CURRENT_FORMAT_VERSION,
                dbVersion = SnowballDb.Schema.version,
                exportedAt = exportedAt,
                categories = categories,
                debts = debts,
                payments = payments,
                settings = settings,
            )
        )
    }

    fun import(json: String): ImportResult {
        val backup = try {
            BackupCodec.decode(json)
        } catch (e: BackupFormatException) {
            return ImportResult.Failure(e.message ?: "Couldn't read that backup.")
        }
        if (backup.dbVersion != SnowballDb.Schema.version) {
            return ImportResult.Failure("This backup was made by an incompatible version of Snowball.")
        }
        return try {
            db.transaction {
                // Clear children before parents so foreign keys hold either way.
                db.paymentQueries.deleteAll()
                db.debtQueries.deleteAll()
                db.categoryQueries.deleteAll()

                // Restore parents before children, preserving original IDs.
                backup.categories.forEach { c ->
                    db.categoryQueries.insertWithId(
                        id = c.id,
                        name = c.name,
                        isSystem = if (c.isSystem) 1L else 0L,
                        behavior = c.behavior,
                        iconKey = c.iconKey,
                        createdAt = c.createdAt,
                    )
                }
                backup.debts.forEach { d ->
                    db.debtQueries.insertWithId(
                        id = d.id,
                        name = d.name,
                        categoryId = d.categoryId,
                        monthlyAmount = d.monthlyAmount,
                        totalPayments = d.totalPayments,
                        dueDay = d.dueDay,
                        useLastDayOfMonth = if (d.useLastDayOfMonth) 1L else 0L,
                        startDate = d.startDate,
                        firstPaymentDate = d.firstPaymentDate,
                        isArchived = if (d.isArchived) 1L else 0L,
                        notes = d.notes,
                        createdAt = d.createdAt,
                    )
                }
                backup.payments.forEach { p ->
                    db.paymentQueries.insertWithId(
                        id = p.id,
                        debtId = p.debtId,
                        paidDate = p.paidDate,
                        amount = p.amount,
                        createdAt = p.createdAt,
                    )
                }

                // Settings is a fixed single row (id = 1); ensure it exists, then overwrite.
                db.settingsQueries.insertIfMissing()
                backup.settings.let { st ->
                    db.settingsQueries.replaceAll(
                        incomePerCutoff = st.incomePerCutoff,
                        currency = st.currency,
                        notificationsEnabled = if (st.notificationsEnabled) 1L else 0L,
                        notificationHour = st.notificationHour,
                        notificationMinute = st.notificationMinute,
                        firstLaunchSeen = if (st.firstLaunchSeen) 1L else 0L,
                        swipeCoachmarkSeen = if (st.swipeCoachmarkSeen) 1L else 0L,
                        paidAheadKey = st.paidAheadKey,
                    )
                }
            }
            ImportResult.Success(backup.categories.size, backup.debts.size, backup.payments.size)
        } catch (e: Exception) {
            ImportResult.Failure("Import failed and your data was left unchanged. (${e.message})")
        }
    }
}
