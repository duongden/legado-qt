package io.legado.app.utils

import android.widget.TextView
import androidx.collection.LruCache
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.model.TranslationLoader
import splitties.init.appCtx
import java.util.regex.Pattern
import kotlinx.coroutines.*

/**
 * Translation utility for VietPhrase Chinese-Vietnamese translation
 * Ported from Dictionary.js logic
 */
object TranslateUtils {
    
    // Cache for translated texts (max 10MB worth of strings)
    private val translationCache = LruCache<String, String>(10 * 1024 * 1024)
    
    // Pending translation jobs
    private val pendingJobs = mutableMapOf<String, Job>()
    
    // Coroutine scope for translation
    private val translationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Regex patterns for processText
    private val trimSpacesBefore = Pattern.compile(" +([,.?!\\]>”’):])")
    private val trimSpacesAfter = Pattern.compile("([<\\[“‘(]) +")
    private val capitalizeRegex = Pattern.compile("(^\\s*|[.!?“‘”’\\[-]\\s*)(\\p{Ll})", Pattern.MULTILINE) // \p{Ll} is lowercase letter

    // Punctuation mapping
    private val punctuationMapping = mapOf(
        '。' to ". ", '，' to ", ", '、' to ", ", '；' to ";", '！' to "!", '？' to "?",
        '：' to ": ", '（' to "(", '）' to ")", '〔' to "[", '〕' to "]", '【' to "[",
        '】' to "]", '《' to "<", '》' to ">", '｛' to "{", '｝' to "}", '『' to "[",
        '』' to "]", '〈' to "<", '〉' to ">", '～' to "~", '—' to "-", '…' to "...",
        '〖' to "[", '〗' to "]", '〘' to "[", '〙' to "]", '〚' to "[", '〛' to "]", '　' to " "
    )

    /**
     * Check if translation is enabled
     */
    fun isTranslateEnabled(): Boolean {
        return appCtx.getPrefBoolean(PreferKey.translateEnable, false)
    }
    
    /**
     * Translate text for metadata (book names, authors, etc.)
     */
    suspend fun translateMeta(text: String?): String {
        return translateText(text, true)
    }
    
    /**
     * Translate text for content (chapter content)
     */
    suspend fun translateContent(text: String?): String {
        return translateText(text, false)
    }

    private suspend fun translateText(text: String?, isMeta: Boolean): String {
        if (text.isNullOrBlank()) return text ?: ""
        if (!isTranslateEnabled()) return text
        
        val cacheKey = generateCacheKey(text, isMeta)
        translationCache.get(cacheKey)?.let { return it }
        
        return try {
            val result = performTranslation(text)
            translationCache.put(cacheKey, result)
            result
        } catch (e: Exception) {
            e.printStackTrace()
            text
        }
    }
    
