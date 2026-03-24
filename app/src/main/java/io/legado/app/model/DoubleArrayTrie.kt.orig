package io.legado.app.model

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/**
 * Double-Array Trie implementation for memory-efficient dictionary storage
 * Based on the algorithm by Jun-ichi Aoe for compact trie representation
 */
class DoubleArrayTrie {
    
    // Double array structure
    private var base: IntArray = IntArray(0)
    private var check: IntArray = IntArray(0)
    private var valueIndex: IntArray = IntArray(0)
    private var values: Array<String> = emptyArray()
    
    // Character code mapping
    private val charMap = mutableMapOf<Char, Int>()
    private var maxCharValue = 0
    
    // Statistics
    private var size = 0

    private var used: BooleanArray = BooleanArray(0)
    private var nextCheckPos: Int = 0

    private companion object {
        private const val MAGIC = 0x44415452
        private const val VERSION = 2
    }
    
    /**
     * Build from key-value pairs
     */
    fun build(entries: List<Pair<String, String>>) {
        if (entries.isEmpty()) {
            build(emptyArray(), emptyArray())
            return
        }

        val sortedEntries = entries.sortedBy { it.first }
        val keys = Array(sortedEntries.size) { i -> sortedEntries[i].first }
        val values = Array(sortedEntries.size) { i -> sortedEntries[i].second }
        build(keys, values)
    }

    /**
     * Build from keys and values.
     * Keys must be sorted in lexicographical order.
     */
    fun build(keys: Array<String>, values: Array<String>) {
        if (keys.isEmpty()) {
            base = IntArray(1)
            check = IntArray(1)
            valueIndex = IntArray(1) { -1 }
            this.values = emptyArray()
            used = BooleanArray(1)
            size = 0
            return
        }

        size = keys.size

        buildCharMapping(keys)

        val initialSize = maxOf(1024, calculateInitialSize(keys))
        base = IntArray(initialSize)
        check = IntArray(initialSize)
        Arrays.fill(check, -1)
        valueIndex = IntArray(initialSize) { -1 }
        this.values = values
        used = BooleanArray(initialSize)
        nextCheckPos = 0

        val startTime = System.currentTimeMillis()
        check[1] = 0
        base[1] = 1
        val root = Node(code = 0, depth = 0, left = 0, right = keys.size)
        val siblings = fetch(root, keys)
        insert(siblings, 1, keys, values)
        val buildTime = System.currentTimeMillis() - startTime

        val compactStart = System.currentTimeMillis()
        compactArrays()
        val compactTime = System.currentTimeMillis() - compactStart
    }
    
    /**
     * Find longest matching prefix in text
     */
    fun findLongestMatch(text: String, startIndex: Int): Pair<Int, String>? {
        if (base.isEmpty() || startIndex >= text.length) return null
        
        var currentState = 1 // root state
        var lastMatch: Pair<Int, String>? = null
        var currentIndex = startIndex
        
        while (currentIndex < text.length) {
            val char = text[currentIndex]
            val charCode = charMap[char] ?: return lastMatch
            
            val nextState = base[currentState] + charCode
            
            if (nextState >= check.size || check[nextState] != currentState) {
                break
            }

            // Terminal transition (code = 0) stores the value for this prefix
            val termState = base[nextState] + 0
            if (termState < check.size && check[termState] == nextState) {
                val idx = valueIndex[termState]
                if (idx >= 0 && idx < values.size) {
                    lastMatch = Pair(currentIndex - startIndex + 1, values[idx])
                }
            }
            
            currentState = nextState
            currentIndex++
        }
        
        return lastMatch
    }
    
