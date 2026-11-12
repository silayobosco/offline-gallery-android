package com.boscoovic.offlinegallery.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

data class SecureMeta(
    val displayName: String,
    val mimeType: String,
    val isVideo: Boolean,
    val size: Long,
    val dateAdded: Long
)

@Singleton
class SecureStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val secureDir: File by lazy {
        File(context.filesDir, "secure_vault").apply {
            if (!exists()) mkdirs()
        }
    }

    private val secureCacheDir: File by lazy {
        File(context.cacheDir, "secure_cache").apply {
            if (!exists()) mkdirs()
        }
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    suspend fun encryptAndStore(
        mediaId: Long,
        inputStream: InputStream,
        meta: SecureMeta? = null
    ): File = withContext(Dispatchers.IO) {
        val targetFile = File(secureDir, "sec_$mediaId.enc")
        if (targetFile.exists()) targetFile.delete()

        val encryptedFile = EncryptedFile.Builder(
            context,
            targetFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileOutput().use { outputStream ->
            inputStream.copyTo(outputStream)
        }

        if (meta != null) {
            val metaFile = File(secureDir, "sec_$mediaId.meta")
            metaFile.writeText("${meta.displayName}\n${meta.mimeType}\n${meta.isVideo}\n${meta.size}\n${meta.dateAdded}")
        }

        targetFile
    }

    fun getSecureMeta(mediaId: Long): SecureMeta? {
        val metaFile = File(secureDir, "sec_$mediaId.meta")
        if (!metaFile.exists()) return null
        return try {
            val lines = metaFile.readLines()
            SecureMeta(
                displayName = lines.getOrElse(0) { "Secure_$mediaId" },
                mimeType = lines.getOrElse(1) { "image/jpeg" },
                isVideo = lines.getOrElse(2) { "false" }.toBoolean(),
                size = lines.getOrElse(3) { "0" }.toLongOrNull() ?: 0L,
                dateAdded = lines.getOrElse(4) { "0" }.toLongOrNull() ?: System.currentTimeMillis() / 1000
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getDecryptedFile(mediaId: Long, isVideo: Boolean = false): File = withContext(Dispatchers.IO) {
        val ext = if (isVideo) "mp4" else "jpg"
        val cached = File(secureCacheDir, "dec_${mediaId}.$ext")
        if (cached.exists() && cached.length() > 0L) {
            return@withContext cached
        }

        val targetFile = File(secureDir, "sec_$mediaId.enc")
        if (!targetFile.exists()) {
            return@withContext targetFile
        }

        val encryptedFile = EncryptedFile.Builder(
            context,
            targetFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileInput().use { input ->
            cached.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        cached
    }

    suspend fun decryptToStream(mediaId: Long): InputStream = withContext(Dispatchers.IO) {
        val targetFile = File(secureDir, "sec_$mediaId.enc")
        val encryptedFile = EncryptedFile.Builder(
            context,
            targetFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        encryptedFile.openFileInput()
    }

    suspend fun deleteSecureFile(mediaId: Long): Boolean = withContext(Dispatchers.IO) {
        val targetFile = File(secureDir, "sec_$mediaId.enc")
        if (targetFile.exists()) targetFile.delete()

        val metaFile = File(secureDir, "sec_$mediaId.meta")
        if (metaFile.exists()) metaFile.delete()

        File(secureCacheDir, "dec_${mediaId}.jpg").delete()
        File(secureCacheDir, "dec_${mediaId}.mp4").delete()
        true
    }

    fun clearCache() {
        try {
            secureCacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSecureFile(mediaId: Long): File {
        return File(secureDir, "sec_$mediaId.enc")
    }
}

