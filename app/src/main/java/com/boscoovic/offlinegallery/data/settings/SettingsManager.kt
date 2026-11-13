package com.boscoovic.offlinegallery.data.settings

import android.content.Context
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode(val title: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class DefaultVideoEndAction(val title: String) {
    LOOP("Loop / Repeat one"),
    NEXT("Auto play next video"),
    STOP("Stop at end")
}

enum class ViewerScrollDirection(val title: String) {
    VERTICAL("Vertical (TikTok style)"),
    HORIZONTAL("Horizontal (Classic Gallery)")
}

@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MediaRepository
) {
    private val prefs = context.getSharedPreferences("offline_gallery_preferences", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        try {
            ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, true))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _defaultGridColumns = MutableStateFlow(prefs.getInt(KEY_GRID_COLUMNS, 3).coerceIn(2, 5))
    val defaultGridColumns: StateFlow<Int> = _defaultGridColumns.asStateFlow()

    private val _autoPlayVideos = MutableStateFlow(prefs.getBoolean(KEY_AUTO_PLAY_VIDEOS, true))
    val autoPlayVideos: StateFlow<Boolean> = _autoPlayVideos.asStateFlow()

    private val _videoEndAction = MutableStateFlow(
        try {
            DefaultVideoEndAction.valueOf(prefs.getString(KEY_VIDEO_END_ACTION, DefaultVideoEndAction.LOOP.name) ?: DefaultVideoEndAction.LOOP.name)
        } catch (e: Exception) {
            DefaultVideoEndAction.LOOP
        }
    )
    val videoEndAction: StateFlow<DefaultVideoEndAction> = _videoEndAction.asStateFlow()

    private val _showDetailsBanner = MutableStateFlow(prefs.getBoolean(KEY_SHOW_DETAILS_BANNER, true))
    val showDetailsBanner: StateFlow<Boolean> = _showDetailsBanner.asStateFlow()

    private val _viewerScrollDirection = MutableStateFlow(
        try {
            ViewerScrollDirection.valueOf(prefs.getString(KEY_SCROLL_DIRECTION, ViewerScrollDirection.VERTICAL.name) ?: ViewerScrollDirection.VERTICAL.name)
        } catch (e: Exception) {
            ViewerScrollDirection.VERTICAL
        }
    )
    val viewerScrollDirection: StateFlow<ViewerScrollDirection> = _viewerScrollDirection.asStateFlow()

    private val _hasSeenViewerOnboarding = MutableStateFlow(prefs.getBoolean(KEY_VIEWER_ONBOARDING_SEEN, false))
    val hasSeenViewerOnboarding: StateFlow<Boolean> = _hasSeenViewerOnboarding.asStateFlow()

    private val _showFilmstrip = MutableStateFlow(prefs.getBoolean(KEY_SHOW_FILMSTRIP, true))
    val showFilmstrip: StateFlow<Boolean> = _showFilmstrip.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
    }

    fun setDefaultGridColumns(columns: Int) {
        val clamped = columns.coerceIn(2, 5)
        _defaultGridColumns.value = clamped
        prefs.edit().putInt(KEY_GRID_COLUMNS, clamped).apply()
    }

    fun setAutoPlayVideos(enabled: Boolean) {
        _autoPlayVideos.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_PLAY_VIDEOS, enabled).apply()
    }

    fun setVideoEndAction(action: DefaultVideoEndAction) {
        _videoEndAction.value = action
        prefs.edit().putString(KEY_VIDEO_END_ACTION, action.name).apply()
    }

    fun setShowDetailsBanner(enabled: Boolean) {
        _showDetailsBanner.value = enabled
        prefs.edit().putBoolean(KEY_SHOW_DETAILS_BANNER, enabled).apply()
    }

    fun setViewerScrollDirection(direction: ViewerScrollDirection) {
        _viewerScrollDirection.value = direction
        prefs.edit().putString(KEY_SCROLL_DIRECTION, direction.name).apply()
    }

    fun markViewerOnboardingSeen() {
        _hasSeenViewerOnboarding.value = true
        prefs.edit().putBoolean(KEY_VIEWER_ONBOARDING_SEEN, true).apply()
    }

    fun resetViewerOnboarding() {
        _hasSeenViewerOnboarding.value = false
        prefs.edit().putBoolean(KEY_VIEWER_ONBOARDING_SEEN, false).apply()
    }

    fun setShowFilmstrip(enabled: Boolean) {
        _showFilmstrip.value = enabled
        prefs.edit().putBoolean(KEY_SHOW_FILMSTRIP, enabled).apply()
    }

    suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        var size = 0L
        fun calc(file: File) {
            if (file.isDirectory) {
                file.listFiles()?.forEach { calc(it) }
            } else {
                size += file.length()
            }
        }
        calc(context.cacheDir)
        size
    }

    suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        val initialSize = getCacheSizeBytes()
        fun deleteRecursively(file: File) {
            if (file.isDirectory) {
                file.listFiles()?.forEach { deleteRecursively(it) }
            }
            file.delete()
        }
        context.cacheDir.listFiles()?.forEach { deleteRecursively(it) }
        initialSize
    }

    companion object {
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_DYNAMIC_COLOR = "pref_dynamic_color"
        private const val KEY_GRID_COLUMNS = "pref_grid_columns"
        private const val KEY_AUTO_PLAY_VIDEOS = "pref_auto_play_videos"
        private const val KEY_VIDEO_END_ACTION = "pref_video_end_action"
        private const val KEY_SHOW_DETAILS_BANNER = "pref_show_details_banner"
        private const val KEY_SCROLL_DIRECTION = "pref_viewer_scroll_direction"
        private const val KEY_VIEWER_ONBOARDING_SEEN = "pref_viewer_onboarding_seen"
        private const val KEY_SHOW_FILMSTRIP = "pref_show_filmstrip"
    }
}

