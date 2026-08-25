package com.navigator.app.bridge

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.navigator.app.watch.NotificationWatchOutput
import com.navigator.core.format.NotificationFormatter
import com.navigator.core.parse.GoogleMapsNotificationParser
import com.navigator.core.parse.RawNavNotification

/**
 * Listens for Google Maps turn-by-turn notifications, parses them into a NavigationState via
 * [GoogleMapsNotificationParser], formats them, and mirrors them to the watch.
 *
 * Requires the user to grant notification access (Settings -> Notification access). This is
 * the Phase-3 bridge; Phase 4 replaces it with the Navigation SDK as the state producer.
 */
class GoogleMapsBridgeService : NotificationListenerService() {

    private val parser = GoogleMapsNotificationParser()
    private val formatter = NotificationFormatter()
    private val watchOutput by lazy { NotificationWatchOutput(applicationContext) }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (notification.packageName != GOOGLE_MAPS_PACKAGE) return

        val extras = notification.notification.extras ?: return
        val raw = RawNavNotification(
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
            subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
            ticker = notification.notification.tickerText?.toString(),
        )

        val state = parser.parse(raw) ?: return
        val update = formatter.format(state) ?: return
        watchOutput.show(update)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn?.packageName == GOOGLE_MAPS_PACKAGE) {
            watchOutput.clear()
        }
    }

    companion object {
        private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
    }
}
