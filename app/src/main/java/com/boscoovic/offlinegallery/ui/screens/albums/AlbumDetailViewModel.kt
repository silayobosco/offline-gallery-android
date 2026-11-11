package com.boscoovic.offlinegallery.ui.screens.albums

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boscoovic.offlinegallery.data.model.Album
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.data.model.MediaType
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailUiState(
    val albumId: Long = 0L,
    val albumName: String = "",
    val isCustom: Boolean = false,
    val items: List<MediaItem> = emptyList(),
    val allAlbums: List<Album> = emptyList(),
    val isSidebarVisible: Boolean = true,
    val isSelectionMode: Boolean = false,
    val selectedItemIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true
) {
    val subtitle: String
        get() {
            val imgCount = items.count { it.mediaType == MediaType.IMAGE }
            val vidCount = items.count { it.mediaType == MediaType.VIDEO }
            val parts = mutableListOf<String>()
            if (imgCount > 0) parts.add("$imgCount images")
            if (vidCount > 0) parts.add("$vidCount videos")
            return if (parts.isNotEmpty()) parts.joinToString(" ") else "0 items"
        }
}

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val repository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialAlbumId: Long = savedStateHandle.get<String>("albumId")?.toLongOrNull() ?: 0L
    private val initialAlbumName: String = java.net.URLDecoder.decode(savedStateHandle.get<String>("albumName") ?: "", "UTF-8")
    private val initialIsCustom: Boolean = savedStateHandle.get<String>("isCustom")?.toBooleanStrictOrNull() ?: false

    private val _currentAlbumId = MutableStateFlow(initialAlbumId)
    private val _currentAlbumName = MutableStateFlow(initialAlbumName)
    private val _isCurrentCustom = MutableStateFlow(initialIsCustom)
    private val _isSidebarVisible = MutableStateFlow(true)
    private val _selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<AlbumDetailUiState> = combine(
        _currentAlbumId,
        _currentAlbumName,
        _isCurrentCustom,
        _isSidebarVisible
    ) { albumId, albumName, isCustom, sidebarVisible ->
        Tuple4(albumId, albumName, isCustom, sidebarVisible)
    }.flatMapLatest { tuple ->
        combine(
            repository.getAlbumMedia(tuple.v1, tuple.v3),
            repository.getAlbums(),
            _selectedItemIds
        ) { mediaList, albums, selectedIds ->
            AlbumDetailUiState(
                albumId = tuple.v1,
                albumName = tuple.v2,
                isCustom = tuple.v3,
                isSidebarVisible = tuple.v4,
                items = mediaList,
                allAlbums = albums,
                isSelectionMode = selectedIds.isNotEmpty(),
                selectedItemIds = selectedIds,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlbumDetailUiState(
            albumId = initialAlbumId,
            albumName = initialAlbumName,
            isCustom = initialIsCustom
        )
    )

    fun selectAlbum(album: Album) {
        _currentAlbumId.value = album.id
        _currentAlbumName.value = album.name
        _isCurrentCustom.value = album.isCustom
        clearSelection()
    }

    fun toggleSidebar(visible: Boolean? = null) {
        _isSidebarVisible.value = visible ?: !_isSidebarVisible.value
    }

    fun toggleItemSelection(mediaId: Long) {
        val current = _selectedItemIds.value.toMutableSet()
        if (current.contains(mediaId)) current.remove(mediaId) else current.add(mediaId)
        _selectedItemIds.value = current
    }

    fun selectAll() {
        _selectedItemIds.value = uiState.value.items.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedItemIds.value = emptySet()
    }

    fun deleteSelected() {
        val selected = _selectedItemIds.value
        val itemsToDelete = uiState.value.items.filter { selected.contains(it.id) }
        viewModelScope.launch {
            repository.moveToTrash(itemsToDelete)
            clearSelection()
        }
    }

    fun moveOrAddItemsToAlbum(targetAlbum: Album, items: List<MediaItem>, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            if (targetAlbum.isCustom) {
                repository.addItemsToCustomAlbum(targetAlbum.id, items.map { it.id })
                onComplete("Added ${items.size} item(s) to ${targetAlbum.name}")
            } else {
                val success = repository.moveMedia(items, targetAlbum.name)
                if (success) {
                    onComplete("Moved ${items.size} item(s) to ${targetAlbum.name}")
                } else {
                    onComplete("Failed to move items to ${targetAlbum.name}")
                }
            }
            clearSelection()
        }
    }

    private data class Tuple4<A, B, C, D>(val v1: A, val v2: B, val v3: C, val v4: D)
}
