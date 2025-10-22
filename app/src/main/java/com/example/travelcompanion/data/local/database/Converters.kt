package com.example.travelcompanion.data.local.database

import androidx.room.TypeConverter
import com.example.travelcompanion.data.local.entity.MediaType
import com.example.travelcompanion.data.local.entity.TripType

class Converters {
    @TypeConverter
    fun fromTripType(value: TripType): String = value.name

    @TypeConverter
    fun toTripType(value: String): TripType = TripType.valueOf(value)

    @TypeConverter
    fun fromMediaType(value: MediaType): String = value.name

    @TypeConverter
    fun toMediaType(value: String): MediaType = MediaType.valueOf(value)
}
