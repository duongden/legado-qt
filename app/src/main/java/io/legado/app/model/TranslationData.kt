package io.legado.app.model

/**
 * Translation data holder containing VietPhrase dictionaries
 */
data class TranslationData(
    val names: Trie,
    val vietPhrase: Trie,
    val chinesePhienAm: Map<String, String>
)

/**
 * Simple Trie structure for efficient dictionary lookups
 */
class Trie {
    private val root = TrieNode()
    
    class TrieNode {
        val children = mutableMapOf<Char, TrieNode>()
        var value: String? = null
        var isEndOfWord = false
    }
    
    /**
     * Insert a key-value pair into the trie
     */
    fun insert(key: String, value: String) {
        var  node = root
        for (char in key) {
            node = node.children.getOrPut(char) { TrieNode() }
        }
        node.value = value
        node.isEndOfWord = true
    }
    
    /**
     * Find the longest matching prefix in the text and return its translation
     * @param text The text to search in
     * @param startIndex Starting position in the text
     * @return Pair of (matched length, translation value) or null if no match
     */
    fun findLongestMatch(text: String, startIndex: Int): Pair<Int, String>? {
        var node = root
        var lastMatch: Pair<Int, String>? = null
        var currentIndex = startIndex
        
        while (currentIndex < text.length) {
            val char = text[currentIndex]
            node = node.children[char] ?: break
            
            if (node.isEndOfWord && node.value != null) {
                lastMatch = Pair(currentIndex - startIndex + 1, node.value!!)
            }
            
            currentIndex++
        }
        
        return lastMatch
    }
}
