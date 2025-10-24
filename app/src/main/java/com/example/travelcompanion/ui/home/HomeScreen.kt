package com.example.travelcompanion.ui.home

import android.Manifest
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
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

    // Launcher per richiedere permessi
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions.values.all { it }
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
            MapView(hasLocationPermission)

            // Controlli in basso
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tipo viaggio
                if (isTracking) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "🚗 Registrando: ${selectedTripType.name}",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
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
fun MapView(hasPermission: Boolean) {
    // Posizione default: Bologna
    val bologna = LatLng(44.4949, 11.3426)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bologna, 12f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        if (hasPermission) {
            Marker(
                state = MarkerState(position = bologna),
                title = "La tua posizione",
                snippet = "Bologna"
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