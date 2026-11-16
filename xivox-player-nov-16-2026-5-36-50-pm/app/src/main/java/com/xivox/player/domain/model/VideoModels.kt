package com.xivox.player.domain.model

import android.net.Uri

enum class SortOption(val label: String) {
    DATE("Date Added"),
    NAME("Name"),
    DURATION("Duration"),
    SIZE("Size")
}

enum class LayoutMode {
    GRID,
    LIST
}

enum class LibraryTab {
    VIDEOS,
    FOLDERS
}

data class ExternalSubtitleFile(
    val uri: Uri,
    val displayName: String,
    val languageLabel: String,
    val mimeType: String
)

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class PlayerTrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String?,
    val isSelected: Boolean,
    val isSupported: Boolean
)

data class VideoItem(
    val id: String,
    val mediaStoreId: Long,
    val uri: Uri,
    val title: String,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val dateAddedSec: Long,
    val bucketId: String,
    val folderName: String,
    val folderPath: String,
    val isSafDocument: Boolean = false
) {
    val resolutionLabel: String
        get() = when {
            height >= 2160 || width >= 3840 -> "4K"
            height >= 1440 || width >= 2560 -> "1440p"
            height >= 1080 || width >= 1920 -> "1080p"
            height >= 720 || width >= 1280 -> "720p"
            height > 0 -> "${height}p"
            else -> "HD"
        }

    val formatExtension: String
        get() {
            val ext = displayName.substringAfterLast('.', "").uppercase()
            return if (ext.isNotBlank() && ext.length <= 5) ext else mimeType.substringAfterLast('/').uppercase()
        }
}

data class VideoFolder(
    val bucketId: String,
    val name: String,
    val path: String,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val previewUri: Uri?
)

data class ContinueWatchingEntry(
    val videoId: String,
    val uriString: String,
    val title: String,
    val durationMs: Long,
    val lastPositionMs: Long,
    val isCompleted: Boolean,
    val lastPlayedAt: Long,
    val resolution: String,
    val folderName: String
) {
    val progressRatio: Float
        get() = if (durationMs > 0L) {
            (lastPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
}
