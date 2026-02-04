package io.legado.app.model

import io.legado.app.model.dictionary.BinaryDictionary
import io.legado.app.model.dictionary.DictionaryCompiler
import io.legado.app.utils.DictManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

/**
 * Loads VietPhrase translation dictionaries into memory using BinaryDictionary.
 * Supports Parallel Loading and Build-Time/On-Device Compilation.
 */
object TranslationLoader {

    @Volatile
    private var translationData: TranslationData? = null

    @Volatile
    private var isLoading = false

    /**
     * Load translation data (lazy loading, singleton pattern)
     */
    suspend fun loadTranslationData(): TranslationData? = withContext(Dispatchers.IO) {
        // Return cached data if available
        if (translationData != null) {
            // android.util.Log.d("TranslationLoader", "Returning cached data")
            return@withContext translationData
        }

        // Prevent concurrent loading
        if (isLoading) {
            android.util.Log.d("TranslationLoader", "Waiting for other thread to load...")
            while (isLoading) {
                delay(100)
                if (translationData != null) return@withContext translationData
            }
        }

        isLoading = true
        android.util.Log.d("TranslationLoader", "Starting loadTranslationData...")
        val startTime = System.currentTimeMillis()
        try {
            val namesDeferred = async { 
                val t = System.currentTimeMillis()
                val d = loadOrCompile(DictManager.DictType.NAMES, "names.bin") 
                android.util.Log.d("TranslationLoader", "Loaded NAMES in ${System.currentTimeMillis() - t}ms")
                d
            }
            val vpDeferred = async { 
                val t = System.currentTimeMillis()
                val d = loadOrCompile(DictManager.DictType.VIETPHRASE, "vietphrase.bin") 
                android.util.Log.d("TranslationLoader", "Loaded VIETPHRASE in ${System.currentTimeMillis() - t}ms")
                d
            }
            val phienAmDeferred = async { 
                val t = System.currentTimeMillis()
                val d = loadOrCompile(DictManager.DictType.PHIENAM, "phienam.bin") 
                android.util.Log.d("TranslationLoader", "Loaded PHIENAM in ${System.currentTimeMillis() - t}ms")
                d
            }

            translationData = TranslationData(
                namesDeferred.await(),
                vpDeferred.await(),
                phienAmDeferred.await()
            )
            
            android.util.Log.d("TranslationLoader", "Total load time: ${System.currentTimeMillis() - startTime}ms")
            translationData
        } catch (e: Exception) {
            android.util.Log.e("TranslationLoader", "Error loading data", e)
            e.printStackTrace()
            null
        } finally {
            isLoading = false
        }
    }

    private fun loadOrCompile(type: DictManager.DictType, assetBin: String, retry: Boolean = true): BinaryDictionary {
        val cacheDir = File(appCtx.filesDir, "dict_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        // 1. Check User Custom Dict (TXT)
        if (DictManager.hasCustomDict(type)) {
            val txtFile = DictManager.getCustomDictFile(type)
            val cacheFile = File(cacheDir, "user_${type.fileName}.bin")

            // Compile if missing or outdated
            if (!cacheFile.exists() || cacheFile.lastModified() < txtFile.lastModified()) {
                DictionaryCompiler.compile(txtFile, cacheFile)
            }
            return try {
                mapFile(cacheFile)
            } catch (e: Exception) {
                if (retry) {
                    android.util.Log.e("TranslationLoader", "User dict corrupted, determining...", e)
                    cacheFile.delete()
                    return loadOrCompile(type, assetBin, false)
                }
                throw e
            }
        }

        // 2. Load Asset Binary (Prebuilt)
        return try {
            try {
                mapAsset("dict/$assetBin")
            } catch (e: Exception) {
                // If mapAsset fails (e.g. invalid format in asset or missing), fallback to runtime compile
                throw e
            }
        } catch (e: Exception) {
            // 3. Fallback: Compile from Asset TXT (Runtime Build)
            val cacheFile = File(cacheDir, "asset_$assetBin")
            if (!cacheFile.exists()) {
                val assetTxt = getAssetTxtPath(type)
                val tmpFile = File.createTempFile("compile", ".txt", appCtx.cacheDir)
                appCtx.assets.open(assetTxt).use { input ->
                    FileOutputStream(tmpFile).use { output -> input.copyTo(output) }
                }
                DictionaryCompiler.compile(tmpFile, cacheFile)
                tmpFile.delete()
            }
            try {
                mapFile(cacheFile)
            } catch (ex: Exception) {
                if (retry) {
                    android.util.Log.e("TranslationLoader", "Asset dict cache corrupted, rebuilding...", ex)
                    cacheFile.delete()
                    return loadOrCompile(type, assetBin, false)
                }
                throw ex
            }
        }
    }

    private fun getAssetTxtPath(type: DictManager.DictType): String {
        return when (type) {
            DictManager.DictType.NAMES -> "translate/vietphrase/Names.txt"
            DictManager.DictType.VIETPHRASE -> "translate/vietphrase/VietPhrase.txt"
            DictManager.DictType.PHIENAM -> "translate/vietphrase/ChinesePhienAmWords.txt"
        }
    }

    private fun mapFile(file: File): BinaryDictionary {
        val channel = FileInputStream(file).channel
        val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
        return BinaryDictionary(buffer)
    }

    private fun mapAsset(path: String): BinaryDictionary {
        val afd = appCtx.assets.openFd(path)
        val channel = FileInputStream(afd.fileDescriptor).channel
        val buffer = channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.length)
        return BinaryDictionary(buffer)
    }

    /**
     * Clear cached data
     */
    fun clearCache() {
        translationData?.names?.close()
        translationData?.vietPhrase?.close()
        translationData?.chinesePhienAm?.close()
        translationData = null
    }

    /**
     * Reload specific dictionary type
     */
    suspend fun reloadType(type: DictManager.DictType) {
        // Since we load all at once, we just clear and let next request reload
        // Or we could be granular, but parallel loading is fast enough (10-50ms)
        clearCache()
    }
}
