package com.example.travelcompanion.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.travelcompanion.data.local.dao.*
import com.example.travelcompanion.data.local.entity.*

@Database(
    entities = [Trip::class, TripLocation::class, TripMedia::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun tripLocationDao(): TripLocationDao
    abstract fun tripMediaDao(): TripMediaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "travel_companion_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
