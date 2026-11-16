package com.xivox.player.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaybackHistoryEntity::class,
        HiddenFolderEntity::class,
        FavoriteVideoEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class XivoxDatabase : RoomDatabase() {

    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun hiddenFolderDao(): HiddenFolderDao
    abstract fun favoriteVideoDao(): FavoriteVideoDao

    companion object {
        @Volatile
        private var INSTANCE: XivoxDatabase? = null

        fun getInstance(context: Context): XivoxDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    XivoxDatabase::class.java,
                    "xivox_player.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
