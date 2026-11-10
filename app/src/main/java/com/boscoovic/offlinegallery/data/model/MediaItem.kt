package com.boscoovic.offlinegallery.data.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val path: String,
    val displayName: String,
    val mimeType: String,
    val mediaType: MediaType,
    val size: Long,
    val dateAdded: Long,
    val dateTaken: Long,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val bucketId: Long = 0L,
    val bucketName: String = "",
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val formattedDuration: String
        get() {
            if (mediaType != MediaType.VIDEO || durationMs <= 0) return ""
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val formattedSize: String
        get() {
            val kb = size / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> "%.2f GB".format(gb)
                mb >= 1.0 -> "%.1f MB".format(mb)
                kb >= 1.0 -> "%.0f KB".format(kb)
                else -> "$size B"
            }
        }
}

