package com.example.travelcompanion.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.travelcompanion.data.local.database.AppDatabase
import com.example.travelcompanion.data.local.entity.Trip
import com.example.travelcompanion.data.local.entity.TripType
import com.example.travelcompanion.data.repository.TripRepository
import com.example.travelcompanion.service.LocationService
import com.example.travelcompanion.util.PermissionHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTripList: () -> Unit,
    onNavigateToTripDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Repository
    val repository = remember {
        val database = AppDatabase.getDatabase(context)
        TripRepository(
            database.tripDao(),
            database.tripLocationDao(),
            database.tripMediaDao()
        )
    }

    var hasLocationPermission by remember {
        mutableStateOf(PermissionHelper.hasLocationPermissions(context))
    }

    var isTracking by remember { mutableStateOf(false) }
    var currentTripId by remember { mutableLongStateOf(-1L) }
    var selectedTripType by remember { mutableStateOf(TripType.LOCAL) }
    var showTripTypeDialog by remember { mutableStateOf(false) }

    // Posizione corrente dell'utente
    var currentUserLocation by remember { mutableStateOf<LatLng?>(null) }

    // Stats del viaggio corrente (solo distanza e punti)
    var currentTrip by remember { mutableStateOf<Trip?>(null) }
    var locationCount by remember { mutableIntStateOf(0) }

    // Launcher per richiedere permessi
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.all { it }
    }

    // Ottieni posizione iniziale dell'utente
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            currentUserLocation = LatLng(it.latitude, it.longitude)
                        }
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

    }

    // Aggiorna posizione e stats durante il tracking (ogni 3 secondi)
    LaunchedEffect(isTracking) {
        if (isTracking && currentTripId != -1L) {
            while (isTracking) {
                try {
                    // Ottieni ultima posizione dal DB
                    val locations = repository.getLocationsForTripSync(currentTripId)
                    if (locations.isNotEmpty()) {
                        val lastLocation = locations.last()
                        currentUserLocation = LatLng(lastLocation.latitude, lastLocation.longitude)
                        locationCount = locations.size
                    }

                    // Aggiorna stats viaggio
                    currentTrip = repository.getTripById(currentTripId)

                    delay(3000) // Ogni 3 secondi
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            locationCount = 0
            currentTrip = null
        }
    }

    // 🧹 Aggiungi questo blocco sotto:
    DisposableEffect(Unit) {
        onDispose {
            // Ferma il tracking se la schermata viene chiusa
            isTracking = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Travel Companion") },
                actions = {
                    TextButton(onClick = onNavigateToTripList) {
                        Text("I miei viaggi")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Mappa
            MapView(
                hasLocationPermission = hasLocationPermission,
                isTracking = isTracking,
                userLocation = currentUserLocation
            )

            // Controlli in basso
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Card info viaggio corrente
                if (isTracking && currentTrip != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🚗 Registrando: ${
                                    when(selectedTripType) {
                                        TripType.LOCAL -> "Locale"
                                        TripType.DAY -> "Giornaliero"
                                        TripType.MULTI_DAY -> "Multi-giorno"
                                    }
                                }",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "%.2f km".format(currentTrip?.totalDistance),
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Text(
                                        text = "Distanza",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$locationCount",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Text(
                                        text = "Punti GPS",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Bottone Start/Stop
                FloatingActionButton(
                    onClick = {
                        if (!hasLocationPermission) {
                            permissionLauncher.launch(PermissionHelper.LOCATION_PERMISSIONS)
                        } else {
                            if (isTracking) {
                                // STOP tracking
                                scope.launch {
                                    // Termina il trip
                                    repository.getTripById(currentTripId)?.let { trip ->
                                        repository.updateTrip(
                                            trip.copy(
                                                endTime = System.currentTimeMillis(),
                                                isActive = false
                                            )
                                        )
                                    }

                                    // Ferma il service
                                    LocationService.stopService(context)

                                    isTracking = false
                                    currentTripId = -1L
                                }
                            } else {
                                // START tracking - mostra dialog
                                showTripTypeDialog = true
                            }
                        }
                    },
                    containerColor = if (isTracking)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = if (isTracking) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isTracking) "Stop" else "Start"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isTracking) "Ferma viaggio" else "Inizia viaggio",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    // Dialog selezione tipo viaggio
    if (showTripTypeDialog) {
        TripTypeDialog(
            onDismiss = { showTripTypeDialog = false },
            onTripTypeSelected = { tripType ->
                selectedTripType = tripType
                showTripTypeDialog = false

                // Crea nuovo trip nel database
                scope.launch {
                    try {
                        val newTrip = Trip(
                            type = tripType,
                            destination = "In viaggio...",
                            startTime = System.currentTimeMillis(),
                            isActive = true
                        )
                        val tripId = repository.insertTrip(newTrip)
                        currentTripId = tripId

                        // Avvia LocationService
                        LocationService.startService(context, tripId)

                        isTracking = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }
}

@Composable
fun MapView(
    hasLocationPermission: Boolean,
    isTracking: Boolean,
    userLocation: LatLng?
) {
    // Posizione default (Bologna) se non abbiamo GPS
    val defaultPosition = LatLng(44.4949, 11.3426)

    // Usa posizione utente se disponibile, altrimenti default
    val displayPosition = userLocation ?: defaultPosition

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(displayPosition, 15f)
    }

    LaunchedEffect(displayPosition) {
        if (!isTracking) {
            // Aggiorna camera quando otteniamo la posizione iniziale
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(displayPosition, 15f),
                durationMs = 1000
            )
        }
    }


    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = hasLocationPermission
        )
    ) {
        // Mostra marker solo durante tracking
        if (isTracking && userLocation != null) {
            Marker(
                state = MarkerState(position = userLocation),
                title = "In viaggio",
                snippet = "Posizione corrente"
            )
        }
    }
}

@Composable
fun TripTypeDialog(
    onDismiss: () -> Unit,
    onTripTypeSelected: (TripType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleziona tipo viaggio") },
        text = {
            Column {
                TripType.values().forEach { type ->
                    TextButton(
                        onClick = { onTripTypeSelected(type) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = when(type) {
                                TripType.LOCAL -> "🏙️ Locale (in città)"
                                TripType.DAY -> "🚗 Giornaliero"
                                TripType.MULTI_DAY -> "✈️ Multi-giorno"
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}