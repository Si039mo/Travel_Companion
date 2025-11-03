package com.example.travelcompanion.ui.triplist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.travelcompanion.data.local.database.AppDatabase
import com.example.travelcompanion.data.local.entity.Trip
import com.example.travelcompanion.data.local.entity.TripType
import com.example.travelcompanion.data.repository.TripRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    onNavigateBack: () -> Unit,
    onTripClick: (Long) -> Unit
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

    val trips by repository.getAllTrips().collectAsState(initial = emptyList())

    var selectedFilter by remember { mutableStateOf<TripType?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Trip?>(null) }

    val filteredTrips = if (selectedFilter != null) {
        trips.filter { it.type == selectedFilter }
    } else {
        trips
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("I Miei Viaggi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filtri
            FilterChips(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            // Lista viaggi
            if (filteredTrips.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTrips) { trip ->
                        TripCard(
                            trip = trip,
                            onClick = { onTripClick(trip.id) },
                            onDeleteClick = { showDeleteDialog = trip }
                        )
                    }
                }
            }
        }
    }

    // Dialog conferma eliminazione
    showDeleteDialog?.let { trip ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Elimina viaggio?") },
            text = { Text("Questa azione non può essere annullata.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.deleteTrip(trip)
                            showDeleteDialog = null
                        }
                    }
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
fun FilterChips(
    selectedFilter: TripType?,
    onFilterSelected: (TripType?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedFilter == null,
            onClick = { onFilterSelected(null) },
            label = { Text("Tutti") }
        )

        TripType.values().forEach { type ->
            FilterChip(
                selected = selectedFilter == type,
                onClick = { onFilterSelected(type) },
                label = {
                    Text(
                        when(type) {
                            TripType.LOCAL -> "🏙️ Locale"
                            TripType.DAY -> "🚗 Giornaliero"
                            TripType.MULTI_DAY -> "✈️ Multi-giorno"
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun TripCard(
    trip: Trip,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Tipo viaggio
                Text(
                    text = when(trip.type) {
                        TripType.LOCAL -> "🏙️ Locale"
                        TripType.DAY -> "🚗 Giornaliero"
                        TripType.MULTI_DAY -> "✈️ Multi-giorno"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Destinazione
                Text(
                    text = trip.destination,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Data e distanza
                Text(
                    text = formatDate(trip.startTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (trip.totalDistance > 0) {
                    Text(
                        text = "%.2f km".format(trip.totalDistance),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Badge se attivo
                if (trip.isActive) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🔴 In corso",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Bottone elimina
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Elimina",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🗺️",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nessun viaggio ancora",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Inizia il tuo primo viaggio dalla Home!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ITALIAN)
    return sdf.format(Date(timestamp))
}
