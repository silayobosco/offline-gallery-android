package com.boscoovic.offlinegallery.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val secureDir: File by lazy {
        File(context.filesDir, "secure_vault").apply {
            if (!exists()) mkdirs()
        }
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    suspend fun encryptAndStore(mediaId: Long, inputStream: InputStream): File = withContext(Dispatchers.IO) {
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
        targetFile
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
        if (targetFile.exists()) targetFile.delete() else true
    }

    fun getSecureFile(mediaId: Long): File {
        return File(secureDir, "sec_$mediaId.enc")
    }
}
