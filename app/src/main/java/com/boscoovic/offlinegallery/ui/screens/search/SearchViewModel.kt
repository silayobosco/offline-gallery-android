package com.boscoovic.offlinegallery.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boscoovic.offlinegallery.data.local.entity.RecentSearchEntity
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

enum class SearchQuickFilter(val label: String) {
    ALL("All"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    SCREENSHOTS("Screenshots"),
    CAMERA("Camera"),
    FAVORITES("Favorites"),
    LARGE_FILES("Large files")
}

data class SearchUiState(
    val query: String = "",
    val selectedFilter: SearchQuickFilter = SearchQuickFilter.ALL,
    val results: List<MediaItem> = emptyList(),
    val recentSearches: List<RecentSearchEntity> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _quickFilter = MutableStateFlow(SearchQuickFilter.ALL)

    val uiState: StateFlow<SearchUiState> = _query.flatMapLatest { q ->
        repository.searchMedia(q)
    }.combine(_query) { results, query ->
        Pair(results, query)
    }.combine(_quickFilter) { (results, query), filter ->
        Triple(applyQuickFilter(results, filter), query, filter)
    }.combine(repository.getRecentSearches()) { (filteredResults, query, filter), recent ->
        SearchUiState(
            query = query,
            selectedFilter = filter,
            results = filteredResults,
            recentSearches = recent,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun selectQuickFilter(filter: SearchQuickFilter) {
        _quickFilter.value = filter
    }

    fun submitSearch(q: String) {
        if (q.isNotBlank()) {
            _query.value = q
            viewModelScope.launch {
                repository.addRecentSearch(q)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearRecentSearches()
        }
    }

    private fun applyQuickFilter(items: List<MediaItem>, filter: SearchQuickFilter): List<MediaItem> {
        return when (filter) {
            SearchQuickFilter.ALL -> items
            SearchQuickFilter.PHOTOS -> items.filter { it.mediaType == MediaType.IMAGE }
            SearchQuickFilter.VIDEOS -> items.filter { it.mediaType == MediaType.VIDEO }
            SearchQuickFilter.SCREENSHOTS -> items.filter {
                it.bucketName.contains("screenshot", ignoreCase = true) ||
                    it.path.contains("screenshot", ignoreCase = true)
            }
            SearchQuickFilter.CAMERA -> items.filter {
                it.bucketName.contains("camera", ignoreCase = true) ||
                    it.bucketName.equals("DCIM", ignoreCase = true)
            }
            SearchQuickFilter.FAVORITES -> items.filter { it.isFavorite }
            SearchQuickFilter.LARGE_FILES -> items.filter { it.size >= 10L * 1024L * 1024L }
        }
    }
}
