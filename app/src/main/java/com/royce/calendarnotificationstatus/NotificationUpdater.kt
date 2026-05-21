package com.royce.calendarnotificationstatus

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.content.ContentUris
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toBitmap
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object NotificationUpdater {
    private const val CHANNEL_ID = "calendar_status_channel_v3"
    private const val NOTIFICATION_ID = 1001

    fun updateNotification(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("notification_enabled", false)
        val notificationManager = NotificationManagerCompat.from(context)

        if (!isEnabled) {
            notificationManager.cancel(NOTIFICATION_ID)
            cancelAlarms(context)
            return
        }

        createNotificationChannel(context)

        val events = CalendarHelper.getUpcomingEvents(context)
        
        val collapsedViews = RemoteViews(context.packageName, R.layout.notification_calendar)
        val expandedViews = RemoteViews(context.packageName, R.layout.notification_calendar)
        
        val now = Calendar.getInstance()
        val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault())
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayNumber = dayNumberFormat.format(now.time)

        val dynamicBitmap = createDynamicCalendarBitmap(context, dayNumber)
        val dynamicIcon = IconCompat.createWithBitmap(dynamicBitmap)

        collapsedViews.removeAllViews(R.id.events_container)
        expandedViews.removeAllViews(R.id.events_container)

        var nextEventTimeMillis: Long? = null

        if (events.isEmpty()) {
            val emptyView = RemoteViews(context.packageName, R.layout.notification_empty_event)
            collapsedViews.addView(R.id.events_container, emptyView)
            
            val emptyViewExpanded = RemoteViews(context.packageName, R.layout.notification_empty_event)
            expandedViews.addView(R.id.events_container, emptyViewExpanded)
        } else {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val dayOfYearFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            var previousDayStr: String? = null
            
            // Get enabled meeting platforms from SharedPreferences
            val enabledPlatforms = prefs.getStringSet("enabled_meeting_platforms", setOf(MeetingPlatform.ZOOM.name)) ?: setOf(MeetingPlatform.ZOOM.name)
            
            // Dynamically build the meeting URL regex based on user settings
            val meetingUrlRegex: Regex? = if (enabledPlatforms.isNotEmpty()) {
                val segments = enabledPlatforms.mapNotNull { platformName ->
                    try {
                        MeetingPlatform.valueOf(platformName).regexSegment
                    } catch (e: Exception) {
                        null
                    }
                }
                if (segments.isNotEmpty()) {
                    val combinedSegments = segments.joinToString("|")
                    Regex("""https?://(?:[a-zA-Z0-9-]+\.)?($combinedSegments)[^\s]+""")
                } else null
            } else null
            
            for ((index, event) in events.withIndex()) {
                val eventDate = Date(event.beginTime)
                val currentDayStr = dayOfYearFormat.format(eventDate)

                // Add day separator if the day changes (but not before the first event)
                if (previousDayStr != null && previousDayStr != currentDayStr) {
                    val separatorView = RemoteViews(context.packageName, R.layout.notification_day_separator)
                    expandedViews.addView(R.id.events_container, separatorView)
                }
                previousDayStr = currentDayStr

                val itemView = RemoteViews(context.packageName, R.layout.notification_event_item)
                itemView.setTextViewText(R.id.event_title, event.title)
                
                itemView.setTextViewText(R.id.event_day_of_week, dayOfWeekFormat.format(eventDate))
                itemView.setTextViewText(R.id.event_day_number, dayNumberFormat.format(eventDate))
                
                if (event.allDay) {
                    itemView.setTextViewText(R.id.event_time, "All day")
                } else {
                    val startStr = timeFormat.format(eventDate)
                    val endStr = timeFormat.format(Date(event.endTime))
                    itemView.setTextViewText(R.id.event_time, "$startStr - $endStr")
                }
                
                val hsv = FloatArray(3)
                Color.colorToHSV(event.color, hsv)
                // Drop saturation drastically to pull it towards white/pastel
                hsv[1] = hsv[1] * 0.3f
                // Force high brightness so it remains white rather than gray
                hsv[2] = Math.max(hsv[2], 0.9f)
                val pastelColor = Color.HSVToColor(hsv)

                val titleHsv = FloatArray(3)
                Color.colorToHSV(event.color, titleHsv)
                // Keep the title slightly more saturated so it pops
                titleHsv[1] = titleHsv[1] * 0.55f
                titleHsv[2] = Math.max(titleHsv[2], 0.9f)
                val titlePastelColor = Color.HSVToColor(titleHsv)

                itemView.setInt(R.id.event_color, "setBackgroundColor", event.color)
                itemView.setTextColor(R.id.event_title, titlePastelColor)
                itemView.setTextColor(R.id.event_day_of_week, titlePastelColor)
                itemView.setTextColor(R.id.event_day_number, titlePastelColor)
                itemView.setTextColor(R.id.event_time, pastelColor)

                // Add happening now pulse if the event is currently active
                val currentTime = System.currentTimeMillis()
                if (currentTime in event.beginTime..event.endTime) {
                    itemView.setViewVisibility(R.id.pulse_flipper, android.view.View.VISIBLE)
                    
                    // Note: RemoteViews doesn't allow changing backgroundTint directly on <shape> drawables
                    // easily without API 31+. Instead, we will tint the text itself to match the pastel color
                    // for the 'NOW' indicator to tie it into the theme.
                    itemView.setTextColor(R.id.pulse_active, titlePastelColor)
                    itemView.setTextColor(R.id.pulse_dim, titlePastelColor)
                    itemView.setTextColor(R.id.pulse_faded, titlePastelColor)
                } else {
                    itemView.setViewVisibility(R.id.pulse_flipper, android.view.View.GONE)
                }

                // Check for Smart Intent Actions
                val meetingUrl = if (meetingUrlRegex != null) {
                    event.location?.let { meetingUrlRegex.find(it)?.value } 
                        ?: event.description?.let { meetingUrlRegex.find(it)?.value }
                } else null

                val hasMeetingUrl = meetingUrl != null
                val hasPhysicalLocation = !event.location.isNullOrBlank() && !hasMeetingUrl

                if (hasMeetingUrl || hasPhysicalLocation) {
                    itemView.setViewVisibility(R.id.action_container, android.view.View.VISIBLE)
                    
                    if (hasMeetingUrl) {
                        itemView.setViewVisibility(R.id.action_join, android.view.View.VISIBLE)
                        itemView.setTextColor(R.id.action_join, pastelColor)
                        
                        val joinIntent = Intent(Intent.ACTION_VIEW, Uri.parse(meetingUrl))
                        val joinPendingIntent = PendingIntent.getActivity(
                            context,
                            (event.id * 10 + 1).toInt(), // Unique request code
                            joinIntent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                        itemView.setOnClickPendingIntent(R.id.action_join, joinPendingIntent)
                    } else {
                        itemView.setViewVisibility(R.id.action_join, android.view.View.GONE)
                    }

                    if (hasPhysicalLocation) {
                        itemView.setViewVisibility(R.id.action_map, android.view.View.VISIBLE)
                        itemView.setTextColor(R.id.action_map, pastelColor)
                        
                        val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(event.location)}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                        val mapPendingIntent = PendingIntent.getActivity(
                            context,
                            (event.id * 10 + 2).toInt(), // Unique request code
                            mapIntent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                        itemView.setOnClickPendingIntent(R.id.action_map, mapPendingIntent)
                    } else {
                        itemView.setViewVisibility(R.id.action_map, android.view.View.GONE)
                    }
                } else {
                    itemView.setViewVisibility(R.id.action_container, android.view.View.GONE)
                }
                
                // Add to expanded view always
                expandedViews.addView(R.id.events_container, itemView)
                
                // Create intent to open Google Calendar to the specific day
                val builderUri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
                ContentUris.appendId(builderUri, event.beginTime)
                val openEventIntent = Intent(Intent.ACTION_VIEW).setData(builderUri.build())
                val openEventPendingIntent = PendingIntent.getActivity(
                    context,
                    event.id.toInt(), // Use event ID as request code to ensure unique intents
                    openEventIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                itemView.setOnClickPendingIntent(R.id.event_item_root, openEventPendingIntent)
                
                // Add to collapsed view only if it's the first event (max 1 event to avoid clipping)
                if (index < 1) {
                    val collapsedItemView = RemoteViews(context.packageName, R.layout.notification_event_item)
                    collapsedItemView.setTextViewText(R.id.event_title, event.title)
                    collapsedItemView.setTextViewText(R.id.event_day_of_week, dayOfWeekFormat.format(eventDate))
                    collapsedItemView.setTextViewText(R.id.event_day_number, dayNumberFormat.format(eventDate))
                    if (event.allDay) {
                        collapsedItemView.setTextViewText(R.id.event_time, "All day")
                    } else {
                        val startStr = timeFormat.format(eventDate)
                        val endStr = timeFormat.format(Date(event.endTime))
                        collapsedItemView.setTextViewText(R.id.event_time, "$startStr - $endStr")
                    }
                    collapsedItemView.setInt(R.id.event_color, "setBackgroundColor", event.color)
                    collapsedItemView.setTextColor(R.id.event_title, titlePastelColor)
                    collapsedItemView.setTextColor(R.id.event_day_of_week, titlePastelColor)
                    collapsedItemView.setTextColor(R.id.event_day_number, titlePastelColor)
                    collapsedItemView.setTextColor(R.id.event_time, pastelColor)
                    collapsedItemView.setOnClickPendingIntent(R.id.event_item_root, openEventPendingIntent)
                    collapsedViews.addView(R.id.events_container, collapsedItemView)
                }

                // Track the earliest next event end time to schedule an update alarm
                if (event.endTime > currentTime) {
                    if (nextEventTimeMillis == null || event.endTime < nextEventTimeMillis) {
                        nextEventTimeMillis = event.endTime
                    }
                }
                if (event.beginTime > currentTime) {
                    if (nextEventTimeMillis == null || event.beginTime < nextEventTimeMillis) {
                        nextEventTimeMillis = event.beginTime
                    }
                }
            }
        }

        // Calculate next midnight for daily icon update
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextMidnightMillis = tomorrow.timeInMillis
        if (nextEventTimeMillis == null || nextMidnightMillis < nextEventTimeMillis) {
            nextEventTimeMillis = nextMidnightMillis
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationDismissReceiver::class.java)
        val deletePendingIntent = PendingIntent.getBroadcast(
            context, 0, dismissIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val appIconBitmap = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)?.toBitmap()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(dynamicIcon)
            .setLargeIcon(appIconBitmap)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setOngoing(true) // Sticky notification
            .setPriority(NotificationCompat.PRIORITY_LOW) // Usually preferred for sticky items so it doesn't buzz
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deletePendingIntent)

        notificationManager.notify(NOTIFICATION_ID, builder.build())

        // Schedule next update if needed
        scheduleUpdateAlarm(context, nextEventTimeMillis)
    }

    private fun createNotificationChannel(context: Context) {
        val name = "Calendar Status"
        val descriptionText = "Shows upcoming calendar events"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            setShowBadge(false)
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        notificationManager.deleteNotificationChannel("calendar_status_channel")
        notificationManager.deleteNotificationChannel("calendar_status_channel_v2")

        notificationManager.createNotificationChannel(channel)
    }

    private fun scheduleUpdateAlarm(context: Context, timeInMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
    }

    private fun cancelAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun createDynamicCalendarBitmap(context: Context, dayNumber: String): Bitmap {
        // Density independent size. Standard status bar icon size is 24dp.
        // MUST be 24dp or Android will reject it for the header small icon and fallback to app logo!
        val density = context.resources.displayMetrics.density
        val size = (24 * density).toInt()

        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        // Draw the rounded box border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE // The system will tint this appropriately
            style = Paint.Style.STROKE
            strokeWidth = 1 * density // Reduced thickness
        }

        val padding = 1 * density // Reduced padding to allow more text room
        val rect = RectF(padding, padding, size - padding, size - padding)
        val cornerRadius = 4 * density
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        // Draw the day number text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            // Adjust text size to bump up visibility
            textSize = 15 * density
        }

        // Calculate vertical centering
        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(dayNumber, 0, dayNumber.length, textBounds)
        val xPos = canvas.width / 2f
        val yPos = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)

        canvas.drawText(dayNumber, xPos, yPos, textPaint)

        return bitmap
    }
}








