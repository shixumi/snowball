package com.snowball.data.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Thrown when a backup string can't be parsed or its format isn't supported. */
class BackupFormatException(message: String) : Exception(message)

/**
 * Pure (no DB) encode/decode of the backup envelope. Kept separate from
 * [BackupService] so it's unit-testable without a database.
 */
object BackupCodec {
    const val CURRENT_FORMAT_VERSION = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(backup: BackupFile): String = json.encodeToString(BackupFile.serializer(), backup)

    /**
     * @throws BackupFormatException if the text isn't valid JSON for our schema,
     * or carries an unrecognised [BackupFile.formatVersion].
     */
    fun decode(text: String): BackupFile {
        // Tolerate transfer artifacts: a UTF-8/UTF-16 BOM or surrounding whitespace.
        val cleaned = text.trim().removePrefix("﻿").trim()
        val parsed = try {
            json.decodeFromString(BackupFile.serializer(), cleaned)
        } catch (e: SerializationException) {
            throw BackupFormatException(notValidMessage(cleaned))
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException(notValidMessage(cleaned))
        }
        if (parsed.formatVersion != CURRENT_FORMAT_VERSION) {
            throw BackupFormatException("This backup was made by an incompatible version of Snowball.")
        }
        return parsed
    }

    /** Diagnostic message: reveals whether the input was empty / what it started with. */
    private fun notValidMessage(text: String): String {
        if (text.isEmpty()) return "The file was empty or couldn't be read."
        val head = text.take(50).replace("\n", " ").replace("\r", " ")
        return "Not a valid Snowball export. Read ${text.length} chars starting: \"$head…\""
    }
}
