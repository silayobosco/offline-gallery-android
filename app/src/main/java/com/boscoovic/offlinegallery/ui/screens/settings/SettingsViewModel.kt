package com.boscoovic.offlinegallery.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import com.boscoovic.offlinegallery.data.settings.DefaultVideoEndAction
import com.boscoovic.offlinegallery.data.settings.SettingsManager
import com.boscoovic.offlinegallery.data.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val gridColumns: Int = 3,
    val autoPlayVideos: Boolean = true,
    val videoEndAction: DefaultVideoEndAction = DefaultVideoEndAction.LOOP,
    val showDetailsBanner: Boolean = true,
    val trashCount: Int = 0,
    val cacheSizeText: String = "Calculating..."
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val repository: MediaRepository
) : ViewModel() {

    private val _cacheSizeText = MutableStateFlow("Calculating...")

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsManager.themeMode,
        settingsManager.dynamicColor,
        settingsManager.defaultGridColumns,
        settingsManager.autoPlayVideos,
        settingsManager.videoEndAction,
        settingsManager.showDetailsBanner,
        repository.getTrashItems().map { it.size },
        _cacheSizeText
    ) { args: Array<Any?> ->
        SettingsUiState(
            themeMode = args[0] as ThemeMode,
            dynamicColor = args[1] as Boolean,
            gridColumns = args[2] as Int,
            autoPlayVideos = args[3] as Boolean,
            videoEndAction = args[4] as DefaultVideoEndAction,
            showDetailsBanner = args[5] as Boolean,
            trashCount = args[6] as Int,
            cacheSizeText = args[7] as String
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        refreshCacheSize()
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsManager.setThemeMode(mode)
    }

    fun setDynamicColor(enabled: Boolean) {
        settingsManager.setDynamicColor(enabled)
    }

    fun setDefaultGridColumns(columns: Int) {
        settingsManager.setDefaultGridColumns(columns)
    }

    fun setAutoPlayVideos(enabled: Boolean) {
        settingsManager.setAutoPlayVideos(enabled)
    }

    fun setVideoEndAction(action: DefaultVideoEndAction) {
        settingsManager.setVideoEndAction(action)
    }

    fun setShowDetailsBanner(enabled: Boolean) {
        settingsManager.setShowDetailsBanner(enabled)
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val bytes = settingsManager.getCacheSizeBytes()
            _cacheSizeText.value = formatBytes(bytes)
        }
    }

    fun clearCache(onCleared: (String) -> Unit) {
        viewModelScope.launch {
            val clearedBytes = settingsManager.clearCache()
            val text = formatBytes(clearedBytes)
            refreshCacheSize()
            onCleared(text)
        }
    }

    fun emptyTrash(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.emptyTrash()
            onDone()
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes <= 0L -> "0 B"
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024L * 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
