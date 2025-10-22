package com.example.travelcompanion.data.repository

import com.example.travelcompanion.data.local.dao.*
import com.example.travelcompanion.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class TripRepository(
    private val tripDao: TripDao,
    private val locationDao: TripLocationDao,
    private val mediaDao: TripMediaDao
) {
    // Trip operations
    suspend fun insertTrip(trip: Trip): Long = tripDao.insert(trip)

    suspend fun updateTrip(trip: Trip) = tripDao.update(trip)

    suspend fun deleteTrip(trip: Trip) = tripDao.delete(trip)

    suspend fun getTripById(id: Long): Trip? = tripDao.getTripById(id)

    fun getAllTrips(): Flow<List<Trip>> = tripDao.getAllTrips()

    fun getTripsByType(type: TripType): Flow<List<Trip>> = tripDao.getTripsByType(type)

    suspend fun getActiveTrip(): Trip? = tripDao.getActiveTrip()

    suspend fun getTripsInRange(startTime: Long, endTime: Long): List<Trip> =
        tripDao.getTripsInRange(startTime, endTime)

    // Location operations
    suspend fun insertLocation(location: TripLocation) = locationDao.insert(location)

    suspend fun insertLocations(locations: List<TripLocation>) = locationDao.insertAll(locations)

    fun getLocationsForTrip(tripId: Long): Flow<List<TripLocation>> =
        locationDao.getLocationsForTrip(tripId)

    suspend fun getLocationsForTripSync(tripId: Long): List<TripLocation> =
        locationDao.getLocationsForTripSync(tripId)

    // Media operations
    suspend fun insertMedia(media: TripMedia): Long = mediaDao.insert(media)

    fun getMediaForTrip(tripId: Long): Flow<List<TripMedia>> =
        mediaDao.getMediaForTrip(tripId)

    fun getMediaByType(tripId: Long, type: MediaType): Flow<List<TripMedia>> =
        mediaDao.getMediaByType(tripId, type)

    suspend fun deleteMedia(media: TripMedia) = mediaDao.delete(media)
}
