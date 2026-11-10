package com.boscoovic.offlinegallery.data.repository

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.boscoovic.offlinegallery.data.local.dao.TrashDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@HiltWorker
class TrashPurgeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val trashDao: TrashDao
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "TrashPurgePeriodicWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val expiredItems = trashDao.getExpiredItems(now)
            for (item in expiredItems) {
                val file = File(item.trashedFilePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            trashDao.deleteExpiredItems(now)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
