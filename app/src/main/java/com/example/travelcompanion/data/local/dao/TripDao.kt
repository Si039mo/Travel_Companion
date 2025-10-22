package com.example.travelcompanion.data.local.dao

import androidx.room.*
import com.example.travelcompanion.data.local.entity.Trip
import com.example.travelcompanion.data.local.entity.TripType
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: Trip): Long

    @Update
    suspend fun update(trip: Trip)

    @Delete
    suspend fun delete(trip: Trip)

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): Trip?

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE type = :type ORDER BY startTime DESC")
    fun getTripsByType(type: TripType): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTrip(): Trip?

    @Query("SELECT * FROM trips WHERE startTime >= :startTime AND startTime <= :endTime")
    suspend fun getTripsInRange(startTime: Long, endTime: Long): List<Trip>

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun getTripCount(): Int
}
