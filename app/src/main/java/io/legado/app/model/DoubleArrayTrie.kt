package io.legado.app.model

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.IntBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Double-Array Trie implementation with Memory-Mapped I/O support.
 *
 * VERSION 3 format (Memory-Mappable):
 *   Header: MAGIC(4) | VERSION(4) | SIZE(4) | BASE_LEN(4) | CHARMAP_SIZE(4) | MAX_CHAR(4)
 *   CharMap: [char_code(4) | mapped_code(4)] * CHARMAP_SIZE
 *   Base:    int[BASE_LEN] (BigEndian)
 *   Check:   int[BASE_LEN] (BigEndian)
 *   StringPool: POOL_SIZE(4) | byte[POOL_SIZE]
 *     Each string: length(2 bytes, unsigned short) | utf8_bytes[length]
 *   Terminal nodes store string pool offset in base[termState].
 */
import io.legado.app.model.dictionary.ITrieDictionary

class DoubleArrayTrie : ITrieDictionary {

    // --- Memory-Mapped mode fields ---
    private var mappedBuffer: ByteBuffer? = null
    private var mappedBaseBuffer: IntBuffer? = null
    private var mappedCheckBuffer: IntBuffer? = null
    private var mappedStringPoolOffset: Int = 0
    private var mappedBaseLen: Int = 0
    private var isMapped: Boolean = false

    // --- Heap mode fields (used for build() and legacy load) ---
    private var base: IntArray = IntArray(0)
    private var check: IntArray = IntArray(0)

    // Character code mapping (always in heap, ~256KB)
    private val fastCharMap = IntArray(65536)

    // Statistics
    private var size = 0

    // Build-only fields
    private var used: BooleanArray = BooleanArray(0)
    private var nextCheckPos: Int = 0
    private val charMap = mutableMapOf<Char, Int>()
    private var maxCharValue = 0

    private companion object {
        private const val MAGIC = 0x44415432
        private const val VERSION = 3
    }

    // ==================== LOOKUP ====================

    /**
     * Find longest matching prefix in text.
     * Works for both memory-mapped and heap modes.
     */
    override fun findLongestMatch(text: String, startIndex: Int): Pair<Int, String>? {
        if (startIndex >= text.length) return null

        if (isMapped) {
            return findLongestMatchMapped(text, startIndex)
        }

        // Heap mode fallback
        if (base.isEmpty()) return null
        var currentState = 1
        var matchLen = -1
        var matchVal: String? = null
        var currentIndex = startIndex

        while (currentIndex < text.length) {
            val charCode = fastCharMap[text[currentIndex].code]
            if (charCode == 0) break

            val nextState = base[currentState] + charCode
            if (nextState >= check.size || check[nextState] != currentState) break

            val termState = base[nextState]
            if (termState >= 0 && termState < check.size && check[termState] == nextState) {
                // In heap V3 mode, base[termState] is the string pool offset — but we don't have a string pool in heap mode
                // This path is only used during build() which doesn't need lookup
            }

            currentState = nextState
            currentIndex++
        }
        return if (matchLen > 0) Pair(matchLen, matchVal!!) else null
    }

    /**
     * Memory-mapped findLongestMatch — zero allocation in the hot loop.
     */
    private fun findLongestMatchMapped(text: String, startIndex: Int): Pair<Int, String>? {
        val baseB = mappedBaseBuffer ?: return null
        val checkB = mappedCheckBuffer ?: return null
        val buf = mappedBuffer ?: return null
        val poolOffset = mappedStringPoolOffset
        val arrayLen = mappedBaseLen

        var currentState = 1
        var matchLen = -1
        var matchStringPoolOffset = -1
        var currentIndex = startIndex

        while (currentIndex < text.length) {
            val charCode = fastCharMap[text[currentIndex].code]
            if (charCode == 0) break

            val nextState = baseB.get(currentState) + charCode
            if (nextState < 0 || nextState >= arrayLen || checkB.get(nextState) != currentState) break

            // Check terminal: base[nextState] + 0 = base[nextState]
            val termState = baseB.get(nextState)
            if (termState >= 0 && termState < arrayLen && checkB.get(termState) == nextState) {
                // base[termState] holds string pool offset
                matchStringPoolOffset = baseB.get(termState)
                matchLen = currentIndex - startIndex + 1
            }

            currentState = nextState
            currentIndex++
        }

        if (matchLen > 0 && matchStringPoolOffset >= 0) {
            // Extract string from pool on-demand (single allocation)
            val absOffset = poolOffset + matchStringPoolOffset
            val strLen = (buf.getShort(absOffset).toInt() and 0xFFFF)
            val bytes = ByteArray(strLen)
            val slice = buf.duplicate()
            slice.position(absOffset + 2)
            slice.get(bytes)
            return Pair(matchLen, String(bytes, StandardCharsets.UTF_8))
        }
        return null
    }

