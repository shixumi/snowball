package com.snowball.data.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackupCodecTest {

    private fun sample() = BackupFile(
        formatVersion = BackupCodec.CURRENT_FORMAT_VERSION,
        dbVersion = 5,
        exportedAt = 1_750_000_000_000,
        categories = listOf(
            CategoryDto(1, "Credit Card", true, "SCHEDULED", "credit_card", 0),
            CategoryDto(3, "Gym", false, "LEDGER", "fitness", 10),
        ),
        debts = listOf(
            DebtDto(7, "Laptop", 1, 2500.0, 12, 15, false, "2026-01-15", "2026-02-15", false, "0% plan", 100),
        ),
        payments = listOf(
            PaymentDto(20, 7, "2026-02-15", 2500.0, 200),
            PaymentDto(21, 7, "2026-03-15", 2500.0, 300),
        ),
        settings = SettingsDto(15000.0, "PHP", true, 9, 0, true, false, "15:2026-06"),
    )

    @Test
    fun round_trip_preserves_everything() {
        val original = sample()
        val decoded = BackupCodec.decode(BackupCodec.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun decode_rejects_garbage() {
        assertFailsWith<BackupFormatException> { BackupCodec.decode("not json at all") }
    }

    @Test
    fun decode_rejects_empty() {
        assertFailsWith<BackupFormatException> { BackupCodec.decode("") }
    }

    @Test
    fun decode_rejects_unsupported_format_version() {
        val json = BackupCodec.encode(sample().copy(formatVersion = 99))
        val ex = assertFailsWith<BackupFormatException> { BackupCodec.decode(json) }
        assertTrue(ex.message!!.contains("incompatible"))
    }

    @Test
    fun nullable_notes_round_trips() {
        val original = sample().let { it.copy(debts = it.debts.map { d -> d.copy(notes = null) }) }
        val decoded = BackupCodec.decode(BackupCodec.encode(original))
        assertEquals(null, decoded.debts.first().notes)
    }
}
