package com.xivox.player.ui.screens.player

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import com.xivox.player.XivoxApplication
import com.xivox.player.data.local.db.PlaybackHistoryEntity
import com.xivox.player.data.subtitle.SubtitleParser
import com.xivox.player.domain.model.ExternalSubtitleFile
import com.xivox.player.domain.model.PlayerTrackOption
import com.xivox.player.domain.model.SubtitleCue
import com.xivox.player.domain.model.VideoItem
import com.xivox.player.domain.model.XivoxSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class VideoResizeMode(val label: String) {
    FIT("Fit"),
    ZOOM("Zoom"),
    FILL("Stretch")
}

data class PlayerUiState(
    val currentVideo: VideoItem? = null,
    val playlist: List<VideoItem> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = true,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isTemp2xActive: Boolean = false,
    val resizeMode: VideoResizeMode = VideoResizeMode.FIT,

    // Subtitles & Audio
    val subtitlesEnabled: Boolean = true,
    val embeddedAudioTracks: List<PlayerTrackOption> = emptyList(),
    val embeddedSubtitleTracks: List<PlayerTrackOption> = emptyList(),
    val externalSubtitles: List<ExternalSubtitleFile> = emptyList(),
    val activeExternalSubtitle: ExternalSubtitleFile? = null,
    val externalSubtitleCues: List<SubtitleCue> = emptyList(),
    val activeSubtitleText: String? = null,

    // Error handling
    val errorMessage: String? = null
) {
    val hasPrevious: Boolean get() = currentIndex > 0
    val hasNext: Boolean get() = currentIndex in 0 until playlist.lastIndex
}

