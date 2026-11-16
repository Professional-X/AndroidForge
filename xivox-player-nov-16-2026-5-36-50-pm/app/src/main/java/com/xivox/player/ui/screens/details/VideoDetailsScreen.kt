package com.xivox.player.ui.screens.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xivox.player.XivoxApplication
import com.xivox.player.data.local.db.PlaybackHistoryEntity
import com.xivox.player.domain.model.ExternalSubtitleFile
import com.xivox.player.domain.model.VideoItem
import com.xivox.player.ui.components.GlassSurfaceCard
import com.xivox.player.ui.components.VideoThumbnailBox
import com.xivox.player.ui.theme.LocalXivoxPalette
import com.xivox.player.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailsScreen(
    video: VideoItem,
    history: PlaybackHistoryEntity?,
    isFavorite: Boolean,
    safTreeUris: Set<String>,
    onBack: () -> Unit,
    onPlayFromPosition: (Long) -> Unit,
    onToggleFavorite: () -> Unit
) {
    val palette = LocalXivoxPalette.current
    val context = LocalContext.current
    var companionSubtitles by remember { mutableStateOf<List<ExternalSubtitleFile>>(emptyList()) }

    LaunchedEffect(video.id) {
        val app = context.applicationContext as XivoxApplication
        companionSubtitles = app.mediaScannerRepository.discoverCompanionSubtitles(video, safTreeUris)
    }

    val resumablePos = history?.takeIf {
        !it.isCompleted && it.lastPositionMs >= 5_000L &&
            (it.durationMs <= 0L || it.lastPositionMs < it.durationMs - 5_000L)
    }?.lastPositionMs ?: 0L

    val progressRatio = if (history != null && history.durationMs > 0L) {
        (history.lastPositionMs.toFloat() / history.durationMs.toFloat()).coerceIn(0f, 1f)
    } else null

    Scaffold(
        containerColor = palette.background,
        topBar = {
            TopAppBar(
                title = { Text("Video Details", color = palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = palette.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) palette.accentPrimary else palette.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = palette.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            VideoThumbnailBox(video = video, progressRatio = progressRatio)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary
                )
                Text(
                    text = "${video.resolutionLabel} · ${video.formatExtension} · ${Formatters.formatFileSize(video.sizeBytes)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.accentPrimary
                )
            }

            // Primary Playback CTAs
            if (resumablePos > 0L) {
                Button(
                    onClick = { onPlayFromPosition(resumablePos) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accentPrimary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Resume from ${Formatters.formatDurationMs(resumablePos)}",
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = { onPlayFromPosition(0L) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Icon(Icons.Rounded.Replay, contentDescription = null, tint = palette.textPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play from Beginning", color = palette.textPrimary)
                }
            } else {
                Button(
                    onClick = { onPlayFromPosition(0L) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accentPrimary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play Video", fontWeight = FontWeight.Bold)
                }
            }

            // Technical Specifications
            GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Media Specifications",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    DetailRow("File Name", video.displayName)
                    HorizontalDivider(color = palette.border)
                    DetailRow("Resolution", "${video.width} × ${video.height} (${video.resolutionLabel})")
                    HorizontalDivider(color = palette.border)
                    DetailRow("Duration", Formatters.formatDurationMs(video.durationMs))
                    HorizontalDivider(color = palette.border)
                    DetailRow("Container / MIME", video.mimeType)
                    HorizontalDivider(color = palette.border)
                    DetailRow("File Size", Formatters.formatFileSize(video.sizeBytes))
                    HorizontalDivider(color = palette.border)
                    DetailRow("Folder", "${video.folderName} (${video.folderPath})")
                }
            }

            // Detected External Subtitles
            GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Subtitles, contentDescription = null, tint = palette.accentPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "External Subtitles",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                    }
                    if (companionSubtitles.isEmpty()) {
                        Text(
                            text = "No matching sidecar subtitle files found in this folder. Embedded tracks and manual .srt/.vtt/.ass loading are available inside the player.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    } else {
                        companionSubtitles.forEach { sub ->
                            Text(
                                text = "• ${sub.displayName} (${sub.languageLabel})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.accentPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val palette = LocalXivoxPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}
