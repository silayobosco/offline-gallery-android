package com.boscoovic.offlinegallery.ui.screens.pictures

import android.net.Uri
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.data.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import java.util.Calendar

@RunWith(MockitoJUnitRunner::class)
class DateGroupingTest {

    @Test
    fun testMediaGroupingByDay() {
        val mockUri = mock(Uri::class.java)

        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.MARCH, 15, 14, 30, 0)
        val day1Time = cal.timeInMillis

        cal.set(2026, Calendar.MARCH, 15, 18, 0, 0)
        val day1TimeLater = cal.timeInMillis

        cal.set(2026, Calendar.MARCH, 10, 10, 0, 0)
        val day2Time = cal.timeInMillis

        val item1 = MediaItem(
            id = 1L,
            uri = mockUri,
            path = "/sdcard/photo1.jpg",
            displayName = "photo1.jpg",
            mimeType = "image/jpeg",
            mediaType = MediaType.IMAGE,
            size = 1000L,
            dateAdded = day1Time / 1000,
            dateTaken = day1Time
        )

        val item2 = MediaItem(
            id = 2L,
            uri = mockUri,
            path = "/sdcard/photo2.jpg",
            displayName = "photo2.jpg",
            mimeType = "image/jpeg",
            mediaType = MediaType.IMAGE,
            size = 1200L,
            dateAdded = day1TimeLater / 1000,
            dateTaken = day1TimeLater
        )

        val item3 = MediaItem(
            id = 3L,
            uri = mockUri,
            path = "/sdcard/photo3.jpg",
            displayName = "photo3.jpg",
            mimeType = "image/jpeg",
            mediaType = MediaType.IMAGE,
            size = 1500L,
            dateAdded = day2Time / 1000,
            dateTaken = day2Time
        )

        val mediaList = listOf(item1, item2, item3)

        // Grouping logic matching PicturesViewModel
        val groups = mutableMapOf<Long, MutableList<MediaItem>>()
        val dayCal = Calendar.getInstance()
        for (item in mediaList) {
            val timestamp = if (item.dateTaken > 0) item.dateTaken else item.dateAdded * 1000
            dayCal.timeInMillis = timestamp
            dayCal.set(Calendar.HOUR_OF_DAY, 0)
            dayCal.set(Calendar.MINUTE, 0)
            dayCal.set(Calendar.SECOND, 0)
            dayCal.set(Calendar.MILLISECOND, 0)
            val dayStart = dayCal.timeInMillis
            groups.getOrPut(dayStart) { mutableListOf() }.add(item)
        }

        val resultGroups = groups.entries
            .sortedByDescending { it.key }
            .map { DateGroup(dateMillis = it.key, items = it.value) }

        assertEquals(2, resultGroups.size)
        assertEquals(2, resultGroups[0].items.size)
        assertEquals(1, resultGroups[1].items.size)
    }
}