@OptIn(UnstableApi::class)
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as XivoxApplication
    private val historyDao = app.database.playbackHistoryDao()
    private val scanner = app.mediaScannerRepository
    private val prefsRepo = app.preferencesRepository

    val settingsFlow: StateFlow<XivoxSettings> = prefsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, XivoxSettings())

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val exoPlayer: ExoPlayer by lazy { createExoPlayer(app) }

    private var tickerJob: Job? = null
    private var lastPersistedTimeMs: Long = 0L
    private var speedBefore2x: Float = 1.0f

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val dur = exoPlayer.duration.takeIf { it != C.TIME_UNSET && it > 0L }
                ?: _uiState.value.currentVideo?.durationMs ?: 0L
            val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
            _uiState.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    durationMs = dur,
                    currentPositionMs = pos,
                    bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                )
            }
            if (playbackState == Player.STATE_ENDED) {
                persistPlaybackProgress(forceCompleted = true)
            } else if (playbackState == Player.STATE_READY) {
                refreshActiveSubtitleCue(pos)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (!isPlaying) {
                persistPlaybackProgress(forceCompleted = false)
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            extractTracks(tracks)
        }

        override fun onPlayerError(error: PlaybackException) {
            val readable = when (error.errorCode) {
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
                    "This video uses an unsupported codec or hardware profile on this device (${error.errorCodeName})."
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE ->
                    "The local video file could not be read or was moved."
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
                    "Unsupported or damaged media container (${error.errorCodeName})."
                else -> error.localizedMessage ?: "Unable to play this media file (${error.errorCodeName})."
            }
            _uiState.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = readable
                )
            }
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        startPositionTicker()
    }

    private fun createExoPlayer(context: Context): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(
            DefaultDataSource.Factory(context),
            extractorsFactory
        )

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    fun loadPlaylistAndPlay(
        playlist: List<VideoItem>,
        startIndex: Int,
        startPositionMs: Long
    ) {
        if (playlist.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, playlist.lastIndex)
        val video = playlist[safeIndex]

        viewModelScope.launch {
            val settings = prefsRepo.settingsFlow.first()
            _uiState.update {
                it.copy(
                    playlist = playlist,
                    currentIndex = safeIndex,
                    currentVideo = video,
                    playbackSpeed = settings.defaultPlaybackSpeed,
                    subtitlesEnabled = settings.subtitlesEnabledByDefault,
                    externalSubtitles = emptyList(),
                    activeExternalSubtitle = null,
                    externalSubtitleCues = emptyList(),
                    activeSubtitleText = null,
                    errorMessage = null,
                    isBuffering = true
                )
            }

            prepareExoPlayerForVideo(video, startPositionMs, settings)
        }
    }

    private suspend fun prepareExoPlayerForVideo(
        video: VideoItem,
        startPositionMs: Long,
        settings: XivoxSettings
    ) {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        val mediaItem = MediaItem.Builder()
            .setUri(video.uri)
            .setMimeType(video.mimeType)
            .build()

        exoPlayer.setPlaybackSpeed(settings.defaultPlaybackSpeed)
        exoPlayer.setMediaItem(mediaItem, startPositionMs.coerceAtLeast(0L))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        // Discover companion subtitles offline
        val externalSubs = scanner.discoverCompanionSubtitles(video, settings.safTreeUris)
        _uiState.update { it.copy(externalSubtitles = externalSubs) }

        if (settings.autoSelectSubtitles && settings.subtitlesEnabledByDefault && externalSubs.isNotEmpty()) {
            val preferred = externalSubs.firstOrNull {
                it.languageLabel.contains(settings.preferredSubtitleLanguage, ignoreCase = true) ||
                    it.displayName.contains(".${settings.preferredSubtitleLanguage}.", ignoreCase = true)
            } ?: externalSubs.first()
            selectExternalSubtitle(preferred)
        }
    }

    fun playNext() {
        val state = _uiState.value
        if (!state.hasNext) return
        persistPlaybackProgress(forceCompleted = false)
        loadPlaylistAndPlay(state.playlist, state.currentIndex + 1, 0L)
    }

    fun playPrevious() {
        val state = _uiState.value
        if (!state.hasPrevious) return
        persistPlaybackProgress(forceCompleted = false)
        loadPlaylistAndPlay(state.playlist, state.currentIndex - 1, 0L)
    }

    fun retryCurrentVideo() {
        val state = _uiState.value
        val video = state.currentVideo ?: return
        viewModelScope.launch {
            val settings = prefsRepo.settingsFlow.first()
            _uiState.update { it.copy(errorMessage = null, isBuffering = true) }
            prepareExoPlayerForVideo(video, state.currentPositionMs, settings)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0L)
            exoPlayer.play()
            return
        }
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun pausePlayback() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
        persistPlaybackProgress(forceCompleted = false)
    }

    fun seekTo(positionMs: Long) {
        val dur = _uiState.value.durationMs.coerceAtLeast(1L)
        val clamped = positionMs.coerceIn(0L, dur)
        exoPlayer.seekTo(clamped)
        _uiState.update { it.copy(currentPositionMs = clamped) }
        refreshActiveSubtitleCue(clamped)
        persistPlaybackProgress(forceCompleted = false)
    }

    fun seekBy(deltaMs: Long) {
        val target = (exoPlayer.currentPosition + deltaMs).coerceAtLeast(0L)
        seekTo(target)
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 2.5f)
        exoPlayer.setPlaybackSpeed(clamped)
        _uiState.update { it.copy(playbackSpeed = clamped, isTemp2xActive = false) }
    }

    fun startTemporary2xSpeed() {
        if (_uiState.value.isTemp2xActive) return
        speedBefore2x = _uiState.value.playbackSpeed
        exoPlayer.setPlaybackSpeed(2.0f)
        _uiState.update { it.copy(playbackSpeed = 2.0f, isTemp2xActive = true) }
    }

    fun stopTemporary2xSpeed() {
        if (!_uiState.value.isTemp2xActive) return
        exoPlayer.setPlaybackSpeed(speedBefore2x)
        _uiState.update { it.copy(playbackSpeed = speedBefore2x, isTemp2xActive = false) }
    }

    fun cycleResizeMode() {
        val entries = VideoResizeMode.entries
        val next = entries[(_uiState.value.resizeMode.ordinal + 1) % entries.size]
        _uiState.update { it.copy(resizeMode = next) }
    }

    fun toggleSubtitles() {
        val next = !_uiState.value.subtitlesEnabled
        _uiState.update {
            it.copy(
                subtitlesEnabled = next,
                activeSubtitleText = if (next) it.activeSubtitleText else null
            )
        }
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !next || _uiState.value.activeExternalSubtitle != null)
            .build()
        if (next) {
            refreshActiveSubtitleCue(exoPlayer.currentPosition)
        }
    }

    fun selectExternalSubtitle(sub: ExternalSubtitleFile) {
        viewModelScope.launch {
            val cues = SubtitleParser.parseFromUri(app, sub.uri, sub.displayName)
            // Disable embedded text track when rendering external subtitle overlay
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            _uiState.update {
                it.copy(
                    subtitlesEnabled = true,
                    activeExternalSubtitle = sub,
                    externalSubtitleCues = cues
                )
            }
            refreshActiveSubtitleCue(exoPlayer.currentPosition)
        }
    }

    fun loadManualExternalSubtitle(uri: Uri) {
        viewModelScope.launch {
            var name = "subtitle.srt"
            runCatching {
                app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) name = cursor.getString(idx) ?: name
                    }
                }
            }
            val subFile = ExternalSubtitleFile(
                uri = uri,
                displayName = name,
                languageLabel = "Custom",
                mimeType = scanner.guessSubtitleMime(name.substringAfterLast('.', "srt"))
            )
            _uiState.update { state ->
                val updatedList = (listOf(subFile) + state.externalSubtitles).distinctBy { it.uri }
                state.copy(externalSubtitles = updatedList)
            }
            selectExternalSubtitle(subFile)
        }
    }

    fun selectEmbeddedSubtitleTrack(option: PlayerTrackOption) {
        val groups = exoPlayer.currentTracks.groups
        val group = groups.getOrNull(option.groupIndex) ?: return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
            .build()
        _uiState.update {
            it.copy(
                subtitlesEnabled = true,
                activeExternalSubtitle = null,
                externalSubtitleCues = emptyList(),
                activeSubtitleText = null
            )
        }
    }

    fun disableAllSubtitles() {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        _uiState.update {
            it.copy(
                subtitlesEnabled = false,
                activeExternalSubtitle = null,
                activeSubtitleText = null
            )
        }
    }

    fun selectAudioTrack(option: PlayerTrackOption) {
        val groups = exoPlayer.currentTracks.groups
        val group = groups.getOrNull(option.groupIndex) ?: return
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
            .build()
    }

    fun updateSubtitleFontSize(sp: Float) = viewModelScope.launch { prefsRepo.setSubtitleFontSize(sp) }
    fun updateSubtitleColor(argb: Long) = viewModelScope.launch { prefsRepo.setSubtitleTextColor(argb) }
    fun updateSubtitleBgOpacity(opacity: Float) = viewModelScope.launch { prefsRepo.setSubtitleBackgroundOpacity(opacity) }
    fun updateSubtitlePosition(fraction: Float) = viewModelScope.launch { prefsRepo.setSubtitleBottomOffset(fraction) }
    fun updateSubtitleDelay(delayMs: Long) = viewModelScope.launch {
        prefsRepo.setSubtitleDelayMs(delayMs)
        refreshActiveSubtitleCue(exoPlayer.currentPosition)
    }

    private fun extractTracks(tracks: Tracks) {
        val audioList = mutableListOf<PlayerTrackOption>()
        val textList = mutableListOf<PlayerTrackOption>()

        tracks.groups.forEachIndexed { gIdx, group ->
            when (group.type) {
                C.TRACK_TYPE_AUDIO -> {
                    for (tIdx in 0 until group.length) {
                        val format = group.getTrackFormat(tIdx)
                        val lang = format.language ?: "und"
                        val label = format.label ?: lang.uppercase()
                        audioList.add(
                            PlayerTrackOption(
                                groupIndex = gIdx,
                                trackIndex = tIdx,
                                label = if (format.channelCount > 0) "$label (${format.channelCount}ch)" else label,
                                language = format.language,
                                isSelected = group.isTrackSelected(tIdx),
                                isSupported = group.isTrackSupported(tIdx)
                            )
                        )
                    }
                }
                C.TRACK_TYPE_TEXT -> {
                    for (tIdx in 0 until group.length) {
                        val format = group.getTrackFormat(tIdx)
                        val lang = format.language ?: "und"
                        val label = format.label ?: lang.uppercase()
                        textList.add(
                            PlayerTrackOption(
                                groupIndex = gIdx,
                                trackIndex = tIdx,
                                label = label,
                                language = format.language,
                                isSelected = group.isTrackSelected(tIdx),
                                isSupported = group.isTrackSupported(tIdx)
                            )
                        )
                    }
                }
            }
        }

        _uiState.update {
            it.copy(
                embeddedAudioTracks = audioList,
                embeddedSubtitleTracks = textList
            )
        }
    }

    private fun startPositionTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration.takeIf { it != C.TIME_UNSET && it > 0L }
                    ?: _uiState.value.durationMs
                val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)

                _uiState.update {
                    it.copy(
                        currentPositionMs = pos,
                        durationMs = dur,
                        bufferedPositionMs = buf
                    )
                }
                refreshActiveSubtitleCue(pos)

                val now = System.currentTimeMillis()
                if (exoPlayer.isPlaying && now - lastPersistedTimeMs >= 2000L) {
                    lastPersistedTimeMs = now
                    persistPlaybackProgress(forceCompleted = false)
                }
                delay(200L)
            }
        }
    }

    private fun refreshActiveSubtitleCue(positionMs: Long) {
        val state = _uiState.value
        if (!state.subtitlesEnabled || state.externalSubtitleCues.isEmpty()) {
            if (state.activeSubtitleText != null) {
                _uiState.update { it.copy(activeSubtitleText = null) }
            }
            return
        }
        val delayMs = settingsFlow.value.subtitleDelayMs
        val cue = SubtitleParser.findActiveCue(state.externalSubtitleCues, positionMs, delayMs)
        val text = cue?.text
        if (text != state.activeSubtitleText) {
            _uiState.update { it.copy(activeSubtitleText = text) }
        }
    }

    fun persistPlaybackProgress(forceCompleted: Boolean) {
        val video = _uiState.value.currentVideo ?: return
        val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
        val dur = exoPlayer.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: video.durationMs
        val completed = forceCompleted || (dur > 0L && pos >= dur - 4_000L)

        viewModelScope.launch {
            historyDao.upsert(
                PlaybackHistoryEntity(
                    videoId = video.id,
                    mediaStoreId = video.mediaStoreId,
                    uriString = video.uri.toString(),
                    title = video.title,
                    durationMs = dur,
                    lastPositionMs = if (completed) 0L else pos,
                    isCompleted = completed,
                    lastPlayedAt = System.currentTimeMillis(),
                    resolution = video.resolutionLabel,
                    folderName = video.folderName
                )
            )
        }
    }

    override fun onCleared() {
        persistPlaybackProgress(forceCompleted = false)
        tickerJob?.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
        super.onCleared()
    }
}
