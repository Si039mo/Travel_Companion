package com.example.travelcompanion.prediction


import com.example.travelcompanion.data.local.entity.Trip
import com.example.travelcompanion.data.local.entity.TripType
import com.example.travelcompanion.data.repository.TripRepository
import java.util.Calendar

/**
 * Modulo per analisi predittiva dei viaggi
 * Analizza pattern passati e genera previsioni future
 */
class PredictionModule(private val repository: TripRepository) {

    /**
     * Analizza i dati storici e genera previsioni complete
     */
    suspend fun generateCompletePrediction(): PredictionResult {
        val historicalData = analyzeHistoricalData()
        val forecast = generateForecast(historicalData)
        val recommendations = generateRecommendations(forecast, historicalData)

        return PredictionResult(
            historicalAnalysis = historicalData,
            forecast = forecast,
            recommendations = recommendations
        )
    }

    /**
     * Analizza gli ultimi 3 mesi di viaggi
     */
    private suspend fun analyzeHistoricalData(): HistoricalAnalysis {
        val calendar = Calendar.getInstance()
        val currentTime = calendar.timeInMillis

        // Ultimi 3 mesi
        calendar.add(Calendar.MONTH, -3)
        val threeMonthsAgo = calendar.timeInMillis

        val trips = repository.getTripsInRange(threeMonthsAgo, currentTime)
            .filter { !it.isActive } // Solo viaggi completati

        if (trips.isEmpty()) {
            return HistoricalAnalysis(
                totalTrips = 0,
                totalDistance = 0.0,
                avgTripsPerMonth = 0.0,
                avgDistancePerMonth = 0.0,
                tripsByType = emptyMap(),
                trend = TrendType.STABLE,
                dataQuality = DataQuality.INSUFFICIENT
            )
        }

        // Calcoli statistici
        val totalDistance = trips.sumOf { it.totalDistance.toDouble() }
        val avgTripsPerMonth = trips.size / 3.0
        val avgDistancePerMonth = totalDistance / 3.0

        // Viaggi per tipo
        val tripsByType = trips.groupBy { it.type }
            .mapValues { it.value.size }

        // Calcolo trend (confronto primo e ultimo mese)
        val trend = calculateTrend(trips)

        // Qualità dei dati
        val dataQuality = when {
            trips.size < 3 -> DataQuality.LOW
            trips.size < 10 -> DataQuality.MEDIUM
            else -> DataQuality.HIGH
        }

        return HistoricalAnalysis(
            totalTrips = trips.size,
            totalDistance = totalDistance,
            avgTripsPerMonth = avgTripsPerMonth,
            avgDistancePerMonth = avgDistancePerMonth,
            tripsByType = tripsByType,
            trend = trend,
            dataQuality = dataQuality
        )
    }

    /**
     * Calcola il trend confrontando primo e ultimo mese
     */
    private fun calculateTrend(trips: List<Trip>): TrendType {
        if (trips.size < 6) return TrendType.STABLE

        val sortedTrips = trips.sortedBy { it.startTime }

        // Primo terzo vs ultimo terzo
        val firstThird = sortedTrips.take(sortedTrips.size / 3)
        val lastThird = sortedTrips.takeLast(sortedTrips.size / 3)

        val firstAvg = firstThird.size.toDouble()
        val lastAvg = lastThird.size.toDouble()

        val change = ((lastAvg - firstAvg) / firstAvg) * 100

        return when {
            change > 20 -> TrendType.INCREASING
            change < -20 -> TrendType.DECREASING
            else -> TrendType.STABLE
        }
    }

    /**
     * Genera previsione per il prossimo mese
     */
    private fun generateForecast(analysis: HistoricalAnalysis): MonthlyForecast {
        if (analysis.dataQuality == DataQuality.INSUFFICIENT) {
            return MonthlyForecast(
                predictedTrips = 0,
                predictedDistance = 0.0,
                confidence = 0.0f,
                message = "Dati insufficienti per generare previsioni"
            )
        }

        // Fattore di aggiustamento basato sul trend
        val trendMultiplier = when (analysis.trend) {
            TrendType.INCREASING -> 1.15
            TrendType.DECREASING -> 0.85
            TrendType.STABLE -> 1.0
        }

        // Previsioni
        val predictedTrips = (analysis.avgTripsPerMonth * trendMultiplier).toInt()
            .coerceAtLeast(0)

        val predictedDistance = analysis.avgDistancePerMonth * trendMultiplier

        // Calcolo confidenza (basato su qualità dati e consistenza)
        val confidence = when (analysis.dataQuality) {
            DataQuality.HIGH -> 0.85f
            DataQuality.MEDIUM -> 0.65f
            DataQuality.LOW -> 0.40f
            DataQuality.INSUFFICIENT -> 0.0f
        }

        val message = when {
            analysis.trend == TrendType.INCREASING ->
                "📈 Trend in crescita! Continua così!"
            analysis.trend == TrendType.DECREASING ->
                "📉 Attività in calo rispetto ai mesi scorsi"
            else ->
                "📊 Attività stabile, mantieni il ritmo"
        }

        return MonthlyForecast(
            predictedTrips = predictedTrips,
            predictedDistance = predictedDistance,
            confidence = confidence,
            message = message
        )
    }

