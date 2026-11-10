package com.boscoovic.offlinegallery.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_items")
data class TrashItemEntity(
    @PrimaryKey
    val mediaId: Long,
    val originalUri: String,
    val originalPath: String,
    val trashedAt: Long,
    val expiresAt: Long,
    val displayName: String,
    val mimeType: String,
    val mediaType: String,
    val size: Long,
    val durationMs: Long = 0L,
    val trashedFilePath: String
)
