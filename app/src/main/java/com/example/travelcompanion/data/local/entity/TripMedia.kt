package com.example.travelcompanion.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trip_media",
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class TripMedia(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val type: MediaType,
    val content: String, // URI per foto, testo per note
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long
)

enum class MediaType {
    PHOTO,
    NOTE
}
