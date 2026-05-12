package com.snowball.data.repo

import com.snowball.data.model.Debt
import com.snowball.db.SnowballDb
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

class DebtRepository(private val db: SnowballDb) {

    fun add(
        name: String,
        categoryId: Long,
        monthlyAmount: Double,
        totalPayments: Int,
        dueDay: Int,
        useLastDayOfMonth: Boolean,
        startDate: LocalDate,
        firstPaymentDate: LocalDate,
        notes: String?,
    ): Long {
        db.debtQueries.insert(
            name = name,
            categoryId = categoryId,
            monthlyAmount = monthlyAmount,
            totalPayments = totalPayments.toLong(),
            dueDay = dueDay.toLong(),
            useLastDayOfMonth = if (useLastDayOfMonth) 1L else 0L,
            startDate = startDate.toString(),
            firstPaymentDate = firstPaymentDate.toString(),
            notes = notes,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
        return db.debtQueries.selectAll().executeAsList().first().id
    }

    fun update(
        id: Long,
        name: String,
        categoryId: Long,
        monthlyAmount: Double,
        totalPayments: Int,
        dueDay: Int,
        useLastDayOfMonth: Boolean,
        startDate: LocalDate,
        firstPaymentDate: LocalDate,
        notes: String?,
    ) {
        db.debtQueries.update(
            name = name,
            categoryId = categoryId,
            monthlyAmount = monthlyAmount,
            totalPayments = totalPayments.toLong(),
            dueDay = dueDay.toLong(),
            useLastDayOfMonth = if (useLastDayOfMonth) 1L else 0L,
            startDate = startDate.toString(),
            firstPaymentDate = firstPaymentDate.toString(),
            notes = notes,
            id = id,
        )
    }

    fun setArchived(id: Long, archived: Boolean) {
        db.debtQueries.setArchived(if (archived) 1L else 0L, id)
    }

    fun delete(id: Long) {
        db.debtQueries.deleteById(id)
    }

    fun byId(id: Long): Debt? =
        db.debtQueries.selectById(id).executeAsOneOrNull()?.toModel()

    fun allActive(): List<Debt> =
        db.debtQueries.selectActive().executeAsList().map { it.toModel() }

    fun all(): List<Debt> =
        db.debtQueries.selectAll().executeAsList().map { it.toModel() }

    private fun com.snowball.db.Debt.toModel(): Debt = Debt(
        id = id,
        name = name,
        categoryId = categoryId,
        monthlyAmount = monthlyAmount,
        totalPayments = totalPayments.toInt(),
        dueDay = dueDay.toInt(),
        useLastDayOfMonth = useLastDayOfMonth == 1L,
        startDate = LocalDate.parse(startDate),
        firstPaymentDate = if (firstPaymentDate.isBlank()) LocalDate.parse(startDate) else LocalDate.parse(firstPaymentDate),
        isArchived = isArchived == 1L,
        notes = notes,
    )
}
