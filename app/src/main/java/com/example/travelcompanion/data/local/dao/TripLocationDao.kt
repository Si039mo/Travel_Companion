package com.example.travelcompanion.data.local.dao

import androidx.room.*
import com.example.travelcompanion.data.local.entity.TripLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface TripLocationDao {
    @Insert
    suspend fun insert(location: TripLocation)

    @Insert
    suspend fun insertAll(locations: List<TripLocation>)

    @Query("SELECT * FROM trip_locations WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getLocationsForTrip(tripId: Long): Flow<List<TripLocation>>

    @Query("SELECT * FROM trip_locations WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getLocationsForTripSync(tripId: Long): List<TripLocation>

    @Query("DELETE FROM trip_locations WHERE tripId = :tripId")
    suspend fun deleteLocationsForTrip(tripId: Long)

    @Query("SELECT COUNT(*) FROM trip_locations WHERE tripId = :tripId")
    suspend fun getLocationCount(tripId: Long): Int
}
