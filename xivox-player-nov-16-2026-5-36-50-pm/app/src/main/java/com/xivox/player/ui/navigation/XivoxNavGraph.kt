package com.xivox.player.ui.navigation

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xivox.player.domain.model.VideoItem
import com.xivox.player.ui.components.ResumePromptDialog
import com.xivox.player.ui.screens.details.VideoDetailsScreen
import com.xivox.player.ui.screens.home.HomeScreen
import com.xivox.player.ui.screens.library.LibraryScreen
import com.xivox.player.ui.screens.player.PlayerScreen
import com.xivox.player.ui.screens.player.PlayerViewModel
import com.xivox.player.ui.screens.settings.SettingsScreen
import com.xivox.player.ui.screens.splash.SplashScreen
import com.xivox.player.ui.state.XivoxMainViewModel
import com.xivox.player.ui.theme.LocalXivoxPalette
import kotlinx.coroutines.launch

object XivoxRoutes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val DETAILS = "details"
    const val PLAYER = "player"
}

@Composable
fun XivoxAppRoot(
    mainViewModel: XivoxMainViewModel,
    incomingExternalUri: Uri?,
    onClearIncomingUri: () -> Unit,
    isInPictureInPictureMode: Boolean,
    onRequestStoragePermission: () -> Unit,
    onPickSafFile: () -> Unit,
    onPickSafFolder: () -> Unit,
    onEnterPip: () -> Unit
) {
    val palette = LocalXivoxPalette.current
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = viewModel()
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var selectedDetailsVideo by remember { mutableStateOf<VideoItem?>(null) }

    fun launchPlayer(video: VideoItem, startPosMs: Long) {
        val list = if (state.visibleVideos.any { it.id == video.id }) {
            state.visibleVideos
        } else {
            listOf(video) + state.allVideos.filterNot { it.id == video.id }
        }
        val idx = list.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
        playerViewModel.loadPlaylistAndPlay(list, idx, startPosMs)
        navController.navigate(XivoxRoutes.PLAYER) {
            launchSingleTop = true
        }
    }

    // Handle external file URIs opened via SAF or system file managers
    LaunchedEffect(incomingExternalUri) {
        if (incomingExternalUri != null) {
            val resolved = mainViewModel.resolveUriToVideoItem(incomingExternalUri)
            onClearIncomingUri()
            mainViewModel.requestPlayVideo(resolved) { vid, startPos ->
                launchPlayer(vid, startPos)
            }
        }
    }

    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    val showBottomBar = currentRoute in setOf(XivoxRoutes.HOME, XivoxRoutes.LIBRARY, XivoxRoutes.SETTINGS)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = XivoxRoutes.SPLASH,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            composable(XivoxRoutes.SPLASH) {
                SplashScreen(
                    onFinished = {
                        navController.navigate(XivoxRoutes.HOME) {
                            popUpTo(XivoxRoutes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(XivoxRoutes.HOME) {
                HomeScreen(
                    state = state,
                    onSearchQueryChange = mainViewModel::updateSearchQuery,
                    onPlayVideo = { video ->
                        mainViewModel.requestPlayVideo(video) { vid, startPos ->
                            launchPlayer(vid, startPos)
                        }
                    },
                    onResumeContinueEntry = { entry ->
                        scope.launch {
                            val resolved = mainViewModel.resolveUriToVideoItem(Uri.parse(entry.uriString))
                            mainViewModel.requestPlayVideo(resolved) { vid, startPos ->
                                launchPlayer(vid, startPos)
                            }
                        }
                    },
                    onOpenDetails = { video ->
                        selectedDetailsVideo = video
                        navController.navigate(XivoxRoutes.DETAILS)
                    },
                    onToggleFavorite = mainViewModel::toggleFavorite,
                    onOpenSettings = {
                        navController.navigate(XivoxRoutes.SETTINGS) { launchSingleTop = true }
                    },
                    onOpenLibrary = {
                        navController.navigate(XivoxRoutes.LIBRARY) { launchSingleTop = true }
                    },
                    onRequestPermission = onRequestStoragePermission,
                    onOpenSafFile = onPickSafFile,
                    onRefresh = mainViewModel::refreshLibrary
                )
            }

            composable(XivoxRoutes.LIBRARY) {
                LibraryScreen(
                    state = state,
                    onSearchQueryChange = mainViewModel::updateSearchQuery,
                    onSortChange = mainViewModel::setSortOption,
                    onLayoutChange = mainViewModel::setLayoutMode,
                    onSelectFolder = mainViewModel::selectFolder,
                    onHideFolder = mainViewModel::hideFolder,
                    onUnhideFolder = mainViewModel::unhideFolder,
                    onPlayVideo = { video ->
                        mainViewModel.requestPlayVideo(video) { vid, startPos ->
                            launchPlayer(vid, startPos)
                        }
                    },
                    onOpenDetails = { video ->
                        selectedDetailsVideo = video
                        navController.navigate(XivoxRoutes.DETAILS)
                    },
                    onToggleFavorite = mainViewModel::toggleFavorite,
                    onRefresh = mainViewModel::refreshLibrary,
                    onPickSafFolder = onPickSafFolder,
                    onPickSafFile = onPickSafFile,
                    onRequestPermission = onRequestStoragePermission
                )
            }

            composable(XivoxRoutes.DETAILS) {
                val video = selectedDetailsVideo
                if (video != null) {
                    VideoDetailsScreen(
                        video = video,
                        history = state.historyByVideoId[video.id],
                        isFavorite = video.id in state.favoriteIds,
                        safTreeUris = state.settings.safTreeUris,
                        onBack = { navController.popBackStack() },
                        onPlayFromPosition = { startPos -> launchPlayer(video, startPos) },
                        onToggleFavorite = { mainViewModel.toggleFavorite(video.id) }
                    )
                }
            }

            composable(XivoxRoutes.SETTINGS) {
                SettingsScreen(
                    settings = state.settings,
                    hiddenFolders = state.hiddenFolders,
                    prefsRepo = mainViewModel.getPreferencesRepository(),
                    onUnhideFolder = mainViewModel::unhideFolder,
                    onAddSafFolder = onPickSafFolder,
                    onRemoveSafFolder = mainViewModel::removeSafFolder,
                    onRescanLibrary = mainViewModel::refreshLibrary,
                    onClearPlaybackHistory = mainViewModel::clearPlaybackHistory
                )
            }

            composable(XivoxRoutes.PLAYER) {
                PlayerScreen(
                    viewModel = playerViewModel,
                    isInPictureInPictureMode = isInPictureInPictureMode,
                    onEnterPip = onEnterPip,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Floating Glassmorphic Bottom Navigation Bar
        if (showBottomBar) {
            XivoxBottomBar(
                currentRoute = currentRoute,
                onSelectRoute = { route ->
                    navController.navigate(route) {
                        popUpTo(XivoxRoutes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            )
        }

        // Persistent Continue Watching Resume Prompt ("RESUME FROM 12:35?")
        val prompt = state.resumePrompt
        if (prompt != null) {
            ResumePromptDialog(
                videoTitle = prompt.video.title,
                savedPositionMs = prompt.savedPositionMs,
                onResume = {
                    mainViewModel.dismissResumePrompt()
                    launchPlayer(prompt.video, prompt.savedPositionMs)
                },
                onStartBeginning = {
                    mainViewModel.dismissResumePrompt()
                    launchPlayer(prompt.video, 0L)
                },
                onDismiss = mainViewModel::dismissResumePrompt
            )
        }
    }
}

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun XivoxBottomBar(
    currentRoute: String?,
    onSelectRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalXivoxPalette.current
    val items = listOf(
        NavDestinationItem(XivoxRoutes.HOME, "Home", Icons.Rounded.Home),
        NavDestinationItem(XivoxRoutes.LIBRARY, "Library", Icons.Rounded.VideoLibrary),
        NavDestinationItem(XivoxRoutes.SETTINGS, "Settings", Icons.Rounded.Settings)
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = palette.surfaceGlass,
        border = BorderStroke(1.dp, palette.border),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) palette.accentPrimary.copy(alpha = 0.14f) else palette.surfaceGlass.copy(alpha = 0f)
                        )
                        .clickable { onSelectRoute(item.route) }
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) palette.accentPrimary else palette.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) palette.accentPrimary else palette.textSecondary
                    )
                }
            }
        }
    }
}
