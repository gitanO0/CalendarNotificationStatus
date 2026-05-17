package com.royce.calendarnotificationstatus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CalendarUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PROVIDER_CHANGED) {
            NotificationUpdater.updateNotification(context)
        }
    }
}

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
