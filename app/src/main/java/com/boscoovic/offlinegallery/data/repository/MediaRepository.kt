package com.boscoovic.offlinegallery.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.boscoovic.offlinegallery.data.local.entity.RecentSearchEntity
import com.boscoovic.offlinegallery.data.local.entity.TrashItemEntity
import com.boscoovic.offlinegallery.data.model.Album
import com.boscoovic.offlinegallery.data.model.ExifData
import com.boscoovic.offlinegallery.data.model.MediaFilter
import com.boscoovic.offlinegallery.data.model.MediaItem
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllMedia(filter: MediaFilter = MediaFilter.ALL): Flow<List<MediaItem>>
    fun getAlbums(): Flow<List<Album>>
    fun getAlbumMedia(albumId: Long, isCustom: Boolean): Flow<List<MediaItem>>
    suspend fun toggleFavorite(mediaId: Long, isFavorite: Boolean)
    suspend fun setHidden(mediaId: Long, isHidden: Boolean)
    suspend fun moveToTrash(items: List<MediaItem>): Boolean
    suspend fun restoreFromTrash(mediaIds: List<Long>): Boolean
    suspend fun permanentDeleteFromTrash(mediaIds: List<Long>): Boolean
    suspend fun emptyTrash(): Boolean
    fun getTrashItems(): Flow<List<TrashItemEntity>>
    suspend fun moveToSecureFolder(items: List<MediaItem>): Boolean
    suspend fun restoreFromSecureFolder(mediaId: Long): Boolean
    fun getSecureItems(): Flow<List<MediaItem>>
    suspend fun createCustomAlbum(name: String): Long
    suspend fun renameCustomAlbum(albumId: Long, newName: String)
    suspend fun deleteCustomAlbum(albumId: Long)
    suspend fun addItemsToCustomAlbum(albumId: Long, mediaIds: List<Long>)
    suspend fun removeItemsFromCustomAlbum(albumId: Long, mediaIds: List<Long>)
    fun searchMedia(query: String): Flow<List<MediaItem>>
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>
    suspend fun addRecentSearch(query: String)
    suspend fun clearRecentSearches()
    suspend fun getExifData(item: MediaItem): ExifData
    suspend fun saveEditedImage(sourceItem: MediaItem, bitmap: Bitmap): Uri?
    suspend fun overwriteOriginalImage(sourceItem: MediaItem, bitmap: Bitmap): Boolean
    suspend fun copyMedia(items: List<MediaItem>, targetDirectoryName: String): Boolean
    suspend fun moveMedia(items: List<MediaItem>, targetDirectoryName: String): Boolean
    suspend fun moveMediaToAlbum(items: List<MediaItem>, targetAlbum: Album): Boolean
}

