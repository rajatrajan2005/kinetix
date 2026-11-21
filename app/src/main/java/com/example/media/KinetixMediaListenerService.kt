package com.example.media

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Enables MediaSessionManager.getActiveSessions() access across any installed music player
 * (Poweramp, Echo, Spotify, YouTube Music, etc.) with zero player-specific dependencies.
 */
class KinetixMediaListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = Unit

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = Unit
}
