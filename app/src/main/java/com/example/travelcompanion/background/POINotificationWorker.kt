package com.example.travelcompanion.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.travelcompanion.R

/**
 * Worker per notifiche periodiche
 * Invia reminder per registrare viaggi
 */
class POINotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Invia notifica reminder
            sendNotification(
                "🗺️ Travel Companion",
                "Non dimenticare di registrare i tuoi viaggi!"
            )
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crea canale notifiche (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reminder Viaggi",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminder per registrare i tuoi viaggi"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Crea notifica
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_ID = "travel_reminders"
        private const val NOTIFICATION_ID = 2001
    }
}
