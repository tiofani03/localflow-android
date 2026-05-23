package com.localflow.sdk.data.cache

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

internal class DiskCache(
    private val context: Context,
    private val apiKey: String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    // Directory scoped to apiKey to separate cache if apiKey rotates or changes
    private val cacheDir: File by lazy {
        File(context.filesDir, "localflow_cache/$apiKey").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    private val translationsFile: File
        get() = File(cacheDir, "translations.json")

    private val metadataFile: File
        get() = File(cacheDir, "metadata.json")

    suspend fun saveCache(
        translations: Map<String, Map<String, String>>,
        metadata: CacheMetadata
    ): Boolean = withContext(Dispatchers.IO) {
        val transSaved = saveJsonAtomically(translationsFile, translations) { trans ->
            json.encodeToString(MapSerializer, trans)
        }
        val metaSaved = saveJsonAtomically(metadataFile, metadata) { meta ->
            json.encodeToString(CacheMetadata.serializer(), meta)
        }
        transSaved && metaSaved
    }

    suspend fun loadTranslations(): Map<String, Map<String, String>>? = withContext(Dispatchers.IO) {
        if (!translationsFile.exists()) return@withContext null
        try {
            val content = translationsFile.readText()
            json.decodeFromString<Map<String, Map<String, String>>>(content)
        } catch (e: Exception) {
            Log.e("DiskCache", "Error loading translations from disk", e)
            null
        }
    }

    suspend fun loadMetadata(): CacheMetadata? = withContext(Dispatchers.IO) {
        if (!metadataFile.exists()) return@withContext null
        try {
            val content = metadataFile.readText()
            json.decodeFromString<CacheMetadata>(content)
        } catch (e: Exception) {
            Log.e("DiskCache", "Error loading metadata from disk", e)
            null
        }
    }

    suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        try {
            if (translationsFile.exists()) translationsFile.delete()
            if (metadataFile.exists()) metadataFile.delete()
            if (cacheDir.exists()) cacheDir.deleteRecursively()
        } catch (e: Exception) {
            Log.e("DiskCache", "Error clearing cache directory", e)
        }
    }

    private fun <T> saveJsonAtomically(
        targetFile: File,
        data: T,
        encodeBlock: (T) -> String
    ): Boolean {
        val tempFile = File(cacheDir, "${targetFile.name}.tmp")
        try {
            val jsonString = encodeBlock(data)
            tempFile.writeText(jsonString)
            
            if (targetFile.exists() && !targetFile.delete()) {
                Log.e("DiskCache", "Failed to delete old file: ${targetFile.absolutePath}")
            }
            
            val renameSuccess = tempFile.renameTo(targetFile)
            if (!renameSuccess) {
                Log.e("DiskCache", "Failed to rename temp file to: ${targetFile.absolutePath}")
                return false
            }
            return true
        } catch (e: IOException) {
            Log.e("DiskCache", "IO error writing to ${targetFile.name}", e)
            if (tempFile.exists()) tempFile.delete()
            return false
        } catch (e: Exception) {
            Log.e("DiskCache", "General error writing to ${targetFile.name}", e)
            if (tempFile.exists()) tempFile.delete()
            return false
        }
    }

    // Helper custom MapSerializer to represent the nested structure
    private val MapSerializer = kotlinx.serialization.serializer<Map<String, Map<String, String>>>()
}