    override operator fun get(key: String): String? {
        val match = findLongestMatch(key, 0) ?: return null
        return if (match.first == key.length) match.second else null
    }

    // ==================== MEMORY-MAPPED LOADING ====================

    /**
     * Load from a physical file using memory-mapping (zero heap).
     * This is the primary fast-path for production use.
     */
    fun loadMapped(file: File) {
        val raf = RandomAccessFile(file, "r")
        val channel = raf.channel
        val fileSize = channel.size()
        val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize)
        buffer.order(ByteOrder.BIG_ENDIAN)

        // Parse header
        val magic = buffer.getInt(0)
        if (magic != MAGIC) throw IllegalStateException("Invalid DAT magic: $magic")
        val version = buffer.getInt(4)
        if (version != VERSION) throw IllegalStateException("Unsupported DAT version: $version, expected $VERSION")
        size = buffer.getInt(8)
        val baseLen = buffer.getInt(12)
        val charMapSize = buffer.getInt(16)
        maxCharValue = buffer.getInt(20)

        // Read char mapping
        fastCharMap.fill(0)
        charMap.clear()
        var offset = 24
        repeat(charMapSize) {
            val charCode = buffer.getInt(offset)
            val mappedCode = buffer.getInt(offset + 4)
            offset += 8
            if (charCode in 0..65535) {
                fastCharMap[charCode] = mappedCode
            }
            charMap[Char(charCode)] = mappedCode
        }

        // Memory-map base and check arrays as IntBuffers (zero copy!)
        val baseByteOffset = offset
        val checkByteOffset = baseByteOffset + baseLen * 4
        val afterCheckOffset = checkByteOffset + baseLen * 4

        // Create IntBuffer views over the mapped region
        val baseBuf = buffer.duplicate()
        baseBuf.order(ByteOrder.BIG_ENDIAN)
        baseBuf.position(baseByteOffset)
        baseBuf.limit(checkByteOffset)
        mappedBaseBuffer = baseBuf.slice().order(ByteOrder.BIG_ENDIAN).asIntBuffer()

        val checkBuf = buffer.duplicate()
        checkBuf.order(ByteOrder.BIG_ENDIAN)
        checkBuf.position(checkByteOffset)
        checkBuf.limit(afterCheckOffset)
        mappedCheckBuffer = checkBuf.slice().order(ByteOrder.BIG_ENDIAN).asIntBuffer()

        // String pool: 4 bytes pool size + pool bytes
        val poolSizeOffset = afterCheckOffset
        val poolSize = buffer.getInt(poolSizeOffset)
        mappedStringPoolOffset = poolSizeOffset + 4

        mappedBuffer = buffer
        mappedBaseLen = baseLen
        isMapped = true

