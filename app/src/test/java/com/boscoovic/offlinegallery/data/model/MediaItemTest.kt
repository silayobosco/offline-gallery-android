package com.boscoovic.offlinegallery.data.model

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class MediaItemTest {

    @Test
    fun testFormattedDuration() {
        val mockUri = mock(Uri::class.java)
        val videoItem = MediaItem(
            id = 1L,
            uri = mockUri,
            path = "/storage/emulated/0/DCIM/video.mp4",
            displayName = "video.mp4",
            mimeType = "video/mp4",
            mediaType = MediaType.VIDEO,
            size = 1048576L,
            dateAdded = 1000L,
            dateTaken = 1000000L,
            durationMs = 125000L // 2 min 5 sec
        )

        assertEquals("2:05", videoItem.formattedDuration)
    }

    @Test
    fun testFormattedSize() {
        val mockUri = mock(Uri::class.java)
        val imageItem = MediaItem(
            id = 2L,
            uri = mockUri,
            path = "/storage/emulated/0/DCIM/photo.jpg",
            displayName = "photo.jpg",
            mimeType = "image/jpeg",
            mediaType = MediaType.IMAGE,
            size = 2621440L, // 2.5 MB
            dateAdded = 1000L,
            dateTaken = 1000000L
        )

        assertEquals("2.5 MB", imageItem.formattedSize)
    }
}