    /**
     * Translate a TextView's text asynchronously
     */
    fun translateView(
        textView: TextView,
        text: String?,
        transform: ((String) -> CharSequence)? = null,
        setter: ((CharSequence) -> Unit)? = null
    ) {
        if (text.isNullOrBlank()) {
            val finalText = transform?.invoke("") ?: ""
            (setter ?: { textView.text = it }).invoke(finalText)
            textView.setTag(R.id.tag_translate_key, null)
            return
        }
        
        if (!isTranslateEnabled()) {
            val finalText = transform?.invoke(text) ?: text
            (setter ?: { textView.text = it }).invoke(finalText)
            textView.setTag(R.id.tag_translate_key, null)
            return
        }
        
        val cacheKey = generateCacheKey(text, true)
        
        // Check if this view is already showing the translated version
        if (textView.getTag(R.id.tag_translate_key) == cacheKey) {
             translationCache.get(cacheKey)?.let { translated ->
                 // Already translated
                 return
             }
        }
        
        // Show original first
        val originalText = transform?.invoke(text) ?: text
        (setter ?: { textView.text = it }).invoke(originalText)
        textView.setTag(R.id.tag_translate_key, cacheKey)
        
        // Check cache
        translationCache.get(cacheKey)?.let { translated ->
            val finalText = transform?.invoke(translated) ?: translated
            (setter ?: { textView.text = it }).invoke(finalText)
            return
        }
        
        // Cancel any pending translation for this view
        pendingJobs[cacheKey]?.cancel()
        
        // Start async translation
        val job = translationScope.launch {
            try {
                val translated = performTranslation(text)
                translationCache.put(cacheKey, translated)
                
                withContext(Dispatchers.Main) {
                    // Only update if the view still has the same cache key
                    if (textView.getTag(R.id.tag_translate_key) == cacheKey) {
                        val finalText = transform?.invoke(translated) ?: translated
                        (setter ?: { textView.text = it }).invoke(finalText)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingJobs.remove(cacheKey)
            }
        }
        
        pendingJobs[cacheKey] = job
    }
    
    /**
     * Perform actual translation using VietPhrase dictionaries logic
     * Ported from Dictionary.js: translate(text)
     */
    private suspend fun performTranslation(text: String): String {
        val data = TranslationLoader.loadTranslationData() ?: return text
        
        // Step 1: Convert Punctuation
        val convertedText = convertPunctuation(text)

        // Step 2: Tokenize and Filter
        val tokens = tokenize(convertedText, data)
        
        // Step 3: Translate and PhienAm
        val translatedWords = ArrayList<String>()
        for (token in tokens) {
            // Filter: skip '的', '了', '著'
            if (token == "的" || token == "了" || token == "著") {
                continue
            }
            
            // Search in dictionaries (Names -> VietPhrase)
            var translation = searchInDictionaries(token, data)
            
            // If translation found, take first part (split '/')
            if (translation != null) {
                if (translation.contains("/")) {
                    translation = translation.split("/")[0]
                }
            } else {
                translation = token
            }
            
            val finalWord = if (translation == token) {
                // Not found in VietPhrase dictionaries
                 data.chinesePhienAm[token] ?: " $token " // Add spaces if not found (likely Chinese or Latin)
            } else {
                translation
            }
            
            translatedWords.add(finalWord!!)
        }
        
        // Step 4: Process Text
        return processText(translatedWords.joinToString(" "))
    }

    private fun searchInDictionaries(key: String, data: io.legado.app.model.TranslationData): String? {
        // Priority: Names > VietPhrase
        data.names.findLongestMatch(key, 0)?.let { (len, value) -> 
            if (len == key.length) return value 
        }
        data.vietPhrase.findLongestMatch(key, 0)?.let { (len, value) -> 
            if (len == key.length) return value 
        }
        
        return null
    }

    private fun tokenize(text: String, data: io.legado.app.model.TranslationData): List<String> {
        val output = ArrayList<String>()
        var currentIndex = 0
        val length = text.length
        
        while (currentIndex < length) {
            var longestMatchLen = 0
            
            // Check names dictionary
            data.names.findLongestMatch(text, currentIndex)?.let { (len, _) ->
                if (len > longestMatchLen) {
                    longestMatchLen = len
                }
            }
            
            // Check vietPhrase dictionary
            data.vietPhrase.findLongestMatch(text, currentIndex)?.let { (len, _) ->
                if (len > longestMatchLen) {
                    longestMatchLen = len
                }
            }
            
            if (longestMatchLen > 0) {
                // Found a word
                output.add(text.substring(currentIndex, currentIndex + longestMatchLen))
                currentIndex += longestMatchLen
            } else {
                // Not found
                if (isChineseCharacter(text[currentIndex])) {
                    output.add(text[currentIndex].toString())
                    currentIndex++
                } else {
                    // Non-Chinese sequence
                    val sb = StringBuilder()
                    sb.append(text[currentIndex])
                    currentIndex++
                    while (currentIndex < length && !isChineseCharacter(text[currentIndex])) {
                        sb.append(text[currentIndex])
                        currentIndex++
                    }
                    output.add(sb.toString())
                }
            }
        }
        return output
    }

    private fun isChineseCharacter(char: Char): Boolean {
        val charCode = char.code
        return charCode in 0x4E00..0x9FFF
    }

    private fun convertPunctuation(text: String): String {
        val sb = StringBuilder()
        for (char in text) {
            sb.append(punctuationMapping[char] ?: char)
        }
        return sb.toString()
    }

    private fun processText(input: String): String {
        // Step 1: Trim spaces
        val lines = input.split("\n")
        val processingLines = lines.map { it.trim() }
        val joined = processingLines.joinToString("\n")
        
        // Step 2: Regex Replacements
        var result = joined
        
        // trimSpacesBefore: / +([,.?!\]\>”’):])/g -> replace with $1
        var matcher = trimSpacesBefore.matcher(result)
        result = matcher.replaceAll("$1")
        
        // trimSpacesAfter: /([<\[“‘(]) +/g -> replace with $1
        matcher = trimSpacesAfter.matcher(result)
        result = matcher.replaceAll("$1")
        
        // capitalizeRegex: /(^\s*|[.!?“‘”’\[-]\s*)(\p{Ll})/gu -> uppercase $2
        val sb = StringBuilder()
        matcher = capitalizeRegex.matcher(result)
        var lastEnd = 0
        while (matcher.find()) {
            sb.append(result.substring(lastEnd, matcher.start()))
            val p1 = matcher.group(1) // punctuation/space prefix
            val p2 = matcher.group(2) // lowercase char
            sb.append(p1).append(p2?.uppercase())
            lastEnd = matcher.end()
        }
        sb.append(result.substring(lastEnd))
        result = sb.toString()
        
        // Replace special quotes and trim multiple spaces
        result = result.replace(Regex("[“‘”’]"), "\"")
        result = result.replace(Regex(" +"), " ")
        
        return result
    }

    /**
     * Generate cache key for text
     */
    private fun generateCacheKey(text: String, isMeta: Boolean): String {
        val md5 = MD5Utils.md5Encode16(text)
        val type = if (isMeta) "meta" else "content"
        return "translate|vietphrase|v2|$type|$md5"
    }
    
    /**
     * Clear translation cache
     */
    fun clearCache() {
        translationCache.evictAll()
        TranslationLoader.clearCache()
    }
    
    /**
     * Cancel all pending translations
     */
    fun cancelAll() {
        pendingJobs.values.forEach { it.cancel() }
        pendingJobs.clear()
    }
}
