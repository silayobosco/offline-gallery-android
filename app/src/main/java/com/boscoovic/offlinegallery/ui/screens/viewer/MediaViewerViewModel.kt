package com.boscoovic.offlinegallery.ui.screens.viewer

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boscoovic.offlinegallery.data.model.ExifData
import com.boscoovic.offlinegallery.data.model.MediaFilter
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaViewerUiState(
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val isChromeVisible: Boolean = true,
    val showDetailsSheet: Boolean = false,
    val currentExif: ExifData? = null
)

@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    private val repository: MediaRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val initialMediaId: Long = savedStateHandle.get<String>("mediaId")?.toLongOrNull() ?: 0L
    private val contextType: String = savedStateHandle.get<String>("contextType") ?: "ALL"
    private val contextArg: String = savedStateHandle.get<String>("contextArg") ?: ""

    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _currentIndex = MutableStateFlow(0)
    private val _isChromeVisible = MutableStateFlow(true)
    private val _showDetailsSheet = MutableStateFlow(false)
    private val _currentExif = MutableStateFlow<ExifData?>(null)

    val uiState: StateFlow<MediaViewerUiState> = combine(
        _items,
        _currentIndex,
        _isChromeVisible,
        _showDetailsSheet,
        _currentExif
    ) { items, index, chrome, showDetails, exif ->
        MediaViewerUiState(
            items = items,
            currentIndex = index,
            isChromeVisible = chrome,
            showDetailsSheet = showDetails,
            currentExif = exif
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MediaViewerUiState()
    )

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            val flow = when (contextType) {
                "ALBUM" -> {
                    val albumId = contextArg.toLongOrNull() ?: 0L
                    repository.getAlbumMedia(albumId, isCustom = false)
                }
                "CUSTOM_ALBUM" -> {
                    val albumId = contextArg.toLongOrNull() ?: 0L
                    repository.getAlbumMedia(albumId, isCustom = true)
                }
                "FAVORITES" -> repository.getAllMedia(MediaFilter.FAVORITES)
                else -> repository.getAllMedia(MediaFilter.ALL)
            }

            flow.collect { list ->
                _items.value = list
                val targetIndex = list.indexOfFirst { it.id == initialMediaId }
                if (targetIndex >= 0) {
                    _currentIndex.value = targetIndex
                    loadExif(list[targetIndex])
                } else if (list.isNotEmpty()) {
                    _currentIndex.value = 0
                    loadExif(list[0])
                }
            }
        }
    }

    fun onPageChanged(index: Int) {
        _currentIndex.value = index
        val items = _items.value
        if (index in items.indices) {
            loadExif(items[index])
        }
    }

    fun toggleChrome() {
        _isChromeVisible.value = !_isChromeVisible.value
    }

    fun toggleDetailsSheet(show: Boolean) {
        _showDetailsSheet.value = show
    }

    fun toggleFavorite() {
        val current = currentItem ?: return
        viewModelScope.launch {
            repository.toggleFavorite(current.id, !current.isFavorite)
        }
    }

    fun setFavorite(isFavorite: Boolean) {
        val current = currentItem ?: return
        viewModelScope.launch {
            repository.toggleFavorite(current.id, isFavorite)
        }
    }

    fun deleteCurrent(onDeleted: () -> Unit) {
        val current = currentItem ?: return
        viewModelScope.launch {
            repository.moveToTrash(listOf(current))
            onDeleted()
        }
    }

    fun shareCurrent() {
        val current = currentItem ?: return
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, current.uri)
            type = current.mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Share media").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun loadExif(item: MediaItem) {
        viewModelScope.launch {
            _currentExif.value = repository.getExifData(item)
        }
    }

    val currentItem: MediaItem?
        get() {
            val list = _items.value
            val idx = _currentIndex.value
            return if (idx in list.indices) list[idx] else null
        }
}

