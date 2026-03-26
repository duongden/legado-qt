#!/usr/bin/env kotlin
/**
 * Pre-build .dat (DoubleArrayTrie binary) files from .txt dictionary files.
 * Run on a desktop machine with plenty of RAM.
 *
 * Usage: 
 *   cd legado-qt
 *   kotlinc -script scripts/prebuild_dat.main.kts
 *
 * Or with kotlin runner:
 *   kotlin scripts/prebuild_dat.main.kts
 */

import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays

// ── Inline DoubleArrayTrie (same logic as DoubleArrayTrie.kt but without Android/ITrieDictionary deps) ──

class DoubleArrayTrie {
    private var base = IntArray(0)
    private var check = IntArray(0)
    private var valueIndex = IntArray(0)
    private var used = BooleanArray(0)
    private var values = arrayOf<String>()
    private var size = 0
    private var nextCheckPos = 0
    private val charMap = HashMap<Char, Int>()
    private var maxCharValue = 0

    companion object {
        const val MAGIC = 0x44415432 // "DAT2"
        const val VERSION = 2
    }

    fun build(entries: List<Pair<String, String>>) {
        if (entries.isEmpty()) {
            base = IntArray(1)
            check = IntArray(1) { -1 }
            valueIndex = IntArray(1) { -1 }
            used = BooleanArray(1)
            values = emptyArray()
            size = 0
            return
        }

        val sorted = entries.sortedBy { it.first }
        val keys = Array(sorted.size) { sorted[it].first }
        val vals = Array(sorted.size) { sorted[it].second }

        size = keys.size
        values = vals

        buildCharMapping(keys)

        val initSize = calculateInitialSize(keys)
        base = IntArray(initSize)
        check = IntArray(initSize) { -1 }
        valueIndex = IntArray(initSize) { -1 }
        used = BooleanArray(initSize)
        nextCheckPos = 0

        val root = Node(code = 0, depth = 0, left = 0, right = keys.size)
        val siblings = fetch(root, keys)
        insert(siblings, 0, keys, vals)

        compactArrays()
        println("  Built trie: ${keys.size} entries, base size: ${base.size}")
    }

    fun save(output: OutputStream) {
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

            val buf = ByteBuffer.allocate(base.size * 4).order(ByteOrder.BIG_ENDIAN)
            buf.asIntBuffer().put(base)
            dos.write(buf.array())

            buf.clear()
            buf.asIntBuffer().put(check)
            dos.write(buf.array())

            val valuePairs = mutableListOf<Pair<Int, String>>()
            for (i in valueIndex.indices) {
                if (valueIndex[i] >= 0 && valueIndex[i] < values.size) {
                    valuePairs.add(i to values[valueIndex[i]])
                }
            }
            dos.writeInt(valuePairs.size)
            for ((pos, value) in valuePairs) {
                dos.writeInt(pos)
                dos.writeUTF(value)
            }
        }
    }

    private fun buildCharMapping(keys: Array<String>) {
        val uniqueChars = mutableSetOf<Char>()
        keys.forEach { key -> key.forEach { uniqueChars.add(it) } }
        charMap.clear()
        var code = 1
        uniqueChars.sorted().forEach { charMap[it] = code++ }
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
                siblings[siblings.size - 1] = Node(last.code, last.depth, last.left, i + 1)
            }
            i++
        }
        return siblings
    }

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

        for (s in siblings) { check[begin + s.code] = parentIndex }
        for (s in siblings) {
            val idx = begin + s.code
            if (s.code == 0) { valueIndex[idx] = s.left; continue }
            val newSiblings = fetch(s, keys)
            if (newSiblings.isEmpty()) continue
            insert(newSiblings, idx, keys, values)
        }
    }

    private fun ensureCapacity(minSize: Int) {
        if (minSize <= base.size) return
        var newSize = base.size
        while (newSize < minSize) { newSize *= 2; if (newSize <= 0) { newSize = minSize; break } }
        val oldSize = base.size
        base = base.copyOf(newSize)
        check = check.copyOf(newSize)
        valueIndex = valueIndex.copyOf(newSize)
        used = used.copyOf(newSize)
        Arrays.fill(check, oldSize, newSize, -1)
        Arrays.fill(valueIndex, oldSize, newSize, -1)
    }

    private fun compactArrays() {
        var maxUsed = 0
        for (i in check.indices) { if (check[i] != -1) maxUsed = i }
        if (maxUsed < base.size - 1) {
            val s = maxUsed + 1
            base = base.copyOf(s); check = check.copyOf(s); valueIndex = valueIndex.copyOf(s); used = used.copyOf(s)
        }
    }
}

// ── Main ──

fun loadEntriesFromText(file: File): List<Pair<String, String>> {
    val entries = mutableListOf<Pair<String, String>>()
    var total = 0
    BufferedReader(InputStreamReader(FileInputStream(file), Charsets.UTF_8), 32768).use { reader ->
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            total++
            val l = line!!
            if (l.isNotBlank() && l.contains('=')) {
                val clean = l.replace("\u0000", "").replace("\u0001", "").replace("\u0004", "").trim()
                if (clean.isNotEmpty() && clean.contains("=")) {
                    val parts = clean.split("=", limit = 2)
                    if (parts.size == 2) {
                        val k = parts[0].trim(); val v = parts[1].trim()
                        if (k.isNotEmpty() && v.isNotEmpty()) entries.add(k to v)
                    }
                }
            }
            if (total % 50000 == 0) println("  Read $total lines...")
        }
    }
    println("  Total lines: $total, valid entries: ${entries.size}")
    return entries
}

val assetsDir = File("app/src/main/assets/translate/vietphrase")

val dictFiles = listOf(
    "Names" to "Names",
    "VietPhrase" to "VietPhrase"
)

for ((txtName, datName) in dictFiles) {
    val txtFile = File(assetsDir, "$txtName.txt")
    val datFile = File(assetsDir, "$datName.dat")
    
    if (!txtFile.exists()) {
        println("ERROR: $txtFile not found!")
        continue
    }
    
    println("Building $datName.dat from $txtName.txt...")
    val entries = loadEntriesFromText(txtFile)
    
    val trie = DoubleArrayTrie()
    trie.build(entries)
    
    FileOutputStream(datFile).use { fos ->
        BufferedOutputStream(fos, 1024 * 1024).use { bos ->
            trie.save(bos)
        }
    }
    println("Saved $datFile (${datFile.length() / 1024}KB)")
    println()
}

println("Done! Pre-built .dat files are in $assetsDir")
