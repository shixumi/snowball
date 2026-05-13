package com.snowball.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.snowball.MainActivity
import com.snowball.R
import java.time.LocalDate

class PaydayWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val lastDayOfMonth = today.lengthOfMonth()
        if (today.dayOfMonth == 15 || today.dayOfMonth == lastDayOfMonth) {
            post()
        }
        return Result.success()
    }

    private fun post() {
        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            applicationContext, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(applicationContext, NotificationChannelInit.CHANNEL_ID)
            .setContentTitle("Payday — Snowball")
            .setContentText("Time to settle this cutoff.")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1, notif)
    }
}
