package com.xivox.player.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ClosedCaptionDisabled
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.xivox.player.domain.model.OrientationMode
import com.xivox.player.domain.model.SubtitleColorPreset
import com.xivox.player.ui.components.GlassSurfaceCard
import com.xivox.player.ui.theme.LocalXivoxPalette
import com.xivox.player.util.Formatters
import kotlinx.coroutines.delay
import kotlin.math.abs

private enum class GestureHudType { BRIGHTNESS, VOLUME, SEEK }
private enum class PlayerSheetType { TRACKS_AND_SUBTITLES, SPEED }

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    isInPictureInPictureMode: Boolean,
    onEnterPip: () -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalXivoxPalette.current
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsFlow.collectAsStateWithLifecycle()

    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf<PlayerSheetType?>(null) }

    // Gesture HUD state
    var hudType by remember { mutableStateOf<GestureHudType?>(null) }
    var hudPercent by remember { mutableFloatStateOf(0f) }
    var hudSeekTargetMs by remember { mutableLongStateOf(0L) }
    var hudSeekDeltaMs by remember { mutableLongStateOf(0L) }
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) }

    // Seekbar drag state
    var isSeekingBar by remember { mutableStateOf(false) }
    var seekBarDragMs by remember { mutableLongStateOf(0L) }

    // Keep screen on + immersive system bars + orientation setup
    DisposableEffect(activity, settings.orientationMode) {
        val window = activity?.window
        if (window != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (activity != null) {
            activity.requestedOrientation = when (settings.orientationMode) {
                OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                OrientationMode.SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
                OrientationMode.AUTO_VIDEO -> {
                    val v = state.currentVideo
                    if (v != null && v.height > v.width && v.width > 0) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
                }
            }
        }

        onDispose {
            if (window != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                WindowCompat.getInsetsController(window, window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = originalOrientation
        }
    }

    // Persist playback progress on lifecycle pause/stop
    DisposableEffect(lifecycleOwner, isInPictureInPictureMode) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && !isInPictureInPictureMode) {
                viewModel.pausePlayback()
            } else if (event == Lifecycle.Event.ON_STOP) {
                viewModel.persistPlaybackProgress(forceCompleted = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-hide controls
    LaunchedEffect(controlsVisible, state.isPlaying, isLocked, activeSheet) {
        if (controlsVisible && state.isPlaying && !isLocked && activeSheet == null) {
            delay(settings.controlsTimeoutSeconds * 1000L)
            controlsVisible = false
        }
    }

    // Auto-clear double tap ripple
    LaunchedEffect(doubleTapFeedback) {
        if (doubleTapFeedback != null) {
            delay(650L)
            doubleTapFeedback = null
        }
    }

    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadManualExternalSubtitle(uri)
        }
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Media3 ExoPlayer Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = viewModel.exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    keepScreenOn = true
                }
            },
            update = { playerView ->
                playerView.resizeMode = when (state.resizeMode) {
                    VideoResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    VideoResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    VideoResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Full Gesture Surface (taps, double-taps, vertical brightness/volume, horizontal seek, 2x long press)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked, settings) {
                    detectTapGestures(
                        onTap = {
                            controlsVisible = !controlsVisible
                        },
                        onDoubleTap = { offset ->
                            if (isLocked || !settings.doubleTapSeekEnabled) return@detectTapGestures
                            val isLeft = offset.x < size.width / 2f
                            if (isLeft) {
                                viewModel.seekBy(-10_000L)
                                doubleTapFeedback = "-10s"
                            } else {
                                viewModel.seekBy(10_000L)
                                doubleTapFeedback = "+10s"
                            }
                        },
                        onLongPress = {
                            if (!isLocked && settings.longPress2xSpeedEnabled) {
                                viewModel.startTemporary2xSpeed()
                            }
                        },
                        onPress = {
                            tryAwaitRelease()
                            viewModel.stopTemporary2xSpeed()
                        }
                    )
                }
                .pointerInput(isLocked, settings, state.durationMs) {
                    if (isLocked) return@pointerInput
                    var dragMode: GestureHudType? = null
                    var accumulatedX = 0f
                    var accumulatedY = 0f
                    var startBrightness = 0.5f
                    var startVolume = 0.5f
                    var startSeekPos = 0L

                    detectDragGestures(
                        onDragStart = { startOffset ->
                            dragMode = null
                            accumulatedX = 0f
                            accumulatedY = 0f
                            startSeekPos = viewModel.exoPlayer.currentPosition.coerceAtLeast(0L)
                            val winBrightness = activity?.window?.attributes?.screenBrightness ?: 0.5f
                            startBrightness = if (winBrightness < 0f) 0.5f else winBrightness
                            startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume.toFloat()

                            // Decide candidate axis lazily on drag
                            dragMode = if (startOffset.x < size.width / 2f) {
                                GestureHudType.BRIGHTNESS
                            } else {
                                GestureHudType.VOLUME
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedX += dragAmount.x
                            accumulatedY += dragAmount.y

                            val activeAxis = if (abs(accumulatedX) > abs(accumulatedY) * 1.3f && abs(accumulatedX) > 24f) {
                                GestureHudType.SEEK
                            } else if (abs(accumulatedY) > 18f) {
                                if (change.position.x < size.width / 2f) GestureHudType.BRIGHTNESS else GestureHudType.VOLUME
                            } else {
                                dragMode
                            }

                            when (activeAxis) {
                                GestureHudType.BRIGHTNESS -> {
                                    if (!settings.brightnessSwipeEnabled) return@detectDragGestures
                                    val delta = -accumulatedY / (size.height * 0.7f)
                                    val target = (startBrightness + delta).coerceIn(0.02f, 1.0f)
                                    activity?.window?.let { win ->
                                        val lp = win.attributes
                                        lp.screenBrightness = target
                                        win.attributes = lp
                                    }
                                    hudType = GestureHudType.BRIGHTNESS
                                    hudPercent = target
                                }
                                GestureHudType.VOLUME -> {
                                    if (!settings.volumeSwipeEnabled) return@detectDragGestures
                                    val delta = -accumulatedY / (size.height * 0.7f)
                                    val targetFrac = (startVolume + delta).coerceIn(0f, 1f)
                                    val targetVol = (targetFrac * maxVolume).toInt().coerceIn(0, maxVolume)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                    hudType = GestureHudType.VOLUME
                                    hudPercent = targetFrac
                                }
                                GestureHudType.SEEK -> {
                                    if (!settings.horizontalSeekSwipeEnabled || state.durationMs <= 0L) return@detectDragGestures
                                    val seekDelta = ((accumulatedX / size.width.coerceAtLeast(1)) * 90_000f).toLong()
                                    val targetPos = (startSeekPos + seekDelta).coerceIn(0L, state.durationMs)
                                    hudType = GestureHudType.SEEK
                                    hudSeekTargetMs = targetPos
                                    hudSeekDeltaMs = seekDelta
                                }
                                null -> Unit
                            }
                        },
                        onDragEnd = {
                            if (hudType == GestureHudType.SEEK) {
                                viewModel.seekTo(hudSeekTargetMs)
                            }
                            hudType = null
                        },
                        onDragCancel = {
                            hudType = null
                        }
                    )
                }
        )

        // Custom External Subtitle Overlay (positioned & styled per DataStore settings)
        val activeSub = state.activeSubtitleText
        if (state.subtitlesEnabled && !activeSub.isNullOrBlank()) {
            val bottomPaddingFraction = settings.subtitleBottomOffsetFraction.coerceIn(0.04f, 0.45f)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = (bottomPaddingFraction * 360f).dp
                    )
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = settings.subtitleBackgroundOpacity))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = activeSub,
                    color = Color(settings.subtitleTextColorArgb),
                    fontSize = settings.subtitleFontSizeSp.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = (settings.subtitleFontSizeSp * 1.32f).sp
                )
            }
        }

        // Buffering Indicator
        if (state.isBuffering && state.errorMessage == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = palette.accentPrimary
            )
        }

        // Temporary 2x Long-Press Speed Indicator
        AnimatedVisibility(
            visible = state.isTemp2xActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, palette.accentPrimary)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.FastForward, contentDescription = null, tint = palette.accentPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("2.0x Speed", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Double-Tap Skip Feedback
        AnimatedVisibility(
            visible = doubleTapFeedback != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(
                if (doubleTapFeedback == "-10s") Alignment.CenterStart else Alignment.CenterEnd
            ).padding(horizontal = 40.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.75f),
                border = BorderStroke(1.dp, palette.accentPrimary)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (doubleTapFeedback == "-10s") Icons.Rounded.FastRewind else Icons.Rounded.FastForward,
                        contentDescription = null,
                        tint = palette.accentPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(doubleTapFeedback.orEmpty(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Brightness / Volume / Horizontal Seek Gesture HUD
        AnimatedVisibility(
            visible = hudType != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.80f),
                border = BorderStroke(1.dp, palette.border)
            ) {
                when (hudType) {
                    GestureHudType.BRIGHTNESS, GestureHudType.VOLUME -> {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (hudType == GestureHudType.BRIGHTNESS) Icons.Rounded.BrightnessHigh else Icons.Rounded.VolumeUp,
                                contentDescription = null,
                                tint = palette.accentPrimary
                            )
                            Text(
                                text = "${(hudPercent * 100).toInt()}%",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    GestureHudType.SEEK -> {
                        Column(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = Formatters.formatDurationMs(hudSeekTargetMs),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = Formatters.formatSignedDeltaMs(hudSeekDeltaMs),
                                color = palette.accentPrimary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    null -> Unit
                }
            }
        }

        // Screen Lock Floating Unlock Button
        if (isLocked && controlsVisible && !isInPictureInPictureMode) {
            OutlinedButton(
                onClick = { isLocked = false },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, palette.accentPrimary)
            ) {
                Icon(Icons.Rounded.LockOpen, contentDescription = "Unlock", tint = palette.accentPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock Screen", color = Color.White)
            }
        }

        // Fullscreen Player Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible && !isLocked && !isInPictureInPictureMode && state.errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        viewModel.persistPlaybackProgress(false)
                        onBack()
                    }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.currentVideo?.title.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${state.currentVideo?.resolutionLabel.orEmpty()} · ${state.currentVideo?.formatExtension.orEmpty()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.accentPrimary
                        )
                    }

                    IconButton(onClick = {
                        val isCurrentlyLandscape = (activity?.resources?.configuration?.orientation
                            ?: 0) == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                        activity?.requestedOrientation = if (isCurrentlyLandscape) {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        }
                    }) {
                        Icon(Icons.Rounded.ScreenRotation, contentDescription = "Rotate", tint = Color.White)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        IconButton(onClick = onEnterPip) {
                            Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = "Picture in Picture", tint = Color.White)
                        }
                    }

                    IconButton(onClick = { activeSheet = PlayerSheetType.TRACKS_AND_SUBTITLES }) {
                        Icon(Icons.Rounded.Subtitles, contentDescription = "Subtitles & Audio", tint = palette.accentPrimary)
                    }

                    IconButton(onClick = { isLocked = true }) {
                        Icon(Icons.Rounded.Lock, contentDescription = "Lock Screen", tint = Color.White)
                    }
                }

                // Center Transport Buttons
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.playPrevious() },
                        enabled = state.hasPrevious,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (state.hasPrevious) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.seekBy(-10_000L) },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(Icons.Rounded.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(palette.accentBrush)
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.seekBy(10_000L) },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                    ) {
                        Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    IconButton(
                        onClick = { viewModel.playNext() },
                        enabled = state.hasNext,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = if (state.hasNext) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                    }
                }

                // Bottom Control Dock
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.90f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    val displayedPos = if (isSeekingBar) seekBarDragMs else state.currentPositionMs
                    val maxDur = state.durationMs.coerceAtLeast(1L)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Formatters.formatDurationMs(displayedPos),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                        Slider(
                            value = displayedPos.toFloat().coerceIn(0f, maxDur.toFloat()),
                            onValueChange = { value ->
                                isSeekingBar = true
                                seekBarDragMs = value.toLong()
                            },
                            onValueChangeFinished = {
                                viewModel.seekTo(seekBarDragMs)
                                isSeekingBar = false
                            },
                            valueRange = 0f..maxDur.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = palette.accentPrimary,
                                activeTrackColor = palette.accentPrimary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        )
                        Text(
                            text = Formatters.formatDurationMs(state.durationMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { activeSheet = PlayerSheetType.SPEED }) {
                            Icon(Icons.Rounded.Speed, contentDescription = null, tint = palette.accentPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${state.playbackSpeed}x", color = Color.White)
                        }

                        TextButton(onClick = { viewModel.toggleSubtitles() }) {
                            Icon(
                                imageVector = if (state.subtitlesEnabled) Icons.Rounded.ClosedCaption else Icons.Rounded.ClosedCaptionDisabled,
                                contentDescription = null,
                                tint = if (state.subtitlesEnabled) palette.accentPrimary else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.subtitlesEnabled) "Subtitles ON" else "Subtitles OFF",
                                color = Color.White
                            )
                        }

                        TextButton(onClick = { viewModel.cycleResizeMode() }) {
                            Icon(Icons.Rounded.AspectRatio, contentDescription = null, tint = palette.accentPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(state.resizeMode.label, color = Color.White)
                        }
                    }
                }
            }
        }

        // Non-crashing Codec / Playback Error Overlay
        if (state.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFFB7185),
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = "Playback Error",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                        Text(
                            text = state.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onBack,
                                border = BorderStroke(1.dp, palette.border)
                            ) {
                                Text("Go Back", color = palette.textPrimary)
                            }
                            Button(
                                onClick = { viewModel.retryCurrentVideo() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accentPrimary,
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheets: Speed or Tracks & Subtitle Customizer
    if (activeSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = palette.surface
        ) {
            when (activeSheet) {
                PlayerSheetType.SPEED -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Playback Speed",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                        val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 2.5f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            speeds.take(4).forEach { spd ->
                                SpeedChip(spd, state.playbackSpeed == spd) {
                                    viewModel.setPlaybackSpeed(spd)
                                    activeSheet = null
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            speeds.drop(4).forEach { spd ->
                                SpeedChip(spd, state.playbackSpeed == spd) {
                                    viewModel.setPlaybackSpeed(spd)
                                    activeSheet = null
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                PlayerSheetType.TRACKS_AND_SUBTITLES -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Audio & Subtitles",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )

                        // Audio Tracks
                        if (state.embeddedAudioTracks.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Audiotrack, contentDescription = null, tint = palette.accentPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Audio Track", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            }
                            state.embeddedAudioTracks.forEach { track ->
                                TrackSelectionRow(
                                    label = track.label,
                                    selected = track.isSelected,
                                    onClick = { viewModel.selectAudioTrack(track) }
                                )
                            }
                            HorizontalDivider(color = palette.border)
                        }

                        // Subtitle Tracks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Subtitles, contentDescription = null, tint = palette.accentPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Subtitles", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            }
                            OutlinedButton(
                                onClick = {
                                    subtitlePickerLauncher.launch(
                                        arrayOf("application/x-subrip", "text/vtt", "text/plain", "*/*")
                                    )
                                },
                                border = BorderStroke(1.dp, palette.accentPrimary)
                            ) {
                                Text("Load .SRT / .VTT / .ASS", color = palette.accentPrimary)
                            }
                        }

                        TrackSelectionRow(
                            label = "Subtitles Off",
                            selected = !state.subtitlesEnabled,
                            onClick = { viewModel.disableAllSubtitles() }
                        )

                        state.externalSubtitles.forEach { sub ->
                            TrackSelectionRow(
                                label = "External: ${sub.displayName} (${sub.languageLabel})",
                                selected = state.subtitlesEnabled && state.activeExternalSubtitle?.uri == sub.uri,
                                onClick = { viewModel.selectExternalSubtitle(sub) }
                            )
                        }

                        state.embeddedSubtitleTracks.forEach { track ->
                            TrackSelectionRow(
                                label = "Embedded: ${track.label}",
                                selected = state.subtitlesEnabled && state.activeExternalSubtitle == null && track.isSelected,
                                onClick = { viewModel.selectEmbeddedSubtitleTrack(track) }
                            )
                        }

                        HorizontalDivider(color = palette.border)

                        // Subtitle Styling & Sync Controls
                        Text(
                            text = "Subtitle Appearance & Sync",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )

                        Text(
                            text = "Font Size: ${settings.subtitleFontSizeSp.toInt()} sp",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                        Slider(
                            value = settings.subtitleFontSizeSp,
                            onValueChange = { viewModel.updateSubtitleFontSize(it) },
                            valueRange = 12f..32f,
                            colors = SliderDefaults.colors(thumbColor = palette.accentPrimary, activeTrackColor = palette.accentPrimary)
                        )

                        Text("Text Color", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SubtitleColorPreset.entries.forEach { preset ->
                                val selected = settings.subtitleTextColorArgb == preset.colorArgb
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) palette.accentPrimary.copy(alpha = 0.18f) else palette.background,
                                    border = BorderStroke(1.dp, if (selected) palette.accentPrimary else palette.border),
                                    modifier = Modifier.clickable { viewModel.updateSubtitleColor(preset.colorArgb) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color(preset.colorArgb))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(preset.label, color = palette.textPrimary, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Background Opacity: ${(settings.subtitleBackgroundOpacity * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                        Slider(
                            value = settings.subtitleBackgroundOpacity,
                            onValueChange = { viewModel.updateSubtitleBgOpacity(it) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = palette.accentPrimary, activeTrackColor = palette.accentPrimary)
                        )

                        Text(
                            text = "Vertical Position: ${(settings.subtitleBottomOffsetFraction * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                        Slider(
                            value = settings.subtitleBottomOffsetFraction,
                            onValueChange = { viewModel.updateSubtitlePosition(it) },
                            valueRange = 0.04f..0.40f,
                            colors = SliderDefaults.colors(thumbColor = palette.accentPrimary, activeTrackColor = palette.accentPrimary)
                        )

                        Text(
                            text = "Subtitle Delay: ${settings.subtitleDelayMs} ms",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.updateSubtitleDelay(settings.subtitleDelayMs - 250L) }) {
                                Text("-250ms", color = palette.textPrimary)
                            }
                            OutlinedButton(onClick = { viewModel.updateSubtitleDelay(0L) }) {
                                Text("Reset", color = palette.accentPrimary)
                            }
                            OutlinedButton(onClick = { viewModel.updateSubtitleDelay(settings.subtitleDelayMs + 250L) }) {
                                Text("+250ms", color = palette.textPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                null -> Unit
            }
        }
    }
}

@Composable
private fun SpeedChip(speed: Float, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalXivoxPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) palette.accentPrimary else palette.background,
        border = BorderStroke(1.dp, if (selected) palette.accentPrimary else palette.border),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = "${speed}x",
            color = if (selected) Color.Black else palette.textPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun TrackSelectionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalXivoxPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) palette.accentPrimary.copy(alpha = 0.16f) else palette.background,
        border = BorderStroke(1.dp, if (selected) palette.accentPrimary else palette.border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (selected) palette.accentPrimary else palette.textPrimary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}
