package com.snowball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.snowball.data.Repos
import com.snowball.data.db.DatabaseDriverFactory
import com.snowball.data.db.createDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = createDatabase(DatabaseDriverFactory(applicationContext))
        val repos = Repos(db)
        setContent { App(repos) }
    }
}
