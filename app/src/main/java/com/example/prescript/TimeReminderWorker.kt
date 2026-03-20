package com.example.prescript

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class TimeReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString("USER_ID") ?: return Result.failure()
        val prefs = applicationContext.getSharedPreferences("PreScriptPrefs_$userId", Context.MODE_PRIVATE)
        val endTime = prefs.getLong("TIMER_END_TIME", 0L)
        val currentTime = System.currentTimeMillis()

        // Check if there are approximately 6 hours left
        val sixHoursInMillis = 6 * 60 * 60 * 1000L
        if (endTime > currentTime && (endTime - currentTime) <= sixHoursInMillis && (endTime - currentTime) > (6 * 60 * 60 * 1000L - 5 * 60 * 1000L)) { // Ensure it's not already past the 6-hour mark and within a reasonable window
            showNotification(
                applicationContext,
                "Time is running out!",
                "You have less than 6 hours to complete your daily prescript!"
            )
        }
        return Result.success()
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "prescript_reminder_channel"
        val notificationId = 101 // Unique ID for this notification

        // Notification channel is created in MainActivity to avoid duplication and ensure it's called once.
        // Make sure a channel with 'prescript_reminder_channel' ID is created in MainActivity.

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_bell) // Placeholder. You should create this drawable.
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // Dismisses the notification when clicked

        notificationManager.notify(notificationId, builder.build())
    }
}
