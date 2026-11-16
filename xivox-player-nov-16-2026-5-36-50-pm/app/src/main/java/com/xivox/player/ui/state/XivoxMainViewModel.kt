package com.xivox.player.ui.state

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xivox.player.XivoxApplication
import com.xivox.player.data.local.db.FavoriteVideoEntity
import com.xivox.player.data.local.db.HiddenFolderEntity
import com.xivox.player.data.local.db.PlaybackHistoryEntity
import com.xivox.player.domain.model.ContinueWatchingEntry
import com.xivox.player.domain.model.LayoutMode
import com.xivox.player.domain.model.ResumeBehavior
import com.xivox.player.domain.model.SortOption
import com.xivox.player.domain.model.VideoFolder
import com.xivox.player.domain.model.VideoItem
import com.xivox.player.domain.model.XivoxSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ResumePromptState(
    val video: VideoItem,
    val savedPositionMs: Long
)

data class MainUiState(
    val isScanning: Boolean = false,
    val hasStoragePermission: Boolean = false,
    val allVideos: List<VideoItem> = emptyList(),
    val visibleVideos: List<VideoItem> = emptyList(),
    val folders: List<VideoFolder> = emptyList(),
    val hiddenFolders: List<HiddenFolderEntity> = emptyList(),
    val continueWatching: List<ContinueWatchingEntry> = emptyList(),
    val recentlyAdded: List<VideoItem> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val historyByVideoId: Map<String, PlaybackHistoryEntity> = emptyMap(),
    val settings: XivoxSettings = XivoxSettings(),
    val searchQuery: String = "",
    val selectedBucketId: String? = null,
    val resumePrompt: ResumePromptState? = null,
    val scanError: String? = null
)

class XivoxMainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as XivoxApplication
    private val scanner = app.mediaScannerRepository
    private val prefsRepo = app.preferencesRepository
    private val historyDao = app.database.playbackHistoryDao()
    private val hiddenFolderDao = app.database.hiddenFolderDao()
    private val favoriteDao = app.database.favoriteVideoDao()

    private val rawVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val isScanning = MutableStateFlow(false)
    private val hasPermission = MutableStateFlow(false)
    private val searchQuery = MutableStateFlow("")
    private val selectedBucketId = MutableStateFlow<String?>(null)
    private val resumePrompt = MutableStateFlow<ResumePromptState?>(null)
    private val scanError = MutableStateFlow<String?>(null)

    val settingsFlow: StateFlow<XivoxSettings> = prefsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, XivoxSettings())

    private val historyFlow = historyDao.observeAllHistory()
    private val hiddenFoldersFlow = hiddenFolderDao.observeHiddenFolders()
    private val favoriteIdsFlow = favoriteDao.observeFavoriteIds().map { it.toSet() }

    val uiState: StateFlow<MainUiState> = combine(
        combine(rawVideos, isScanning, hasPermission, searchQuery, selectedBucketId) { v, scan, perm, q, bucket ->
            Tuple5(v, scan, perm, q, bucket)
        },
        combine(historyFlow, hiddenFoldersFlow, favoriteIdsFlow, settingsFlow) { hist, hidden, favs, prefs ->
            Tuple4(hist, hidden, favs, prefs)
        },
        resumePrompt,
        scanError
    ) { scanTuple, dbTuple, prompt, err ->
        val (videos, scanning, perm, query, bucket) = scanTuple
        val (historyList, hiddenList, favIds, settings) = dbTuple

        val hiddenSet = hiddenList.map { it.bucketId }.toSet()
        val unhiddenVideos = videos.filterNot { it.bucketId in hiddenSet }

        val folders = scanner.groupIntoFolders(unhiddenVideos)

        val sortedVideos = when (settings.sortOption) {
            SortOption.NAME -> unhiddenVideos.sortedBy { it.title.lowercase() }
            SortOption.DATE -> unhiddenVideos.sortedByDescending { it.dateAddedSec }
            SortOption.DURATION -> unhiddenVideos.sortedByDescending { it.durationMs }
            SortOption.SIZE -> unhiddenVideos.sortedByDescending { it.sizeBytes }
        }

        val trimmedQuery = query.trim().lowercase()
        val filtered = sortedVideos.filter { video ->
            val matchesBucket = bucket == null || video.bucketId == bucket
            val matchesQuery = trimmedQuery.isEmpty() ||
                video.title.lowercase().contains(trimmedQuery) ||
                video.displayName.lowercase().contains(trimmedQuery) ||
                video.folderName.lowercase().contains(trimmedQuery)
            matchesBucket && matchesQuery
        }

        val historyMap = historyList.associateBy { it.videoId }
        val continueList = historyList
            .filter { !it.isCompleted && it.lastPositionMs >= 5_000L }
            .map {
                ContinueWatchingEntry(
                    videoId = it.videoId,
                    uriString = it.uriString,
                    title = it.title,
                    durationMs = it.durationMs,
                    lastPositionMs = it.lastPositionMs,
                    isCompleted = it.isCompleted,
                    lastPlayedAt = it.lastPlayedAt,
                    resolution = it.resolution,
                    folderName = it.folderName
                )
            }

        MainUiState(
            isScanning = scanning,
            hasStoragePermission = perm,
            allVideos = sortedVideos,
            visibleVideos = filtered,
            folders = folders,
            hiddenFolders = hiddenList,
            continueWatching = continueList,
            recentlyAdded = unhiddenVideos.sortedByDescending { it.dateAddedSec }.take(12),
            favoriteIds = favIds,
            historyByVideoId = historyMap,
            settings = settings,
            searchQuery = query,
            selectedBucketId = bucket,
            resumePrompt = prompt,
            scanError = err
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    fun setStoragePermissionGranted(granted: Boolean) {
        hasPermission.value = granted
        if (granted) {
            refreshLibrary()
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            isScanning.value = true
            scanError.value = null
            runCatching {
                val settings = prefsRepo.settingsFlow.first()
                scanner.scanDeviceVideos(
                    safTreeUris = settings.safTreeUris,
                    minDurationSeconds = settings.minVideoDurationSeconds
                )
            }.onSuccess { list ->
                rawVideos.value = list
            }.onFailure { throwable ->
                scanError.value = throwable.localizedMessage ?: "Unable to scan device media."
            }
            isScanning.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectFolder(bucketId: String?) {
        selectedBucketId.value = bucketId
    }

    fun setSortOption(option: SortOption) {
        viewModelScope.launch { prefsRepo.setSortOption(option) }
    }

    fun setLayoutMode(mode: LayoutMode) {
        viewModelScope.launch { prefsRepo.setLayoutMode(mode) }
    }

    fun toggleFavorite(videoId: String) {
        viewModelScope.launch {
            if (favoriteDao.isFavorite(videoId)) {
                favoriteDao.removeFavorite(videoId)
            } else {
                favoriteDao.addFavorite(FavoriteVideoEntity(videoId, System.currentTimeMillis()))
            }
        }
    }

    fun hideFolder(folder: VideoFolder) {
        viewModelScope.launch {
            hiddenFolderDao.hideFolder(
                HiddenFolderEntity(
                    bucketId = folder.bucketId,
                    folderName = folder.name,
                    folderPath = folder.path,
                    hiddenAt = System.currentTimeMillis()
                )
            )
            if (selectedBucketId.value == folder.bucketId) {
                selectedBucketId.value = null
            }
        }
    }

    fun unhideFolder(bucketId: String) {
        viewModelScope.launch {
            hiddenFolderDao.unhideFolder(bucketId)
        }
    }

    fun addSafFolder(treeUri: Uri) {
        viewModelScope.launch {
            prefsRepo.addSafTreeUri(treeUri.toString())
            refreshLibrary()
        }
    }

    fun removeSafFolder(treeUriString: String) {
        viewModelScope.launch {
            prefsRepo.removeSafTreeUri(treeUriString)
            refreshLibrary()
        }
    }

    suspend fun resolveUriToVideoItem(uri: Uri): VideoItem {
        val existing = rawVideos.value.firstOrNull { it.uri == uri }
        return existing ?: scanner.resolveSingleUri(uri)
    }

    fun requestPlayVideo(
        video: VideoItem,
        onLaunchPlayer: (VideoItem, Long) -> Unit
    ) {
        viewModelScope.launch {
            val saved = historyDao.getByVideoId(video.id) ?: historyDao.getByUri(video.uri.toString())
            val settings = prefsRepo.settingsFlow.first()
            val validResumePos = saved?.takeIf {
                !it.isCompleted && it.lastPositionMs >= 5_000L &&
                    (it.durationMs <= 0L || it.lastPositionMs < it.durationMs - 5_000L)
            }?.lastPositionMs ?: 0L

            if (validResumePos <= 0L) {
                onLaunchPlayer(video, 0L)
                return@launch
            }

            when (settings.resumeBehavior) {
                ResumeBehavior.ALWAYS_RESUME -> onLaunchPlayer(video, validResumePos)
                ResumeBehavior.START_OVER -> onLaunchPlayer(video, 0L)
                ResumeBehavior.ASK -> {
                    resumePrompt.value = ResumePromptState(video, validResumePos)
                }
            }
        }
    }

    fun dismissResumePrompt() {
        resumePrompt.value = null
    }

    fun removeContinueWatchingEntry(videoId: String) {
        viewModelScope.launch {
            historyDao.deleteByVideoId(videoId)
        }
    }

    fun clearPlaybackHistory() {
        viewModelScope.launch {
            historyDao.clearAll()
        }
    }

    fun getPreferencesRepository() = prefsRepo

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
    private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
}
