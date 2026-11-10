package com.boscoovic.offlinegallery.ui.screens.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boscoovic.offlinegallery.data.model.Album
import com.boscoovic.offlinegallery.data.model.MediaFilter
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumsUiState(
    val albums: List<Album> = emptyList(),
    val favoritesCount: Int = 0,
    val trashCount: Int = 0,
    val secureCount: Int = 0,
    val showCreateDialog: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _showCreateDialog = MutableStateFlow(false)

    val uiState: StateFlow<AlbumsUiState> = combine(
        repository.getAlbums(),
        repository.getAllMedia(MediaFilter.FAVORITES),
        repository.getTrashItems(),
        repository.getSecureItems(),
        _showCreateDialog
    ) { albums, favorites, trash, secure, showDialog ->
        AlbumsUiState(
            albums = albums,
            favoritesCount = favorites.size,
            trashCount = trash.size,
            secureCount = secure.size,
            showCreateDialog = showDialog,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlbumsUiState()
    )

    fun openCreateDialog() {
        _showCreateDialog.value = true
    }

    fun closeCreateDialog() {
        _showCreateDialog.value = false
    }

    fun createCustomAlbum(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createCustomAlbum(name.trim())
            closeCreateDialog()
        }
    }

    fun renameCustomAlbum(albumId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renameCustomAlbum(albumId, newName.trim())
        }
    }

    fun deleteCustomAlbum(albumId: Long) {
        viewModelScope.launch {
            repository.deleteCustomAlbum(albumId)
        }
    }
}
