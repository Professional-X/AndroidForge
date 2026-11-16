package com.xivox.player.ui.screens.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xivox.player.BuildConfig
import com.xivox.player.data.local.db.HiddenFolderEntity
import com.xivox.player.data.local.prefs.XivoxPreferencesRepository
import com.xivox.player.domain.model.AccentTheme
import com.xivox.player.domain.model.OrientationMode
import com.xivox.player.domain.model.ResumeBehavior
import com.xivox.player.domain.model.SubtitleColorPreset
import com.xivox.player.domain.model.ThemeMode
import com.xivox.player.domain.model.XivoxSettings
import com.xivox.player.ui.components.GlassSurfaceCard
import com.xivox.player.ui.components.XivoxLogoMark
import com.xivox.player.ui.theme.LocalXivoxPalette
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settings: XivoxSettings,
    hiddenFolders: List<HiddenFolderEntity>,
    prefsRepo: XivoxPreferencesRepository,
    onUnhideFolder: (String) -> Unit,
    onAddSafFolder: () -> Unit,
    onRemoveSafFolder: (String) -> Unit,
    onRescanLibrary: () -> Unit,
    onClearPlaybackHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalXivoxPalette.current
    val scope = rememberCoroutineScope()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showResetSettingsDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary
                )
                Text(
                    text = "All preferences persist locally via DataStore",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            }
        }

        // Theme & Accent
        item {
            SettingsSectionCard(title = "Appearance & Theme") {
                Text("Surface Mode", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = settings.themeMode == mode
                        SelectableOptionChip(
                            label = mode.label,
                            selected = selected,
                            onClick = { scope.launch { prefsRepo.setThemeMode(mode) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Accent Spectrum", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentTheme.entries.forEach { accent ->
                        val selected = settings.accentTheme == accent
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) Color(accent.primaryHex).copy(alpha = 0.18f) else palette.background,
                            border = BorderStroke(1.dp, if (selected) Color(accent.primaryHex) else palette.border),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { scope.launch { prefsRepo.setAccentTheme(accent) } }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(accent.primaryHex))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = accent.label.substringBefore(" "),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Playback, Speed, Orientation & Resume
        item {
            SettingsSectionCard(title = "Playback & Orientation") {
                Text(
                    text = "Default Playback Speed: ${settings.defaultPlaybackSpeed}x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary
                )
                Slider(
                    value = settings.defaultPlaybackSpeed,
                    onValueChange = { scope.launch { prefsRepo.setDefaultSpeed((it * 4).toInt() / 4f) } },
                    valueRange = 0.5f..2.0f,
                    steps = 5,
                    colors = SliderDefaults.colors(thumbColor = palette.accentPrimary, activeTrackColor = palette.accentPrimary)
                )

                HorizontalDivider(color = palette.border)
                Text("Resume Playback Behavior", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                ResumeBehavior.entries.forEach { behavior ->
                    val selected = settings.resumeBehavior == behavior
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) palette.accentPrimary.copy(alpha = 0.14f) else Color.Transparent)
                            .clickable { scope.launch { prefsRepo.setResumeBehavior(behavior) } }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(behavior.label, color = if (selected) palette.accentPrimary else palette.textPrimary, fontWeight = FontWeight.Medium)
                            Text(behavior.description, color = palette.textSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                HorizontalDivider(color = palette.border)
                Text("Screen Orientation", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrientationMode.entries.take(2).forEach { mode ->
                        SelectableOptionChip(
                            label = mode.label,
                            selected = settings.orientationMode == mode,
                            onClick = { scope.launch { prefsRepo.setOrientationMode(mode) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrientationMode.entries.drop(2).forEach { mode ->
                        SelectableOptionChip(
                            label = mode.label,
                            selected = settings.orientationMode == mode,
                            onClick = { scope.launch { prefsRepo.setOrientationMode(mode) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = palette.border)
                Text(
                    text = "Player Controls Auto-Hide: ${settings.controlsTimeoutSeconds}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary
                )
                Slider(
                    value = settings.controlsTimeoutSeconds.toFloat(),
                    onValueChange = { scope.launch { prefsRepo.setControlsTimeout(it.toInt()) } },
                    valueRange = 2f..10f,
                    steps = 7,
                    colors = SliderDefaults.colors(thumbColor = palette.accentPrimary, activeTrackColor = palette.accentPrimary)
                )
            }
        }

        // Subtitle Preferences
        item {
            SettingsSectionCard(title = "Subtitle Preferences") {
                SettingsToggleRow(
                    title = "Enable Subtitles by Default",
                    subtitle = "Automatically display detected tracks on launch",
                    checked = settings.subtitlesEnabledByDefault,
                    onCheckedChange = { scope.launch { prefsRepo.setSubtitlesEnabledByDefault(it) } }
                )
                SettingsToggleRow(
                    title = "Auto-Select Preferred Track",
                    subtitle = "Match external .srt/.vtt/.ass or embedded language",
                    checked = settings.autoSelectSubtitles,
                    onCheckedChange = { scope.launch { prefsRepo.setAutoSelectSubtitles(it) } }
                )

                HorizontalDivider(color = palette.border)
                Text("Font Size: ${settings.subtitleFontSizeSp.toInt()} sp", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                Slider(
                    value = settings.subtitleFontSizeSp,
                    onValueChange = { scope.launch { prefsRepo.setSubtitleFontSize(it) } },
                    valueRange = 12f..32f,
                    colors = SliderDefaults.colors(thumbColor = palette.accentPrimary, activeTrackColor = palette.accentPrimary)
                )

                Text("Subtitle Text Color", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubtitleColorPreset.entries.forEach { preset ->
                        SelectableOptionChip(
                            label = preset.label,
                            selected = settings.subtitleTextColorArgb == preset.colorArgb,
                            onClick = { scope.launch { prefsRepo.setSubtitleTextColor(preset.colorArgb) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text("Background Opacity: ${(settings.subtitleBackgroundOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                Slider(
                    value = settings.subtitleBackgroundOpacity,
                    onValueChange = { scope.launch { prefsRepo.setSubtitleBackgroundOpacity(it) } },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = palette.accentPrimary, activeTrackColor = palette.accentPrimary)
                )
            }
        }

        // Gesture Controls
        item {
            SettingsSectionCard(title = "Gesture Controls") {
                SettingsToggleRow(
                    title = "Double-Tap Seek (10s)",
                    subtitle = "Double-tap left or right edge to skip",
                    checked = settings.doubleTapSeekEnabled,
                    onCheckedChange = { scope.launch { prefsRepo.setDoubleTapSeekEnabled(it) } }
                )
                SettingsToggleRow(
                    title = "Left Swipe Brightness",
                    subtitle = "Drag vertically on left half to adjust brightness",
                    checked = settings.brightnessSwipeEnabled,
                    onCheckedChange = { scope.launch { prefsRepo.setBrightnessSwipeEnabled(it) } }
                )
                SettingsToggleRow(
                    title = "Right Swipe Volume",
                    subtitle = "Drag vertically on right half to adjust media volume",
                    checked = settings.volumeSwipeEnabled,
                    onCheckedChange = { scope.launch { prefsRepo.setVolumeSwipeEnabled(it) } }
                )
                SettingsToggleRow(
                    title = "Horizontal Seek Swipe",
                    subtitle = "Drag horizontally anywhere to scrub timeline",
                    checked = settings.horizontalSeekSwipeEnabled,
                    onCheckedChange = { scope.launch { prefsRepo.setHorizontalSeekSwipeEnabled(it) } }
                )
                SettingsToggleRow(
                    title = "Long-Press 2x Speed",
                    subtitle = "Hold screen for temporary 2.0x playback speed",
                    checked = settings.longPress2xSpeedEnabled,
                    onCheckedChange = { scope.launch { prefsRepo.setLongPress2xEnabled(it) } }
                )
            }
        }

        // Library Scanning & Folders
        item {
            SettingsSectionCard(title = "Library Scanning & Storage") {
                Text(
                    text = "Ignore Clips Shorter Than: ${settings.minVideoDurationSeconds}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary
                )
                Slider(
                    value = settings.minVideoDurationSeconds.toFloat(),
                    onValueChange = { scope.launch { prefsRepo.setMinVideoDurationSeconds(it.toInt()) } },
                    valueRange = 0f..60f,
                    steps = 11,
                    colors = SliderDefaults.colors(thumbColor = palette.accentPrimary, activeTrackColor = palette.accentPrimary)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onAddSafFolder,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, palette.accentPrimary)
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = palette.accentPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Folder", color = palette.accentPrimary)
                    }
                    OutlinedButton(
                        onClick = onRescanLibrary,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rescan", color = palette.textPrimary)
                    }
                }

                if (settings.safTreeUris.isNotEmpty()) {
                    HorizontalDivider(color = palette.border)
                    Text("Custom SAF Folders (${settings.safTreeUris.size})", style = MaterialTheme.typography.labelMedium, color = palette.accentPrimary)
                    settings.safTreeUris.forEach { uriStr ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = uriStr.substringAfterLast("%3A").ifBlank { uriStr },
                                color = palette.textPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { onRemoveSafFolder(uriStr) }) {
                                Text("Remove", color = Color(0xFFFB7185))
                            }
                        }
                    }
                }

                if (hiddenFolders.isNotEmpty()) {
                    HorizontalDivider(color = palette.border)
                    Text("Hidden Folders (${hiddenFolders.size})", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                    hiddenFolders.forEach { hidden ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(hidden.folderName, color = palette.textPrimary, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onUnhideFolder(hidden.bucketId) }) {
                                Text("Unhide", color = palette.accentPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Data & Privacy Controls
        item {
            SettingsSectionCard(title = "History & Reset") {
                OutlinedButton(
                    onClick = { showClearHistoryDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color(0xFFFB7185).copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color(0xFFFB7185))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Playback History", color = Color(0xFFFB7185))
                }
                OutlinedButton(
                    onClick = { showResetSettingsDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = palette.textPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset All Preferences", color = palette.textPrimary)
                }
            }
        }

        // App Information & Offline Privacy Manifesto
        item {
            SettingsSectionCard(title = "App Information") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    XivoxLogoMark(size = 42.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("XIVOX Video Player", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                        Text("Version ${BuildConfig.VERSION_NAME} · Beyond Playback.", style = MaterialTheme.typography.bodyMedium, color = palette.accentPrimary)
                    }
                }
                HorizontalDivider(color = palette.border)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = palette.accentPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "100% Offline & Private: No internet permission, no accounts, no telemetry, and no ads. All playback history and preferences remain strictly on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Powered by AndroidX Media3 ExoPlayer, Room Database, DataStore, and Coil.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            containerColor = palette.surface,
            title = { Text("Clear Playback History?", color = palette.textPrimary) },
            text = {
                Text(
                    "This removes all saved Continue Watching positions from the local Room database.",
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearPlaybackHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB7185), contentColor = Color.White)
                ) {
                    Text("Clear History")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = palette.textPrimary)
                }
            }
        )
    }

    if (showResetSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showResetSettingsDialog = false },
            containerColor = palette.surface,
            title = { Text("Reset Preferences?", color = palette.textPrimary) },
            text = {
                Text(
                    "Restore all theme, subtitle, gesture, and playback preferences to factory defaults.",
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { prefsRepo.resetAllSettings() }
                        showResetSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentPrimary, contentColor = Color.Black)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetSettingsDialog = false }) {
                    Text("Cancel", color = palette.textPrimary)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    val palette = LocalXivoxPalette.current
    GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.accentPrimary
            )
            content()
        }
    }
}

@Composable
private fun SelectableOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalXivoxPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) palette.accentPrimary.copy(alpha = 0.18f) else palette.background,
        border = BorderStroke(1.dp, if (selected) palette.accentPrimary else palette.border),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) palette.accentPrimary else palette.textPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalXivoxPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = palette.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = palette.accentPrimary
            )
        )
    }
}
