package com.boscoovic.offlinegallery.ui.screens.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class StoryCollection(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverItem: MediaItem,
    val items: List<MediaItem>
)

data class StoriesUiState(
    val stories: List<StoryCollection> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    val uiState: StateFlow<StoriesUiState> = repository.getAllMedia().map { allMedia ->
        val stories = generateStories(allMedia)
        StoriesUiState(stories = stories, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StoriesUiState()
    )

    private fun generateStories(allMedia: List<MediaItem>): List<StoryCollection> {
        val list = mutableListOf<StoryCollection>()
        if (allMedia.isEmpty()) return list

        val cal = Calendar.getInstance()
        val groupedByMonth = allMedia.groupBy { item ->
            val time = if (item.dateTaken > 0) item.dateTaken else item.dateAdded * 1000
            cal.timeInMillis = time
            "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.MONTH)}"
        }

        for ((key, items) in groupedByMonth) {
            if (items.size >= 3) {
                val firstTime = if (items[0].dateTaken > 0) items[0].dateTaken else items[0].dateAdded * 1000
                val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(firstTime))

                list.add(
                    StoryCollection(
                        id = key,
                        title = "Memories from $monthName",
                        subtitle = "${items.size} moments captured",
                        coverItem = items.first(),
                        items = items
                    )
                )
            }
        }
        return list
    }
}

