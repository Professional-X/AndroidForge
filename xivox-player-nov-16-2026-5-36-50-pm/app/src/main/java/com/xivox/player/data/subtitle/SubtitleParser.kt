package com.xivox.player.data.subtitle

import android.content.Context
import android.net.Uri
import com.xivox.player.domain.model.SubtitleCue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object SubtitleParser {

    suspend fun parseFromUri(context: Context, uri: Uri, fileNameHint: String? = null): List<SubtitleCue> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                } ?: ""
            }.map { raw ->
                parseText(raw, fileNameHint ?: uri.lastPathSegment.orEmpty())
            }.getOrDefault(emptyList())
        }

    fun parseText(rawText: String, fileNameOrExtension: String = ""): List<SubtitleCue> {
        val cleaned = rawText.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
        if (cleaned.isBlank()) return emptyList()

        val lowerHint = fileNameOrExtension.lowercase()
        val trimmedStart = cleaned.trimStart()

        return when {
            lowerHint.endsWith(".vtt") || trimmedStart.startsWith("WEBVTT") -> parseVtt(cleaned)
            lowerHint.endsWith(".ass") || lowerHint.endsWith(".ssa") || cleaned.contains("[Events]") -> parseAss(cleaned)
            else -> parseSrt(cleaned)
        }.sortedBy { it.startMs }
    }

    fun findActiveCue(cues: List<SubtitleCue>, positionMs: Long, delayMs: Long): SubtitleCue? {
        if (cues.isEmpty()) return null
        val adjustedPos = positionMs - delayMs
        // Binary search for fast O(log n) frame-accurate cue lookup
        var low = 0
        var high = cues.lastIndex
        while (low <= high) {
            val mid = (low + high) ushr 1
            val cue = cues[mid]
            when {
                adjustedPos < cue.startMs -> high = mid - 1
                adjustedPos > cue.endMs -> low = mid + 1
                else -> return cue
            }
        }
        // Fallback linear scan in case of overlapping cues
        return cues.firstOrNull { adjustedPos in it.startMs..it.endMs }
    }

    private fun parseSrt(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val blocks = content.split(Regex("\n{2,}"))
        for (block in blocks) {
            val lines = block.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
            if (lines.size < 2) continue
            val timingLineIndex = lines.indexOfFirst { it.contains("-->") }
            if (timingLineIndex == -1) continue
            val times = parseTimingRange(lines[timingLineIndex]) ?: continue
            val text = lines.drop(timingLineIndex + 1)
                .joinToString("\n")
                .let(::stripFormattingTags)
                .trim()
            if (text.isNotBlank()) {
                cues.add(SubtitleCue(times.first, times.second, text))
            }
        }
        return cues
    }

    private fun parseVtt(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val blocks = content.split(Regex("\n{2,}"))
        for (block in blocks) {
            val lines = block.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) continue
            val first = lines.first().trim()
            if (first.startsWith("WEBVTT") || first.startsWith("NOTE") || first.startsWith("STYLE")) {
                continue
            }
            val timingIndex = lines.indexOfFirst { it.contains("-->") }
            if (timingIndex == -1) continue
            val times = parseTimingRange(lines[timingIndex]) ?: continue
            val text = lines.drop(timingIndex + 1)
                .joinToString("\n")
                .let(::stripFormattingTags)
                .trim()
            if (text.isNotBlank()) {
                cues.add(SubtitleCue(times.first, times.second, text))
            }
        }
        return cues
    }

    private fun parseAss(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        var inEvents = false
        var startIdx = 1
        var endIdx = 2
        var textIdx = 9

        for (rawLine in content.lines()) {
            val line = rawLine.trim()
            if (line.startsWith("[")) {
                inEvents = line.equals("[Events]", ignoreCase = true)
                continue
            }
            if (!inEvents) continue
            if (line.startsWith("Format:", ignoreCase = true)) {
                val fields = line.substringAfter(":").split(",").map { it.trim().lowercase() }
                fields.indexOf("start").takeIf { it >= 0 }?.let { startIdx = it }
                fields.indexOf("end").takeIf { it >= 0 }?.let { endIdx = it }
                fields.indexOf("text").takeIf { it >= 0 }?.let { textIdx = it }
                continue
            }
            if (line.startsWith("Dialogue:", ignoreCase = true)) {
                val payload = line.substringAfter(":").trimStart()
                val parts = payload.split(",", limit = textIdx + 1)
                if (parts.size <= textIdx) continue
                val startMs = parseAssTimestamp(parts[startIdx].trim()) ?: continue
                val endMs = parseAssTimestamp(parts[endIdx].trim()) ?: continue
                val text = parts[textIdx]
                    .replace(Regex("\\{[^}]*\\}"), "")
                    .replace("\\N", "\n")
                    .replace("\\n", "\n")
                    .trim()
                if (text.isNotBlank()) {
                    cues.add(SubtitleCue(startMs, endMs, text))
                }
            }
        }
        return cues
    }

    private fun parseTimingRange(line: String): Pair<Long, Long>? {
        val parts = line.split("-->")
        if (parts.size != 2) return null
        val start = parseFlexibleTimestamp(parts[0].trim()) ?: return null
        val endToken = parts[1].trim().split(Regex("\\s+")).firstOrNull() ?: return null
        val end = parseFlexibleTimestamp(endToken) ?: return null
        return if (end >= start) start to end else null
    }

    private fun parseFlexibleTimestamp(token: String): Long? {
        val normalized = token.replace(',', '.').trim()
        val segments = normalized.split(':')
        return runCatching {
            when (segments.size) {
                3 -> {
                    val h = segments[0].toLong()
                    val m = segments[1].toLong()
                    val secParts = segments[2].split('.')
                    val s = secParts[0].toLong()
                    val ms = secParts.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLong() ?: 0L
                    ((h * 3600L) + (m * 60L) + s) * 1000L + ms
                }
                2 -> {
                    val m = segments[0].toLong()
                    val secParts = segments[1].split('.')
                    val s = secParts[0].toLong()
                    val ms = secParts.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLong() ?: 0L
                    ((m * 60L) + s) * 1000L + ms
                }
                else -> null
            }
        }.getOrNull()
    }

    private fun parseAssTimestamp(token: String): Long? {
        val parts = token.split(':')
        if (parts.size != 3) return null
        return runCatching {
            val h = parts[0].toLong()
            val m = parts[1].toLong()
            val secParts = parts[2].split('.')
            val s = secParts[0].toLong()
            val centis = secParts.getOrNull(1)?.padEnd(2, '0')?.take(2)?.toLong() ?: 0L
            ((h * 3600L) + (m * 60L) + s) * 1000L + (centis * 10L)
        }.getOrNull()
    }

    private fun stripFormattingTags(text: String): String {
        return text
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\{\\\\[^}]+\\}"), "")
    }
}