    /**
     * Save to binary format
     */
    fun save(output: OutputStream) {
        val bos = if (output is BufferedOutputStream) output else BufferedOutputStream(output, 1024 * 1024)
        DataOutputStream(bos).use { dos ->
            dos.writeInt(MAGIC)
            dos.writeInt(VERSION)
            dos.writeInt(size)
            dos.writeInt(base.size)
            dos.writeInt(charMap.size)
            dos.writeInt(maxCharValue)
            
            // Write char mapping
            charMap.forEach { (char, code) ->
                dos.writeInt(char.code)
                dos.writeInt(code)
            }
            
            // Write arrays (bulk)
            val n = base.size
            val baseBytes = ByteArray(n * 4)
            ByteBuffer.wrap(baseBytes).order(ByteOrder.BIG_ENDIAN).asIntBuffer().put(base)
            dos.write(baseBytes)

            val checkBytes = ByteArray(n * 4)
            ByteBuffer.wrap(checkBytes).order(ByteOrder.BIG_ENDIAN).asIntBuffer().put(check)
            dos.write(checkBytes)

            var valueCount = 0
            for (i in valueIndex.indices) {
                if (valueIndex[i] >= 0) valueCount++
            }
            dos.writeInt(valueCount)
            val startValues = System.currentTimeMillis()
            var written = 0
            val valueStep = maxOf(50_000, valueCount / 10)
            for (i in valueIndex.indices) {
                val idx = valueIndex[i]
                if (idx >= 0 && idx < values.size) {
                    dos.writeInt(i)
                    dos.writeUTF(values[idx])
                    written++
                }
            }

            dos.flush()
        }
    }
    
    /**
     * Load from binary format
     */
    fun load(input: InputStream) {
        val bis = if (input is BufferedInputStream) input else BufferedInputStream(input, 1024 * 1024)
        DataInputStream(bis).use { dis ->
            val first = dis.readInt()
            val hasHeader = first == MAGIC
            val version: Int

            if (hasHeader) {
                version = dis.readInt()
                if (version != VERSION) {
                    throw IllegalStateException("Unsupported DoubleArrayTrie binary version: $version")
                }
                size = dis.readInt()
            } else {
                // Backward compatible format (no header): first int was size
                version = 1
                size = first
            }

            val baseSize = dis.readInt()
            val charMapSize = dis.readInt()
            maxCharValue = dis.readInt()
            
            // Read char mapping
            charMap.clear()
            repeat(charMapSize) {
                val charCode = dis.readInt()
                val mappedCode = dis.readInt()
                charMap[Char(charCode)] = mappedCode
            }
            
            // Read arrays (bulk)
            base = IntArray(baseSize)
            check = IntArray(baseSize)
            valueIndex = IntArray(baseSize) { -1 }
            used = BooleanArray(baseSize)
            nextCheckPos = 0

            val baseBytes = ByteArray(baseSize * 4)
            dis.readFully(baseBytes)
            ByteBuffer.wrap(baseBytes).order(ByteOrder.BIG_ENDIAN).asIntBuffer().get(base)

            val checkBytes = ByteArray(baseSize * 4)
            dis.readFully(checkBytes)
            ByteBuffer.wrap(checkBytes).order(ByteOrder.BIG_ENDIAN).asIntBuffer().get(check)

            if (version == 1) {
                val tmpPairs = ArrayList<Pair<Int, String>>()
                repeat(baseSize) { i ->
                    val v = dis.readUTF()
                    if (v.isNotEmpty()) {
                        tmpPairs.add(Pair(i, v))
                    }
                }
                values = Array(tmpPairs.size) { idx -> tmpPairs[idx].second }
                for (i in tmpPairs.indices) {
                    valueIndex[tmpPairs[i].first] = i
                }
            } else {
                val valueCount = dis.readInt()
                val valuesArr = Array(valueCount) { "" }
                val valueStep = maxOf(50_000, valueCount / 10)
                val startValues = System.currentTimeMillis()
                for (i in 0 until valueCount) {
                    val pos = dis.readInt()
                    val value = dis.readUTF()
                    valuesArr[i] = value
                    if (pos in 0 until baseSize) {
                        valueIndex[pos] = i
                    }
                }
                values = valuesArr
            }
        }
    }
    
    /**
     * Get memory usage statistics
     */
    fun getMemoryStats(): Map<String, Any> {
        val totalMemory = (base.size + check.size + valueIndex.size) * 4 + values.size * 8 // approximate
        return mapOf(
            "entries" to size,
            "baseSize" to base.size,
            "checkSize" to check.size,
            "totalMemoryBytes" to totalMemory,
            "totalMemoryMB" to (totalMemory / (1024 * 1024.0))
        )
    }
    
    private fun buildCharMapping(entries: List<Pair<String, String>>) {
        buildCharMapping(Array(entries.size) { i -> entries[i].first })
    }

