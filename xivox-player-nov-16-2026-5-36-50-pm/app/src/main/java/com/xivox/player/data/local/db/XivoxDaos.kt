package com.xivox.player.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackHistoryDao {

    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAt DESC")
    fun observeAllHistory(): Flow<List<PlaybackHistoryEntity>>

    @Query(
        "SELECT * FROM playback_history " +
            "WHERE isCompleted = 0 AND lastPositionMs >= 5000 " +
            "ORDER BY lastPlayedAt DESC LIMIT 20"
    )
    fun observeContinueWatching(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE videoId = :videoId LIMIT 1")
    suspend fun getByVideoId(videoId: String): PlaybackHistoryEntity?

    @Query("SELECT * FROM playback_history WHERE uriString = :uriString LIMIT 1")
    suspend fun getByUri(uriString: String): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history WHERE videoId = :videoId")
    suspend fun deleteByVideoId(videoId: String)

    @Query("DELETE FROM playback_history")
    suspend fun clearAll()
}

@Dao
interface HiddenFolderDao {

    @Query("SELECT * FROM hidden_folders ORDER BY hiddenAt DESC")
    fun observeHiddenFolders(): Flow<List<HiddenFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideFolder(folder: HiddenFolderEntity)

    @Query("DELETE FROM hidden_folders WHERE bucketId = :bucketId")
    suspend fun unhideFolder(bucketId: String)

    @Query("DELETE FROM hidden_folders")
    suspend fun clearAll()
}

@Dao
interface FavoriteVideoDao {

    @Query("SELECT videoId FROM favorite_videos")
    fun observeFavoriteIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_videos WHERE videoId = :videoId)")
    suspend fun isFavorite(videoId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteVideoEntity)

    @Query("DELETE FROM favorite_videos WHERE videoId = :videoId")
    suspend fun removeFavorite(videoId: String)
}
