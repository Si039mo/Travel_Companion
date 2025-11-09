package com.example.travelcompanion.background


import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Scheduler per lavori in background con WorkManager
 */
object WorkManagerScheduler {

    /**
     * Avvia notifiche periodiche ogni 24 ore
     */
    fun schedulePeriodicNotifications(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<POINotificationWorker>(
            24, TimeUnit.HOURS,  // Ogni 24 ore
            30, TimeUnit.MINUTES // Flessibilità di 30 minuti
        )
            .setConstraints(constraints)
            .setInitialDelay(2, TimeUnit.HOURS) // Primo run dopo 2 ore
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "travel_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Cancella tutte le notifiche programmate
     */
    fun cancelAllWork(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }
}