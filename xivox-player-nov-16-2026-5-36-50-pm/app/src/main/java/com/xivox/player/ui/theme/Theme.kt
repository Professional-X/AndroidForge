package com.xivox.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.xivox.player.domain.model.AccentTheme
import com.xivox.player.domain.model.ThemeMode

@Immutable
data class XivoxPalette(
    val background: Color,
    val surface: Color,
    val surfaceGlass: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val accentBrush: Brush,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

val LocalXivoxPalette = staticCompositionLocalOf {
    XivoxPalette(
        background = AmoledBlack,
        surface = SurfaceElevated,
        surfaceGlass = Color(0xCC0C101C),
        accentPrimary = NeonCyan,
        accentSecondary = ElectricViolet,
        accentBrush = Brush.linearGradient(listOf(NeonCyan, ElectricViolet)),
        border = GlassBorder,
        textPrimary = TextPrimary,
        textSecondary = TextSecondary
    )
}

@Composable
fun XivoxTheme(
    themeMode: ThemeMode = ThemeMode.AMOLED,
    accentTheme: AccentTheme = AccentTheme.CYAN_VIOLET,
    content: @Composable () -> Unit
) {
    val bg = if (themeMode == ThemeMode.AMOLED) AmoledBlack else CosmicBackground
    val surface = if (themeMode == ThemeMode.AMOLED) Color(0xFF0A0D16) else SurfaceElevated
    val accentPrimary = Color(accentTheme.primaryHex)
    val accentSecondary = Color(accentTheme.secondaryHex)

    val palette = XivoxPalette(
        background = bg,
        surface = surface,
        surfaceGlass = surface.copy(alpha = 0.82f),
        accentPrimary = accentPrimary,
        accentSecondary = accentSecondary,
        accentBrush = Brush.linearGradient(listOf(accentPrimary, accentSecondary)),
        border = GlassBorder,
        textPrimary = TextPrimary,
        textSecondary = TextSecondary
    )

    val colorScheme = darkColorScheme(
        primary = accentPrimary,
        secondary = accentSecondary,
        background = bg,
        surface = surface,
        surfaceVariant = SurfaceCard,
        onPrimary = AmoledBlack,
        onSecondary = TextPrimary,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        onSurfaceVariant = TextSecondary,
        error = CrimsonError
    )

    CompositionLocalProvider(LocalXivoxPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = XivoxTypography,
            content = content
        )
    }
}
