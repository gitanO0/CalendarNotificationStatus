package com.royce.calendarnotificationstatus

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log

class CalendarSyncJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        Log.d("CalendarSyncJob", "Calendar content changed, updating notification")
        // Run the update
        NotificationUpdater.updateNotification(applicationContext)
        
        // We must tell the system the job is finished. 
        // We return false because the work is done synchronously inside updateNotification.
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        // Return true to reschedule if the job is interrupted
        return true
    }
}