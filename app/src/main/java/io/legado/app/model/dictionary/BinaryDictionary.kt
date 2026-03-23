package io.legado.app.model.dictionary

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

/**
 * A Read-Only Dictionary that uses a memory-mapped structure.
 *
 * Structure:
 * - Header (16 bytes)
 *   - Magic "DICT" (4 bytes)
 *   - Version (4 bytes)
 *   - Node Count (4 bytes)
 *   - String Pool Size (4 bytes)
 * - Node Table (NodeCount * 12 bytes)
 *   - char code (2 bytes)
 *   - children count (2 bytes)
 *   - children offset (4 bytes) -> Index in Node Table
 *   - value offset (4 bytes) -> Offset in String Pool (-1 if none)
 * - String Pool (Blob)
 */
class BinaryDictionary(private val buffer: ByteBuffer) : AutoCloseable {

    companion object {
        const val MAGIC = 0x54434944 // "DICT" LE
        const val VERSION = 1
        const val NODE_SIZE = 12
        const val HEADER_SIZE = 16
    }

    init {
        // Enforce Little Endian
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        
        // Validate Header (MAGIC check agnostic of endianness reading first)
        // 0x54434944 ("DICT") is 44 49 43 54 in bytes (in file order)
        // We expect buffer to hold these bytes at 0-3.
        val b0 = buffer.get(0)
        val b1 = buffer.get(1)
        val b2 = buffer.get(2)
        val b3 = buffer.get(3)
        
        if (b0 != 0x44.toByte() || b1 != 0x49.toByte() || b2 != 0x43.toByte() || b3 != 0x54.toByte()) {
             // Simple log without formatting to avoid compilation issues
             throw IllegalArgumentException("Invalid dictionary format: Expected 'DICT'")
        }
    }

    private val version = buffer.getInt(4)
    private val nodeCount = buffer.getInt(8)
    private val stringPoolSize = buffer.getInt(12)
    private val nodeTableOffset = HEADER_SIZE
    private val stringPoolOffset = nodeTableOffset + (nodeCount * NODE_SIZE)

    init {
        // Log stats for debugging
        android.util.Log.d("BinaryDictionary", "Loaded dict: Nodes=$nodeCount, StringPool=${stringPoolSize}b, MagicOK")
    }

    /**
     * Find the longest prefix match for [text] starting at [startIndex].
     */
    fun findLongestMatch(text: String, startIndex: Int): Pair<Int, String>? {
        // android.util.Log.v("BinaryDictionary", "findLongestMatch: $startIndex / ${text.length}")
        var currentNode = 0 // Root node index (0)
        var lastMatch: Pair<Int, String>? = null
        
        // Root is virtual or entry 0.
        // Node 0 is ROOT.
        
        var loops = 0
        for (i in startIndex until text.length) {
            val charCode = text[i]
            
            // Find child with charCode
            val nextNode = findChild(currentNode, charCode)
            if (nextNode == -1) break
            
            currentNode = nextNode
            
            // Allow string extraction
            val valOffset = getValueOffset(currentNode)
            if (valOffset != -1) {
                val value = getString(valOffset)
                lastMatch = (i - startIndex + 1) to value
            }
            loops++
        }
        // if (loops > 100) android.util.Log.w("BinaryDictionary", "Deep traversal: $loops")
        
        return lastMatch
    }

    private fun findChild(nodeIdx: Int, char: Char): Int {
        val ptr = nodeTableOffset + (nodeIdx * NODE_SIZE)
        val childrenCount = buffer.getShort(ptr + 2).toInt() and 0xFFFF
        val childrenOffset = buffer.getInt(ptr + 4)
        
        if (childrenCount == 0) return -1
        
        // Binary Search
        var low = 0
        var high = childrenCount - 1
        
        while (low <= high) {
            val mid = (low + high) ushr 1
            val midNodeIdx = childrenOffset + mid
            val midChar = buffer.getChar(nodeTableOffset + (midNodeIdx * NODE_SIZE))
            
            if (midChar < char) {
                low = mid + 1
            } else if (midChar > char) {
                high = mid - 1
            } else {
                return midNodeIdx
            }
        }
        
        return -1
    }

    private fun getValueOffset(nodeIdx: Int): Int {
        val ptr = nodeTableOffset + (nodeIdx * NODE_SIZE)
        return buffer.getInt(ptr + 8)
    }

    private fun getString(offset: Int): String {
        val absStart = stringPoolOffset + offset
        val len = buffer.getShort(absStart).toInt() and 0xFFFF
        val bytes = ByteArray(len)
        val slice = buffer.duplicate()
        slice.order(ByteOrder.LITTLE_ENDIAN)
        slice.position(absStart + 2)
        slice.get(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }

    /**
     * Exact match lookup (acts like Map.get)
     */
    operator fun get(key: String): String? {
        val match = findLongestMatch(key, 0) ?: return null
        return if (match.first == key.length) match.second else null
    }

    override fun close() {
        // MappedByteBuffer is managed by GC, explicit unmap is unsafe in Java 8/Android without proprietary APIs.
        // We rely on GC.
    }
}
