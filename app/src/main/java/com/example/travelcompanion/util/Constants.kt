package com.example.travelcompanion.util

object Constants {
    // Database
    const val DATABASE_NAME = "travel_companion_db"

    // Location
    const val LOCATION_UPDATE_INTERVAL = 3000L // 5 secondi
    const val LOCATION_FASTEST_INTERVAL = 2000L // 2 secondi
    const val LOCATION_MAX_WAIT_TIME = 10000L // 10 secondi

    // Geofencing
    const val GEOFENCE_RADIUS = 200f // 200 metri

    // Notifications
    const val NOTIFICATION_CHANNEL_ID = "trip_tracking_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Trip Tracking"
    const val NOTIFICATION_ID = 1001
}