package com.snowball

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.snowball.data.Repos
import com.snowball.data.db.DatabaseDriverFactory
import com.snowball.data.db.createDatabase
import com.snowball.platform.NotificationScheduler
import platform.UIKit.UIViewController

/** iOS entry point. Hosted by the SwiftUI shell via UIViewControllerRepresentable. */
fun MainViewController(): UIViewController = ComposeUIViewController(
    // The CADisableMinimumFrameDurationOnPhone Info.plist entry is also set, but
    // Xcode's plist processing isn't surfacing it to the runtime check, which then
    // aborts on startup. Disabling the strict check here (compiled into the
    // framework) guarantees the app launches regardless of how the plist is bundled.
    configure = { enforceStrictPlistSanityCheck = false },
) {
    val repos = remember { Repos(createDatabase(DatabaseDriverFactory())) }
    val scheduler = remember { NotificationScheduler() }
    App(repos = repos, notificationScheduler = scheduler)
}