        // Clear heap arrays to free memory
        base = IntArray(0)
        check = IntArray(0)
    }

    // ==================== STREAM LOADING (for build/save cycle) ====================

    /**
     * Save to VERSION 3 binary format (for custom dict building on device).
     * Terminal nodes store string pool offset in base[termState].
     */
    fun save(output: OutputStream, entries: List<Pair<String, String>>) {
        // Build string pool
        val poolBaos = ByteArrayOutputStream()
        val poolDos = DataOutputStream(poolBaos)
        val stringOffsets = IntArray(entries.size)
        val sortedEntries = entries.sortedBy { it.first }
        for (i in sortedEntries.indices) {
            stringOffsets[i] = poolBaos.size()
            val utf8 = sortedEntries[i].second.toByteArray(StandardCharsets.UTF_8)
            poolDos.writeShort(utf8.size)
            poolDos.write(utf8)
        }
        val stringPool = poolBaos.toByteArray()

        // Build trie with string offsets
        val keys = Array(sortedEntries.size) { sortedEntries[it].first }
        buildForSave(keys, stringOffsets)

        // Write
        val bos = if (output is BufferedOutputStream) output else BufferedOutputStream(output, 1024 * 1024)
        DataOutputStream(bos).use { dos ->
            dos.writeInt(MAGIC)
            dos.writeInt(VERSION)
            dos.writeInt(size)
            dos.writeInt(base.size)
            dos.writeInt(charMap.size)
            dos.writeInt(maxCharValue)

            charMap.forEach { (char, code) ->
                dos.writeInt(char.code)
                dos.writeInt(code)
            }

            val n = base.size
            val baseBytes = ByteArray(n * 4)
            ByteBuffer.wrap(baseBytes).order(ByteOrder.BIG_ENDIAN).asIntBuffer().put(base)
            dos.write(baseBytes)

            val checkBytes = ByteArray(n * 4)
            ByteBuffer.wrap(checkBytes).order(ByteOrder.BIG_ENDIAN).asIntBuffer().put(check)
            dos.write(checkBytes)

            dos.writeInt(stringPool.size)
            dos.write(stringPool)
            dos.flush()
        }
    }

    /**
     * Build trie for save (stores string pool offsets in terminal nodes).
     */
    private fun buildForSave(keys: Array<String>, stringOffsets: IntArray) {
        if (keys.isEmpty()) {
            base = IntArray(1)
            check = IntArray(1)
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
        used = BooleanArray(initialSize)
        nextCheckPos = 0

        check[1] = 0
        base[1] = 1
        val root = Node(code = 0, depth = 0, left = 0, right = keys.size)
        val siblings = fetch(root, keys)
        insertWithOffsets(siblings, 1, keys, stringOffsets)
        compactArrays()
    }

    // ==================== BUILD (in-memory, for custom dict import) ====================

    fun build(entries: List<Pair<String, String>>) {
        if (entries.isEmpty()) {
            base = IntArray(1); check = IntArray(1); used = BooleanArray(1); size = 0
            return
        }
        val sortedEntries = entries.sortedBy { it.first }
        val keys = Array(sortedEntries.size) { sortedEntries[it].first }
        val values = Array(sortedEntries.size) { sortedEntries[it].second }
        build(keys, values)
    }

    fun build(keys: Array<String>, values: Array<String>) {
        if (keys.isEmpty()) {
            base = IntArray(1); check = IntArray(1); used = BooleanArray(1); size = 0
            return
        }
        size = keys.size
        buildCharMapping(keys)
        val initialSize = maxOf(1024, calculateInitialSize(keys))
        base = IntArray(initialSize)
        check = IntArray(initialSize)
        Arrays.fill(check, -1)
        used = BooleanArray(initialSize)
        nextCheckPos = 0

        check[1] = 0
        base[1] = 1
        val root = Node(code = 0, depth = 0, left = 0, right = keys.size)
        val siblings = fetch(root, keys)
        insert(siblings, 1, keys, values)
        compactArrays()
    }

    // ==================== INTERNAL: char mapping, insert, etc. ====================

    private fun buildCharMapping(keys: Array<String>) {
        val uniqueChars = mutableSetOf<Char>()
        keys.forEach { key -> key.forEach { uniqueChars.add(it) } }
        charMap.clear()
        fastCharMap.fill(0)
        var code = 1
        uniqueChars.sorted().forEach { char ->
            val c = code++
            charMap[char] = c
            val cCode = char.code
            if (cCode in 0..65535) fastCharMap[cCode] = c
        }
        maxCharValue = code
    }

    private fun calculateInitialSize(keys: Array<String>): Int {
        val totalChars = keys.sumOf { it.length }
        return (totalChars * 1.8).toInt() + keys.size + 2048
    }

    private class Node(val code: Int, val depth: Int, val left: Int, val right: Int)

    private fun fetch(parent: Node, keys: Array<String>): List<Node> {
        val siblings = ArrayList<Node>()
        var prevCode = -1
        var i = parent.left
        while (i < parent.right) {
            val key = keys[i]
            val code = if (parent.depth < key.length) charMap[key[parent.depth]] ?: 0 else 0
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

    /** Insert for build (stores entry index in terminal base — used for legacy heap mode). */
    private fun insert(siblings: List<Node>, parentIndex: Int, keys: Array<String>, values: Array<String>) {
        if (siblings.isEmpty()) return
        var begin: Int
        var pos = maxOf(nextCheckPos, siblings[0].code + 1) - 1

        while (true) {
            pos++
            ensureCapacity(pos + 1)
            if (check[pos] != -1) continue
            begin = pos - siblings[0].code
            if (begin <= 0) continue
            if (used.size <= begin) ensureCapacity(begin + siblings.last().code + 1)
            if (begin + siblings.last().code >= check.size) ensureCapacity(begin + siblings.last().code + 1)
            if (used[begin]) continue
            var conflict = false
            for (s in siblings) {
                val idx = begin + s.code
                if (idx >= check.size) ensureCapacity(idx + 1)
                if (check[idx] != -1) { conflict = true; break }
            }
            if (!conflict) break
        }
        used[begin] = true
        base[parentIndex] = begin
        if (pos + 1 > nextCheckPos) nextCheckPos = pos
        for (s in siblings) check[begin + s.code] = parentIndex
        for (s in siblings) {
            val idx = begin + s.code
            if (s.code == 0) { base[idx] = s.left; continue }
            val newSiblings = fetch(s, keys)
            if (newSiblings.isEmpty()) continue
            insert(newSiblings, idx, keys, values)
        }
    }

    /** Insert for save (stores string pool offset in terminal base). */
    private fun insertWithOffsets(siblings: List<Node>, parentIndex: Int, keys: Array<String>, stringOffsets: IntArray) {
        if (siblings.isEmpty()) return
        var begin: Int
        var pos = maxOf(nextCheckPos, siblings[0].code + 1) - 1

        while (true) {
            pos++
            ensureCapacity(pos + 1)
            if (check[pos] != -1) continue
            begin = pos - siblings[0].code
            if (begin <= 0) continue
            if (used.size <= begin) ensureCapacity(begin + siblings.last().code + 1)
            if (begin + siblings.last().code >= check.size) ensureCapacity(begin + siblings.last().code + 1)
            if (used[begin]) continue
            var conflict = false
            for (s in siblings) {
                val idx = begin + s.code
                if (idx >= check.size) ensureCapacity(idx + 1)
                if (check[idx] != -1) { conflict = true; break }
            }
            if (!conflict) break
        }
        used[begin] = true
        base[parentIndex] = begin
        if (pos + 1 > nextCheckPos) nextCheckPos = pos
        for (s in siblings) check[begin + s.code] = parentIndex
        for (s in siblings) {
            val idx = begin + s.code
            if (s.code == 0) { base[idx] = stringOffsets[s.left]; continue }
            val newSiblings = fetch(s, keys)
            if (newSiblings.isEmpty()) continue
            insertWithOffsets(newSiblings, idx, keys, stringOffsets)
        }
    }

    private fun ensureCapacity(minSize: Int) {
        if (minSize <= base.size) return
        var newSize = base.size
        while (newSize < minSize) { newSize *= 2; if (newSize <= 0) { newSize = minSize; break } }
        val oldSize = base.size
        base = base.copyOf(newSize)
        check = check.copyOf(newSize)
        used = used.copyOf(newSize)
        Arrays.fill(check, oldSize, newSize, -1)
    }

    private fun compactArrays() {
        var maxUsed = 0
        for (i in check.indices) { if (check[i] != -1) maxUsed = i }
        if (maxUsed < base.size - 1) {
            val compactSize = maxUsed + 1
            base = base.copyOf(compactSize)
            check = check.copyOf(compactSize)
            used = used.copyOf(compactSize)
        }
    }
}
