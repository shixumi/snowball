package com.snowball.domain

import com.snowball.data.model.Debt
import com.snowball.data.model.Payment
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InsightsCalculatorTest {

    private fun cf(year: Int, month: Int, payday: Payday, due: Double, allClear: Boolean = false) =
        CutoffForecast(Cutoff(year, month, payday), dueTotal = due, leftOver = 0.0, isAllClear = allClear)

    @Test
    fun aggregate_merges_two_paydays_of_a_month() {
        val rows = InsightsCalculator.aggregateByMonth(
            listOf(
                cf(2026, 6, Payday.FIFTEENTH, 3600.0),
                cf(2026, 6, Payday.THIRTIETH, 8200.0),
            ),
            incomePerCutoff = 10000.0,
        )
        assertEquals(1, rows.size)
        assertEquals(2026, rows[0].year); assertEquals(6, rows[0].month)
        assertEquals(11800.0, rows[0].dueTotal)
        assertEquals(20000.0 - 11800.0, rows[0].leftOver) // income*2 - due
    }

    @Test
    fun aggregate_partial_month_scales_income_by_payday_count() {
        val rows = InsightsCalculator.aggregateByMonth(
            listOf(cf(2026, 6, Payday.THIRTIETH, 8200.0)),
            incomePerCutoff = 10000.0,
        )
        assertEquals(1, rows.size)
        assertEquals(10000.0 - 8200.0, rows[0].leftOver) // income*1 - due
    }

    @Test
    fun aggregate_all_clear_only_when_every_payday_clear() {
        val mixed = InsightsCalculator.aggregateByMonth(
            listOf(
                cf(2026, 6, Payday.FIFTEENTH, 0.0, allClear = true),
                cf(2026, 6, Payday.THIRTIETH, 8200.0, allClear = false),
            ),
            incomePerCutoff = 10000.0,
        )
        assertEquals(false, mixed[0].isAllClear)

        val clear = InsightsCalculator.aggregateByMonth(
            listOf(
                cf(2026, 6, Payday.FIFTEENTH, 0.0, allClear = true),
                cf(2026, 6, Payday.THIRTIETH, 0.0, allClear = true),
            ),
            incomePerCutoff = 10000.0,
        )
        assertEquals(true, clear[0].isAllClear)
    }

    @Test
    fun aggregate_sorts_across_year_boundary() {
        val rows = InsightsCalculator.aggregateByMonth(
            listOf(
                cf(2027, 1, Payday.FIFTEENTH, 100.0),
                cf(2026, 12, Payday.THIRTIETH, 200.0),
            ),
            incomePerCutoff = 10000.0,
        )
        assertEquals(listOf(2026 to 12, 2027 to 1), rows.map { it.year to it.month })
    }

    @Test
    fun aggregate_empty_input_empty_output() {
        assertTrue(InsightsCalculator.aggregateByMonth(emptyList(), 10000.0).isEmpty())
    }

    private fun debt(
        id: Long = 1L,
        monthlyAmount: Double = 1500.0,
        totalPayments: Int = 6,
        startDate: LocalDate = LocalDate(2026, 1, 1),
        firstPaymentDate: LocalDate = startDate,
        dueDay: Int = 10,
        useLastDayOfMonth: Boolean = false,
        isArchived: Boolean = false,
    ) = Debt(
        id = id, name = "D$id", categoryId = 1L,
        monthlyAmount = monthlyAmount, totalPayments = totalPayments,
        dueDay = dueDay, useLastDayOfMonth = useLastDayOfMonth,
        startDate = startDate, firstPaymentDate = firstPaymentDate,
        isArchived = isArchived, notes = null,
    )

    private fun payment(id: Long, debtId: Long, amount: Double = 1500.0, date: LocalDate = LocalDate(2026, 1, 10)) =
        Payment(id = id, debtId = debtId, paidDate = date, amount = amount)

    // ---- snapshot ----

    @Test
    fun snapshot_no_debts() {
        val stats = InsightsCalculator.snapshot(
            activeScheduledDebts = emptyList(),
            paymentsByDebt = emptyMap(),
            incomePerCutoff = 25000.0,
        )
        assertEquals(0.0, stats.remaining)
        assertEquals(0, stats.debtCount)
        assertEquals(0.0, stats.monthlyBurden)
        assertEquals(50000.0, stats.monthlyIncome)
        assertEquals(0, stats.coveragePercent)
    }

    @Test
    fun snapshot_one_debt_partial() {
        // 1500 monthly × 6 total = 9000 scheduled; 2 paid; 4 × 1500 = 6000 remaining
        val d = debt()
        val pays = listOf(
            payment(1L, d.id, date = LocalDate(2026, 1, 10)),
            payment(2L, d.id, date = LocalDate(2026, 2, 10)),
        )
        val stats = InsightsCalculator.snapshot(
            activeScheduledDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to pays),
            incomePerCutoff = 25000.0,
        )
        assertEquals(6000.0, stats.remaining)
        assertEquals(1, stats.debtCount)
        assertEquals(1500.0, stats.monthlyBurden)
        assertEquals(50000.0, stats.monthlyIncome)
        // 1500 / 50000 = 0.03 → 3
        assertEquals(3, stats.coveragePercent)
    }

    @Test
    fun snapshot_income_zero_gives_null_coverage() {
        val d = debt()
        val stats = InsightsCalculator.snapshot(
            activeScheduledDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to emptyList()),
            incomePerCutoff = 0.0,
        )
        assertEquals(0.0, stats.monthlyIncome)
        assertNull(stats.coveragePercent)
    }

    @Test
    fun snapshot_caps_remaining_at_zero_when_overpaid() {
        // Edge case: more payments than totalPayments. Remaining shouldn't go negative.
        val d = debt(totalPayments = 2)
        val pays = (1L..5L).map { payment(it, d.id) }
        val stats = InsightsCalculator.snapshot(
            activeScheduledDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to pays),
            incomePerCutoff = 25000.0,
        )
        assertEquals(0.0, stats.remaining)
    }

    // ---- forecast ----

    @Test
    fun forecast_empty_when_no_debts() {
        val f = InsightsCalculator.forecastCutoffs(
            today = LocalDate(2026, 5, 13),
            activeScheduledDebts = emptyList(),
            paymentsByDebt = emptyMap(),
            incomePerCutoff = 25000.0,
            count = 12,
        )
        assertTrue(f.isEmpty())
    }

    @Test
    fun forecast_emits_count_rows_with_active_debts() {
        val d = debt(
            firstPaymentDate = LocalDate(2026, 1, 10),
            totalPayments = 36,
            dueDay = 10,
        )
        val f = InsightsCalculator.forecastCutoffs(
            today = LocalDate(2026, 5, 13),
            activeScheduledDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to emptyList()),
            incomePerCutoff = 25000.0,
            count = 12,
        )
        assertEquals(12, f.size)
    }

    @Test
    fun forecast_starts_two_cutoffs_after_today() {
        // today=May 13 → current cutoff: Apr 30 payday (windowStart Apr 30, windowEnd May 14)
        // nextCutoff: May 15 payday (May 15-29)
        // nextCutoff.next(): May 30 payday (May 30 - June 14)
        // So first forecast cutoff should have windowStart May 30.
        val d = debt(firstPaymentDate = LocalDate(2026, 1, 10), totalPayments = 36, dueDay = 10)
        val f = InsightsCalculator.forecastCutoffs(
            today = LocalDate(2026, 5, 13),
            activeScheduledDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to emptyList()),
            incomePerCutoff = 25000.0,
            count = 1,
        )
        assertEquals(1, f.size)
        assertEquals(LocalDate(2026, 5, 30), f[0].cutoff.windowStart)
        assertEquals(LocalDate(2026, 6, 14), f[0].cutoff.windowEnd)
    }

    @Test
    fun forecast_debt_rolls_off_when_finished() {
        // 6-cycle debt, all 6 payments will be billed across forecast.
        // After cycle 6 the debt should stop appearing — subsequent rows are All Clear.
        val d = debt(
            firstPaymentDate = LocalDate(2026, 1, 10),
            totalPayments = 6,
            dueDay = 10,
            monthlyAmount = 1500.0,
        )
        val f = InsightsCalculator.forecastCutoffs(
            today = LocalDate(2026, 1, 1),
            // today is so early that no cycles have been billed yet
            activeScheduledDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to emptyList()),
            incomePerCutoff = 25000.0,
            count = 12,
        )
        assertEquals(12, f.size)
        // Cycle 1 (due Jan 10) falls in the CURRENT cutoff, which Insights doesn't display
        // (Home owns it) — it's advanced but not shown. So 5 of the 6 cycles appear in the
        // visible forecast.
        val totalBilled = f.sumOf { it.dueTotal }
        assertEquals(5 * 1500.0, totalBilled)
        // The last rows should be All Clear.
        assertTrue(f.last().isAllClear)
    }

    @Test
    fun forecast_excludes_debt_whose_final_payment_falls_in_a_skipped_near_cutoff() {
        // Regression: a 2-payment debt, cycle 1 already paid (May 30), cycle 2 due Jun 30.
        // On Jun 25 the current cutoff is Jun-15th and the next is Jun-30th (windows the
        // Insights forecast skips). Cycle 2 must be advanced+rolled off in that skipped
        // Jun-30th cutoff, NOT spilled into a later visible cutoff (e.g. Jul 30).
        val d = debt(
            totalPayments = 2,
            dueDay = 30,
            startDate = LocalDate(2026, 5, 30),
            firstPaymentDate = LocalDate(2026, 5, 30),
        )
        val cycle1 = payment(1L, d.id, amount = 1500.0, date = LocalDate(2026, 5, 30))
        val f = InsightsCalculator.forecastCutoffs(
            today = LocalDate(2026, 6, 25),
            activeScheduledDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to listOf(cycle1)),
            incomePerCutoff = 25000.0,
            count = 6,
        )
        // The debt never appears in any displayed cutoff's breakdown.
        assertTrue(
            f.all { fc -> fc.rows.none { it.debt.id == d.id } },
            "Finished-soon debt leaked into a visible cutoff: ${f.filter { fc -> fc.rows.any { it.debt.id == d.id } }.map { it.cutoff }}",
        )
        assertTrue(f.all { it.isAllClear })
    }

    @Test
    fun forecast_heavy_cutoff_has_negative_left_over() {
        // Big debt, small income → forecast rows show negative leftOver.
        val d = debt(
            firstPaymentDate = LocalDate(2026, 1, 10),
            totalPayments = 36,
            dueDay = 10,
            monthlyAmount = 30000.0,
        )
        val f = InsightsCalculator.forecastCutoffs(
            today = LocalDate(2026, 5, 13),
            activeScheduledDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to emptyList()),
            incomePerCutoff = 10000.0,
            count = 3,
        )
        // Each cycle bills 30000 in some cutoff; leftOver = 10000 - 30000 = -20000
        val billedRows = f.filterNot { it.isAllClear }.filter { it.dueTotal > 0.0 }
        assertTrue(billedRows.isNotEmpty())
        billedRows.forEach { row ->
            assertTrue(row.leftOver < 0, "Expected negative leftOver for billed row $row")
        }
    }

    @Test
    fun forecast_rows_carry_the_per_debt_breakdown() {
        val d = debt(
            firstPaymentDate = LocalDate(2026, 1, 10),
            totalPayments = 6,
            dueDay = 10,
            monthlyAmount = 1500.0,
        )
        val f = InsightsCalculator.forecastCutoffs(
            today = LocalDate(2026, 1, 1),
            activeScheduledDebts = listOf(d),
            paymentsByDebt = mapOf(d.id to emptyList()),
            incomePerCutoff = 25000.0,
            count = 12,
        )
        // A billed cutoff carries one breakdown row for the debt, at its monthly amount,
        // and the rows sum to the cutoff's dueTotal.
        val billed = f.first { it.dueTotal > 0.0 }
        assertEquals(1, billed.rows.size)
        assertEquals(d.id, billed.rows[0].debt.id)
        assertEquals(1500.0, billed.rows[0].amount)
        assertEquals(billed.dueTotal, billed.rows.sumOf { it.amount })
        // An all-clear cutoff has no breakdown rows.
        assertTrue(f.last().isAllClear)
        assertTrue(f.last().rows.isEmpty())
    }
}
