package io.legado.app.model

import io.legado.app.utils.DictManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import splitties.init.appCtx
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Loads VietPhrase translation dictionaries into memory using DoubleArrayTrie structures
 * Supports both binary (.dat) and text (.txt) formats for backward compatibility
 */
object TranslationLoader {

    @Volatile
    private var translationData: TranslationData? = null

    @Volatile
    private var isLoading = false

    private val loadMutex = Mutex()

    private const val DEFAULT_BIN_DIR = "translate/binary"
    private const val PHONETIC_BIN_SUFFIX = ".bin"
    private const val PHONETIC_MAGIC = 0x50484D50
    private const val PHONETIC_VERSION = 1

    /**
     * Load translation data (lazy loading, singleton pattern)
     * Priority: Binary files > Custom text files > Default text files
     */
    suspend fun loadTranslationData(): TranslationData? = withContext(Dispatchers.IO) {
        
        // Return cached data if available
        translationData?.let { 
            return@withContext it 
        }

        if (!loadMutex.tryLock()) {
            loadMutex.lock()
        }

        try {
            // Double-check after acquiring lock
            translationData?.let {
                return@withContext it
            }

            isLoading = true
            try {
                val assets = appCtx.assets

                // Load Names dictionary
                val namesTrie = try {
                    loadDictionary(
                        binaryAsset = "translate/vietphrase/Names.dat",
                        textAsset = "translate/vietphrase/Names.txt",
                        customType = DictManager.DictType.NAMES
                    )
                } catch (e: Exception) {
                    android.util.Log.e("TranslationLoader", "Failed to load Names dictionary", e)
                    throw e
                }

                // Load VietPhrase dictionary
                val vietPhraseTrie = try {
                    loadDictionary(
                        binaryAsset = "translate/vietphrase/VietPhrase.dat",
                        textAsset = "translate/vietphrase/VietPhrase.txt",
                        customType = DictManager.DictType.VIETPHRASE
                    )
                } catch (e: Exception) {
                    android.util.Log.e("TranslationLoader", "Failed to load VietPhrase dictionary", e)
                    throw e
                }

                // Load ChinesePhienAm dictionary (always HashMap for phonetic mapping)
                val chinesePhienAm = try {
                    loadPhoneticDictionary(
                        textAsset = "translate/vietphrase/ChinesePhienAmWords.txt",
                        customType = DictManager.DictType.PHIENAM
                    )
                } catch (e: Exception) {
                    android.util.Log.e("TranslationLoader", "Failed to load ChinesePhienAm dictionary", e)
                    throw e
                }

                translationData = TranslationData(namesTrie, vietPhraseTrie, chinesePhienAm)
                translationData
            } catch (e: Exception) {
                android.util.Log.e("TranslationLoader", "Error loading translation data", e)
                null
            } finally {
                isLoading = false
            }
        } finally {
            loadMutex.unlock()
        }
    }