    /**
     * Genera raccomandazioni personalizzate
     */
    private fun generateRecommendations(
        forecast: MonthlyForecast,
        analysis: HistoricalAnalysis
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        // Raccomandazioni basate su trend decrescente
        if (analysis.trend == TrendType.DECREASING) {
            recommendations.add(
                Recommendation(
                    type = RecommendationType.ACTIVITY_BOOST,
                    title = "Aumenta la tua attività",
                    description = "Hai viaggiato meno negli ultimi mesi. Prova a pianificare " +
                            "una gita locale questo weekend!",
                    priority = Priority.HIGH
                )
            )
        }

        // Raccomandazioni basate su bassa distanza
        if (forecast.predictedDistance < 50) {
            recommendations.add(
                Recommendation(
                    type = RecommendationType.EXPLORATION,
                    title = "Esplora nuove destinazioni",
                    description = "La maggior parte dei tuoi viaggi sono locali. " +
                            "Che ne dici di una gita fuori città?",
                    priority = Priority.MEDIUM
                )
            )
        }

        // Raccomandazioni basate su pochi viaggi previsti
        if (forecast.predictedTrips < 3) {
            recommendations.add(
                Recommendation(
                    type = RecommendationType.GOAL_SETTING,
                    title = "Imposta un obiettivo",
                    description = "Prova a fare almeno 1 viaggio a settimana. " +
                            "Anche una passeggiata conta!",
                    priority = Priority.MEDIUM
                )
            )
        }

        // Raccomandazioni basate su tipo di viaggio predominante
        val mostCommonType = analysis.tripsByType.maxByOrNull { it.value }?.key
        if (mostCommonType == TripType.LOCAL && analysis.totalTrips > 5) {
            recommendations.add(
                Recommendation(
                    type = RecommendationType.VARIETY,
                    title = "Varia i tuoi viaggi",
                    description = "Fai principalmente viaggi locali. " +
                            "Prova un viaggio multi-giorno!",
                    priority = Priority.LOW
                )
            )
        }

        // Raccomandazioni positive
        if (analysis.trend == TrendType.INCREASING) {
            recommendations.add(
                Recommendation(
                    type = RecommendationType.POSITIVE_FEEDBACK,
                    title = "Ottimo lavoro! 🎉",
                    description = "La tua attività è in crescita. Continua così!",
                    priority = Priority.LOW
                )
            )
        }

        // Se non ci sono raccomandazioni, aggiungi una generica
        if (recommendations.isEmpty()) {
            recommendations.add(
                Recommendation(
                    type = RecommendationType.GENERAL,
                    title = "Mantieni il ritmo",
                    description = "Stai andando bene! Continua a registrare i tuoi viaggi.",
                    priority = Priority.LOW
                )
            )
        }

        return recommendations.sortedByDescending { it.priority }
    }
}

// ============= DATA CLASSES =============

data class PredictionResult(
    val historicalAnalysis: HistoricalAnalysis,
    val forecast: MonthlyForecast,
    val recommendations: List<Recommendation>
)

data class HistoricalAnalysis(
    val totalTrips: Int,
    val totalDistance: Double,
    val avgTripsPerMonth: Double,
    val avgDistancePerMonth: Double,
    val tripsByType: Map<TripType, Int>,
    val trend: TrendType,
    val dataQuality: DataQuality
)

data class MonthlyForecast(
    val predictedTrips: Int,
    val predictedDistance: Double,
    val confidence: Float, // 0.0 - 1.0
    val message: String
)

data class Recommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val priority: Priority
)

enum class TrendType {
    INCREASING,
    DECREASING,
    STABLE
}

enum class DataQuality {
    HIGH,      // 10+ viaggi
    MEDIUM,    // 3-9 viaggi
    LOW,       // 1-2 viaggi
    INSUFFICIENT // 0 viaggi
}

enum class RecommendationType {
    ACTIVITY_BOOST,
    EXPLORATION,
    GOAL_SETTING,
    VARIETY,
    POSITIVE_FEEDBACK,
    GENERAL
}

enum class Priority {
    HIGH,
    MEDIUM,
    LOW
}