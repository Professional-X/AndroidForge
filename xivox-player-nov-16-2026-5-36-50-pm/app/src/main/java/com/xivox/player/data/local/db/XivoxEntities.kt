package com.xivox.player.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val videoId: String,
    val mediaStoreId: Long,
    val uriString: String,
    val title: String,
    val durationMs: Long,
    val lastPositionMs: Long,
    val isCompleted: Boolean,
    val lastPlayedAt: Long,
    val resolution: String,
    val folderName: String
)

@Entity(tableName = "hidden_folders")
data class HiddenFolderEntity(
    @PrimaryKey val bucketId: String,
    val folderName: String,
    val folderPath: String,
    val hiddenAt: Long
)

@Entity(tableName = "favorite_videos")
data class FavoriteVideoEntity(
    @PrimaryKey val videoId: String,
    val addedAt: Long
)
