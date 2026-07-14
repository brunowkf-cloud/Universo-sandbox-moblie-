package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UniverseEntity::class], version = 1, exportSchema = false)
abstract class UniverseDatabase : RoomDatabase() {
    abstract fun universeDao(): UniverseDao

    companion object {
        @Volatile
        private var INSTANCE: UniverseDatabase? = null

        fun getDatabase(context: Context): UniverseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UniverseDatabase::class.java,
                    "universe_sandbox_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
