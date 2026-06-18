package com.snowball.platform

import androidx.compose.runtime.Composable

/**
 * Platform file transport for backups. Mirrors the launcher pattern used by
 * [rememberRequestNotificationPermission].
 *
 * - The exporter, when invoked with a suggested file name and JSON content,
 *   lets the user save/share a `.json` file (Android SAF create-document; iOS
 *   share sheet).
 * - The importer, when invoked, lets the user pick a `.json` file; [onResult]
 *   receives the file's text, or null if the user cancelled or it couldn't be
 *   read.
 */
@Composable
expect fun rememberBackupExporter(): (fileName: String, content: String) -> Unit

@Composable
expect fun rememberBackupImporter(onResult: (String?) -> Unit): () -> Unit
