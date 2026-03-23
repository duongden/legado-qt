package io.legado.app.model.dictionary

import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*

object DictionaryCompiler {

    private class BuildNode {
        var value: String? = null
        val children = TreeMap<Char, BuildNode>() // Sorted children
    }

    /**
     * Compile a text dictionary file (Key=Value) to a binary dictionary file.
     */
    fun compile(srcFile: File, destFile: File) {
        val root = BuildNode()
        
        // 1. Build Trie in Memory
        BufferedReader(InputStreamReader(FileInputStream(srcFile), StandardCharsets.UTF_8)).use { reader ->
            reader.forEachLine { line ->
                if (line.isNotBlank() && line.contains("=")) {
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        if (key.isNotEmpty() && value.isNotEmpty()) {
                            insert(root, key, value)
                        }
                    }
                }
            }
        }

        // 2. Flatten structure
        val nodes = ArrayList<FlatNode>()
        val stringPool = ByteArrayOutputStream()
        val stringOffsetMap = HashMap<String, Int>()

        // Add Root Node (Dummy char, will be index 0)
        nodes.add(FlatNode(0.toChar())) 

        val queue = LinkedList<Pair<BuildNode, Int>>() // Node -> Index in 'nodes'
        queue.add(root to 0)

        // BFS traversal ensuring children are contiguous
        while (queue.isNotEmpty()) {
            val (buildNode, flatIdx) = queue.poll()!!
            val flatNode = nodes[flatIdx]
            
            // Serialize Value
            if (buildNode.value != null) {
                if (!stringOffsetMap.containsKey(buildNode.value!!)) {
                    stringOffsetMap[buildNode.value!!] = stringPool.size()
                    writeString(stringPool, buildNode.value!!)
                }
                flatNode.valueOffset = stringOffsetMap[buildNode.value!!]!!
            }

            // Process Children
            if (buildNode.children.isNotEmpty()) {
                flatNode.childrenCount = buildNode.children.size
                flatNode.childrenOffset = nodes.size // Children start at end of current list
                
                for ((char, childNode) in buildNode.children) {
                    val childFlatIndex = nodes.size
                    nodes.add(FlatNode(char))
                    queue.add(childNode to childFlatIndex)
                }
            }
        }

        // 3. Write to File
        BufferedOutputStream(FileOutputStream(destFile)).use { out ->
            // Helper to write Int (LE)
            fun writeInt(v: Int) {
                out.write(v and 0xFF)
                out.write((v ushr 8) and 0xFF)
                out.write((v ushr 16) and 0xFF)
                out.write((v ushr 24) and 0xFF)
            }
            
            // Helper to write Short (LE)
            fun writeShort(v: Int) {
                out.write(v and 0xFF)
                out.write((v ushr 8) and 0xFF)
            }
            
            // Header
            writeInt(BinaryDictionary.MAGIC)
            writeInt(BinaryDictionary.VERSION)
            writeInt(nodes.size)
            writeInt(stringPool.size())

            // Node Table
            for (node in nodes) {
                out.write((node.char.code and 0xFF))     // Char Low
                out.write((node.char.code ushr 8) and 0xFF) // Char High (LE)
                writeShort(node.childrenCount)
                writeInt(node.childrenOffset)
                writeInt(node.valueOffset)
            }

            // String Pool
            out.write(stringPool.toByteArray())
        }
    }

    private fun insert(root: BuildNode, key: String, value: String) {
        var node = root
        for (char in key) {
            node = node.children.getOrPut(char) { BuildNode() }
        }
        node.value = value
    }

    private fun writeString(pool: ByteArrayOutputStream, str: String) {
        val bytes = str.toByteArray(StandardCharsets.UTF_8)
        pool.write((bytes.size and 0xFF))     // Low byte
        pool.write((bytes.size ushr 8) and 0xFF) // High byte (Little Endian Short)
        pool.write(bytes)
    }

    private class FlatNode(val char: Char) {
        var valueOffset: Int = -1
        var childrenCount: Int = 0
        var childrenOffset: Int = -1
    }
}
