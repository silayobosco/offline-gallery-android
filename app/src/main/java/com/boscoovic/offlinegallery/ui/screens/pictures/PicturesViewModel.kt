package com.boscoovic.offlinegallery.ui.screens.pictures

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boscoovic.offlinegallery.data.model.Album
import com.boscoovic.offlinegallery.data.model.MediaFilter
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import com.boscoovic.offlinegallery.data.settings.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DateGroup(
    val dateMillis: Long,
    val items: List<MediaItem>
)

data class PicturesUiState(
    val filter: MediaFilter = MediaFilter.ALL,
    val dateGroups: List<DateGroup> = emptyList(),
    val totalCount: Int = 0,
    val columnCount: Int = 3,
    val isSelectionMode: Boolean = false,
    val selectedItemIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true
)

@HiltViewModel
class PicturesViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val settingsManager: SettingsManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _filter = MutableStateFlow(MediaFilter.ALL)
    private val _columnCount = MutableStateFlow(settingsManager.defaultGridColumns.value)
    private val _selectedItemIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<PicturesUiState> = _filter.flatMapLatest { filter ->
        repository.getAllMedia(filter)
    }.combine(_filter) { mediaList, filter ->
        Pair(mediaList, filter)
    }.combine(_columnCount) { (mediaList, filter), columns ->
        Triple(mediaList, filter, columns)
    }.combine(_selectedItemIds) { (mediaList, filter, columns), selectedIds ->
        val dateGroups = groupMediaByDay(mediaList)
        PicturesUiState(
            filter = filter,
            dateGroups = dateGroups,
            totalCount = mediaList.size,
            columnCount = columns,
            isSelectionMode = selectedIds.isNotEmpty(),
            selectedItemIds = selectedIds,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PicturesUiState()
    )

    fun setFilter(filter: MediaFilter) {
        _filter.value = filter
    }

    fun setColumnCount(columns: Int) {
        val clamped = columns.coerceIn(2, 6)
        _columnCount.value = clamped
        settingsManager.setDefaultGridColumns(clamped)
    }

    fun toggleItemSelection(mediaId: Long) {
        val current = _selectedItemIds.value.toMutableSet()
        if (current.contains(mediaId)) {
            current.remove(mediaId)
        } else {
            current.add(mediaId)
        }
        _selectedItemIds.value = current
    }

    fun toggleDaySelection(group: DateGroup) {
        val groupIds = group.items.map { it.id }.toSet()
        val current = _selectedItemIds.value.toMutableSet()
        val allSelected = groupIds.all { current.contains(it) }
        if (allSelected) {
            current.removeAll(groupIds)
        } else {
            current.addAll(groupIds)
        }
        _selectedItemIds.value = current
    }

    fun selectAll(allItems: List<MediaItem>) {
        _selectedItemIds.value = allItems.map { it.id }.toSet()
    }

    fun setSelection(ids: Set<Long>) {
        _selectedItemIds.value = ids
    }

    fun clearSelection() {
        _selectedItemIds.value = emptySet()
    }

    fun deleteSelected(allItems: List<MediaItem>) {
        val selectedIds = _selectedItemIds.value
        val itemsToDelete = allItems.filter { selectedIds.contains(it.id) }
        viewModelScope.launch {
            repository.moveToTrash(itemsToDelete)
            clearSelection()
        }
    }

    fun favoriteSelected(allItems: List<MediaItem>) {
        val selectedIds = _selectedItemIds.value
        val itemsToFav = allItems.filter { selectedIds.contains(it.id) }
        val anyUnfavorited = itemsToFav.any { !it.isFavorite }
        viewModelScope.launch {
            for (item in itemsToFav) {
                repository.toggleFavorite(item.id, anyUnfavorited)
            }
            clearSelection()
        }
    }

    fun hideSelected(allItems: List<MediaItem>) {
        val selectedIds = _selectedItemIds.value
        val itemsToHide = allItems.filter { selectedIds.contains(it.id) }
        viewModelScope.launch {
            repository.moveToSecureFolder(itemsToHide)
            clearSelection()
        }
    }

    fun moveSelectedToAlbum(
        targetAlbum: Album,
        allItems: List<MediaItem>,
        onComplete: (String) -> Unit
    ) {
        val selectedIds = _selectedItemIds.value
        val itemsToMove = allItems.filter { selectedIds.contains(it.id) }
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

    fun shareSelected(allItems: List<MediaItem>) {
        val selectedIds = _selectedItemIds.value
        val itemsToShare = allItems.filter { selectedIds.contains(it.id) }
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
    }

    private fun groupMediaByDay(mediaList: List<MediaItem>): List<DateGroup> {
        val groups = mutableMapOf<Long, MutableList<MediaItem>>()
        val cal = Calendar.getInstance()

        for (item in mediaList) {
            val timestamp = if (item.dateTaken > 0) item.dateTaken else item.dateAdded * 1000
            cal.timeInMillis = timestamp
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis

            groups.getOrPut(dayStart) { mutableListOf() }.add(item)
        }

        return groups.entries
            .sortedByDescending { it.key }
            .map { DateGroup(dateMillis = it.key, items = it.value) }
    }
}

