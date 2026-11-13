package com.boscoovic.offlinegallery.ui.screens.search

import android.net.Uri
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.data.model.MediaType
import com.boscoovic.offlinegallery.data.repository.MediaRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SearchQuickFilterTest {

    private fun makeItem(
        id: Long,
        name: String = "photo_$id.jpg",
        mime: String = "image/jpeg",
        type: MediaType = MediaType.IMAGE,
        bucket: String = "Camera",
        width: Int = 1000,
        height: Int = 1000,
        size: Long = 500_000L,
        isFavorite: Boolean = false
    ): MediaItem {
        return MediaItem(
            id = id,
            uri = mock(Uri::class.java),
            path = "/storage/emulated/0/DCIM/$bucket/$name",
            displayName = name,
            mimeType = mime,
            mediaType = type,
            size = size,
            dateAdded = 1000L,
            dateTaken = 1000_000L,
            bucketName = bucket,
            width = width,
            height = height,
            isFavorite = isFavorite
        )
    }

    @Test
    fun `applyQuickFilter filters gifs, portrait, landscape, videos, and large files`() {
        val repo = mock(MediaRepository::class.java)
        val viewModel = SearchViewModel(repo)

        val gifItem = makeItem(1L, name = "anim.gif", mime = "image/gif", width = 500, height = 500)
        val portraitItem = makeItem(2L, width = 1080, height = 1920, isFavorite = true)
        val landscapeVideo = makeItem(
            3L,
            name = "clip.mp4",
            mime = "video/mp4",
            type = MediaType.VIDEO,
            width = 1920,
            height = 1080,
            size = 20L * 1024L * 1024L
        )
        val screenshot = makeItem(4L, bucket = "Screenshots", width = 1080, height = 2400)

        val all = listOf(gifItem, portraitItem, landscapeVideo, screenshot)

        assertEquals(listOf(gifItem), viewModel.applyQuickFilter(all, SearchQuickFilter.GIFS))
        assertEquals(listOf(portraitItem, screenshot), viewModel.applyQuickFilter(all, SearchQuickFilter.PORTRAIT))
        assertEquals(listOf(landscapeVideo), viewModel.applyQuickFilter(all, SearchQuickFilter.LANDSCAPE))
        assertEquals(listOf(landscapeVideo), viewModel.applyQuickFilter(all, SearchQuickFilter.VIDEOS))
        assertEquals(listOf(portraitItem), viewModel.applyQuickFilter(all, SearchQuickFilter.FAVORITES))
        assertEquals(listOf(landscapeVideo), viewModel.applyQuickFilter(all, SearchQuickFilter.LARGE_FILES))
        assertEquals(listOf(screenshot), viewModel.applyQuickFilter(all, SearchQuickFilter.SCREENSHOTS))
    }
}

