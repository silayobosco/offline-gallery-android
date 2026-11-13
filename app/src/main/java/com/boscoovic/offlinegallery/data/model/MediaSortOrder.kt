package com.boscoovic.offlinegallery.data.model

enum class MediaSortOrder(val label: String) {
    DATE_DESC("Newest first"),
    DATE_ASC("Oldest first"),
    SIZE_DESC("Largest first"),
    NAME_ASC("Name (A–Z)")
}

fun List<MediaItem>.sortedWithOrder(order: MediaSortOrder): List<MediaItem> {
    return when (order) {
        MediaSortOrder.DATE_DESC -> sortedByDescending { if (it.dateTaken > 0) it.dateTaken else it.dateAdded * 1000L }
        MediaSortOrder.DATE_ASC -> sortedBy { if (it.dateTaken > 0) it.dateTaken else it.dateAdded * 1000L }
        MediaSortOrder.SIZE_DESC -> sortedByDescending { it.size }
        MediaSortOrder.NAME_ASC -> sortedBy { it.displayName.lowercase() }
    }
}

