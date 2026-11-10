package com.boscoovic.offlinegallery.ui.screens.albums

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailUiState(
    val albumId: Long = 0L,
    val albumName: String = "",
    val isCustom: Boolean = false,
    val items: List<MediaItem> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedItemIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val repository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val albumId: Long = savedStateHandle.get<String>("albumId")?.toLongOrNull() ?: 0L
    val albumName: String = java.net.URLDecoder.decode(savedStateHandle.get<String>("albumName") ?: "", "UTF-8")
    val isCustom: Boolean = savedStateHandle.get<String>("isCustom")?.toBooleanStrictOrNull() ?: false

    private val _selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<AlbumDetailUiState> = combine(
        repository.getAlbumMedia(albumId, isCustom),
        _selectedItemIds
    ) { mediaList, selectedIds ->
        AlbumDetailUiState(
            albumId = albumId,
            albumName = albumName,
            isCustom = isCustom,
            items = mediaList,
            isSelectionMode = selectedIds.isNotEmpty(),
            selectedItemIds = selectedIds,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlbumDetailUiState(albumId = albumId, albumName = albumName, isCustom = isCustom)
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

    fun deleteSelected() {
        val selected = _selectedItemIds.value
        val itemsToDelete = uiState.value.items.filter { selected.contains(it.id) }
        viewModelScope.launch {
            repository.moveToTrash(itemsToDelete)
            clearSelection()
        }
    }
}

