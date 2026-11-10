package com.boscoovic.offlinegallery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_albums")
data class CustomAlbumEntity(
    @PrimaryKey(autoGenerate = true)
    val albumId: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val coverMediaId: Long? = null
)
