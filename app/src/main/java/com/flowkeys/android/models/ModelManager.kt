package com.flowkeys.android.models

import android.content.Context
import android.os.StatFs
import com.flowkeys.android.core.model.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Manages the lifecycle, storage, validation, and retrieval of on-device ML model assets.
 */
class ModelManager(private val context: Context) {

    private val modelsDir = File(context.noBackupFilesDir, "models").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Checks if the required ASR model for [language] is installed and verified.
     */
    fun isModelInstalled(language: Language): Boolean {
        val item = ModelManifest.getModelForLanguage(language)
        val file = File(modelsDir, item.modelFileName)
        return file.exists() && file.length() > 0
    }

    /**
     * Returns the local File handle to the model weights.
     */
    fun getModelFile(language: Language): File {
        val item = ModelManifest.getModelForLanguage(language)
        return File(modelsDir, item.modelFileName)
    }

    /**
     * Returns the local File handle to the tokenizer tokens.
     */
    fun getTokensFile(language: Language): File {
        val item = ModelManifest.getModelForLanguage(language)
        return File(modelsDir, item.tokensFileName)
    }

    /**
     * Verifies that the internal storage has at least 500 MB free space.
     */
    fun hasSufficientDiskSpace(): Boolean {
        return try {
            val stat = StatFs(context.filesDir.path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes >= (500L * 1024 * 1024)
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Computes the SHA-256 checksum of a downloaded file.
     */
    suspend fun verifyChecksum(file: File, expectedSha256: String): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext false
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { fis ->
                var read = fis.read(buffer)
                while (read != -1) {
                    digest.update(buffer, 0, read)
                    read = fis.read(buffer)
                }
            }
            val hashBytes = digest.digest()
            val hexString = hashBytes.joinToString("") { "%02x".format(it) }
            return@withContext hexString.equals(expectedSha256, ignoreCase = true)
        } catch (e: Exception) {
            return@withContext false
        }
    }

    /**
     * Deletes a model pack to reclaim internal storage.
     */
    fun deleteModel(modelItem: ModelItem): Boolean {
        val modelFile = File(modelsDir, modelItem.modelFileName)
        val tokensFile = File(modelsDir, modelItem.tokensFileName)
        var success = true
        if (modelFile.exists()) success = success && modelFile.delete()
        if (tokensFile.exists()) success = success && tokensFile.delete()
        return success
    }
}
