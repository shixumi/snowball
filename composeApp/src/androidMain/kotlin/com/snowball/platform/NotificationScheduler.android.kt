package com.snowball.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.snowball.notifications.PaydayWorker
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

actual class NotificationScheduler(private val context: Context) {
    actual fun schedule(enabled: Boolean, hour: Int, minute: Int) {
        if (!enabled) {
            cancel()
            return
        }
        val now = LocalDateTime.now()
        val target = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        val firstFire = if (target.isAfter(now)) target else target.plusDays(1)
        val initialDelay = Duration.between(now, firstFire)

        val request = PeriodicWorkRequestBuilder<PaydayWorker>(Duration.ofDays(1))
            .setInitialDelay(initialDelay)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    actual fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    actual fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val WORK_NAME = "snowball.paydayNotification"
    }
}
