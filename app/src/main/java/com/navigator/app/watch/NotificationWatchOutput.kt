package com.navigator.app.watch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.navigator.app.R
import com.navigator.core.watch.WatchOutput
import com.navigator.core.watch.WatchPriority
import com.navigator.core.watch.WatchUpdate

/**
 * Posts navigation updates as standard Android notifications so any watch that mirrors phone
 * notifications (Noise and most others) can display them. This is the default [WatchOutput]
 * implementation; a direct-BLE variant can be added later behind the same interface (D-011).
 */
class NotificationWatchOutput(private val context: Context) : WatchOutput {

    init {
        ensureChannel()
    }

    override fun show(update: WatchUpdate) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(update.primary)
            .apply { update.secondary?.let { setContentText(it) } }
            .setPriority(androidPriorityOf(update.priority))
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setOnlyAlertOnce(true)
            .build()

        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) {
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun clear() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Navigation",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Turn-by-turn navigation instructions mirrored to the watch."
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun androidPriorityOf(priority: WatchPriority): Int = when (priority) {
        WatchPriority.CRITICAL -> NotificationCompat.PRIORITY_HIGH
        WatchPriority.MEDIUM -> NotificationCompat.PRIORITY_DEFAULT
        WatchPriority.LOW -> NotificationCompat.PRIORITY_LOW
    }

    companion object {
        const val CHANNEL_ID = "navigator_navigation"
        const val NOTIFICATION_ID = 1001
    }
}
