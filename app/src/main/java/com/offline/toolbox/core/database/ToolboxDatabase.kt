package com.offline.toolbox.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FavoriteToolEntity::class, RecentToolEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ToolboxDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao

    companion object {
        @Volatile
        private var INSTANCE: ToolboxDatabase? = null

        fun getInstance(context: Context): ToolboxDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ToolboxDatabase::class.java,
                    "offline_toolbox.db"
                ).fallbackToDestructiveMigration().build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
