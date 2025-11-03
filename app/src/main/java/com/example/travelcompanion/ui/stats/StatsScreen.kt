package com.example.travelcompanion.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material.icons.filled.Share
import com.example.travelcompanion.data.local.entity.TripLocation
import com.example.travelcompanion.util.ExportHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
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

    val trips by repository.getAllTrips().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistiche") },
                actions = {
                    // Bottone Export
                    IconButton(
                        onClick = {
                            scope.launch {
                                try {
                                    // Raccogli tutte le location
                                    val locationsMap = mutableMapOf<Long, List<TripLocation>>()
                                    trips.forEach { trip ->
                                        val locations = repository.getLocationsForTripSync(trip.id)
                                        locationsMap[trip.id] = locations
                                    }

                                    // Export
                                    val file = ExportHelper.exportTripsToCSV(
                                        context,
                                        trips,
                                        locationsMap
                                    )

                                    // Share
                                    ExportHelper.shareFile(context, file)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        enabled = trips.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Share, "Esporta")
                    }
                }
            )
        }
    ) { padding ->
        if (trips.isEmpty()) {
            EmptyStats(modifier = Modifier.padding(padding))
        } else {
            StatsContent(
                trips = trips,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
fun EmptyStats(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📊",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nessuna statistica",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Inizia a fare viaggi per vedere le tue statistiche!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatsContent(
    trips: List<Trip>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overview generale
        Text(
            text = "Panoramica",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OverviewCards(trips)

        Spacer(modifier = Modifier.height(8.dp))

        // Per tipo di viaggio
        Text(
            text = "Per Tipo di Viaggio",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        TripsByType(trips)

        Spacer(modifier = Modifier.height(8.dp))

        // Record
        Text(
            text = "Record Personali",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        PersonalRecords(trips)
    }
}

@Composable
fun OverviewCards(trips: List<Trip>) {
    val totalTrips = trips.size
    val totalDistance = trips.sumOf { it.totalDistance.toDouble() }
    val completedTrips = trips.count { !it.isActive }
    val activeTrips = trips.count { it.isActive }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            title = "Viaggi",
            value = totalTrips.toString(),
            icon = "🗺️",
            modifier = Modifier.weight(1f)
        )

        StatCard(
            title = "Totale",
            value = "%.1f km".format(totalDistance),
            icon = "📏",
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            title = "Completati",
            value = completedTrips.toString(),
            icon = "✅",
            modifier = Modifier.weight(1f)
        )

        StatCard(
            title = "In Corso",
            value = activeTrips.toString(),
            icon = "🔴",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TripsByType(trips: List<Trip>) {
    TripType.values().forEach { type ->
        val tripsOfType = trips.filter { it.type == type }
        val count = tripsOfType.size
        val distance = tripsOfType.sumOf { it.totalDistance.toDouble() }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when(type) {
                            TripType.LOCAL -> "🏙️ Locale"
                            TripType.DAY -> "🚗 Giornaliero"
                            TripType.MULTI_DAY -> "✈️ Multi-giorno"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$count ${if (count == 1) "viaggio" else "viaggi"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Text(
                    text = "%.1f km".format(distance),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PersonalRecords(trips: List<Trip>) {
    val longestTrip = trips.maxByOrNull { it.totalDistance }
    val completedTrips = trips.filter { !it.isActive }
    val longestDuration = completedTrips.maxByOrNull {
        (it.endTime ?: it.startTime) - it.startTime
    }

    // Viaggio più lungo
    longestTrip?.let { trip ->
        RecordCard(
            title = "🏆 Viaggio più lungo",
            value = "%.2f km".format(trip.totalDistance),
            subtitle = trip.destination
        )
    }

    // Viaggio più duraturo
    longestDuration?.let { trip ->
        val duration = (trip.endTime ?: trip.startTime) - trip.startTime
        val hours = (duration / 3600000).toInt()
        val minutes = ((duration % 3600000) / 60000).toInt()

        RecordCard(
            title = "⏱️ Viaggio più lungo (tempo)",
            value = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m",
            subtitle = trip.destination
        )
    }

    // Media distanza
    if (trips.isNotEmpty()) {
        val avgDistance = trips.sumOf { it.totalDistance.toDouble() } / trips.size
        RecordCard(
            title = "📊 Distanza media",
            value = "%.2f km".format(avgDistance),
            subtitle = "per viaggio"
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: String,
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
                text = icon,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecordCard(
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
