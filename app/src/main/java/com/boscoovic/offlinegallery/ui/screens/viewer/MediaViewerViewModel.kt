package com.boscoovic.offlinegallery.ui.screens.viewer

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boscoovic.offlinegallery.data.model.Album
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

enum class ShuffleMode {
    OFF,
    FOLDER,
    UNIVERSAL
}

enum class ScrollDirection {
    FORWARD,  // scrolled down (next)
    BACKWARD  // scrolled up (previous)
}

data class MediaViewerUiState(
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val isChromeVisible: Boolean = true,
    val showDetailsSheet: Boolean = false,
    val showMoreSheet: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val shuffleMode: ShuffleMode = ShuffleMode.OFF,
    val currentExif: ExifData? = null,
    val allAlbums: List<Album> = emptyList()
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

    private val _originalItems = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _allMediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _currentIndex = MutableStateFlow(0)
    private val _isChromeVisible = MutableStateFlow(true)
    private val _showDetailsSheet = MutableStateFlow(false)
    private val _showMoreSheet = MutableStateFlow(false)
    private val _shuffleMode = MutableStateFlow(ShuffleMode.OFF)
    private val _currentExif = MutableStateFlow<ExifData?>(null)
    private val _allAlbums = MutableStateFlow<List<Album>>(emptyList())
    private var lastScrollDirection = ScrollDirection.FORWARD

    val uiState: StateFlow<MediaViewerUiState> = combine(
        _items,
        _currentIndex,
        _isChromeVisible,
        _showDetailsSheet,
        _showMoreSheet,
        _shuffleMode,
        _currentExif,
        _allAlbums
    ) { args: Array<Any?> ->
        val currentShuffleMode = args[5] as ShuffleMode
        @Suppress("UNCHECKED_CAST")
        MediaViewerUiState(
            items = args[0] as List<MediaItem>,
            currentIndex = args[1] as Int,
            isChromeVisible = args[2] as Boolean,
            showDetailsSheet = args[3] as Boolean,
            showMoreSheet = args[4] as Boolean,
            isShuffleEnabled = currentShuffleMode != ShuffleMode.OFF,
            shuffleMode = currentShuffleMode,
            currentExif = args[6] as ExifData?,
            allAlbums = args[7] as List<Album>
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MediaViewerUiState()
    )

    init {
        loadItems()
        loadAlbums()
        loadAllMedia()
    }

    private fun loadAllMedia() {
        viewModelScope.launch {
            repository.getAllMedia(MediaFilter.ALL).collect {
                _allMediaItems.value = it
            }
        }
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            repository.getAlbums().collect {
                _allAlbums.value = it
            }
        }
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
                _originalItems.value = list
                val curr = currentItem
                when (_shuffleMode.value) {
                    ShuffleMode.OFF -> {
                        val isInitial = _items.value.isEmpty()
                        _items.value = list
                        if (isInitial) {
                            val targetIndex = list.indexOfFirst { it.id == initialMediaId }
                            if (targetIndex >= 0) {
                                _currentIndex.value = targetIndex
                                loadExif(list[targetIndex])
                            } else if (list.isNotEmpty()) {
                                _currentIndex.value = 0
                                loadExif(list[0])
                            }
                        } else {
                            val currId = curr?.id
                            val targetIndex = if (currId != null) list.indexOfFirst { it.id == currId } else -1
                            if (targetIndex >= 0) {
                                _currentIndex.value = targetIndex
                                loadExif(list[targetIndex])
                            } else if (list.isNotEmpty()) {
                                val oldIndex = _currentIndex.value
                                val fallbackIndex = when (lastScrollDirection) {
                                    ScrollDirection.FORWARD -> oldIndex.coerceIn(0, list.size - 1)
                                    ScrollDirection.BACKWARD -> (oldIndex - 1).coerceIn(0, list.size - 1)
                                }
                                _currentIndex.value = fallbackIndex
                                loadExif(list[fallbackIndex])
                            } else {
                                _currentIndex.value = 0
                            }
                        }
                    }
                    ShuffleMode.FOLDER, ShuffleMode.UNIVERSAL -> {
                        if (_items.value.isEmpty()) {
                            applyShuffle(_shuffleMode.value)
                        } else {
                            val validIds = list.map { it.id }.toSet()
                            val currentQueue = _items.value
                            val updatedQueue = currentQueue.filter { it.id in validIds }
                                .map { item -> list.firstOrNull { it.id == item.id } ?: item }
                            _items.value = updatedQueue

                            val currId = curr?.id
                            val targetIndex = if (currId != null) updatedQueue.indexOfFirst { it.id == currId } else -1
                            if (targetIndex >= 0) {
                                _currentIndex.value = targetIndex
                                loadExif(updatedQueue[targetIndex])
                            } else if (updatedQueue.isNotEmpty()) {
                                val oldIndex = _currentIndex.value
                                val fallbackIndex = when (lastScrollDirection) {
                                    ScrollDirection.FORWARD -> oldIndex.coerceIn(0, updatedQueue.size - 1)
                                    ScrollDirection.BACKWARD -> (oldIndex - 1).coerceIn(0, updatedQueue.size - 1)
                                }
                                _currentIndex.value = fallbackIndex
                                loadExif(updatedQueue[fallbackIndex])
                            } else {
                                _currentIndex.value = 0
                            }
                        }
                    }
                }
            }
        }
    }

    fun setShuffleMode(mode: ShuffleMode) {
        _shuffleMode.value = mode
        applyShuffle(mode)
    }

    private fun applyShuffle(mode: ShuffleMode) {
        val current = currentItem
        val original = _originalItems.value
        val allMedia = _allMediaItems.value

        when (mode) {
            ShuffleMode.OFF -> {
                _items.value = original
                val targetIdx = if (current != null) original.indexOfFirst { it.id == current.id } else -1
                if (targetIdx >= 0) {
                    _currentIndex.value = targetIdx
                }
            }
            ShuffleMode.FOLDER -> {
                val bucketId = current?.bucketId
                val sourceList = if (contextType == "ALBUM" || contextType == "CUSTOM_ALBUM") {
                    original
                } else if (bucketId != null && bucketId != 0L) {
                    allMedia.filter { it.bucketId == bucketId }.ifEmpty { original }
                } else if (current?.bucketName != null) {
                    allMedia.filter { it.bucketName == current.bucketName }.ifEmpty { original }
                } else {
                    original
                }
                if (sourceList.isNotEmpty()) {
                    val remaining = sourceList.filter { it.id != current?.id }.shuffled()
                    val newList = if (current != null) listOf(current) + remaining else remaining
                    _items.value = newList
                    _currentIndex.value = 0
                }
            }
            ShuffleMode.UNIVERSAL -> {
                val sourceList = if (allMedia.isNotEmpty()) allMedia else original
                if (sourceList.isNotEmpty()) {
                    val remaining = sourceList.filter { it.id != current?.id }.shuffled()
                    val newList = if (current != null) listOf(current) + remaining else remaining
                    _items.value = newList
                    _currentIndex.value = 0
                }
            }
        }
    }

    fun cycleShuffleMode(): ShuffleMode {
        val nextMode = when (_shuffleMode.value) {
            ShuffleMode.OFF -> ShuffleMode.FOLDER
            ShuffleMode.FOLDER -> ShuffleMode.UNIVERSAL
            ShuffleMode.UNIVERSAL -> ShuffleMode.OFF
        }
        setShuffleMode(nextMode)
        return nextMode
    }

    fun toggleShuffle() {
        cycleShuffleMode()
    }

    fun onPageChanged(index: Int) {
        val previousIndex = _currentIndex.value
        if (index > previousIndex) {
            lastScrollDirection = ScrollDirection.FORWARD
        } else if (index < previousIndex) {
            lastScrollDirection = ScrollDirection.BACKWARD
        }
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

    fun toggleMoreSheet(show: Boolean) {
        _showMoreSheet.value = show
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

    fun deleteCurrent(onDeleted: (isLastItem: Boolean) -> Unit = {}) {
        val current = currentItem ?: return
        val currentId = current.id
        val oldIndex = _currentIndex.value
        val currentList = _items.value
        val remainingList = currentList.filter { it.id != currentId }
        val wasLastItem = remainingList.isEmpty()

        if (remainingList.isNotEmpty()) {
            val targetIndex = when (lastScrollDirection) {
                ScrollDirection.FORWARD -> oldIndex.coerceIn(0, remainingList.size - 1)
                ScrollDirection.BACKWARD -> (oldIndex - 1).coerceIn(0, remainingList.size - 1)
            }
            _items.value = remainingList
            _currentIndex.value = targetIndex
            loadExif(remainingList[targetIndex])
        } else {
            _items.value = emptyList()
            _currentIndex.value = 0
        }

        viewModelScope.launch {
            repository.moveToTrash(listOf(current))
            onDeleted(wasLastItem)
        }
    }

    fun hideCurrent(onHidden: (isLastItem: Boolean) -> Unit = {}) {
        val current = currentItem ?: return
        val currentId = current.id
        val oldIndex = _currentIndex.value
        val currentList = _items.value
        val remainingList = currentList.filter { it.id != currentId }
        val wasLastItem = remainingList.isEmpty()

        if (remainingList.isNotEmpty()) {
            val targetIndex = when (lastScrollDirection) {
                ScrollDirection.FORWARD -> oldIndex.coerceIn(0, remainingList.size - 1)
                ScrollDirection.BACKWARD -> (oldIndex - 1).coerceIn(0, remainingList.size - 1)
            }
            _items.value = remainingList
            _currentIndex.value = targetIndex
            loadExif(remainingList[targetIndex])
        } else {
            _items.value = emptyList()
            _currentIndex.value = 0
        }

        viewModelScope.launch {
            repository.setHidden(current.id, true)
            onHidden(wasLastItem)
        }
    }

    fun moveCurrentToAlbum(targetAlbum: Album, onMoved: (isLastItem: Boolean) -> Unit = {}) {
        val current = currentItem ?: return
        val currentId = current.id
        val oldIndex = _currentIndex.value
        val currentList = _items.value
        val remainingList = currentList.filter { it.id != currentId }
        val wasLastItem = remainingList.isEmpty()

        if (remainingList.isNotEmpty()) {
            val targetIndex = when (lastScrollDirection) {
                ScrollDirection.FORWARD -> oldIndex.coerceIn(0, remainingList.size - 1)
                ScrollDirection.BACKWARD -> (oldIndex - 1).coerceIn(0, remainingList.size - 1)
            }
            _items.value = remainingList
            _currentIndex.value = targetIndex
            loadExif(remainingList[targetIndex])
        } else {
            _items.value = emptyList()
            _currentIndex.value = 0
        }

        viewModelScope.launch {
            val success = repository.moveMediaToAlbum(listOf(current), targetAlbum)
            if (success) {
                onMoved(wasLastItem)
            }
        }
    }

    fun useAs() {
        val current = currentItem ?: return
        try {
            val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                setDataAndType(current.uri, current.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra("mimeType", current.mimeType)
            }
            val chooser = Intent.createChooser(intent, "Use as").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun editIn(fallbackToInternal: (Long) -> Unit) {
        val current = currentItem ?: return
        try {
            val intent = Intent(Intent.ACTION_EDIT).apply {
                setDataAndType(current.uri, current.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Edit in").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            fallbackToInternal(current.id)
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

