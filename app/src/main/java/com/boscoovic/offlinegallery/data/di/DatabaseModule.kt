package com.boscoovic.offlinegallery.data.di

import android.content.Context
import androidx.room.Room
import com.boscoovic.offlinegallery.data.local.GalleryDatabase
import com.boscoovic.offlinegallery.data.local.dao.CustomAlbumDao
import com.boscoovic.offlinegallery.data.local.dao.MediaMetaDao
import com.boscoovic.offlinegallery.data.local.dao.RecentSearchDao
import com.boscoovic.offlinegallery.data.local.dao.TrashDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GalleryDatabase {
        return Room.databaseBuilder(
            context,
            GalleryDatabase::class.java,
            "offline_gallery.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideMediaMetaDao(db: GalleryDatabase): MediaMetaDao = db.mediaMetaDao()

    @Provides
    fun provideTrashDao(db: GalleryDatabase): TrashDao = db.trashDao()

    @Provides
    fun provideCustomAlbumDao(db: GalleryDatabase): CustomAlbumDao = db.customAlbumDao()

    @Provides
    fun provideRecentSearchDao(db: GalleryDatabase): RecentSearchDao = db.recentSearchDao()
}
