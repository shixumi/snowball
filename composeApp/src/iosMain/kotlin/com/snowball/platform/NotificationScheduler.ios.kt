package com.snowball.platform

/**
 * Stubbed for the initial iOS port. The Settings toggle still persists the user's
 * choice; actually scheduling payday reminders via UNUserNotificationCenter is a
 * follow-up (kept out of v1 to avoid risky platform interop in the first bring-up).
 */
actual class NotificationScheduler {
    actual fun schedule(enabled: Boolean, hour: Int, minute: Int) {}
    actual fun cancel() {}
    actual fun hasPermission(): Boolean = true
}
