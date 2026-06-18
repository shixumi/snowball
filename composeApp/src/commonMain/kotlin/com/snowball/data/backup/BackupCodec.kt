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
        val parsed = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupFormatException("The text isn't a valid Snowball export.")
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException("The text isn't a valid Snowball export.")
        }
        if (parsed.formatVersion != CURRENT_FORMAT_VERSION) {
            throw BackupFormatException("This backup was made by an incompatible version of Snowball.")
        }
        return parsed
    }
}
