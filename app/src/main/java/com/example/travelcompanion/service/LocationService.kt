package com.example.travelcompanion.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.travelcompanion.R
import com.example.travelcompanion.data.local.database.AppDatabase
import com.example.travelcompanion.data.local.entity.TripLocation
import com.example.travelcompanion.data.repository.TripRepository
import com.example.travelcompanion.util.Constants
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LocationService : Service() {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentTripId: Long = -1
    private var totalDistance: Float = 0f
    private var lastLocation: Location? = null

    private lateinit var repository: TripRepository

    override fun onCreate() {
        super.onCreate()

        // Inizializza database e repository
        val database = AppDatabase.getDatabase(this)
        repository = TripRepository(
            database.tripDao(),
            database.tripLocationDao(),
            database.tripMediaDao()
        )

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                currentTripId = intent.getLongExtra(EXTRA_TRIP_ID, -1)
                startForegroundService()
                startLocationUpdates()
            }
            ACTION_STOP_TRACKING -> {
                stopLocationUpdates()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification("Tracking attivo", "Registrazione percorso...")
        startForeground(Constants.NOTIFICATION_ID, notification)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            Constants.LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(Constants.LOCATION_FASTEST_INTERVAL)
            setMaxUpdateDelayMillis(Constants.LOCATION_MAX_WAIT_TIME)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    handleNewLocation(location)
                }
            }
        }

        try {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Permessi non concessi
            stopSelf()
        }
    }

    private fun handleNewLocation(location: Location) {
        if (currentTripId == -1L) return

        // Calcola distanza dall'ultima posizione
        lastLocation?.let { last ->
            val distance = last.distanceTo(location)
            if (distance > 5) { // Ignora variazioni minori di 5 metri
                totalDistance += distance
            }
        }
        lastLocation = location

        // Salva nel database
        serviceScope.launch {
            try {
                val tripLocation = TripLocation(
                    tripId = currentTripId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis(),
                    accuracy = location.accuracy
                )
                repository.insertLocation(tripLocation)

                // Aggiorna trip con distanza totale
                repository.getTripById(currentTripId)?.let { trip ->
                    repository.updateTrip(
                        trip.copy(totalDistance = totalDistance / 1000f) // Converti in km
                    )
                }

                // Aggiorna notifica
                updateNotification(totalDistance)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, text: String): Notification {
        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(distanceMeters: Float) {
        val distanceKm = distanceMeters / 1000f
        val notification = createNotification(
            "Tracking attivo",
            "Percorsi: %.2f km".format(distanceKm)
        )

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(Constants.NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"
        const val EXTRA_TRIP_ID = "EXTRA_TRIP_ID"

        fun startService(context: Context, tripId: Long) {
            val intent = Intent(context, LocationService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_TRIP_ID, tripId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }
}