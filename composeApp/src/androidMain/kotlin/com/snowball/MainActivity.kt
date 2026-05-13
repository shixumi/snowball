package com.snowball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.snowball.data.Repos
import com.snowball.data.db.DatabaseDriverFactory
import com.snowball.data.db.createDatabase
import com.snowball.notifications.NotificationChannelInit
import com.snowball.platform.NotificationScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        NotificationChannelInit.create(this)
        val db = createDatabase(DatabaseDriverFactory(applicationContext))
        val repos = Repos(db)
        setContent {
            val scheduler = remember { NotificationScheduler(applicationContext) }
            App(repos = repos, notificationScheduler = scheduler)
        }
    }
}
