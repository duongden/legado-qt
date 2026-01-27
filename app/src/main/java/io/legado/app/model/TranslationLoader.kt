package io.legado.app.model

import android.content.res.AssetManager
import io.legado.app.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Loads VietPhrase translation dictionaries from assets
 */
object TranslationLoader {
    
    private var translationData: TranslationData? = null
    private var isLoading = false
    
    /**
     * Load all translation dictionaries
     */
    suspend fun loadTranslationData(): TranslationData? = withContext(Dispatchers.IO) {
        if (translationData != null) {
            return@withContext translationData
        }
        
        if (isLoading) {
            // Wait for ongoing loading
            while (isLoading) {
                kotlinx.coroutines.delay(100)
            }
            return@withContext translationData
        }
        
        isLoading = true
        
        try {
            val assets = appCtx.assets
            
            // Load dictionaries
            val names = Trie()
            val vietPhrase = Trie()
            val chinesePhienAm = mutableMapOf<String, String>()
            
            // Load Names.txt
            loadDictionary(assets, "translate/vietphrase/Names.txt", names, true)
            
            // Load VietPhrase.txt
            loadDictionary(assets, "translate/vietphrase/VietPhrase.txt", vietPhrase, true)
            
            // Load ChinesePhienAmWords.txt
            loadPhonetics(assets, "translate/vietphrase/ChinesePhienAmWords.txt", chinesePhienAm)
            
            translationData = TranslationData(names, vietPhrase, chinesePhienAm)
            translationData
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            isLoading = false
        }
    }
    
    /**
     * Load a dictionary file into a Trie
     * @param caseSensitive whether to preserve case when loading
     */
    private fun loadDictionary(assets: AssetManager, path: String, trie: Trie, caseSensitive: Boolean) {
        assets.open(path).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 8192).use { reader ->
                reader.forEachLine { line ->
                    if (line.isNotBlank()) {
                        val parts = line.split("=", limit = 2)
                        if (parts.size == 2) {
                            val key = if (caseSensitive) parts[0].trim() else parts[0].trim()
                            val value = parts[1].trim()
                            trie.insert(key, value)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Load phonetics dictionary
     */
    private fun loadPhonetics(assets: AssetManager, path: String, map: MutableMap<String, String>) {
        assets.open(path).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8), 8192).use { reader ->
                reader.forEachLine { line ->
                    if (line.isNotBlank()) {
                        val parts = line.split("=", limit = 2)
                        if (parts.size == 2) {
                            map[parts[0].trim()] = parts[1].trim()
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Clear loaded data to free memory
     */
    fun clear() {
        translationData = null
    }
}
