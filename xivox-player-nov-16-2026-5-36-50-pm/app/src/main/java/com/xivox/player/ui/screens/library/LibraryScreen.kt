package com.xivox.player.ui.screens.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xivox.player.domain.model.LayoutMode
import com.xivox.player.domain.model.LibraryTab
import com.xivox.player.domain.model.SortOption
import com.xivox.player.domain.model.VideoFolder
import com.xivox.player.domain.model.VideoItem
import com.xivox.player.ui.components.GlassSurfaceCard
import com.xivox.player.ui.components.StoragePermissionCard
import com.xivox.player.ui.components.VideoGridCard
import com.xivox.player.ui.components.VideoListRow
import com.xivox.player.ui.state.MainUiState
import com.xivox.player.ui.theme.LocalXivoxPalette
import com.xivox.player.util.Formatters

@Composable
fun LibraryScreen(
    state: MainUiState,
    onSearchQueryChange: (String) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onLayoutChange: (LayoutMode) -> Unit,
    onSelectFolder: (String?) -> Unit,
    onHideFolder: (VideoFolder) -> Unit,
    onUnhideFolder: (String) -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onOpenDetails: (VideoItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRefresh: () -> Unit,
    onPickSafFolder: () -> Unit,
    onPickSafFile: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalXivoxPalette.current
    var activeTab by remember { mutableStateOf(LibraryTab.VIDEOS) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showHiddenDialog by remember { mutableStateOf(false) }

    val selectedFolderName = remember(state.selectedBucketId, state.folders) {
        state.folders.firstOrNull { it.bucketId == state.selectedBucketId }?.name
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (state.selectedBucketId != null) {
                    IconButton(onClick = { onSelectFolder(null) }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "All Folders", tint = palette.textPrimary)
                    }
                }
                Column {
                    Text(
                        text = selectedFolderName ?: "Video Library",
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${state.visibleVideos.size} videos · ${state.folders.size} folders",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPickSafFile) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = "Open File", tint = palette.textPrimary)
                }
                IconButton(onClick = onPickSafFolder) {
                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = "Add Folder", tint = palette.accentPrimary)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = palette.textPrimary)
                }
            }
        }

        if (state.isScanning) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = palette.accentPrimary,
                trackColor = palette.border
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text("Filter by title or folder...", color = palette.textSecondary) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.accentPrimary) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = palette.textSecondary)
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

        Spacer(modifier = Modifier.height(10.dp))

        // Mode Tabs + Sort + Grid/List Toggle + Hidden Folders button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = activeTab == LibraryTab.VIDEOS && state.selectedBucketId == null,
                    onClick = {
                        activeTab = LibraryTab.VIDEOS
                        onSelectFolder(null)
                    },
                    label = { Text("Videos") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.accentPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = palette.accentPrimary
                    )
                )
                FilterChip(
                    selected = activeTab == LibraryTab.FOLDERS && state.selectedBucketId == null,
                    onClick = {
                        activeTab = LibraryTab.FOLDERS
                        onSelectFolder(null)
                    },
                    label = { Text("Folders") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.accentPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = palette.accentPrimary
                    )
                )
                if (state.hiddenFolders.isNotEmpty()) {
                    FilterChip(
                        selected = false,
                        onClick = { showHiddenDialog = true },
                        label = { Text("Hidden (${state.hiddenFolders.size})") }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Rounded.Sort, contentDescription = "Sort", tint = palette.textPrimary)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = palette.surface
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        color = if (state.settings.sortOption == option) palette.accentPrimary else palette.textPrimary,
                                        fontWeight = if (state.settings.sortOption == option) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSortChange(option)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        val next = if (state.settings.layoutMode == LayoutMode.GRID) LayoutMode.LIST else LayoutMode.GRID
                        onLayoutChange(next)
                    }
                ) {
                    Icon(
                        imageVector = if (state.settings.layoutMode == LayoutMode.GRID) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                        contentDescription = "Toggle Grid or List",
                        tint = palette.accentPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!state.hasStoragePermission && state.allVideos.isEmpty()) {
            StoragePermissionCard(
                onGrantPermission = onRequestPermission,
                onPickSafFile = onPickSafFile
            )
        } else if (activeTab == LibraryTab.FOLDERS && state.selectedBucketId == null) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.folders, key = { it.bucketId }) { folder ->
                    GlassSurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        onClick = {
                            onSelectFolder(folder.bucketId)
                            activeTab = LibraryTab.VIDEOS
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(palette.accentPrimary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = palette.accentPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${folder.videoCount} videos · ${Formatters.formatFileSize(folder.totalSizeBytes)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                            }
                            IconButton(onClick = { onHideFolder(folder) }) {
                                Icon(
                                    imageVector = Icons.Rounded.VisibilityOff,
                                    contentDescription = "Hide Folder",
                                    tint = palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        } else if (state.settings.layoutMode == LayoutMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 164.dp),
                contentPadding = PaddingValues(bottom = 104.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.visibleVideos, key = { it.id }) { video ->
                    val hist = state.historyByVideoId[video.id]
                    val ratio = if (hist != null && hist.durationMs > 0L && !hist.isCompleted) {
                        hist.lastPositionMs.toFloat() / hist.durationMs.toFloat()
                    } else null
                    VideoGridCard(
                        video = video,
                        progressRatio = ratio,
                        isFavorite = video.id in state.favoriteIds,
                        onPlay = { onPlayVideo(video) },
                        onOpenDetails = { onOpenDetails(video) },
                        onToggleFavorite = { onToggleFavorite(video.id) }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.visibleVideos, key = { it.id }) { video ->
                    val hist = state.historyByVideoId[video.id]
                    val ratio = if (hist != null && hist.durationMs > 0L && !hist.isCompleted) {
                        hist.lastPositionMs.toFloat() / hist.durationMs.toFloat()
                    } else null
                    VideoListRow(
                        video = video,
                        progressRatio = ratio,
                        isFavorite = video.id in state.favoriteIds,
                        onPlay = { onPlayVideo(video) },
                        onOpenDetails = { onOpenDetails(video) },
                        onToggleFavorite = { onToggleFavorite(video.id) }
                    )
                }
            }
        }
    }

    if (showHiddenDialog) {
        AlertDialog(
            onDismissRequest = { showHiddenDialog = false },
            containerColor = palette.surface,
            title = { Text("Hidden Folders", color = palette.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.hiddenFolders.forEach { hidden ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(hidden.folderName, color = palette.textPrimary, fontWeight = FontWeight.Medium)
                                Text(hidden.folderPath, color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { onUnhideFolder(hidden.bucketId) },
                                border = BorderStroke(1.dp, palette.border)
                            ) {
                                Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp), tint = palette.accentPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Show", color = palette.accentPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHiddenDialog = false }) {
                    Text("Done", color = palette.accentPrimary)
                }
            }
        )
    }
}
