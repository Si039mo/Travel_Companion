package com.example.travelcompanion.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.travelcompanion.data.local.entity.Trip
import com.example.travelcompanion.data.local.entity.TripLocation
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ExportHelper {

    fun exportTripsToCSV(
        context: Context,
        trips: List<Trip>,
        locations: Map<Long, List<TripLocation>>
    ): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "trips_export_$timestamp.csv"

        val exportDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val file = File(exportDir, fileName)

        file.bufferedWriter().use { writer ->
            // Header
            writer.write("ID,Tipo,Destinazione,Data Inizio,Data Fine,Distanza (km),Durata (min),Punti GPS,Note\n")

            // Dati
            trips.forEach { trip ->
                val startDate = formatDate(trip.startTime)
                val endDate = if (trip.endTime != null) formatDate(trip.endTime!!) else "In corso"
                val duration = if (trip.endTime != null) {
                    ((trip.endTime!! - trip.startTime) / 60000).toString()
                } else {
                    "N/A"
                }
                val gpsPoints = locations[trip.id]?.size ?: 0

                writer.write(
                    "${trip.id}," +
                            "${trip.type}," +
                            "\"${trip.destination}\"," +
                            "$startDate," +
                            "$endDate," +
                            "${trip.totalDistance}," +
                            "$duration," +
                            "$gpsPoints," +
                            "\"${trip.notes}\"\n"
                )
            }
        }

        return file
    }

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Export Viaggi - Travel Companion")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Condividi Export"))
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALIAN)
        return sdf.format(Date(timestamp))
    }
}