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

enum class StoryCategory(val badge: String) {
    ON_THIS_DAY("On This Day"),
    FAVORITES("Favorites"),
    TRIP_EVENT("Highlights"),
    MONTHLY("Monthly"),
    YEAR_REVIEW("Year in Review")
}

data class StoryCollection(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverItem: MediaItem,
    val items: List<MediaItem>,
    val category: StoryCategory = StoryCategory.MONTHLY
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

    fun generateStories(allMedia: List<MediaItem>, nowMs: Long = System.currentTimeMillis()): List<StoryCollection> {
        val list = mutableListOf<StoryCollection>()
        if (allMedia.isEmpty()) return list

        fun itemTimeMs(item: MediaItem): Long =
            if (item.dateTaken > 0) item.dateTaken else item.dateAdded * 1000L

        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val todayMonth = nowCal.get(Calendar.MONTH)
        val todayDay = nowCal.get(Calendar.DAY_OF_MONTH)
        val todayYear = nowCal.get(Calendar.YEAR)

        val cal = Calendar.getInstance()

        // 1. On This Day (same month & day in previous years)
        val onThisDayItems = allMedia.filter { item ->
            cal.timeInMillis = itemTimeMs(item)
            cal.get(Calendar.MONTH) == todayMonth &&
                cal.get(Calendar.DAY_OF_MONTH) == todayDay &&
                cal.get(Calendar.YEAR) < todayYear
        }
        if (onThisDayItems.isNotEmpty()) {
            val dateLabel = SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date(nowMs))
            list.add(
                StoryCollection(
                    id = "on_this_day_${todayMonth}_$todayDay",
                    title = "On This Day • $dateLabel",
                    subtitle = "${onThisDayItems.size} memory(s) from past years",
                    coverItem = onThisDayItems.first(),
                    items = onThisDayItems,
                    category = StoryCategory.ON_THIS_DAY
                )
            )
        }

        // 2. Favorites Reel
        val favorites = allMedia.filter { it.isFavorite }
        if (favorites.size >= 3) {
            list.add(
                StoryCollection(
                    id = "favorites_reel",
                    title = "Favorites Reel",
                    subtitle = "${favorites.size} loved moments",
                    coverItem = favorites.first(),
                    items = favorites,
                    category = StoryCategory.FAVORITES
                )
            )
        }

        // 3. Trip / Event Burst Clusters (items within 48h gap, >= 5 items, not spanning > 10 days)
        val sortedByTime = allMedia.sortedByDescending { itemTimeMs(it) }
        val twoDaysMs = 48L * 60L * 60L * 1000L
        var currentCluster = mutableListOf<MediaItem>()
        val clusters = mutableListOf<List<MediaItem>>()
        for (item in sortedByTime) {
            if (currentCluster.isEmpty()) {
                currentCluster.add(item)
            } else {
                val prevTime = itemTimeMs(currentCluster.last())
                val curTime = itemTimeMs(item)
                if (prevTime - curTime <= twoDaysMs) {
                    currentCluster.add(item)
                } else {
                    if (currentCluster.size >= 5) clusters.add(currentCluster)
                    currentCluster = mutableListOf(item)
                }
            }
        }
        if (currentCluster.size >= 5) clusters.add(currentCluster)

        val shortDateFmt = SimpleDateFormat("MMM d", Locale.getDefault())
        clusters.take(3).forEachIndexed { idx, cluster ->
            val newest = itemTimeMs(cluster.first())
            val oldest = itemTimeMs(cluster.last())
            val startStr = shortDateFmt.format(Date(oldest))
            val endStr = shortDateFmt.format(Date(newest))
            val rangeStr = if (startStr == endStr) startStr else "$startStr – $endStr"
            list.add(
                StoryCollection(
                    id = "cluster_${idx}_$oldest",
                    title = "Event Highlights • $rangeStr",
                    subtitle = "${cluster.size} moments captured together",
                    coverItem = cluster.first(),
                    items = cluster,
                    category = StoryCategory.TRIP_EVENT
                )
            )
        }

        // 4. Monthly Memories
        val groupedByMonth = allMedia.groupBy { item ->
            cal.timeInMillis = itemTimeMs(item)
            "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.MONTH)}"
        }

        for ((key, items) in groupedByMonth) {
            if (items.size >= 3) {
                val firstTime = itemTimeMs(items[0])
                val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(firstTime))

                list.add(
                    StoryCollection(
                        id = key,
                        title = "Memories from $monthName",
                        subtitle = "${items.size} moments captured",
                        coverItem = items.first(),
                        items = items,
                        category = StoryCategory.MONTHLY
                    )
                )
            }
        }

        // 5. Year in Review
        val groupedByYear = allMedia.groupBy { item ->
            cal.timeInMillis = itemTimeMs(item)
            cal.get(Calendar.YEAR)
        }
        for ((year, items) in groupedByYear) {
            if (items.size >= 10) {
                list.add(
                    StoryCollection(
                        id = "year_$year",
                        title = "Year in Review • $year",
                        subtitle = "${items.size} photos & videos",
                        coverItem = items.first(),
                        items = items,
                        category = StoryCategory.YEAR_REVIEW
                    )
                )
            }
        }

        return list
    }
}

