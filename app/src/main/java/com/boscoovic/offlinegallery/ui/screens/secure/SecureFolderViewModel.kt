package com.boscoovic.offlinegallery.ui.screens.secure

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

data class SecureFolderUiState(
    val isUnlocked: Boolean = false,
    val items: List<MediaItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SecureFolderViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _isUnlocked = MutableStateFlow(false)

    val uiState: StateFlow<SecureFolderUiState> = combine(
        _isUnlocked,
        repository.getSecureItems()
    ) { unlocked, items ->
        SecureFolderUiState(
            isUnlocked = unlocked,
            items = if (unlocked) items else emptyList(),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SecureFolderUiState()
    )

    fun onAuthenticated() {
        _isUnlocked.value = true
    }

    fun lock() {
        _isUnlocked.value = false
    }

    fun restoreItem(mediaId: Long) {
        viewModelScope.launch {
            repository.restoreFromSecureFolder(mediaId)
        }
    }
}

