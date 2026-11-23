package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AccountEntity::class,
        BookmarkEntity::class,
        HistoryEntity::class,
        WhitelistSiteEntity::class,
        ShieldStatsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FireDragonDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun whitelistDao(): WhitelistDao
    abstract fun shieldStatsDao(): ShieldStatsDao

    companion object {
        @Volatile
        private var INSTANCE: FireDragonDatabase? = null

        fun getInstance(context: Context): FireDragonDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FireDragonDatabase::class.java,
                    "firedragon_browser.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
