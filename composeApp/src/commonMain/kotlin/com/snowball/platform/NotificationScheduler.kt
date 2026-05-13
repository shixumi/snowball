package com.snowball.platform

expect class NotificationScheduler {
    fun schedule(enabled: Boolean, hour: Int, minute: Int)
    fun cancel()
    fun hasPermission(): Boolean
}
