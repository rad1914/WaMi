// @path: app/src/main/java/com/radwrld/resonance/data/db/AppDatabase.kt
package com.radwrld.resonance.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TrackResult::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackResultDao(): TrackResultDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "mood-db").build().also { instance = it }
            }
    }
}
