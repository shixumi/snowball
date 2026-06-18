package com.snowball.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberBackupExporter(): (String, String) -> Unit {
    val context = LocalContext.current
    // CreateDocument returns a destination Uri AFTER the user picks a name/location,
    // so we stash the content to write once that Uri comes back.
    var pending by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val content = pending
        pending = null
        if (uri != null && content != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(content.encodeToByteArray()) }
        }
    }
    return { fileName, content ->
        pending = content
        launcher.launch(fileName)
    }
}

@Composable
actual fun rememberBackupImporter(onResult: (String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            onResult(null)
        } else {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            }.getOrNull()
            onResult(text)
        }
    }
    // Accept json by mime type, plus permissive fallbacks since some providers
    // label .json files as text/plain or octet-stream.
    return { launcher.launch(arrayOf("application/json", "text/plain", "*/*")) }
}
