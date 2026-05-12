package com.snowball.data.repo

import com.snowball.data.model.Payment
import com.snowball.db.SnowballDb
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

class PaymentRepository(private val db: SnowballDb) {

    fun markPaid(debtId: Long, paidDate: LocalDate, amount: Double) {
        db.paymentQueries.insert(
            debtId = debtId,
            paidDate = paidDate.toString(),
            amount = amount,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
    }

    fun delete(paymentId: Long) {
        db.paymentQueries.deleteById(paymentId)
    }

    fun countForDebt(debtId: Long): Int =
        db.paymentQueries.countForDebt(debtId).executeAsOne().toInt()

    fun historyForDebt(debtId: Long): List<Payment> =
        db.paymentQueries.selectAllForDebt(debtId).executeAsList().map { row ->
            Payment(
                id = row.id,
                debtId = row.debtId,
                paidDate = LocalDate.parse(row.paidDate),
                amount = row.amount,
            )
        }

    fun latestForDebt(debtId: Long): Payment? =
        db.paymentQueries.selectLatestForDebt(debtId).executeAsOneOrNull()?.let { row ->
            Payment(row.id, row.debtId, LocalDate.parse(row.paidDate), row.amount)
        }

    fun inDateRangeForDebt(debtId: Long, from: LocalDate, to: LocalDate): List<Payment> =
        db.paymentQueries
            .selectInDateRange(debtId = debtId, from = from.toString(), to = to.toString())
            .executeAsList()
            .map { row ->
                Payment(row.id, row.debtId, LocalDate.parse(row.paidDate), row.amount)
            }
}
