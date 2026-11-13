package com.boscoovic.offlinegallery.data.mediastore

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.boscoovic.offlinegallery.data.local.entity.TrashItemEntity
import com.boscoovic.offlinegallery.data.model.ExifData
import com.boscoovic.offlinegallery.data.model.MediaItem
import com.boscoovic.offlinegallery.data.model.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreFetcher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver = context.contentResolver

    suspend fun queryAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        mediaList.addAll(queryImages())
        mediaList.addAll(queryVideos())
        mediaList.sortByDescending { if (it.dateTaken > 0) it.dateTaken else it.dateAdded * 1000 }
        mediaList
    }

    suspend fun queryTrashedMedia(): List<TrashItemEntity> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext emptyList()
        val trashed = mutableListOf<TrashItemEntity>()
        trashed.addAll(queryTrashedTable(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, isVideo = false))
        trashed.addAll(queryTrashedTable(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, isVideo = true))
        trashed.sortByDescending { it.trashedAt }
        trashed
    }

    private fun queryTrashedTable(tableUri: Uri, isVideo: Boolean): List<TrashItemEntity> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        val items = mutableListOf<TrashItemEntity>()
        val projectionList = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_EXPIRES
        )
        if (isVideo) {
            projectionList.add(MediaStore.Video.Media.DURATION)
        }
        val queryArgs = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.MediaColumns.DATE_MODIFIED)
            )
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
        }
        try {
            contentResolver.query(tableUri, projectionList.toTypedArray(), queryArgs, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val modCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val expCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_EXPIRES)
                val durCol = if (isVideo) cursor.getColumnIndex(MediaStore.Video.Media.DURATION) else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "TRASH_$id" else "TRASH_$id"
                    val mime = if (mimeCol >= 0) {
                        cursor.getString(mimeCol) ?: if (isVideo) "video/mp4" else "image/jpeg"
                    } else if (isVideo) "video/mp4" else "image/jpeg"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val modifiedSec = if (modCol >= 0) cursor.getLong(modCol) else (System.currentTimeMillis() / 1000L)
                    val expiresSec = if (expCol >= 0) cursor.getLong(expCol) else 0L
                    val durationMs = if (durCol >= 0) cursor.getLong(durCol) else 0L

                    val trashedAtMs = modifiedSec * 1000L
                    val expiresAtMs = if (expiresSec > 0L) expiresSec * 1000L else trashedAtMs + 30L * 24 * 60 * 60 * 1000L
                    val uri = ContentUris.withAppendedId(tableUri, id)

                    items.add(
                        TrashItemEntity(
                            mediaId = id,
                            originalUri = uri.toString(),
                            originalPath = path,
                            trashedAt = trashedAtMs,
                            expiresAt = expiresAtMs,
                            displayName = name,
                            mimeType = mime,
                            mediaType = if (isVideo) MediaType.VIDEO.name else MediaType.IMAGE.name,
                            size = size,
                            durationMs = durationMs,
                            trashedFilePath = "" // Empty path marks MediaStore System Trash item
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }

    suspend fun queryImages(bucketId: Long? = null): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val selection = if (bucketId != null) {
            "${MediaStore.Images.Media.BUCKET_ID} = ?"
        } else null

        val selectionArgs = if (bucketId != null) {
            arrayOf(bucketId.toString())
        } else null

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val path = cursor.getString(dataCol) ?: ""
                val name = cursor.getString(nameCol) ?: "IMG_$id"
                val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                val size = cursor.getLong(sizeCol)
                val dateAdded = cursor.getLong(dateAddedCol)
                val dateTaken = cursor.getLong(dateTakenCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                val bId = cursor.getLong(bucketIdCol)
                val bName = cursor.getString(bucketNameCol) ?: "Pictures"

                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                items.add(
                    MediaItem(
                        id = id,
                        uri = uri,
                        path = path,
                        displayName = name,
                        mimeType = mime,
                        mediaType = MediaType.IMAGE,
                        size = size,
                        dateAdded = dateAdded,
                        dateTaken = if (dateTaken > 0) dateTaken else dateAdded * 1000,
                        durationMs = 0L,
                        width = width,
                        height = height,
                        bucketId = bId,
                        bucketName = bName
                    )
                )
            }
        }
        items
    }

    suspend fun queryVideos(bucketId: Long? = null): List<MediaItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        val selection = if (bucketId != null) {
            "${MediaStore.Video.Media.BUCKET_ID} = ?"
        } else null

        val selectionArgs = if (bucketId != null) {
            arrayOf(bucketId.toString())
        } else null

        val sortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC, ${MediaStore.Video.Media.DATE_ADDED} DESC"

        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val path = cursor.getString(dataCol) ?: ""
                val name = cursor.getString(nameCol) ?: "VID_$id"
                val mime = cursor.getString(mimeCol) ?: "video/mp4"
                val size = cursor.getLong(sizeCol)
                val dateAdded = cursor.getLong(dateAddedCol)
                val dateTaken = cursor.getLong(dateTakenCol)
                val duration = cursor.getLong(durationCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)
                val bId = cursor.getLong(bucketIdCol)
                val bName = cursor.getString(bucketNameCol) ?: "Movies"

                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                items.add(
                    MediaItem(
                        id = id,
                        uri = uri,
                        path = path,
                        displayName = name,
                        mimeType = mime,
                        mediaType = MediaType.VIDEO,
                        size = size,
                        dateAdded = dateAdded,
                        dateTaken = if (dateTaken > 0) dateTaken else dateAdded * 1000,
                        durationMs = duration,
                        width = width,
                        height = height,
                        bucketId = bId,
                        bucketName = bName
                    )
                )
            }
        }
        items
    }

    suspend fun getExifData(item: MediaItem): ExifData = withContext(Dispatchers.IO) {
        if (item.mediaType != MediaType.IMAGE) {
            return@withContext ExifData(width = item.width, height = item.height)
        }
        try {
            val inputStream = contentResolver.openInputStream(item.uri) ?: return@withContext ExifData()
            inputStream.use { stream ->
                val exif = ExifInterface(stream)
                val latLong = FloatArray(2)
                val hasLatLong = exif.getLatLong(latLong)

                ExifData(
                    make = exif.getAttribute(ExifInterface.TAG_MAKE),
                    model = exif.getAttribute(ExifInterface.TAG_MODEL),
                    fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER),
                    exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
                    iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                        ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS),
                    focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH),
                    flash = exif.getAttribute(ExifInterface.TAG_FLASH),
                    whiteBalance = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE),
                    dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
                    latitude = if (hasLatLong) latLong[0].toDouble() else null,
                    longitude = if (hasLatLong) latLong[1].toDouble() else null,
                    width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, item.width),
                    height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, item.height),
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                )
            }
        } catch (e: Exception) {
            ExifData(width = item.width, height = item.height)
        }
    }
}

