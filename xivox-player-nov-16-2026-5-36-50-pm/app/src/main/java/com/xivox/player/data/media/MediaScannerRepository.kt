package com.xivox.player.data.media

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MimeTypes
import com.xivox.player.domain.model.ExternalSubtitleFile
import com.xivox.player.domain.model.VideoFolder
import com.xivox.player.domain.model.VideoItem
import com.xivox.player.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaScannerRepository(private val context: Context) {

    suspend fun scanDeviceVideos(
        safTreeUris: Set<String> = emptySet(),
        minDurationSeconds: Int = 0
    ): List<VideoItem> = withContext(Dispatchers.IO) {
        val results = LinkedHashMap<String, VideoItem>()

        // 1. Scan MediaStore Video Collection (scoped storage safe)
        scanMediaStoreVideos(minDurationSeconds).forEach { item ->
            results[item.uri.toString()] = item
        }

        // 2. Scan user-selected SAF folders (custom folders / SD card / USB OTG)
        safTreeUris.forEach { treeUriString ->
            runCatching {
                val treeUri = Uri.parse(treeUriString)
                scanSafTree(treeUri, minDurationSeconds).forEach { item ->
                    results.putIfAbsent(item.uri.toString(), item)
                }
            }
        }

        results.values.toList()
    }

    private fun scanMediaStoreVideos(minDurationSeconds: Int): List<VideoItem> {
        val videos = mutableListOf<VideoItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = buildList {
            add(MediaStore.Video.Media._ID)
            add(MediaStore.Video.Media.DISPLAY_NAME)
            add(MediaStore.Video.Media.TITLE)
            add(MediaStore.Video.Media.DURATION)
            add(MediaStore.Video.Media.SIZE)
            add(MediaStore.Video.Media.WIDTH)
            add(MediaStore.Video.Media.HEIGHT)
            add(MediaStore.Video.Media.MIME_TYPE)
            add(MediaStore.Video.Media.DATE_ADDED)
            add(MediaStore.Video.Media.BUCKET_ID)
            add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Video.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Video.Media.DATA)
            }
        }.toTypedArray()

        val minDurationMs = minDurationSeconds * 1000L
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else {
                    @Suppress("DEPRECATION")
                    cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val displayName = cursor.getString(nameCol).orEmpty()
                    val title = cursor.getString(titleCol).orEmpty().ifBlank {
                        displayName.substringBeforeLast('.')
                    }
                    val durationMs = cursor.getLong(durCol).coerceAtLeast(0L)
                    if (minDurationMs > 0L && durationMs in 1 until minDurationMs) continue

                    val sizeBytes = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    if (sizeBytes == 0L) continue

                    val width = cursor.getInt(widthCol).coerceAtLeast(0)
                    val height = cursor.getInt(heightCol).coerceAtLeast(0)
                    val mimeType = cursor.getString(mimeCol) ?: "video/mp4"
                    val dateAdded = cursor.getLong(dateCol).coerceAtLeast(0L)
                    val bucketId = cursor.getString(bucketIdCol) ?: "internal"
                    val folderName = cursor.getString(bucketNameCol)?.ifBlank { "Device Storage" } ?: "Device Storage"
                    val rawPath = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else ""
                    val folderPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        rawPath.ifBlank { folderName }
                    } else {
                        File(rawPath).parent ?: folderName
                    }

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    videos.add(
                        VideoItem(
                            id = "ms_$id",
                            mediaStoreId = id,
                            uri = contentUri,
                            title = title,
                            displayName = displayName.ifBlank { title },
                            durationMs = durationMs,
                            sizeBytes = sizeBytes,
                            width = width,
                            height = height,
                            mimeType = mimeType,
                            dateAddedSec = dateAdded,
                            bucketId = bucketId,
                            folderName = folderName,
                            folderPath = folderPath,
                            isSafDocument = false
                        )
                    )
                }
            }
        }
        return videos
    }

    private fun scanSafTree(treeUri: Uri, minDurationSeconds: Int): List<VideoItem> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val folderName = root.name ?: "Custom Folder"
        val bucketId = "saf_${treeUri.toString().hashCode()}"
        val items = mutableListOf<VideoItem>()
        collectSafVideosRecursive(root, bucketId, folderName, treeUri.toString(), minDurationSeconds, items, depth = 0)
        return items
    }

    private fun collectSafVideosRecursive(
        dir: DocumentFile,
        bucketId: String,
        folderName: String,
        folderPath: String,
        minDurationSeconds: Int,
        out: MutableList<VideoItem>,
        depth: Int
    ) {
        if (depth > 4 || !dir.isDirectory) return
        val children = dir.listFiles()
        for (file in children) {
            if (file.isDirectory) {
                collectSafVideosRecursive(
                    file,
                    bucketId,
                    file.name ?: folderName,
                    "$folderPath/${file.name.orEmpty()}",
                    minDurationSeconds,
                    out,
                    depth + 1
                )
            } else if (file.isFile) {
                val name = file.name.orEmpty()
                val ext = name.substringAfterLast('.', "").lowercase()
                val mime = file.type ?: ""
                if (mime.startsWith("video/") || ext in Formatters.SUPPORTED_VIDEO_EXTENSIONS) {
                    val meta = extractMetadataQuick(file.uri)
                    if (minDurationSeconds > 0 && meta.durationMs in 1 until (minDurationSeconds * 1000L)) {
                        continue
                    }
                    out.add(
                        VideoItem(
                            id = "saf_${file.uri.toString().hashCode()}",
                            mediaStoreId = -1L,
                            uri = file.uri,
                            title = name.substringBeforeLast('.').ifBlank { "Video" },
                            displayName = name,
                            durationMs = meta.durationMs,
                            sizeBytes = file.length(),
                            width = meta.width,
                            height = meta.height,
                            mimeType = mime.ifBlank { guessVideoMime(ext) },
                            dateAddedSec = file.lastModified() / 1000L,
                            bucketId = bucketId,
                            folderName = folderName,
                            folderPath = folderPath,
                            isSafDocument = true
                        )
                    )
                }
            }
        }
    }

    suspend fun resolveSingleUri(uri: Uri): VideoItem = withContext(Dispatchers.IO) {
        var displayName = "Local Video"
        var sizeBytes = 0L

        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx) ?: displayName
                    if (sizeIdx >= 0) sizeBytes = cursor.getLong(sizeIdx)
                }
            }
        }

        val meta = extractMetadataQuick(uri)
        val ext = displayName.substringAfterLast('.', "").lowercase()
        val mime = context.contentResolver.getType(uri) ?: guessVideoMime(ext)

        VideoItem(
            id = "uri_${uri.toString().hashCode()}",
            mediaStoreId = -1L,
            uri = uri,
            title = displayName.substringBeforeLast('.').ifBlank { displayName },
            displayName = displayName,
            durationMs = meta.durationMs,
            sizeBytes = sizeBytes,
            width = meta.width,
            height = meta.height,
            mimeType = mime,
            dateAddedSec = System.currentTimeMillis() / 1000L,
            bucketId = "external_uri",
            folderName = "Opened File",
            folderPath = uri.toString(),
            isSafDocument = true
        )
    }

    /**
     * Automatically searches local storage (MediaStore Files + SAF folders) for matching
     * external subtitle files (.srt, .vtt, .ass, .ssa) sharing the video's base filename.
     */
    suspend fun discoverCompanionSubtitles(
        video: VideoItem,
        safTreeUris: Set<String>
    ): List<ExternalSubtitleFile> = withContext(Dispatchers.IO) {
        val baseName = video.displayName.substringBeforeLast('.').lowercase()
        if (baseName.isBlank()) return@withContext emptyList()

        val found = LinkedHashMap<String, ExternalSubtitleFile>()

        // 1. Search MediaStore.Files for sibling subtitle files
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val filesUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.RELATIVE_PATH
            )
            val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            val args = arrayOf("$baseName%")

            runCatching {
                context.contentResolver.query(filesUri, projection, selection, args, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH)
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameCol).orEmpty()
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (ext !in Formatters.SUPPORTED_SUBTITLE_EXTENSIONS) continue
                        val relPath = cursor.getString(pathCol).orEmpty()
                        if (video.folderPath.isNotBlank() && relPath.isNotBlank() &&
                            !video.folderPath.contains(relPath, ignoreCase = true) &&
                            !relPath.contains(video.folderName, ignoreCase = true)
                        ) {
                            continue
                        }
                        val subUri = ContentUris.withAppendedId(filesUri, cursor.getLong(idCol))
                        found[subUri.toString()] = ExternalSubtitleFile(
                            uri = subUri,
                            displayName = name,
                            languageLabel = inferLanguageLabel(name, baseName),
                            mimeType = guessSubtitleMime(ext)
                        )
                    }
                }
            }
        }

        // 2. Search persisted SAF folders for matching subtitles
        safTreeUris.forEach { treeUriStr ->
            runCatching {
                val root = DocumentFile.fromTreeUri(context, Uri.parse(treeUriStr)) ?: return@forEach
                scanSafSubtitles(root, baseName, found, depth = 0)
            }
        }

        found.values.toList()
    }

    private fun scanSafSubtitles(
        dir: DocumentFile,
        baseName: String,
        out: MutableMap<String, ExternalSubtitleFile>,
        depth: Int
    ) {
        if (depth > 3 || !dir.isDirectory) return
        for (file in dir.listFiles()) {
            if (file.isDirectory) {
                scanSafSubtitles(file, baseName, out, depth + 1)
            } else if (file.isFile) {
                val name = file.name.orEmpty()
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext in Formatters.SUPPORTED_SUBTITLE_EXTENSIONS &&
                    name.lowercase().startsWith(baseName)
                ) {
                    out[file.uri.toString()] = ExternalSubtitleFile(
                        uri = file.uri,
                        displayName = name,
                        languageLabel = inferLanguageLabel(name, baseName),
                        mimeType = guessSubtitleMime(ext)
                    )
                }
            }
        }
    }

    fun groupIntoFolders(videos: List<VideoItem>): List<VideoFolder> {
        return videos
            .groupBy { it.bucketId }
            .map { (bucketId, list) ->
                val first = list.first()
                VideoFolder(
                    bucketId = bucketId,
                    name = first.folderName,
                    path = first.folderPath,
                    videoCount = list.size,
                    totalSizeBytes = list.sumOf { it.sizeBytes },
                    previewUri = first.uri
                )
            }
            .sortedByDescending { it.videoCount }
    }

    private data class QuickMeta(val durationMs: Long, val width: Int, val height: Int)

    private fun extractMetadataQuick(uri: Uri): QuickMeta {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            QuickMeta(dur, w, h)
        } catch (_: Exception) {
            QuickMeta(0L, 0, 0)
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun inferLanguageLabel(fileName: String, baseName: String): String {
        val withoutExt = fileName.substringBeforeLast('.')
        val suffix = withoutExt.removePrefix(baseName).trim('.', '_', '-', ' ')
        return if (suffix.isBlank()) "Default (${fileName.substringAfterLast('.').uppercase()})" else suffix.uppercase()
    }

    private fun guessVideoMime(ext: String): String = when (ext) {
        "mkv" -> MimeTypes.VIDEO_MATROSKA
        "webm" -> MimeTypes.VIDEO_WEBM
        "avi" -> "video/x-msvideo"
        "mov" -> "video/quicktime"
        "flv" -> "video/x-flv"
        "3gp" -> MimeTypes.VIDEO_H263
        "ts", "mts" -> "video/mp2t"
        "mpg", "mpeg" -> MimeTypes.VIDEO_MPEG
        else -> MimeTypes.VIDEO_MP4
    }

    fun guessSubtitleMime(ext: String): String = when (ext.lowercase()) {
        "vtt" -> MimeTypes.TEXT_VTT
        "ass", "ssa" -> MimeTypes.TEXT_SSA
        else -> MimeTypes.APPLICATION_SUBRIP
    }
}