    /**
     * Load dictionary with DoubleArrayTrie
     * Priority: Binary file > Custom text file > Default text file
     */
    private suspend fun loadDictionary(
        binaryAsset: String,
        textAsset: String,
        customType: DictManager.DictType
    ): DoubleArrayTrie = withContext(Dispatchers.IO) {
        val trie = DoubleArrayTrie()

        // Try custom dictionary first
        if (DictManager.hasCustomDict(customType)) {
            val customFile = DictManager.getCustomDictFile(customType)
            val customBinaryFile = File(customFile.parent, "${customFile.nameWithoutExtension}.dat")

            if (customBinaryFile.exists()) {
                try {
                    FileInputStream(customBinaryFile).use { stream ->
                        trie.load(stream)
                        return@withContext trie
                    }
                } catch (e: Exception) {
                    try {
                        customBinaryFile.delete()
                        android.util.Log.w("TranslationLoader", "Deleted corrupted custom binary: ${customBinaryFile.path}")
                    } catch (_: Exception) {
                    }
                }
            }

            if (customFile.exists()) {
                FileInputStream(customFile).use { stream ->
                    val entries = loadEntriesFromText(stream)
                    trie.build(entries)

                    try {
                        val tmpFile = File(customBinaryFile.parentFile, customBinaryFile.name + ".tmp")
                        try {
                            FileOutputStream(tmpFile).use { fos ->
                                BufferedOutputStream(fos, 1024 * 1024).use { bos ->
                                    trie.save(bos)
                                }
                            }
                            if (customBinaryFile.exists()) customBinaryFile.delete()
                            if (!tmpFile.renameTo(customBinaryFile)) {
                                throw IllegalStateException("Failed to rename temp cache file to final: ${customBinaryFile.name}")
                            }
                        } catch (e: Exception) {
                            try { tmpFile.delete() } catch (_: Exception) { }
                            android.util.Log.w("TranslationLoader", "Failed to cache custom trie", e)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("TranslationLoader", "Failed to cache custom trie", e)
                    }

                    return@withContext trie
                }
            }
        }

        // Try persistent default binary in filesDir
        try {
            val binDir = File(appCtx.filesDir, DEFAULT_BIN_DIR)
            val binFile = File(binDir, File(binaryAsset).name)
            if (binFile.exists()) {
                FileInputStream(binFile).use { stream ->
                    trie.load(stream)
                    return@withContext trie
                }
            }
        } catch (e: Exception) {
            try {
                val binDir = File(appCtx.filesDir, DEFAULT_BIN_DIR)
                val binFile = File(binDir, File(binaryAsset).name)
                if (binFile.exists()) {
                    binFile.delete()
                    android.util.Log.w("TranslationLoader", "Deleted corrupted persistent binary: ${binFile.path}")
                }
            } catch (_: Exception) {
            }
            android.util.Log.w("TranslationLoader", "Failed to load persistent binary, will fallback", e)
        }
        
        // Try to load binary file first (pre-built or cached)
        try {
            appCtx.assets.open(binaryAsset).use { stream ->
                trie.load(stream)
                return@withContext trie
            }
        } catch (e: Exception) {
        }
        
        // Load default text file and cache it
        appCtx.assets.open(textAsset).use { stream ->
            val entries = loadEntriesFromText(stream)
            
            // Build trie
            trie.build(entries)
            
            // Try to cache to app's internal storage for future use
            try {
                val binDir = File(appCtx.filesDir, DEFAULT_BIN_DIR)
                binDir.mkdirs()
                val binFile = File(binDir, File(binaryAsset).name)
                val tmpFile = File(binDir, binFile.name + ".tmp")
                try {
                    FileOutputStream(tmpFile).use { fos ->
                        BufferedOutputStream(fos, 1024 * 1024).use { bos ->
                            trie.save(bos)
                        }
                    }
                    if (binFile.exists()) binFile.delete()
                    if (!tmpFile.renameTo(binFile)) {
                        throw IllegalStateException("Failed to rename temp cache file to final: ${binFile.name}")
                    }
                } catch (e: Exception) {
                    try { tmpFile.delete() } catch (_: Exception) { }
                    android.util.Log.w("TranslationLoader", "Failed to cache default trie", e)
                }
            } catch (e: Exception) {
                android.util.Log.w("TranslationLoader", "Failed to cache default trie", e)
            }
        }
        
        trie
    }
    
    /**
     * Load phonetic dictionary as HashMap (kept as is for performance)
     */
    private suspend fun loadPhoneticDictionary(
        textAsset: String,
        customType: DictManager.DictType
    ): Map<String, String> = withContext(Dispatchers.IO) {
        if (DictManager.hasCustomDict(customType)) {
            val customFile = DictManager.getCustomDictFile(customType)
            val customBin = File(customFile.parent, "${customFile.nameWithoutExtension}$PHONETIC_BIN_SUFFIX")

            if (customBin.exists()) {
                try {
                    android.util.Log.d("TranslationLoader", "Trying to load custom phonetic binary: ${customBin.path}")
                    return@withContext loadPhoneticBinary(customBin)
                } catch (e: Exception) {
                    try { customBin.delete() } catch (_: Exception) { }
                    android.util.Log.w("TranslationLoader", "Failed to load custom phonetic binary, fallback", e)
                }
            }

            if (customFile.exists()) {
                android.util.Log.d("TranslationLoader", "Loading custom phonetic file: ${customFile.path}")
                FileInputStream(customFile).use { stream ->
                    val map = loadMapFromStream(stream)
                    android.util.Log.d("TranslationLoader", "Custom phonetic entries loaded: ${map.size}")
                    try {
                        savePhoneticBinary(map, customBin)
                        android.util.Log.d("TranslationLoader", "Custom phonetic cached to: ${customBin.path}")
                    } catch (e: Exception) {
                        android.util.Log.w("TranslationLoader", "Failed to cache custom phonetic", e)
                    }
                    return@withContext map
                }
            }
        }

        val defaultBinDir = File(appCtx.filesDir, DEFAULT_BIN_DIR)
        val defaultBin = File(defaultBinDir, File(textAsset).nameWithoutExtension + PHONETIC_BIN_SUFFIX)
        if (defaultBin.exists()) {
            try {
                android.util.Log.d("TranslationLoader", "Trying to load default phonetic binary: ${defaultBin.path}")
                return@withContext loadPhoneticBinary(defaultBin)
            } catch (e: Exception) {
                try { defaultBin.delete() } catch (_: Exception) { }
                android.util.Log.w("TranslationLoader", "Failed to load default phonetic binary, fallback", e)
            }
        }
        
        // Load default file
        android.util.Log.d("TranslationLoader", "Loading default phonetic file: $textAsset")
        appCtx.assets.open(textAsset).use { stream ->
            val map = loadMapFromStream(stream)
            android.util.Log.d("TranslationLoader", "Default phonetic entries loaded: ${map.size}")
            try {
                defaultBinDir.mkdirs()
                savePhoneticBinary(map, defaultBin)
                android.util.Log.d("TranslationLoader", "Default phonetic cached to: ${defaultBin.path}")
            } catch (e: Exception) {
                android.util.Log.w("TranslationLoader", "Failed to cache default phonetic", e)
            }
            map
        }
    }

    suspend fun prebuildAll() {
        prebuildType(DictManager.DictType.NAMES)
        prebuildType(DictManager.DictType.VIETPHRASE)
        prebuildType(DictManager.DictType.PHIENAM)
    }

    suspend fun prebuildType(type: DictManager.DictType) = withContext(Dispatchers.IO) {
        loadMutex.withLock {
            when (type) {
                DictManager.DictType.NAMES -> {
                    loadDictionary(
                        binaryAsset = "translate/vietphrase/Names.dat",
                        textAsset = "translate/vietphrase/Names.txt",
                        customType = DictManager.DictType.NAMES
                    )
                }
                DictManager.DictType.VIETPHRASE -> {
                    loadDictionary(
                        binaryAsset = "translate/vietphrase/VietPhrase.dat",
                        textAsset = "translate/vietphrase/VietPhrase.txt",
                        customType = DictManager.DictType.VIETPHRASE
                    )
                }
                DictManager.DictType.PHIENAM -> {
                    loadPhoneticDictionary(
                        textAsset = "translate/vietphrase/ChinesePhienAmWords.txt",
                        customType = DictManager.DictType.PHIENAM
                    )
                }
            }
        }
    }

    private fun loadPhoneticBinary(file: File): Map<String, String> {
        DataInputStream(FileInputStream(file)).use { dis ->
            val magic = dis.readInt()
            val version = dis.readInt()
            if (magic != PHONETIC_MAGIC || version != PHONETIC_VERSION) {
                throw IllegalStateException("Unsupported phonetic binary format")
            }
            val size = dis.readInt()
            val map = HashMap<String, String>(size)
            repeat(size) {
                val k = dis.readUTF()
                val v = dis.readUTF()
                map[k] = v
            }
            return map
        }
    }

    private fun savePhoneticBinary(map: Map<String, String>, file: File) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        DataOutputStream(BufferedOutputStream(FileOutputStream(tmp), 256 * 1024)).use { dos ->
            dos.writeInt(PHONETIC_MAGIC)
            dos.writeInt(PHONETIC_VERSION)
            dos.writeInt(map.size)
            map.forEach { (k, v) ->
                dos.writeUTF(k)
                dos.writeUTF(v)
            }
        }
        if (file.exists()) file.delete()
        if (!tmp.renameTo(file)) {
            tmp.delete()
            throw IllegalStateException("Failed to finalize phonetic binary")
        }
    }
    
