package com.snowball.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannelInit {
    const val CHANNEL_ID = "payday"
    fun create(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Payday reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Reminders on the 15th and last day of each month."
        }
        nm.createNotificationChannel(channel)
    }
}
