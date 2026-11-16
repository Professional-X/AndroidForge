package com.xivox.player.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.xivox.player.domain.model.VideoItem
import com.xivox.player.ui.theme.LocalXivoxPalette
import com.xivox.player.util.Formatters

@Composable
fun XivoxLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val palette = LocalXivoxPalette.current
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) * 0.44f

        val hex = Path().apply {
            for (i in 0 until 6) {
                val angle = Math.toRadians((60 * i - 90).toDouble())
                val px = cx + (r * kotlin.math.cos(angle)).toFloat()
                val py = cy + (r * kotlin.math.sin(angle)).toFloat()
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }

        drawPath(
            path = hex,
            brush = Brush.linearGradient(listOf(palette.accentPrimary, palette.accentSecondary)),
            style = Stroke(width = w * 0.065f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val playTriangle = Path().apply {
            moveTo(cx - r * 0.28f, cy - r * 0.36f)
            lineTo(cx + r * 0.44f, cy)
            lineTo(cx - r * 0.28f, cy + r * 0.36f)
            close()
        }
        drawPath(
            path = playTriangle,
            brush = Brush.linearGradient(listOf(palette.accentPrimary, palette.accentSecondary))
        )

        drawLine(
            color = Color.White,
            start = Offset(cx - r * 0.44f, cy + r * 0.42f),
            end = Offset(cx + r * 0.44f, cy - r * 0.42f),
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun GlassSurfaceCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val palette = LocalXivoxPalette.current
    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = palette.surfaceGlass,
        border = BorderStroke(1.dp, palette.border)
    ) {
        content()
    }
}

@Composable
fun VideoThumbnailBox(
    video: VideoItem,
    progressRatio: Float?,
    modifier: Modifier = Modifier
) {
    val palette = LocalXivoxPalette.current
    val context = LocalContext.current

    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0A0D16))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(video.uri)
                .videoFrameMillis(1500L)
                .crossfade(true)
                .build(),
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle scrim for timestamp legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.72f))
                    )
                )
        )

        // Duration label
        if (video.durationMs > 0L) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = Formatters.formatDurationMs(video.durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }

        // Resolution tag
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = video.resolutionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = palette.accentPrimary
            )
        }

        // Progress bar if partially watched
        if (progressRatio != null && progressRatio > 0.01f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.White.copy(alpha = 0.22f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressRatio.coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(palette.accentBrush)
                )
            }
        }
    }
}

@Composable
fun VideoGridCard(
    video: VideoItem,
    progressRatio: Float?,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onOpenDetails: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalXivoxPalette.current
    GlassSurfaceCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        onClick = onPlay
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            VideoThumbnailBox(video = video, progressRatio = progressRatio)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenDetails)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${video.formatExtension} · ${Formatters.formatFileSize(video.sizeBytes)} · ${video.folderName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) palette.accentPrimary else palette.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun VideoListRow(
    video: VideoItem,
    progressRatio: Float?,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onOpenDetails: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalXivoxPalette.current
    GlassSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        onClick = onPlay
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoThumbnailBox(
                video = video,
                progressRatio = progressRatio,
                modifier = Modifier.width(136.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenDetails)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${video.resolutionLabel} · ${video.formatExtension} · ${Formatters.formatFileSize(video.sizeBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.folderName,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) palette.accentPrimary else palette.textSecondary
                )
            }
        }
    }
}

@Composable
fun ResumePromptDialog(
    videoTitle: String,
    savedPositionMs: Long,
    onResume: () -> Unit,
    onStartBeginning: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalXivoxPalette.current
    val timeText = Formatters.formatDurationMs(savedPositionMs)

    Dialog(onDismissRequest = onDismiss) {
        GlassSurfaceCard(
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.accentBrush, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.accentBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "RESUME FROM $timeText?",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            ),
                            color = palette.accentPrimary
                        )
                        Text(
                            text = videoTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accentPrimary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Resume from $timeText",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onStartBeginning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Icon(Icons.Rounded.Replay, contentDescription = null, tint = palette.textPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Play from beginning",
                        color = palette.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun StoragePermissionCard(
    onGrantPermission: () -> Unit,
    onPickSafFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalXivoxPalette.current
    GlassSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.VideoLibrary,
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Access Local Device Videos",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
            }
            Text(
                text = "XIVOX scans only local video files on your device. No internet permission, no uploads, and no tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onGrantPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accentPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Scan Device Videos", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onPickSafFile,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open File", color = palette.textPrimary)
                }
            }
        }
    }
}