    /**
     * Load entries from text file (key=value format)
     */
    private fun loadEntriesFromText(inputStream: InputStream): List<Pair<String, String>> {
        val entries = mutableListOf<Pair<String, String>>()
        var totalLines = 0
        var validEntries = 0
        var filteredEntries = 0
        
        inputStream.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8), 32768).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    totalLines++
                    val currentLine = line!!
                    
                    if (currentLine.isNotBlank()) {
                        // Fast check for "=" before expensive operations
                        if (currentLine.contains('=')) {
                            // Only remove actual control characters, not legitimate content
                            val cleanLine = currentLine.replace("\u0000", "").replace("\u0001", "").replace("\u0004", "").trim()
                            
                            if (cleanLine.isNotEmpty() && cleanLine.contains("=")) {
                                val parts = cleanLine.split("=", limit = 2)
                                if (parts.size == 2) {
                                    val key = parts[0].trim()
                                    val value = parts[1].trim()
                                    // Simplified validation - only check for empty/null
                                    if (key.isNotEmpty() && value.isNotEmpty()) {
                                        entries.add(Pair(key, value))
                                        validEntries++
                                    } else {
                                        filteredEntries++
                                    }
                                }
                            }
                        }
                    }
                    
                    // Log progress every 5000 lines to avoid spam
                    if (totalLines % 5000 == 0) {
                        android.util.Log.d("TranslationLoader", "Processed $totalLines lines...")
                    }
                }
            }
        }
        android.util.Log.d("TranslationLoader", "Lines processed: $totalLines, Valid entries: $validEntries, Filtered: $filteredEntries")
        return entries
    }

    /**
     * Load a Map from input stream (key=value format)
     */
    private fun loadMapFromStream(inputStream: InputStream): Map<String, String> {
        val map = mutableMapOf<String, String>()
        var totalLines = 0
        
        inputStream.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8), 32768).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    totalLines++
                    val currentLine = line!!
                    
                    if (currentLine.isNotBlank()) {
                        // Fast check for "=" before expensive operations
                        if (currentLine.contains('=')) {
                            // Only remove actual control characters, not legitimate content
                            val cleanLine = currentLine.replace("\u0000", "").replace("\u0001", "").replace("\u0004", "").trim()
                            
                            if (cleanLine.isNotEmpty() && cleanLine.contains("=")) {
                                val parts = cleanLine.split("=", limit = 2)
                                if (parts.size == 2) {
                                    val key = parts[0].trim()
                                    val value = parts[1].trim()
                                    // Simplified validation - only check for empty/null
                                    if (key.isNotEmpty() && value.isNotEmpty()) {
                                        map[key] = value
                                    }
                                }
                            }
                        }
                    }
                    
                    // Log progress every 5000 lines
                    if (totalLines % 5000 == 0) {
                        android.util.Log.d("TranslationLoader", "Phonetic processed $totalLines lines...")
                    }
                }
            }
        }
        android.util.Log.d("TranslationLoader", "Phonetic map loaded: ${map.size} entries from $totalLines lines")
        return map
    }

    /**
     * Clear cached data (called when custom dict is imported/reset)
     */
    fun clearCache() {
        translationData = null
    }
    
    /**
     * Clear all cached binary files (for debugging or reset)
     */
    fun clearAllBinaryCache() {
        try {
            // Clear app cache directory
            val cacheDir = File(appCtx.cacheDir, "dictionaries")
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".dat")) {
                        file.delete()
                        android.util.Log.d("TranslationLoader", "Deleted cached binary: ${file.name}")
                    }
                }
            }
            
            // Clear custom binary files
            val customDir = DictManager.getCustomDictDir()
            customDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".dat")) {
                    file.delete()
                    android.util.Log.d("TranslationLoader", "Deleted custom binary: ${file.name}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TranslationLoader", "Error clearing binary cache", e)
        }
    }

    /**
     * Reload specific dictionary type
     */
    suspend fun reloadType(type: DictManager.DictType) {
        clearCache()
        // Next call to loadTranslationData will reload all
    }
}
