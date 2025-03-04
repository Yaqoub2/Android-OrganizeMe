package com.example.organizeme.DB

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Pickup::class, Day::class, Hour::class, Hour_Blueprint::class], version = 1)
abstract class Pickup_Database: RoomDatabase() {
    abstract fun dao(): Dao

    companion object {
        @Volatile
        private var INSTANCE: Pickup_Database? = null

        fun getDatabase(context: Context): Pickup_Database {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Pickup_Database::class.java,
                    "Pickup_Database"
                )
                    .createFromAsset("Pickup_Database.db")
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}