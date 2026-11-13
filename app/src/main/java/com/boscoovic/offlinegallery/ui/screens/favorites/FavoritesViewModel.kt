package com.boscoovic.offlinegallery.ui.screens.favorites

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boscoovic.offlinegallery.data.model.Album
import com.boscoovic.offlinegallery.data.model.MediaFilter
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val items: List<MediaItem> = emptyList(),
    val allAlbums: List<Album> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedItemIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: MediaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<FavoritesUiState> = combine(
        repository.getAllMedia(MediaFilter.FAVORITES),
        repository.getAlbums(),
        _selectedItemIds
    ) { items, albums, selectedIds ->
        FavoritesUiState(
            items = items,
            allAlbums = albums,
            isSelectionMode = selectedIds.isNotEmpty(),
            selectedItemIds = selectedIds,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FavoritesUiState()
    )

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

    fun unfavoriteSelected(onComplete: (String) -> Unit) {
        val selected = _selectedItemIds.value
        val itemsToUpdate = uiState.value.items.filter { selected.contains(it.id) }
        if (itemsToUpdate.isEmpty()) return
        viewModelScope.launch {
            for (item in itemsToUpdate) {
                repository.toggleFavorite(item.id, false)
            }
            onComplete("Removed ${itemsToUpdate.size} item(s) from Favorites")
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

    fun deleteSelected() {
        val selected = _selectedItemIds.value
        val itemsToDelete = uiState.value.items.filter { selected.contains(it.id) }
        if (itemsToDelete.isEmpty()) return
        viewModelScope.launch {
            repository.moveToTrash(itemsToDelete)
            clearSelection()
        }
    }

    fun moveSelectedToAlbum(targetAlbum: Album, onComplete: (String) -> Unit) {
        val selected = _selectedItemIds.value
        val itemsToMove = uiState.value.items.filter { selected.contains(it.id) }
        if (itemsToMove.isEmpty()) return
        viewModelScope.launch {
            if (targetAlbum.isCustom) {
                repository.addItemsToCustomAlbum(targetAlbum.id, itemsToMove.map { it.id })
                onComplete("Added ${itemsToMove.size} item(s) to ${targetAlbum.name}")
            } else {
                val success = repository.moveMediaToAlbum(itemsToMove, targetAlbum)
                if (success) {
                    onComplete("Moved ${itemsToMove.size} item(s) to ${targetAlbum.name}")
                } else {
                    onComplete("Failed to move items to ${targetAlbum.name}")
                }
            }
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
}

