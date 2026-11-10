package com.boscoovic.offlinegallery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.boscoovic.offlinegallery.data.local.entity.CustomAlbumEntity
import com.boscoovic.offlinegallery.data.local.entity.CustomAlbumItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomAlbumDao {

    @Query("SELECT * FROM custom_albums ORDER BY createdAt DESC")
    fun getAllAlbums(): Flow<List<CustomAlbumEntity>>

    @Query("SELECT * FROM custom_albums WHERE albumId = :albumId")
    suspend fun getAlbumById(albumId: Long): CustomAlbumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: CustomAlbumEntity): Long

    @Query("UPDATE custom_albums SET name = :newName WHERE albumId = :albumId")
    suspend fun renameAlbum(albumId: Long, newName: String)

    @Query("UPDATE custom_albums SET coverMediaId = :mediaId WHERE albumId = :albumId")
    suspend fun updateCover(albumId: Long, mediaId: Long?)

    @Query("DELETE FROM custom_albums WHERE albumId = :albumId")
    suspend fun deleteAlbum(albumId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<CustomAlbumItemEntity>)

    @Query("DELETE FROM custom_album_items WHERE albumId = :albumId AND mediaId = :mediaId")
    suspend fun removeItem(albumId: Long, mediaId: Long)

    @Query("DELETE FROM custom_album_items WHERE albumId = :albumId AND mediaId IN (:mediaIds)")
    suspend fun removeItems(albumId: Long, mediaIds: List<Long>)

    @Query("SELECT mediaId FROM custom_album_items WHERE albumId = :albumId ORDER BY addedAt DESC")
    fun getMediaIdsForAlbum(albumId: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM custom_album_items WHERE albumId = :albumId")
    fun getAlbumItemCount(albumId: Long): Flow<Int>
}
