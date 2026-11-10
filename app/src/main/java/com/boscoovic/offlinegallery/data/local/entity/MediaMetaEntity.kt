package com.boscoovic.offlinegallery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_meta")
data class MediaMetaEntity(
    @PrimaryKey
    val mediaId: Long,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val cachedLat: Double? = null,
    val cachedLng: Double? = null,
    val cachedExifJson: String? = null
)
