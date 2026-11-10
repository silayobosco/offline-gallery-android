package com.boscoovic.offlinegallery.ui.screens.recyclebin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boscoovic.offlinegallery.data.local.entity.TrashItemEntity
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecycleBinUiState(
    val items: List<TrashItemEntity> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<RecycleBinUiState> = combine(
        repository.getTrashItems(),
        _selectedIds
    ) { items, selectedIds ->
        RecycleBinUiState(
            items = items,
            selectedIds = selectedIds,
            isSelectionMode = selectedIds.isNotEmpty(),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecycleBinUiState()
    )

    fun toggleItemSelection(id: Long) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedIds.value = current
    }

    fun selectAll() {
        _selectedIds.value = uiState.value.items.map { it.mediaId }.toSet()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun restoreSelected() {
        val ids = _selectedIds.value.toList()
        viewModelScope.launch {
            repository.restoreFromTrash(ids)
            clearSelection()
        }
    }

    fun deleteSelectedPermanently() {
        val ids = _selectedIds.value.toList()
        viewModelScope.launch {
            repository.permanentDeleteFromTrash(ids)
            clearSelection()
        }
    }

    fun emptyBin() {
        viewModelScope.launch {
            repository.emptyTrash()
            clearSelection()
        }
    }
}

