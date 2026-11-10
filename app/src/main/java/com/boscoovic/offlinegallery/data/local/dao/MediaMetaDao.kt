package com.boscoovic.offlinegallery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.boscoovic.offlinegallery.data.local.entity.MediaMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaMetaDao {

    @Query("SELECT * FROM media_meta WHERE mediaId = :mediaId")
    suspend fun getMeta(mediaId: Long): MediaMetaEntity?

    @Query("SELECT * FROM media_meta")
    fun getAllMeta(): Flow<List<MediaMetaEntity>>

    @Query("SELECT mediaId FROM media_meta WHERE isFavorite = 1")
    fun getFavoriteMediaIds(): Flow<List<Long>>

    @Query("SELECT mediaId FROM media_meta WHERE isHidden = 1")
    fun getHiddenMediaIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(meta: MediaMetaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(metas: List<MediaMetaEntity>)

    @Query("UPDATE media_meta SET isFavorite = :isFavorite WHERE mediaId = :mediaId")
    suspend fun setFavorite(mediaId: Long, isFavorite: Boolean)

    @Query("UPDATE media_meta SET isHidden = :isHidden WHERE mediaId = :mediaId")
    suspend fun setHidden(mediaId: Long, isHidden: Boolean)

    @Query("DELETE FROM media_meta WHERE mediaId = :mediaId")
    suspend fun delete(mediaId: Long)

    @Query("DELETE FROM media_meta WHERE mediaId NOT IN (:activeMediaIds)")
    suspend fun reconcileActive(activeMediaIds: List<Long>)
}

