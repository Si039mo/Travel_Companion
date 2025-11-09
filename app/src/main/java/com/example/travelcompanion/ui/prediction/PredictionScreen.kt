package com.example.travelcompanion.ui.prediction


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.travelcompanion.data.local.database.AppDatabase
import com.example.travelcompanion.data.repository.TripRepository
import com.example.travelcompanion.prediction.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionScreen(
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

    val predictionModule = remember { PredictionModule(repository) }

    var predictionResult by remember { mutableStateOf<PredictionResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Carica previsioni all'avvio
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                predictionResult = predictionModule.generateCompletePrediction()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Previsioni Viaggi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Indietro")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                try {
                                    predictionResult = predictionModule.generateCompletePrediction()
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, "Aggiorna")
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
            when {
                isLoading -> {
                    LoadingState()
                }
                predictionResult == null -> {
                    ErrorState()
                }
                else -> {
                    PredictionContent(predictionResult!!)
                }
            }
        }
    }
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Analisi dati in corso...")
        }
    }
}

@Composable
fun ErrorState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Errore nel caricamento",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun PredictionContent(result: PredictionResult) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sezione Previsioni
        ForecastSection(result.forecast, result.historicalAnalysis)

        Spacer(modifier = Modifier.height(8.dp))

        // Sezione Analisi Storica
        HistoricalSection(result.historicalAnalysis)

        Spacer(modifier = Modifier.height(8.dp))

        // Sezione Raccomandazioni
        RecommendationsSection(result.recommendations)
    }
}

@Composable
fun ForecastSection(forecast: MonthlyForecast, analysis: HistoricalAnalysis) {
    Text(
        text = "📈 Previsioni Prossimo Mese",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Messaggio principale
            Text(
                text = forecast.message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Divider()

            // Viaggi previsti
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Viaggi Previsti",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Media attuale: %.1f/mese".format(analysis.avgTripsPerMonth),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = "${forecast.predictedTrips}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Distanza prevista
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Distanza Prevista",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Media attuale: %.1f km/mese".format(analysis.avgDistancePerMonth),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = "%.0f km".format(forecast.predictedDistance),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Barra confidenza
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Affidabilità Previsione",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${(forecast.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = forecast.confidence,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = when {
                        forecast.confidence >= 0.7f -> MaterialTheme.colorScheme.tertiary
                        forecast.confidence >= 0.4f -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
        }
    }
}

@Composable
fun HistoricalSection(analysis: HistoricalAnalysis) {
    Text(
        text = "📊 Analisi Ultimi 3 Mesi",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    // Overview Cards
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SmallStatCard(
            title = "Viaggi",
            value = analysis.totalTrips.toString(),
            icon = "🗺️",
            modifier = Modifier.weight(1f)
        )
        SmallStatCard(
            title = "Totale",
            value = "%.0f km".format(analysis.totalDistance),
            icon = "📏",
            modifier = Modifier.weight(1f)
        )
    }

    // Trend Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (analysis.trend) {
                TrendType.INCREASING -> MaterialTheme.colorScheme.tertiaryContainer
                TrendType.DECREASING -> MaterialTheme.colorScheme.errorContainer
                TrendType.STABLE -> MaterialTheme.colorScheme.secondaryContainer
            }
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
                    text = "Trend",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (analysis.trend) {
                        TrendType.INCREASING -> "In Crescita"
                        TrendType.DECREASING -> "In Calo"
                        TrendType.STABLE -> "Stabile"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = when (analysis.trend) {
                    TrendType.INCREASING -> "📈"
                    TrendType.DECREASING -> "📉"
                    TrendType.STABLE -> "📊"
                },
                style = MaterialTheme.typography.displayMedium
            )
        }
    }

    // Viaggi per tipo
    if (analysis.tripsByType.isNotEmpty()) {
        Text(
            text = "Distribuzione per Tipo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        analysis.tripsByType.forEach { (type, count) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (type) {
                            com.example.travelcompanion.data.local.entity.TripType.LOCAL -> "🏙️ Locale"
                            com.example.travelcompanion.data.local.entity.TripType.DAY -> "🚗 Giornaliero"
                            com.example.travelcompanion.data.local.entity.TripType.MULTI_DAY -> "✈️ Multi-giorno"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "$count ${if (count == 1) "viaggio" else "viaggi"}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendationsSection(recommendations: List<Recommendation>) {
    Text(
        text = "💡 Raccomandazioni",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    if (recommendations.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessuna raccomandazione al momento",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        recommendations.forEach { recommendation ->
            RecommendationCard(recommendation)
        }
    }
}

@Composable
fun RecommendationCard(recommendation: Recommendation) {
    val containerColor = when (recommendation.priority) {
        Priority.HIGH -> MaterialTheme.colorScheme.errorContainer
        Priority.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
        Priority.LOW -> MaterialTheme.colorScheme.tertiaryContainer
    }

    val icon = when (recommendation.type) {
        RecommendationType.ACTIVITY_BOOST -> "🚀"
        RecommendationType.EXPLORATION -> "🗺️"
        RecommendationType.GOAL_SETTING -> "🎯"
        RecommendationType.VARIETY -> "🌈"
        RecommendationType.POSITIVE_FEEDBACK -> "⭐"
        RecommendationType.GENERAL -> "💬"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SmallStatCard(
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
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}