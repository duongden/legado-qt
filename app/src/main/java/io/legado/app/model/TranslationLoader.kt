package io.legado.app.model

import io.legado.app.utils.DictManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Loads VietPhrase translation dictionaries into memory using Trie structures
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
        translationData?.let { return@withContext it }

        // Prevent concurrent loading
        if (isLoading) {
            while (isLoading) {
                kotlinx.coroutines.delay(100)
            }
            return@withContext translationData
        }

        isLoading = true
        try {
            val assets = appCtx.assets

            // Load Names.txt
            val namesStream = if (DictManager.hasCustomDict(DictManager.DictType.NAMES)) {
                java.io.FileInputStream(DictManager.getCustomDictFile(DictManager.DictType.NAMES))
            } else {
                assets.open("translate/vietphrase/Names.txt")
            }
            val namesTrie = loadTrieFromStream(namesStream)

            // Load VietPhrase.txt
            val vpStream = if (DictManager.hasCustomDict(DictManager.DictType.VIETPHRASE)) {
                java.io.FileInputStream(DictManager.getCustomDictFile(DictManager.DictType.VIETPHRASE))
            } else {
                assets.open("translate/vietphrase/VietPhrase.txt")
            }
            val vietPhraseTrie = loadTrieFromStream(vpStream)

            // Load ChinesePhienAmWords.txt
            val phoneticStream = if (DictManager.hasCustomDict(DictManager.DictType.PHIENAM)) {
                java.io.FileInputStream(DictManager.getCustomDictFile(DictManager.DictType.PHIENAM))
            } else {
                assets.open("translate/vietphrase/ChinesePhienAmWords.txt")
            }
            val chinesePhienAm = loadMapFromStream(phoneticStream)

            translationData = TranslationData(namesTrie, vietPhraseTrie, chinesePhienAm)
            translationData
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            isLoading = false
        }
    }

    /**
     * Load a Trie from input stream (key=value format)
     */
    private fun loadTrieFromStream(inputStream: InputStream): Trie {
        val trie = Trie()
        inputStream.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8), 16384).use { reader ->
                reader.forEachLine { line ->
                    if (line.isNotBlank()) {
                        val parts = line.split("=", limit = 2)
                        if (parts.size == 2) {
                            val key = parts[0].trim()
                            val value = parts[1].trim()
                            if (key.isNotEmpty() && value.isNotEmpty()) {
                                trie.insert(key, value)
                            }
                        }
                    }
                }
            }
        }
        return trie
    }

    /**
     * Load a Map from input stream (key=value format)
     */
    private fun loadMapFromStream(inputStream: InputStream): Map<String, String> {
        val map = mutableMapOf<String, String>()
        inputStream.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8), 16384).use { reader ->
                reader.forEachLine { line ->
                    if (line.isNotBlank()) {
                        val parts = line.split("=", limit = 2)
                        if (parts.size == 2) {
                            val key = parts[0].trim()
                            val value = parts[1].trim()
                            if (key.isNotEmpty() && value.isNotEmpty()) {
                                map[key] = value
                            }
                        }
                    }
                }
            }
        }
        return map
    }

    /**
     * Clear cached data (called when custom dict is imported/reset)
     */
    fun clearCache() {
        translationData = null
    }

    /**
     * Reload specific dictionary type
     */
    suspend fun reloadType(type: DictManager.DictType) {
        clearCache()
        // Next call to loadTranslationData will reload all
    }
}
