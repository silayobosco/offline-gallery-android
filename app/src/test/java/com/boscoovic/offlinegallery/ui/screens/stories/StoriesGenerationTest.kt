package com.boscoovic.offlinegallery.ui.screens.stories

import android.net.Uri
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.data.model.MediaType
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import java.util.Calendar

@RunWith(MockitoJUnitRunner::class)
class StoriesGenerationTest {

    private fun makeItem(
        id: Long,
        dateTakenMs: Long,
        isFavorite: Boolean = false
    ): MediaItem {
        return MediaItem(
            id = id,
            uri = mock(Uri::class.java),
            path = "/storage/emulated/0/DCIM/img_$id.jpg",
            displayName = "img_$id.jpg",
            mimeType = "image/jpeg",
            mediaType = MediaType.IMAGE,
            size = 1024L,
            dateAdded = dateTakenMs / 1000L,
            dateTaken = dateTakenMs,
            isFavorite = isFavorite
        )
    }

    @Test
    fun `generateStories produces On This Day, Favorites, Event, Monthly, and Year stories`() {
        val repo = mock(MediaRepository::class.java)
        val viewModel = StoriesViewModel(repo)

        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 13, 12, 0, 0)
        }
        val pastSameDay = Calendar.getInstance().apply {
            set(2024, Calendar.NOVEMBER, 13, 10, 0, 0)
        }.timeInMillis

        val recentClusterBase = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 10, 10, 0, 0)
        }.timeInMillis

        val items = mutableListOf<MediaItem>()
        items.add(makeItem(1L, pastSameDay, isFavorite = true))
        for (i in 0 until 10) {
            items.add(
                makeItem(
                    id = 10L + i,
                    dateTakenMs = recentClusterBase + i * 3600_000L,
                    isFavorite = i < 3
                )
            )
        }

        val stories = viewModel.generateStories(items, nowMs = nowCal.timeInMillis)

        assertTrue(stories.any { it.category == StoryCategory.ON_THIS_DAY })
        assertTrue(stories.any { it.category == StoryCategory.FAVORITES })
        assertTrue(stories.any { it.category == StoryCategory.TRIP_EVENT })
        assertTrue(stories.any { it.category == StoryCategory.MONTHLY })
        assertTrue(stories.any { it.category == StoryCategory.YEAR_REVIEW })
    }
}

