package com.xivox.player.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.xivox.player.domain.model.ContinueWatchingEntry
import com.xivox.player.domain.model.VideoItem
import com.xivox.player.ui.components.GlassSurfaceCard
import com.xivox.player.ui.components.StoragePermissionCard
import com.xivox.player.ui.components.VideoGridCard
import com.xivox.player.ui.components.VideoListRow
import com.xivox.player.ui.components.VideoThumbnailBox
import com.xivox.player.ui.components.XivoxLogoMark
import com.xivox.player.ui.state.MainUiState
import com.xivox.player.ui.theme.LocalXivoxPalette
import com.xivox.player.util.Formatters

@Composable
fun HomeScreen(
    state: MainUiState,
    onSearchQueryChange: (String) -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onResumeContinueEntry: (ContinueWatchingEntry) -> Unit,
    onOpenDetails: (VideoItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSafFile: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalXivoxPalette.current
    val videosById = state.allVideos.associateBy { it.id }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // XIVOX Top Branding & Quick Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    XivoxLogoMark(size = 38.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "XIVOX",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Beyond Playback.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.accentPrimary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenSafFile) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = "Open Local Video",
                            tint = palette.textPrimary
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh Library",
                            tint = palette.textPrimary
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = palette.accentPrimary
                        )
                    }
                }
            }
        }

        // Glassmorphic Search Bar
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = {
                    Text(
                        text = "Search local videos or folders...",
                        color = palette.textSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = palette.accentPrimary
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = palette.textSecondary
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.surfaceGlass,
                    unfocusedContainerColor = palette.surfaceGlass,
                    focusedBorderColor = palette.accentPrimary,
                    unfocusedBorderColor = palette.border,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary
                )
            )
        }

        // Permission prompt when storage permission is not yet granted
        if (!state.hasStoragePermission && state.allVideos.isEmpty()) {
            item {
                StoragePermissionCard(
                    onGrantPermission = onRequestPermission,
                    onPickSafFile = onOpenSafFile
                )
            }
        }

        // Continue Watching Carousel (Room persistent)
        if (state.searchQuery.isBlank() && state.continueWatching.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Continue Watching",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "${state.continueWatching.size} saved",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.accentPrimary
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.continueWatching, key = { it.videoId }) { entry ->
                            ContinueWatchingCard(
                                entry = entry,
                                onClick = {
                                    val match = videosById[entry.videoId]
                                    if (match != null) onPlayVideo(match) else onResumeContinueEntry(entry)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Recently Added Horizontal Shelf
        if (state.searchQuery.isBlank() && state.recentlyAdded.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recently Added",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                        TextButton(onClick = onOpenLibrary) {
                            Text("See All", color = palette.accentPrimary)
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.recentlyAdded, key = { it.id }) { video ->
                            val hist = state.historyByVideoId[video.id]
                            val ratio = if (hist != null && hist.durationMs > 0L) {
                                hist.lastPositionMs.toFloat() / hist.durationMs.toFloat()
                            } else null
                            GlassSurfaceCard(
                                modifier = Modifier.width(210.dp),
                                cornerRadius = 16.dp,
                                onClick = { onPlayVideo(video) }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    VideoThumbnailBox(video = video, progressRatio = ratio)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = video.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${video.resolutionLabel} · ${video.folderName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // All Videos / Search Results Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.searchQuery.isBlank()) "All Videos" else "Search Results (${state.visibleVideos.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
                if (state.allVideos.isNotEmpty()) {
                    TextButton(onClick = onOpenLibrary) {
                        Text("${state.allVideos.size} total", color = palette.textSecondary)
                    }
                }
            }
        }

        if (state.visibleVideos.isEmpty() && state.hasStoragePermission && !state.isScanning) {
            item {
                GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (state.searchQuery.isBlank()) "No Local Videos Found" else "No Matching Videos",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                        Text(
                            text = "Tap the folder icon above to open any local video or add a custom folder.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        } else {
            items(state.visibleVideos.take(24), key = { it.id }) { video ->
                val hist = state.historyByVideoId[video.id]
                val ratio = if (hist != null && hist.durationMs > 0L && !hist.isCompleted) {
                    hist.lastPositionMs.toFloat() / hist.durationMs.toFloat()
                } else null

                if (state.settings.layoutMode == com.xivox.player.domain.model.LayoutMode.LIST) {
                    VideoListRow(
                        video = video,
                        progressRatio = ratio,
                        isFavorite = video.id in state.favoriteIds,
                        onPlay = { onPlayVideo(video) },
                        onOpenDetails = { onOpenDetails(video) },
                        onToggleFavorite = { onToggleFavorite(video.id) }
                    )
                } else {
                    VideoGridCard(
                        video = video,
                        progressRatio = ratio,
                        isFavorite = video.id in state.favoriteIds,
                        onPlay = { onPlayVideo(video) },
                        onOpenDetails = { onOpenDetails(video) },
                        onToggleFavorite = { onToggleFavorite(video.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingCard(
    entry: ContinueWatchingEntry,
    onClick: () -> Unit
) {
    val palette = LocalXivoxPalette.current
    val context = LocalContext.current
    val remainingMs = (entry.durationMs - entry.lastPositionMs).coerceAtLeast(0L)

    GlassSurfaceCard(
        modifier = Modifier.width(268.dp),
        cornerRadius = 18.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(138.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0A0D16))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(android.net.Uri.parse(entry.uriString))
                        .videoFrameMillis(entry.lastPositionMs.coerceAtLeast(1000L))
                        .crossfade(true)
                        .build(),
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(palette.accentBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color.Black
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(entry.progressRatio)
                            .height(3.dp)
                            .background(palette.accentBrush)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Resume at ${Formatters.formatDurationMs(entry.lastPositionMs)} · ${Formatters.formatDurationMs(remainingMs)} left",
                style = MaterialTheme.typography.labelMedium,
                color = palette.accentPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
