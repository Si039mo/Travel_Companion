package com.example.travelcompanion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TripType,
    val destination: String,
    val startTime: Long,
    val endTime: Long? = null,
    val totalDistance: Float = 0f,
    val isActive: Boolean = true,
    val notes: String = ""
)

enum class TripType {
    LOCAL,      // Gita locale (in città)
    DAY,        // Gita giornaliera
    MULTI_DAY   // Viaggio multi-giorno
}
