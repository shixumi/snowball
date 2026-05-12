package com.snowball.repo

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.snowball.data.repo.DebtRepository
import com.snowball.data.repo.PaymentRepository
import com.snowball.db.SnowballDb
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaymentRepositoryTest {
    private fun fresh(): Triple<PaymentRepository, DebtRepository, Long> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SnowballDb.Schema.create(driver)
        val db = SnowballDb(driver)
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        db.categoryQueries.insertOrIgnore("Credit Card", 1, "SCHEDULED", now)
        val catId = db.categoryQueries.selectAll().executeAsList().first().id
        val debtRepo = DebtRepository(db)
        val debtId = debtRepo.add("d1", catId, 1000.0, 12, 15, false, LocalDate(2026, 1, 15), null)
        return Triple(PaymentRepository(db), debtRepo, debtId)
    }

    @Test
    fun marking_paid_creates_row() {
        val (pay, _, debtId) = fresh()
        pay.markPaid(debtId, LocalDate(2026, 1, 15), 1000.0)
        assertEquals(1, pay.countForDebt(debtId))
    }

    @Test
    fun multiple_payments_count() {
        val (pay, _, debtId) = fresh()
        pay.markPaid(debtId, LocalDate(2026, 1, 15), 1000.0)
        pay.markPaid(debtId, LocalDate(2026, 2, 15), 1000.0)
        assertEquals(2, pay.countForDebt(debtId))
    }

    @Test
    fun delete_payment_undoes_mark() {
        val (pay, _, debtId) = fresh()
        pay.markPaid(debtId, LocalDate(2026, 1, 15), 1000.0)
        val rows = pay.historyForDebt(debtId)
        pay.delete(rows.first().id)
        assertEquals(0, pay.countForDebt(debtId))
    }

    @Test
    fun history_sorted_recent_first() {
        val (pay, _, debtId) = fresh()
        pay.markPaid(debtId, LocalDate(2026, 1, 15), 1000.0)
        pay.markPaid(debtId, LocalDate(2026, 3, 15), 1000.0)
        pay.markPaid(debtId, LocalDate(2026, 2, 15), 1000.0)
        val history = pay.historyForDebt(debtId)
        assertTrue(history[0].paidDate > history[1].paidDate)
        assertTrue(history[1].paidDate > history[2].paidDate)
    }
}
