package com.xivox.player.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xivox.player.domain.model.AccentTheme
import com.xivox.player.domain.model.LayoutMode
import com.xivox.player.domain.model.OrientationMode
import com.xivox.player.domain.model.ResumeBehavior
import com.xivox.player.domain.model.SortOption
import com.xivox.player.domain.model.SubtitleColorPreset
import com.xivox.player.domain.model.ThemeMode
import com.xivox.player.domain.model.XivoxSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.xivoxDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "xivox_settings"
)

class XivoxPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val LAYOUT_MODE = stringPreferencesKey("layout_mode")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val RESUME_BEHAVIOR = stringPreferencesKey("resume_behavior")
        val ORIENTATION_MODE = stringPreferencesKey("orientation_mode")
        val CONTROLS_TIMEOUT = intPreferencesKey("controls_timeout_sec")
        val SHOW_PLAYER_CLOCK = booleanPreferencesKey("show_player_clock")

        val SUBTITLES_ENABLED = booleanPreferencesKey("subtitles_enabled_default")
        val AUTO_SELECT_SUBTITLES = booleanPreferencesKey("auto_select_subtitles")
        val PREFERRED_SUB_LANG = stringPreferencesKey("preferred_sub_lang")
        val SUB_FONT_SIZE = floatPreferencesKey("sub_font_size_sp")
        val SUB_TEXT_COLOR = longPreferencesKey("sub_text_color_argb")
        val SUB_BG_OPACITY = floatPreferencesKey("sub_bg_opacity")
        val SUB_BOTTOM_OFFSET = floatPreferencesKey("sub_bottom_offset")
        val SUB_DELAY_MS = longPreferencesKey("sub_delay_ms")

        val GESTURE_DOUBLE_TAP = booleanPreferencesKey("gesture_double_tap")
        val GESTURE_BRIGHTNESS = booleanPreferencesKey("gesture_brightness")
        val GESTURE_VOLUME = booleanPreferencesKey("gesture_volume")
        val GESTURE_SEEK_SWIPE = booleanPreferencesKey("gesture_seek_swipe")
        val GESTURE_LONG_PRESS_2X = booleanPreferencesKey("gesture_long_press_2x")

        val MIN_VIDEO_DURATION_SEC = intPreferencesKey("min_video_duration_sec")
        val SAF_TREE_URIS = stringSetPreferencesKey("saf_tree_uris")
    }

    val settingsFlow: Flow<XivoxSettings> = context.xivoxDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            XivoxSettings(
                themeMode = enumOrDefault(prefs[Keys.THEME_MODE], ThemeMode.AMOLED),
                accentTheme = enumOrDefault(prefs[Keys.ACCENT_THEME], AccentTheme.CYAN_VIOLET),
                layoutMode = enumOrDefault(prefs[Keys.LAYOUT_MODE], LayoutMode.GRID),
                sortOption = enumOrDefault(prefs[Keys.SORT_OPTION], SortOption.DATE),
                defaultPlaybackSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1.0f,
                resumeBehavior = enumOrDefault(prefs[Keys.RESUME_BEHAVIOR], ResumeBehavior.ASK),
                orientationMode = enumOrDefault(prefs[Keys.ORIENTATION_MODE], OrientationMode.AUTO_VIDEO),
                controlsTimeoutSeconds = prefs[Keys.CONTROLS_TIMEOUT] ?: 4,
                showBatteryAndClockInPlayer = prefs[Keys.SHOW_PLAYER_CLOCK] ?: true,

                subtitlesEnabledByDefault = prefs[Keys.SUBTITLES_ENABLED] ?: true,
                autoSelectSubtitles = prefs[Keys.AUTO_SELECT_SUBTITLES] ?: true,
                preferredSubtitleLanguage = prefs[Keys.PREFERRED_SUB_LANG] ?: "en",
                subtitleFontSizeSp = prefs[Keys.SUB_FONT_SIZE] ?: 18f,
                subtitleTextColorArgb = prefs[Keys.SUB_TEXT_COLOR] ?: SubtitleColorPreset.PURE_WHITE.colorArgb,
                subtitleBackgroundOpacity = prefs[Keys.SUB_BG_OPACITY] ?: 0.72f,
                subtitleBottomOffsetFraction = prefs[Keys.SUB_BOTTOM_OFFSET] ?: 0.10f,
                subtitleDelayMs = prefs[Keys.SUB_DELAY_MS] ?: 0L,

                doubleTapSeekEnabled = prefs[Keys.GESTURE_DOUBLE_TAP] ?: true,
                brightnessSwipeEnabled = prefs[Keys.GESTURE_BRIGHTNESS] ?: true,
                volumeSwipeEnabled = prefs[Keys.GESTURE_VOLUME] ?: true,
                horizontalSeekSwipeEnabled = prefs[Keys.GESTURE_SEEK_SWIPE] ?: true,
                longPress2xSpeedEnabled = prefs[Keys.GESTURE_LONG_PRESS_2X] ?: true,

                minVideoDurationSeconds = prefs[Keys.MIN_VIDEO_DURATION_SEC] ?: 0,
                safTreeUris = prefs[Keys.SAF_TREE_URIS] ?: emptySet()
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setAccentTheme(accent: AccentTheme) = edit { it[Keys.ACCENT_THEME] = accent.name }
    suspend fun setLayoutMode(layout: LayoutMode) = edit { it[Keys.LAYOUT_MODE] = layout.name }
    suspend fun setSortOption(sort: SortOption) = edit { it[Keys.SORT_OPTION] = sort.name }
    suspend fun setDefaultSpeed(speed: Float) = edit { it[Keys.DEFAULT_SPEED] = speed.coerceIn(0.25f, 2.5f) }
    suspend fun setResumeBehavior(behavior: ResumeBehavior) = edit { it[Keys.RESUME_BEHAVIOR] = behavior.name }
    suspend fun setOrientationMode(mode: OrientationMode) = edit { it[Keys.ORIENTATION_MODE] = mode.name }
    suspend fun setControlsTimeout(seconds: Int) = edit { it[Keys.CONTROLS_TIMEOUT] = seconds.coerceIn(2, 10) }
    suspend fun setShowPlayerClock(show: Boolean) = edit { it[Keys.SHOW_PLAYER_CLOCK] = show }

    suspend fun setSubtitlesEnabledByDefault(enabled: Boolean) = edit { it[Keys.SUBTITLES_ENABLED] = enabled }
    suspend fun setAutoSelectSubtitles(enabled: Boolean) = edit { it[Keys.AUTO_SELECT_SUBTITLES] = enabled }
    suspend fun setPreferredSubtitleLanguage(lang: String) = edit { it[Keys.PREFERRED_SUB_LANG] = lang }
    suspend fun setSubtitleFontSize(sp: Float) = edit { it[Keys.SUB_FONT_SIZE] = sp.coerceIn(12f, 34f) }
    suspend fun setSubtitleTextColor(argb: Long) = edit { it[Keys.SUB_TEXT_COLOR] = argb }
    suspend fun setSubtitleBackgroundOpacity(opacity: Float) = edit { it[Keys.SUB_BG_OPACITY] = opacity.coerceIn(0f, 1f) }
    suspend fun setSubtitleBottomOffset(fraction: Float) = edit { it[Keys.SUB_BOTTOM_OFFSET] = fraction.coerceIn(0.04f, 0.45f) }
    suspend fun setSubtitleDelayMs(delayMs: Long) = edit { it[Keys.SUB_DELAY_MS] = delayMs.coerceIn(-10_000L, 10_000L) }

    suspend fun setDoubleTapSeekEnabled(enabled: Boolean) = edit { it[Keys.GESTURE_DOUBLE_TAP] = enabled }
    suspend fun setBrightnessSwipeEnabled(enabled: Boolean) = edit { it[Keys.GESTURE_BRIGHTNESS] = enabled }
    suspend fun setVolumeSwipeEnabled(enabled: Boolean) = edit { it[Keys.GESTURE_VOLUME] = enabled }
    suspend fun setHorizontalSeekSwipeEnabled(enabled: Boolean) = edit { it[Keys.GESTURE_SEEK_SWIPE] = enabled }
    suspend fun setLongPress2xEnabled(enabled: Boolean) = edit { it[Keys.GESTURE_LONG_PRESS_2X] = enabled }

    suspend fun setMinVideoDurationSeconds(sec: Int) = edit { it[Keys.MIN_VIDEO_DURATION_SEC] = sec.coerceAtLeast(0) }

    suspend fun addSafTreeUri(uriString: String) = edit { prefs ->
        val current = prefs[Keys.SAF_TREE_URIS] ?: emptySet()
        prefs[Keys.SAF_TREE_URIS] = current + uriString
    }

    suspend fun removeSafTreeUri(uriString: String) = edit { prefs ->
        val current = prefs[Keys.SAF_TREE_URIS] ?: emptySet()
        prefs[Keys.SAF_TREE_URIS] = current - uriString
    }

    suspend fun resetAllSettings() = edit { it.clear() }

    private suspend inline fun edit(crossinline block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.xivoxDataStore.edit { prefs -> block(prefs) }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }
}
