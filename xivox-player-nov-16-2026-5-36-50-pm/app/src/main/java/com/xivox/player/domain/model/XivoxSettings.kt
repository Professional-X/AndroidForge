package com.xivox.player.domain.model

enum class ThemeMode(val label: String, val subtitle: String) {
    AMOLED("AMOLED Black", "Pure #000000 OLED canvas"),
    DARK_COSMOS("Cosmic Obsidian", "Deep twilight slate surfaces")
}

enum class AccentTheme(val label: String, val primaryHex: Long, val secondaryHex: Long) {
    CYAN_VIOLET("Cyber Cyan", 0xFF00E5FF, 0xFF8B5CF6),
    ELECTRIC_VIOLET("Electric Violet", 0xFF9B5CFF, 0xFF00C8FF),
    AURORA_EMERALD("Aurora Mint", 0xFF10B981, 0xFF00E5FF),
    SOLAR_AMBER("Solar Amber", 0xFFF59E0B, 0xFFEC4899)
}

enum class OrientationMode(val label: String) {
    AUTO_VIDEO("Match Video"),
    LANDSCAPE("Landscape"),
    PORTRAIT("Portrait"),
    SYSTEM("System Sensor")
}

enum class ResumeBehavior(val label: String, val description: String) {
    ASK("Ask Every Time", "Show Resume from MM:SS? prompt"),
    ALWAYS_RESUME("Always Resume", "Jump directly to last saved position"),
    START_OVER("Start From Beginning", "Always play from 00:00")
}

enum class SubtitleColorPreset(val label: String, val colorArgb: Long) {
    PURE_WHITE("White", 0xFFFFFFFF),
    NEON_CYAN("Neon Cyan", 0xFF00E5FF),
    CINEMA_YELLOW("Cinema Gold", 0xFFFACC15),
    SOFT_SILVER("Silver", 0xFFCBD5E1)
}

data class XivoxSettings(
    val themeMode: ThemeMode = ThemeMode.AMOLED,
    val accentTheme: AccentTheme = AccentTheme.CYAN_VIOLET,
    val layoutMode: LayoutMode = LayoutMode.GRID,
    val sortOption: SortOption = SortOption.DATE,
    val defaultPlaybackSpeed: Float = 1.0f,
    val resumeBehavior: ResumeBehavior = ResumeBehavior.ASK,
    val orientationMode: OrientationMode = OrientationMode.AUTO_VIDEO,
    val controlsTimeoutSeconds: Int = 4,
    val showBatteryAndClockInPlayer: Boolean = true,

    // Subtitles
    val subtitlesEnabledByDefault: Boolean = true,
    val autoSelectSubtitles: Boolean = true,
    val preferredSubtitleLanguage: String = "en",
    val subtitleFontSizeSp: Float = 18f,
    val subtitleTextColorArgb: Long = SubtitleColorPreset.PURE_WHITE.colorArgb,
    val subtitleBackgroundOpacity: Float = 0.72f,
    val subtitleBottomOffsetFraction: Float = 0.10f,
    val subtitleDelayMs: Long = 0L,

    // Gestures
    val doubleTapSeekEnabled: Boolean = true,
    val brightnessSwipeEnabled: Boolean = true,
    val volumeSwipeEnabled: Boolean = true,
    val horizontalSeekSwipeEnabled: Boolean = true,
    val longPress2xSpeedEnabled: Boolean = true,

    // Scanner
    val minVideoDurationSeconds: Int = 0,
    val safTreeUris: Set<String> = emptySet()
)
