package com.snowball.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject

/** Walks to the top-most presented controller of the key window (scene-safe). */
private fun topmostViewController(): UIViewController? {
    val windows = UIApplication.sharedApplication.windows
    val keyWindow = (windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true }
        ?: windows.firstOrNull()) as? UIWindow
    var top = keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberBackupExporter(): (fileName: String, content: String) -> Unit = { fileName, content ->
    // Write to a temp file, then present the system share sheet so the user can
    // AirDrop / Mail / save-to-Files the .json.
    val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
    (content as NSString).writeToURL(url, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    val activityVC = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
    topmostViewController()?.presentViewController(activityVC, animated = true, completion = null)
}

private class BackupPickerDelegate(
    private val onResult: (String?) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        if (url == null) {
            onResult(null)
            return
        }
        // Picked files are security-scoped; must open access before reading (harmless
        // when the picker already handed back a sandbox copy).
        val accessed = url.startAccessingSecurityScopedResource()
        val text = try {
            NSString.stringWithContentsOfURL(url, NSUTF8StringEncoding, null) as String?
        } finally {
            if (accessed) url.stopAccessingSecurityScopedResource()
        }
        onResult(text)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(null)
    }
}

@Composable
actual fun rememberBackupImporter(onResult: (String?) -> Unit): () -> Unit {
    val latestOnResult by rememberUpdatedState(onResult)
    // The picker holds its delegate weakly, so retain it for the composition's lifetime.
    val delegate = remember { BackupPickerDelegate { latestOnResult(it) } }
    return {
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeJSON),
            asCopy = true,
        )
        picker.delegate = delegate
        topmostViewController()?.presentViewController(picker, animated = true, completion = null)
    }
}
