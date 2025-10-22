package com.example.travelcompanion.data.local.dao

import androidx.room.*
import com.example.travelcompanion.data.local.entity.TripMedia
import com.example.travelcompanion.data.local.entity.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface TripMediaDao {
    @Insert
    suspend fun insert(media: TripMedia): Long

    @Query("SELECT * FROM trip_media WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getMediaForTrip(tripId: Long): Flow<List<TripMedia>>

    @Query("SELECT * FROM trip_media WHERE tripId = :tripId AND type = :type ORDER BY timestamp ASC")
    fun getMediaByType(tripId: Long, type: MediaType): Flow<List<TripMedia>>

    @Delete
    suspend fun delete(media: TripMedia)

    @Query("DELETE FROM trip_media WHERE tripId = :tripId")
    suspend fun deleteMediaForTrip(tripId: Long)
}
