package com.royce.calendarnotificationstatus

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import java.util.Calendar

data class CalendarEvent(
    val id: Long,
    val title: String,
    val beginTime: Long,
    val endTime: Long,
    val color: Int,
    val allDay: Boolean
)

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String
)

object CalendarHelper {

    fun getAvailableCalendars(context: Context): List<CalendarInfo> {
        val calendars = mutableListOf<CalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME
        )
        
        try {
            val cursor: Cursor? = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                "${CalendarContract.Calendars.ACCOUNT_NAME} ASC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
            )
            
            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                
                while (it.moveToNext()) {
                    calendars.add(
                        CalendarInfo(
                            id = it.getLong(idIdx),
                            displayName = it.getString(nameIdx) ?: "Unknown",
                            accountName = it.getString(accountIdx) ?: "Unknown"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return calendars
    }

    fun getUpcomingEvents(context: Context, limit: Int = 20): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, 90) // Look 90 days into the future
        val end = calendar.timeInMillis
        
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, end)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.ALL_DAY
        )

        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val selectedCalendars = prefs.getStringSet("selected_calendars", null)

        var selection = "${CalendarContract.Instances.VISIBLE} = 1"
        
        if (selectedCalendars != null && selectedCalendars.isNotEmpty()) {
            val ids = selectedCalendars.joinToString(",")
            selection += " AND ${CalendarContract.Instances.CALENDAR_ID} IN ($ids)"
        }

        try {
            val cursor: Cursor? = context.contentResolver.query(
                builder.build(),
                projection,
                selection,
                null,
                "${CalendarContract.Instances.BEGIN} ASC"
            )

            if (cursor == null || cursor.count == 0) {
                var calendarCount = 0
                try {
                    val calCursor = context.contentResolver.query(
                        CalendarContract.Calendars.CONTENT_URI,
                        arrayOf(CalendarContract.Calendars._ID),
                        null, null, null
                    )
                    calendarCount = calCursor?.count ?: 0
                    calCursor?.close()
                } catch (e: Exception) {}

                events.add(CalendarEvent(-1, "OS returned 0 Events. Visible Calendars: $calendarCount", now, now + 1000, 0, false))
            }

            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val titleIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val colorIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.DISPLAY_COLOR)
                val allDayIdx = it.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

                while (it.moveToNext() && events.size < limit) {
                    val title = it.getString(titleIdx) ?: "No Title"
                    val begin = it.getLong(beginIdx)
                    val endTime = it.getLong(endIdx)
                    
                    // Skip if the event has already ended
                    if (endTime <= now) continue

                    // Filter out Google Calendar "Working Location" pseudo-events
                    val lowerTitle = title.lowercase()
                    if (lowerTitle == "home" || lowerTitle == "office" || lowerTitle.startsWith("out of office")) {
                        continue
                    }

                    events.add(
                        CalendarEvent(
                            id = it.getLong(idIdx),
                            title = title,
                            beginTime = begin,
                            endTime = endTime,
                            color = it.getInt(colorIdx),
                            allDay = it.getInt(allDayIdx) == 1
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return events
    }
}
