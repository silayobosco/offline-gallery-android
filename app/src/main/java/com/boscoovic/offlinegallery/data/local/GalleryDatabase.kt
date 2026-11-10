package com.boscoovic.offlinegallery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.boscoovic.offlinegallery.data.local.dao.CustomAlbumDao
import com.boscoovic.offlinegallery.data.local.dao.MediaMetaDao
import com.boscoovic.offlinegallery.data.local.dao.RecentSearchDao
import com.boscoovic.offlinegallery.data.local.dao.TrashDao
import com.boscoovic.offlinegallery.data.local.entity.CustomAlbumEntity
import com.boscoovic.offlinegallery.data.local.entity.CustomAlbumItemEntity
import com.boscoovic.offlinegallery.data.local.entity.MediaMetaEntity
import com.boscoovic.offlinegallery.data.local.entity.RecentSearchEntity
import com.boscoovic.offlinegallery.data.local.entity.TrashItemEntity

@Database(
    entities = [
        MediaMetaEntity::class,
        TrashItemEntity::class,
        CustomAlbumEntity::class,
        CustomAlbumItemEntity::class,
        RecentSearchEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GalleryDatabase : RoomDatabase() {
    abstract fun mediaMetaDao(): MediaMetaDao
    abstract fun trashDao(): TrashDao
    abstract fun customAlbumDao(): CustomAlbumDao
    abstract fun recentSearchDao(): RecentSearchDao
}
