package com.royce.calendarnotificationstatus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationUpdater.updateNotification(context)
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Triggered by the exact alarm when an event starts or ends
        NotificationUpdater.updateNotification(context)
    }
}

class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("notification_enabled", false)
        if (isEnabled) {
            // Re-add the notification since it's enabled but was dismissed
            NotificationUpdater.updateNotification(context)
        }
    }
}
