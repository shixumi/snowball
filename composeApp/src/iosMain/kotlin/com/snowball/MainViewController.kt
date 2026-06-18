package com.snowball

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.snowball.data.Repos
import com.snowball.data.db.DatabaseDriverFactory
import com.snowball.data.db.createDatabase
import com.snowball.platform.NotificationScheduler
import platform.UIKit.UIViewController

/** iOS entry point. Hosted by the SwiftUI shell via UIViewControllerRepresentable. */
fun MainViewController(): UIViewController = ComposeUIViewController {
    val repos = remember { Repos(createDatabase(DatabaseDriverFactory())) }
    val scheduler = remember { NotificationScheduler() }
    App(repos = repos, notificationScheduler = scheduler)
}
