package com.xivox.player.util

import java.util.Locale
import kotlin.math.abs

object Formatters {

    val SUPPORTED_VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "mov", "m4v", "webm",
        "flv", "3gp", "ts", "mts", "mpg", "mpeg"
    )

    val SUPPORTED_SUBTITLE_EXTENSIONS = setOf(
        "srt", "vtt", "ass", "ssa", "sub"
    )

    fun formatDurationMs(durationMs: Long): String {
        val totalSeconds = (durationMs.coerceAtLeast(0L)) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatSignedDeltaMs(deltaMs: Long): String {
        val prefix = if (deltaMs >= 0) "+" else "-"
        return prefix + formatDurationMs(abs(deltaMs))
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
            else -> "$bytes B"
        }
    }

    fun formatRelativeDate(epochMillis: Long): String {
        val diff = System.currentTimeMillis() - epochMillis
        val mins = diff / 60_000L
        val hours = diff / 3_600_000L
        val days = diff / 86_400_000L
        return when {
            mins < 1 -> "Just now"
            mins < 60 -> "${mins}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> "${days / 7}w ago"
        }
    }
}
