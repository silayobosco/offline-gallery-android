package com.boscoovic.offlinegallery.ui.screens.albums

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boscoovic.offlinegallery.data.model.Album
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.data.model.MediaType
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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

    fun favoriteSelected(onComplete: (String) -> Unit) {
        val selected = _selectedItemIds.value
        val itemsToFav = uiState.value.items.filter { selected.contains(it.id) }
        if (itemsToFav.isEmpty()) return
        val anyUnfavorited = itemsToFav.any { !it.isFavorite }
        viewModelScope.launch {
            for (item in itemsToFav) {
                repository.toggleFavorite(item.id, anyUnfavorited)
            }
            val msg = if (anyUnfavorited) {
                "Added ${itemsToFav.size} item(s) to Favorites"
            } else {
                "Removed ${itemsToFav.size} item(s) from Favorites"
            }
            onComplete(msg)
            clearSelection()
        }
    }

    fun hideSelected(onComplete: (String) -> Unit) {
        val selected = _selectedItemIds.value
        val itemsToHide = uiState.value.items.filter { selected.contains(it.id) }
        if (itemsToHide.isEmpty()) return
        viewModelScope.launch {
            repository.moveToSecureFolder(itemsToHide)
            onComplete("Moved ${itemsToHide.size} item(s) to Secure Folder")
            clearSelection()
        }
    }

    fun shareSelected() {
        val selected = _selectedItemIds.value
        val itemsToShare = uiState.value.items.filter { selected.contains(it.id) }
        if (itemsToShare.isEmpty()) return

        val uris = ArrayList(itemsToShare.map { it.uri })
        val shareIntent = Intent().apply {
            action = if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE
            if (uris.size == 1) {
                putExtra(Intent.EXTRA_STREAM, uris[0])
                type = itemsToShare[0].mimeType
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                type = "*/*"
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Share media").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        clearSelection()
    }

    fun moveOrAddItemsToAlbum(targetAlbum: Album, items: List<MediaItem>, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            if (targetAlbum.isCustom) {
                repository.addItemsToCustomAlbum(targetAlbum.id, items.map { it.id })
                if (_isCurrentCustom.value && _currentAlbumId.value != targetAlbum.id) {
                    repository.removeItemsFromCustomAlbum(_currentAlbumId.value, items.map { it.id })
                }
                onComplete("Added ${items.size} item(s) to ${targetAlbum.name}")
            } else {
                val success = repository.moveMediaToAlbum(items, targetAlbum)
                if (_isCurrentCustom.value) {
                    repository.removeItemsFromCustomAlbum(_currentAlbumId.value, items.map { it.id })
                }
                if (success) {
                    onComplete("Moved ${items.size} item(s) to ${targetAlbum.name}")
                } else {
                    onComplete("Failed to move items to ${targetAlbum.name}")
                }
            }
            clearSelection()
        }
    }

    fun getNextAlbum(): Album? {
        val albums = uiState.value.allAlbums
        if (albums.size <= 1) return null
        val currentIdx = albums.indexOfFirst { it.id == uiState.value.albumId && it.isCustom == uiState.value.isCustom }
        return if (currentIdx >= 0) {
            albums[(currentIdx + 1) % albums.size]
        } else {
            albums.firstOrNull()
        }
    }

    fun switchToNextAlbum(): Album? {
        val next = getNextAlbum() ?: return null
        selectAlbum(next)
        return next
    }

    fun renameCustomAlbum(newName: String) {
        if (!uiState.value.isCustom || newName.isBlank()) return
        viewModelScope.launch {
            repository.renameCustomAlbum(uiState.value.albumId, newName.trim())
            _currentAlbumName.value = newName.trim()
        }
    }

    fun deleteCurrentAlbum(onComplete: () -> Unit) {
        if (!uiState.value.isCustom) return
        viewModelScope.launch {
            repository.deleteCustomAlbum(uiState.value.albumId)
            onComplete()
        }
    }

    private data class Tuple4<A, B, C, D>(val v1: A, val v2: B, val v3: C, val v4: D)
}
