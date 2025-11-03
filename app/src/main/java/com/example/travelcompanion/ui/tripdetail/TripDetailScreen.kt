package com.example.travelcompanion.ui.tripdetail

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.travelcompanion.data.local.database.AppDatabase
import com.example.travelcompanion.data.local.entity.MediaType
import com.example.travelcompanion.data.local.entity.Trip
import com.example.travelcompanion.data.local.entity.TripLocation
import com.example.travelcompanion.data.local.entity.TripMedia
import com.example.travelcompanion.data.local.entity.TripType
import com.example.travelcompanion.data.repository.TripRepository
import com.example.travelcompanion.util.CameraHelper
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val repository = remember {
        val database = AppDatabase.getDatabase(context)
        TripRepository(
            database.tripDao(),
            database.tripLocationDao(),
            database.tripMediaDao()
        )
    }

    var trip by remember { mutableStateOf<Trip?>(null) }
    val locations by repository.getLocationsForTrip(tripId).collectAsState(initial = emptyList())
    val media by repository.getMediaForTrip(tripId).collectAsState(initial = emptyList())

    var showEditDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            scope.launch {
                val newMedia = TripMedia(
                    tripId = tripId,
                    type = MediaType.PHOTO,
                    content = photoUri.toString(),
                    latitude = locations.lastOrNull()?.latitude,
                    longitude = locations.lastOrNull()?.longitude,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertMedia(newMedia)
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            photoUri = CameraHelper.createImageUri(context)
            photoUri?.let { cameraLauncher.launch(it) }
        }
    }

    LaunchedEffect(tripId) {
        scope.launch {
            trip = repository.getTripById(tripId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dettaglio Viaggio") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Indietro")
                    }
                },
                actions = {
                    // Bottone modifica
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, "Modifica")
                    }
                    // Bottone camera
                    IconButton(onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Icon(Icons.Default.Add, "Foto")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNoteDialog = true }
            ) {
                Icon(Icons.Default.Add, "Aggiungi nota")
            }
        }
    ) { padding ->
        if (trip == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Mappa con percorso (50% schermo)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.5f)
                ) {
                    TripMapView(locations)
                }

                // Info viaggio (50% schermo - scrollabile)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.5f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    trip?.let { TripInfo(it, locations) }

                    // Media section
                    if (media.isNotEmpty()) {
                        MediaSection(media)
                    }
                }
            }
        }
    }

    // Dialog modifica destinazione
    if (showEditDialog) {
        EditTripDialog(
            trip = trip,
            onDismiss = { showEditDialog = false },
            onSave = { newDestination, newNotes ->
                scope.launch {
                    trip?.let {
                        repository.updateTrip(
                            it.copy(
                                destination = newDestination,
                                notes = newNotes
                            )
                        )
                        trip = repository.getTripById(tripId)
                    }
                    showEditDialog = false
                }
            }
        )
    }

    // Dialog aggiungi nota
    if (showNoteDialog) {
        AddNoteDialog(
            onDismiss = { showNoteDialog = false },
            onSave = { noteText ->
                scope.launch {
                    val newMedia = TripMedia(
                        tripId = tripId,
                        type = MediaType.NOTE,
                        content = noteText,
                        latitude = locations.lastOrNull()?.latitude,
                        longitude = locations.lastOrNull()?.longitude,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.insertMedia(newMedia)
                    showNoteDialog = false
                }
            }
        )
    }
}

@Composable
fun TripMapView(locations: List<TripLocation>) {
    if (locations.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nessun percorso registrato",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Centro mappa sulla prima posizione
    val firstLocation = locations.first()
    val startLatLng = LatLng(firstLocation.latitude, firstLocation.longitude)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLatLng, 14f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = false)
    ) {
        // Marker iniziale (verde)
        Marker(
            state = MarkerState(position = startLatLng),
            title = "Inizio",
            snippet = formatTime(locations.first().timestamp)
        )

        // Marker finale (rosso) se c'è più di una posizione
        if (locations.size > 1) {
            val lastLocation = locations.last()
            val endLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
            Marker(
                state = MarkerState(position = endLatLng),
                title = "Fine",
                snippet = formatTime(lastLocation.timestamp)
            )
        }

        // Polyline del percorso
        if (locations.size > 1) {
            Polyline(
                points = locations.map { LatLng(it.latitude, it.longitude) },
                color = androidx.compose.ui.graphics.Color.Blue,
                width = 10f
            )
        }
    }
}

@Composable
fun TripInfo(trip: Trip, locations: List<TripLocation>) {
    // Tipo viaggio
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when(trip.type) {
                    TripType.LOCAL -> "🏙️ Viaggio Locale"
                    TripType.DAY -> "🚗 Gita Giornaliera"
                    TripType.MULTI_DAY -> "✈️ Viaggio Multi-giorno"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (trip.isActive) {
                Text(
                    text = "🔴 In corso",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Stats
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Distanza
        StatCard(
            title = "Distanza",
            value = "%.2f km".format(trip.totalDistance),
            modifier = Modifier.weight(1f)
        )

        // Durata
        val duration = if (trip.endTime != null) {
            calculateDuration(trip.startTime, trip.endTime!!)
        } else {
            "In corso..."
        }
        StatCard(
            title = "Durata",
            value = duration,
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Punti registrati
        StatCard(
            title = "Punti GPS",
            value = locations.size.toString(),
            modifier = Modifier.weight(1f)
        )

        // Velocità media
        val avgSpeed = if (trip.endTime != null && trip.totalDistance > 0) {
            val hours = (trip.endTime!! - trip.startTime) / 3600000f
            val speed = trip.totalDistance / hours
            "%.1f km/h".format(speed)
        } else {
            "N/A"
        }
        StatCard(
            title = "Vel. Media",
            value = avgSpeed,
            modifier = Modifier.weight(1f)
        )
    }

    // Date
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow("Inizio:", formatDateTime(trip.startTime))
            if (trip.endTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("Fine:", formatDateTime(trip.endTime!!))
            }

            if (trip.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Note:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = trip.notes,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun MediaSection(media: List<TripMedia>) {
    Text(
        text = "Foto e Note",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Foto
    val photos = media.filter { it.type == MediaType.PHOTO }
    if (photos.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photos) { photo ->
                Card(
                    modifier = Modifier.size(120.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(Uri.parse(photo.content)),
                        contentDescription = "Foto",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    // Note
    val notes = media.filter { it.type == MediaType.NOTE }
    notes.forEach { note ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTime(note.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun EditTripDialog(
    trip: Trip?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var destination by remember { mutableStateOf(trip?.destination ?: "") }
    var notes by remember { mutableStateOf(trip?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica Viaggio") },
        text = {
            Column {
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destinazione") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(destination, notes) }) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var noteText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi Nota") },
        text = {
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Nota") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                placeholder = { Text("Es: Pranzo ottimo al ristorante XY") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(noteText) },
                enabled = noteText.isNotBlank()
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ITALIAN)
    return sdf.format(Date(timestamp))
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.ITALIAN)
    return sdf.format(Date(timestamp))
}

private fun calculateDuration(startTime: Long, endTime: Long): String {
    val durationMs = endTime - startTime
    val hours = (durationMs / 3600000).toInt()
    val minutes = ((durationMs % 3600000) / 60000).toInt()
    val seconds = ((durationMs % 60000) / 1000).toInt()

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}