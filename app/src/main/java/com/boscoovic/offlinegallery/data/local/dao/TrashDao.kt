package com.boscoovic.offlinegallery.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.boscoovic.offlinegallery.data.local.entity.TrashItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {

    @Query("SELECT * FROM trash_items ORDER BY trashedAt DESC")
    fun getAllTrashItems(): Flow<List<TrashItemEntity>>

    @Query("SELECT * FROM trash_items WHERE mediaId = :mediaId")
    suspend fun getTrashItem(mediaId: Long): TrashItemEntity?

    @Query("SELECT mediaId FROM trash_items")
    fun getAllTrashedMediaIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashItem(item: TrashItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TrashItemEntity>)

    @Query("DELETE FROM trash_items WHERE mediaId = :mediaId")
    suspend fun delete(mediaId: Long)

    @Query("DELETE FROM trash_items WHERE mediaId IN (:mediaIds)")
    suspend fun deleteAll(mediaIds: List<Long>)

    @Query("SELECT * FROM trash_items WHERE expiresAt <= :currentTime")
    suspend fun getExpiredItems(currentTime: Long): List<TrashItemEntity>

    @Query("DELETE FROM trash_items WHERE expiresAt <= :currentTime")
    suspend fun deleteExpiredItems(currentTime: Long): Int

    @Query("DELETE FROM trash_items")
    suspend fun clearAll()
}

