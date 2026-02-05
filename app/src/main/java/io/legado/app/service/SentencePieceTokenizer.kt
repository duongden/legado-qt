package io.legado.app.service

import android.content.Context
import io.legado.app.constant.AppLog
import org.json.JSONObject
import java.io.File

/**
 * SentencePiece-compatible tokenizer for Vietnamese to Chinese translation
 * Uses vocab-based greedy tokenization optimized for short search queries
 */
class SentencePieceTokenizer(context: Context) {
    
    private var vocabMap: Map<String, Int> = emptyMap()
    private var reverseVocabMap: Map<Int, String> = emptyMap()
    private var sortedVocabKeys: List<String> = emptyList()
    
    // Special tokens from config
    companion object {
        const val EOS_TOKEN = "</s>"
        const val UNK_TOKEN = "<unk>"
        const val PAD_TOKEN = "<pad>"
        const val EOS_TOKEN_ID = 0
        const val UNK_TOKEN_ID = 1
        const val PAD_TOKEN_ID = 39753
        const val DECODER_START_TOKEN_ID = 39753
        const val SENTENCEPIECE_SPACE = "▁"
    }
    
    init {
        try {
            loadVocab(context)
        } catch (e: Exception) {
            AppLog.put("Failed to initialize SentencePieceTokenizer: ${e.localizedMessage}", e)
        }
    }
    
    private fun loadVocab(context: Context) {
        try {
            AppLog.put("Reading vocab.json...")
            val vocabJson = context.assets.open("aimodel/vocab.json").bufferedReader().readText()
            AppLog.put("Parsing vocab.json (${vocabJson.length} chars)...")
            
            // Use Android JSONObject for fast parsing
            val jsonObject = JSONObject(vocabJson)
            val result = mutableMapOf<String, Int>()
            
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.getInt(key)
                result[key] = value
            }
            
            vocabMap = result
            reverseVocabMap = vocabMap.entries.associate { (k, v) -> v to k }
            AppLog.put("Vocabulary loaded: ${vocabMap.size} tokens")
        } catch (e: Exception) {
            AppLog.put("Failed to load vocabulary: ${e.localizedMessage}", e)
        }
    }
    
    /**
     * Encode Vietnamese text to token IDs using greedy longest-match tokenization
     */
    fun encode(text: String): IntArray {
        if (vocabMap.isEmpty()) {
            AppLog.put("Vocab not loaded")
            return intArrayOf()
        }
        
        return try {
            val tokens = greedyTokenize(text)
            val ids = tokens.map { token ->
                vocabMap[token] ?: UNK_TOKEN_ID
            }.toIntArray()
            
            AppLog.put("Encoded '$text' -> ${tokens.size} tokens, ids: ${ids.take(10).joinToString()}...")
            ids
        } catch (e: Exception) {
            AppLog.put("Encoding failed: ${e.localizedMessage}", e)
            intArrayOf()
        }
    }
    
    /**
     * Greedy longest-match tokenization similar to SentencePiece
     */
    private fun greedyTokenize(text: String): List<String> {
        if (sortedVocabKeys.isEmpty()) {
            // Sort vocab keys by length descending for greedy matching
            sortedVocabKeys = vocabMap.keys
                .filter { it.length > 1 && !it.startsWith("<") } // Exclude special tokens
                .sortedByDescending { it.length }
        }
        
        val tokens = mutableListOf<String>()
        // Add space prefix for SentencePiece compatibility
        val normalizedText = SENTENCEPIECE_SPACE + text.replace(" ", SENTENCEPIECE_SPACE)
        var remaining = normalizedText
        
        while (remaining.isNotEmpty()) {
            var matched = false
            
            // Try to find longest matching token
            for (vocabToken in sortedVocabKeys) {
                if (remaining.startsWith(vocabToken)) {
                    tokens.add(vocabToken)
                    remaining = remaining.substring(vocabToken.length)
                    matched = true
                    break
                }
            }
            
            // If no match, take single character as unknown
            if (!matched) {
                val char = remaining.first().toString()
                // Try to find single char in vocab
                if (vocabMap.containsKey(char)) {
                    tokens.add(char)
                } else {
                    tokens.add(UNK_TOKEN)
                }
                remaining = remaining.substring(1)
            }
        }
        
        return tokens
    }
    
    /**
     * Decode token IDs to Chinese text
     */
    fun decode(tokenIds: IntArray): String {
        if (reverseVocabMap.isEmpty()) {
            AppLog.put("Reverse vocab not initialized")
            return ""
        }
        
        return try {
            val tokens = tokenIds
                .filter { it != EOS_TOKEN_ID && it != PAD_TOKEN_ID } // Remove special tokens
                .map { id -> reverseVocabMap[id] ?: UNK_TOKEN }
            
            // Join tokens and handle SentencePiece spaces (▁ = word boundary)
            val text = tokens.joinToString("")
                .replace("▁", " ")
                .replace(SENTENCEPIECE_SPACE, " ")
                .trim()
            
            AppLog.put("Decoded ids: ${tokenIds.take(10).joinToString()}... -> '$text'")
            text
        } catch (e: Exception) {
            AppLog.put("Decoding failed: ${e.localizedMessage}", e)
            ""
        }
    }
    
    fun isReady(): Boolean {
        return vocabMap.isNotEmpty()
    }
    
    fun close() {
        vocabMap = emptyMap()
        reverseVocabMap = emptyMap()
        sortedVocabKeys = emptyList()
    }
}
