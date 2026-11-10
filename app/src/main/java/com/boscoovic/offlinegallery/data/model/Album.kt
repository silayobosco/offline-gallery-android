package com.boscoovic.offlinegallery.data.model

import android.net.Uri

data class Album(
    val id: Long,
    val name: String,
    val coverUri: Uri?,
    val itemCount: Int,
    val isCustom: Boolean = false,
    val relativePath: String? = null
)