    private fun buildCharMapping(keys: Array<String>) {
        val uniqueChars = mutableSetOf<Char>()
        keys.forEach { key ->
            key.forEach { char ->
                uniqueChars.add(char)
            }
        }

        charMap.clear()
        var code = 1
        uniqueChars.sorted().forEach { char ->
            charMap[char] = code++
        }
        maxCharValue = code
    }
    
    private fun calculateInitialSize(entries: List<Pair<String, String>>): Int {
        return calculateInitialSize(Array(entries.size) { i -> entries[i].first })
    }

    private fun calculateInitialSize(keys: Array<String>): Int {
        val totalChars = keys.sumOf { it.length }
        // + keys.size for terminal nodes
        return (totalChars * 1.8).toInt() + keys.size + 2048
    }

    private class Node(
        val code: Int,
        val depth: Int,
        val left: Int,
        val right: Int
    )

    private fun fetch(parent: Node, keys: Array<String>): List<Node> {
        val siblings = ArrayList<Node>()
        var prevCode = -1
        var i = parent.left
        while (i < parent.right) {
            val key = keys[i]
            val code = if (parent.depth < key.length) {
                charMap[key[parent.depth]] ?: 0
            } else {
                0
            }
            if (code != prevCode) {
                siblings.add(Node(code = code, depth = parent.depth + 1, left = i, right = i + 1))
                prevCode = code
            } else {
                val last = siblings.last()
                siblings[siblings.size - 1] = Node(code = last.code, depth = last.depth, left = last.left, right = i + 1)
            }
            i++
        }
        return siblings
    }

    private fun insert(siblings: List<Node>, parentIndex: Int, keys: Array<String>, values: Array<String>) {
        if (siblings.isEmpty()) return

        var begin = 0
        var pos = maxOf(nextCheckPos, siblings[0].code + 1) - 1

        while (true) {
            pos++
            ensureCapacity(pos + 1)
            if (check[pos] != -1) continue

            begin = pos - siblings[0].code
            if (begin <= 0) continue
            if (used.size <= begin) ensureCapacity(begin + siblings.last().code + 1)
            if (begin + siblings.last().code >= check.size) {
                ensureCapacity(begin + siblings.last().code + 1)
            }
            if (used[begin]) continue

            var conflict = false
            for (s in siblings) {
                val idx = begin + s.code
                if (idx >= check.size) {
                    ensureCapacity(idx + 1)
                }
                if (check[idx] != -1) {
                    conflict = true
                    break
                }
            }
            if (!conflict) {
                break
            }
        }

        used[begin] = true
        base[parentIndex] = begin

        if (pos + 1 > nextCheckPos) {
            nextCheckPos = pos
        }

        for (s in siblings) {
            val idx = begin + s.code
            check[idx] = parentIndex
        }

        for (s in siblings) {
            val idx = begin + s.code
            if (s.code == 0) {
                // terminal
                valueIndex[idx] = s.left
                continue
            }
            val newSiblings = fetch(s, keys)
            if (newSiblings.isEmpty()) {
                // leaf without explicit terminal (should not happen because we add code=0)
                continue
            }
            insert(newSiblings, idx, keys, values)
        }
    }

    private fun ensureCapacity(minSize: Int) {
        if (minSize <= base.size) return
        var newSize = base.size
        while (newSize < minSize) {
            newSize *= 2
            if (newSize <= 0) {
                newSize = minSize
                break
            }
        }
        val oldSize = base.size
        base = base.copyOf(newSize)
        check = check.copyOf(newSize)
        valueIndex = valueIndex.copyOf(newSize)
        used = used.copyOf(newSize)
        Arrays.fill(check, oldSize, newSize, -1)
        java.util.Arrays.fill(valueIndex, oldSize, newSize, -1)
    }
    
    private fun compactArrays() {
        // Find actual used range
        var maxUsed = 0
        for (i in check.indices) {
            if (check[i] != -1) {
                maxUsed = i
            }
        }
        
        if (maxUsed < base.size - 1) {
            val compactSize = maxUsed + 1
            base = base.copyOf(compactSize)
            check = check.copyOf(compactSize)
            valueIndex = valueIndex.copyOf(compactSize)
            used = used.copyOf(compactSize)
        }
    }
}
