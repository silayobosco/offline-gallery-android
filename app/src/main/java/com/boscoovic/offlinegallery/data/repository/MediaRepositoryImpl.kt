package com.boscoovic.offlinegallery.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.boscoovic.offlinegallery.data.local.dao.CustomAlbumDao
import com.boscoovic.offlinegallery.data.local.dao.MediaMetaDao
import com.boscoovic.offlinegallery.data.local.dao.RecentSearchDao
import com.boscoovic.offlinegallery.data.local.dao.TrashDao
import com.boscoovic.offlinegallery.data.local.entity.CustomAlbumEntity
import com.boscoovic.offlinegallery.data.local.entity.CustomAlbumItemEntity
import com.boscoovic.offlinegallery.data.local.entity.MediaMetaEntity
import com.boscoovic.offlinegallery.data.local.entity.RecentSearchEntity
import com.boscoovic.offlinegallery.data.local.entity.TrashItemEntity
import com.boscoovic.offlinegallery.data.mediastore.MediaStoreFetcher
import com.boscoovic.offlinegallery.data.mediastore.MediaStoreObserver
import com.boscoovic.offlinegallery.data.model.Album
import com.boscoovic.offlinegallery.data.model.ExifData
import com.boscoovic.offlinegallery.data.model.MediaFilter
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.data.model.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fetcher: MediaStoreFetcher,
    private val observer: MediaStoreObserver,
    private val metaDao: MediaMetaDao,
    private val trashDao: TrashDao,
    private val customAlbumDao: CustomAlbumDao,
    private val recentSearchDao: RecentSearchDao,
    private val secureStorageManager: SecureStorageManager
) : MediaRepository {

    private val contentResolver = context.contentResolver

    private val trashDir: File by lazy {
        File(context.filesDir, "trash_bin").apply {
            if (!exists()) mkdirs()
        }
    }

    override fun getAllMedia(filter: MediaFilter): Flow<List<MediaItem>> {
        return observer.observeMedia().flatMapLatest {
            flow {
                val rawMedia = fetcher.queryAllMedia()
                emit(rawMedia)
            }
        }.combine(metaDao.getAllMeta()) { mediaList, metaList ->
            val metaMap = metaList.associateBy { it.mediaId }
            mediaList.map { item ->
                val meta = metaMap[item.id]
                item.copy(
                    isFavorite = meta?.isFavorite ?: false,
                    isHidden = meta?.isHidden ?: false
                )
            }
        }.combine(trashDao.getAllTrashedMediaIds()) { mediaList, trashedIds ->
            val trashedSet = trashedIds.toSet()
            // Filter out trashed and hidden items for standard view
            mediaList.filter { !trashedSet.contains(it.id) && !it.isHidden }
        }.map { mediaList ->
            when (filter) {
                MediaFilter.ALL -> mediaList
                MediaFilter.PHOTOS -> mediaList.filter { it.mediaType == MediaType.IMAGE }
                MediaFilter.VIDEOS -> mediaList.filter { it.mediaType == MediaType.VIDEO }
                MediaFilter.FAVORITES -> mediaList.filter { it.isFavorite }
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getAlbums(): Flow<List<Album>> {
        return getAllMedia(MediaFilter.ALL).combine(customAlbumDao.getAllAlbums()) { mediaList, customAlbums ->
            val albumList = mutableListOf<Album>()

            // 1. Bucket auto-albums from MediaStore
            val groupedByBucket = mediaList.groupBy { it.bucketId }
            for ((bucketId, items) in groupedByBucket) {
                if (items.isNotEmpty()) {
                    val firstItem = items.first()
                    albumList.add(
                        Album(
                            id = bucketId,
                            name = firstItem.bucketName.ifBlank { "Album" },
                            coverUri = firstItem.uri,
                            itemCount = items.size,
                            isCustom = false
                        )
                    )
                }
            }

            // 2. Custom virtual albums
            val mediaMap = mediaList.associateBy { it.id }
            for (custom in customAlbums) {
                val coverUri = custom.coverMediaId?.let { mediaMap[it]?.uri }
                albumList.add(
                    Album(
                        id = custom.albumId,
                        name = custom.name,
                        coverUri = coverUri,
                        itemCount = 0, // will be updated reactively or when accessed
                        isCustom = true
                    )
                )
            }

            albumList
        }.flowOn(Dispatchers.IO)
    }

    override fun getAlbumMedia(albumId: Long, isCustom: Boolean): Flow<List<MediaItem>> {
        return if (isCustom) {
            customAlbumDao.getMediaIdsForAlbum(albumId).flatMapLatest { ids ->
                val idSet = ids.toSet()
                getAllMedia(MediaFilter.ALL).map { allMedia ->
                    allMedia.filter { idSet.contains(it.id) }
                }
            }
        } else {
            getAllMedia(MediaFilter.ALL).map { allMedia ->
                allMedia.filter { it.bucketId == albumId }
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun toggleFavorite(mediaId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val existing = metaDao.getMeta(mediaId)
        if (existing == null) {
            metaDao.insertOrUpdate(MediaMetaEntity(mediaId = mediaId, isFavorite = isFavorite))
        } else {
            metaDao.setFavorite(mediaId, isFavorite)
        }
    }

    override suspend fun moveToTrash(items: List<MediaItem>): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val expires = now + 30L * 24 * 60 * 60 * 1000 // 30 days retention

        for (item in items) {
            try {
                // Copy to private trash directory
                val targetFile = File(trashDir, "trash_${item.id}_${item.displayName}")
                contentResolver.openInputStream(item.uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val trashEntity = TrashItemEntity(
                    mediaId = item.id,
                    originalUri = item.uri.toString(),
                    originalPath = item.path,
                    trashedAt = now,
                    expiresAt = expires,
                    displayName = item.displayName,
                    mimeType = item.mimeType,
                    mediaType = item.mediaType.name,
                    size = item.size,
                    durationMs = item.durationMs,
                    trashedFilePath = targetFile.absolutePath
                )
                trashDao.insertTrashItem(trashEntity)

                // Delete from active MediaStore
                try {
                    contentResolver.delete(item.uri, null, null)
                } catch (e: Exception) {
                    // SecurityException might be caught on scoped storage if not granted
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        true
    }

    override suspend fun restoreFromTrash(mediaIds: List<Long>): Boolean = withContext(Dispatchers.IO) {
        for (id in mediaIds) {
            val trashItem = trashDao.getTrashItem(id) ?: continue
            val trashFile = File(trashItem.trashedFilePath)
            if (trashFile.exists()) {
                val isVideo = trashItem.mediaType == MediaType.VIDEO.name
                val tableUri = if (isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, trashItem.displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, trashItem.mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Restored")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val newUri = contentResolver.insert(tableUri, values)
                if (newUri != null) {
                    contentResolver.openOutputStream(newUri)?.use { out ->
                        trashFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        contentResolver.update(newUri, values, null, null)
                    }
                    trashFile.delete()
                    trashDao.delete(id)
                }
            }
        }
        true
    }

    override suspend fun permanentDeleteFromTrash(mediaIds: List<Long>): Boolean = withContext(Dispatchers.IO) {
        for (id in mediaIds) {
            val item = trashDao.getTrashItem(id)
            if (item != null) {
                val file = File(item.trashedFilePath)
                if (file.exists()) {
                    file.delete()
                }
                trashDao.delete(id)
            }
        }
        true
    }

    override suspend fun emptyTrash(): Boolean = withContext(Dispatchers.IO) {
        val allItems = trashDao.getAllTrashItems().first()
        for (item in allItems) {
            val file = File(item.trashedFilePath)
            if (file.exists()) {
                file.delete()
            }
        }
        trashDao.clearAll()
        true
    }

    override fun getTrashItems(): Flow<List<TrashItemEntity>> {
        return trashDao.getAllTrashItems().flowOn(Dispatchers.IO)
    }

    override suspend fun moveToSecureFolder(items: List<MediaItem>): Boolean = withContext(Dispatchers.IO) {
        for (item in items) {
            try {
                contentResolver.openInputStream(item.uri)?.use { stream ->
                    secureStorageManager.encryptAndStore(item.id, stream)
                }
                val existing = metaDao.getMeta(item.id)
                if (existing == null) {
                    metaDao.insertOrUpdate(MediaMetaEntity(mediaId = item.id, isHidden = true))
                } else {
                    metaDao.setHidden(item.id, true)
                }
                try {
                    contentResolver.delete(item.uri, null, null)
                } catch (ignored: Exception) {}
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        true
    }

    override suspend fun restoreFromSecureFolder(mediaId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = secureStorageManager.decryptToStream(mediaId)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "restored_$mediaId.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SecureRestored")
                }
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { out ->
                    inputStream.copyTo(out)
                }
            }
            secureStorageManager.deleteSecureFile(mediaId)
            metaDao.setHidden(mediaId, false)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getSecureItems(): Flow<List<MediaItem>> {
        return metaDao.getHiddenMediaIds().map { hiddenIds ->
            hiddenIds.mapNotNull { id ->
                val file = secureStorageManager.getSecureFile(id)
                if (file.exists()) {
                    MediaItem(
                        id = id,
                        uri = Uri.fromFile(file),
                        path = file.absolutePath,
                        displayName = "Secure Item $id",
                        mimeType = "image/jpeg",
                        mediaType = MediaType.IMAGE,
                        size = file.length(),
                        dateAdded = System.currentTimeMillis() / 1000,
                        dateTaken = System.currentTimeMillis(),
                        isHidden = true
                    )
                } else null
            }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun createCustomAlbum(name: String): Long = withContext(Dispatchers.IO) {
        customAlbumDao.insertAlbum(CustomAlbumEntity(name = name))
    }

    override suspend fun renameCustomAlbum(albumId: Long, newName: String) = withContext(Dispatchers.IO) {
        customAlbumDao.renameAlbum(albumId, newName)
    }

    override suspend fun deleteCustomAlbum(albumId: Long) = withContext(Dispatchers.IO) {
        customAlbumDao.deleteAlbum(albumId)
    }

    override suspend fun addItemsToCustomAlbum(albumId: Long, mediaIds: List<Long>) = withContext(Dispatchers.IO) {
        val items = mediaIds.map { CustomAlbumItemEntity(albumId = albumId, mediaId = it) }
        customAlbumDao.insertItems(items)
        if (mediaIds.isNotEmpty()) {
            customAlbumDao.updateCover(albumId, mediaIds.first())
        }
    }

    override suspend fun removeItemsFromCustomAlbum(albumId: Long, mediaIds: List<Long>) = withContext(Dispatchers.IO) {
        customAlbumDao.removeItems(albumId, mediaIds)
    }

    override fun searchMedia(query: String): Flow<List<MediaItem>> {
        return getAllMedia(MediaFilter.ALL).map { list ->
            if (query.isBlank()) list
            else {
                val q = query.lowercase().trim()
                list.filter { item ->
                    item.displayName.lowercase().contains(q) ||
                    item.bucketName.lowercase().contains(q) ||
                    (q == "photo" || q == "photos" || q == "image" && item.mediaType == MediaType.IMAGE) ||
                    (q == "video" || q == "videos" && item.mediaType == MediaType.VIDEO) ||
                    (q == "favorite" || q == "favorites" && item.isFavorite)
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getRecentSearches(): Flow<List<RecentSearchEntity>> {
        return recentSearchDao.getRecentSearches().flowOn(Dispatchers.IO)
    }

    override suspend fun addRecentSearch(query: String) = withContext(Dispatchers.IO) {
        if (query.isNotBlank()) {
            recentSearchDao.insertSearch(RecentSearchEntity(query = query.trim()))
        }
    }

    override suspend fun clearRecentSearches() = withContext(Dispatchers.IO) {
        recentSearchDao.clearAll()
    }

    override suspend fun getExifData(item: MediaItem): ExifData {
        return fetcher.getExifData(item)
    }

    override suspend fun saveEditedImage(sourceItem: MediaItem, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        try {
            val fileName = "EDIT_${System.currentTimeMillis()}_${sourceItem.displayName}"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Edited")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val newUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
            contentResolver.openOutputStream(newUri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            // Preserve EXIF from source if possible
            try {
                contentResolver.openInputStream(sourceItem.uri)?.use { srcStream ->
                    val srcExif = ExifInterface(srcStream)
                    contentResolver.openFileDescriptor(newUri, "rw")?.use { pfd ->
                        val dstExif = ExifInterface(pfd.fileDescriptor)
                        val tags = arrayOf(
                            ExifInterface.TAG_MAKE,
                            ExifInterface.TAG_MODEL,
                            ExifInterface.TAG_F_NUMBER,
                            ExifInterface.TAG_EXPOSURE_TIME,
                            ExifInterface.TAG_ISO_SPEED_RATINGS,
                            ExifInterface.TAG_FOCAL_LENGTH,
                            ExifInterface.TAG_WHITE_BALANCE
                        )
                        for (tag in tags) {
                            srcExif.getAttribute(tag)?.let { dstExif.setAttribute(tag, it) }
                        }
                        dstExif.saveAttributes()
                    }
                }
            } catch (ignored: Exception) {}

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(newUri, values, null, null)
            }
            newUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun overwriteOriginalImage(sourceItem: MediaItem, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        try {
            contentResolver.openOutputStream(sourceItem.uri, "rwt")?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            } ?: return@withContext false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun copyMedia(items: List<MediaItem>, targetDirectoryName: String): Boolean = withContext(Dispatchers.IO) {
        val targetRelativePath = when {
            targetDirectoryName.equals("Camera", ignoreCase = true) -> "${Environment.DIRECTORY_DCIM}/Camera"
            targetDirectoryName.equals("Screenshots", ignoreCase = true) -> "${Environment.DIRECTORY_PICTURES}/Screenshots"
            targetDirectoryName.equals("Download", ignoreCase = true) || targetDirectoryName.equals("Downloads", ignoreCase = true) -> Environment.DIRECTORY_DOWNLOADS
            else -> "${Environment.DIRECTORY_PICTURES}/$targetDirectoryName"
        }

        for (item in items) {
            try {
                val isVideo = item.mediaType == MediaType.VIDEO
                val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, item.displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, targetRelativePath)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                val newUri = contentResolver.insert(collection, values)
                if (newUri != null) {
                    contentResolver.openInputStream(item.uri)?.use { input ->
                        contentResolver.openOutputStream(newUri)?.use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        contentResolver.update(newUri, values, null, null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        true
    }

    override suspend fun moveMedia(items: List<MediaItem>, targetDirectoryName: String): Boolean = withContext(Dispatchers.IO) {
        copyMedia(items, targetDirectoryName)
        for (item in items) {
            try {
                contentResolver.delete(item.uri, null, null)
            } catch (e: Exception) {}
        }
        true
    }
}

