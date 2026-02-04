#!/usr/bin/env kotlin

import java.io.*
import java.nio.file.*

/**
 * Pre-build script to convert text dictionaries to DoubleArrayTrie binary format
 * Usage: kotlin DictionaryBuilder.kt <input_dir> <output_dir>
 */
@Suppress("UNCHECKED_CAST")
class DictionaryBuilder {
    
    data class TrieNode(
        val children: MutableMap<Char, TrieNode> = mutableMapOf(),
        var value: String? = null,
        var isEndOfWord: Boolean = false
    )
    
    class SimpleTrie {
        val root = TrieNode()
        
        fun insert(key: String, value: String) {
            var node = root
            for (char in key) {
                node = node.children.getOrPut(char) { TrieNode() }
            }
            node.value = value
            node.isEndOfWord = true
        }
        
        fun collectEntries(): List<Pair<String, String>> {
            val entries = mutableListOf<Pair<String, String>>()
            collectEntries(root, StringBuilder(), entries)
            return entries
        }
        
        private fun collectEntries(node: TrieNode, prefix: StringBuilder, entries: MutableList<Pair<String, String>>) {
            if (node.isEndOfWord && node.value != null) {
                entries.add(Pair(prefix.toString(), node.value!!))
            }
            
            node.children.forEach { (char, child) ->
                prefix.append(char)
                collectEntries(child, prefix, entries)
                prefix.deleteCharAt(prefix.length - 1)
            }
        }
    }
    
    class DoubleArrayTrieBuilder {
        data class BuildResult(
            val base: IntArray,
            val check: IntArray,
            val valueArray: Array<String?>,
            val charMap: Map<Char, Int>,
            val maxCharValue: Int
        )
        
        fun build(entries: List<Pair<String, String>>): BuildResult {
            if (entries.isEmpty()) {
                return BuildResult(
                    base = IntArray(1),
                    check = IntArray(1),
                    valueArray = emptyArray(),
                    charMap = emptyMap(),
                    maxCharValue = 0
                )
            }
            
            // Create character mapping
            val charMap = mutableMapOf<Char, Int>()
            val uniqueChars = mutableSetOf<Char>()
            entries.forEach { (key, _) ->
                key.forEach { char ->
                    uniqueChars.add(char)
                }
            }
            
            var code = 1
            uniqueChars.sorted().forEach { char ->
                charMap[char] = code++
            }
            val maxCharValue = code
            
            // Initialize arrays
            val totalChars = entries.sumOf { it.first.length }
            var baseSize = (totalChars * 1.5).toInt() + 1000
            var base = IntArray(baseSize)
            var check = IntArray(baseSize)
            var valueArray = arrayOfNulls<String>(baseSize)
            Arrays.fill(check, -1)
            
            // Build trie
            check[1] = 0 // root
            
            entries.forEach { (key, value) ->
                var currentState = 1
                
                for (i in key.indices) {
                    val char = key[i]
                    val charCode = charMap[char] ?: continue
                    
                    var nextState = base[currentState] + charCode
                    
                    // Expand if needed
                    while (nextState >= baseSize) {
                        val newSize = baseSize * 2
                        base = base.copyOf(newSize)
                        check = check.copyOf(newSize)
                        valueArray = valueArray.copyOf(newSize)
                        Arrays.fill(check, baseSize, check.size, -1)
                        baseSize = newSize
                    }
                    
                    if (check[nextState] == -1) {
                        // Empty slot
                        check[nextState] = currentState
                        base[nextState] = if (i == key.length - 1) {
                            valueArray[nextState] = value
                            0 // terminal
                        } else {
                            1 // default base for non-terminal
                        }
                    } else if (check[nextState] != currentState) {
                        // Simple collision handling - relocate
                        val newBase = findAvailableBase(currentState, charMap, check, baseSize)
                        base[currentState] = newBase
                        nextState = newBase + charCode
                        
                        if (nextState >= baseSize) {
                            val newSize = baseSize * 2
                            base = base.copyOf(newSize)
                            check = check.copyOf(newSize)
                            valueArray = valueArray.copyOf(newSize)
                            Arrays.fill(check, baseSize, check.size, -1)
                            baseSize = newSize
                        }
                        
                        check[nextState] = currentState
                        base[nextState] = if (i == key.length - 1) {
                            valueArray[nextState] = value
                            0
                        } else {
                            1
                        }
                    }
                    
                    currentState = nextState
                }
            }
            
            // Compact arrays
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
                valueArray = valueArray.copyOf(compactSize)
            }
            
            return BuildResult(base, check, valueArray, charMap, maxCharValue)
        }
        
        private fun findAvailableBase(
            state: Int,
            charMap: Map<Char, Int>,
            check: IntArray,
            size: Int
        ): Int {
            var baseValue = 1
            while (true) {
                var conflict = false
                
                for (charCode in charMap.values) {
                    val nextState = baseValue + charCode
                    if (nextState < size && check[nextState] != -1) {
                        conflict = true
                        break
                    }
                }
                
                if (!conflict) {
                    return baseValue
                }
                baseValue++
            }
        }
    }
    
    fun convertTextToBinary(inputFile: Path, outputFile: Path) {
        println("Converting ${inputFile.fileName} to ${outputFile.fileName}...")
        
        // Load text file
        val trie = SimpleTrie()
        val lines = Files.readAllLines(inputFile, Charsets.UTF_8)
        
        var entryCount = 0
        lines.forEach { line ->
            if (line.isNotBlank()) {
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    if (key.isNotEmpty() && value.isNotEmpty()) {
                        trie.insert(key, value)
                        entryCount++
                    }
                }
            }
        }
        
        println("Loaded $entryCount entries")
        
        // Build DoubleArrayTrie
        val builder = DoubleArrayTrieBuilder()
        val result = builder.build(trie.collectEntries())
        
        // Save binary format
        DataOutputStream(Files.newOutputStream(outputFile)).use { dos ->
            dos.writeInt(entryCount)
            dos.writeInt(result.base.size)
            dos.writeInt(result.charMap.size)
            dos.writeInt(result.maxCharValue)
            
            // Write char mapping
            result.charMap.forEach { (char, code) ->
                dos.writeInt(char.code)
                dos.writeInt(code)
            }
            
            // Write arrays
            result.base.forEach { dos.writeInt(it) }
            result.check.forEach { dos.writeInt(it) }
            result.valueArray.forEach { value ->
                dos.writeUTF(value ?: "")
            }
        }
        
        // Calculate memory usage
        val totalMemory = (result.base.size + result.check.size) * 4 + result.valueArray.size * 8
        println("Binary file created: ${outputFile.toFile().length()} bytes")
        println("Estimated memory usage: ${totalMemory / 1024} KB (${totalMemory / (1024 * 1024.0)} MB)")
        println("Compression ratio: ${(1.0 - outputFile.toFile().length().toDouble() / inputFile.toFile().length()) * 100}%")
    }
    
    fun processDirectory(inputDir: Path, outputDir: Path) {
        Files.createDirectories(outputDir)
        
        val dictionaryFiles = listOf(
            "Names.txt" to "Names.dat",
            "VietPhrase.txt" to "VietPhrase.dat"
        )
        
        dictionaryFiles.forEach { (inputFile, outputFile) ->
            val inputPath = inputDir.resolve(inputFile)
            val outputPath = outputDir.resolve(outputFile)
            
            if (Files.exists(inputPath)) {
                convertTextToBinary(inputPath, outputPath)
            } else {
                println("Warning: $inputFile not found in $inputDir")
            }
        }
    }
}

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Usage: kotlin DictionaryBuilder.kt <input_dir> <output_dir>")
        println("Example: kotlin DictionaryBuilder.kt ./assets/translate/vietphrase ./assets/translate/vietphrase")
        return
    }
    
    val inputDir = Paths.get(args[0])
    val outputDir = Paths.get(args[1])
    
    if (!Files.exists(inputDir)) {
        println("Error: Input directory $inputDir does not exist")
        return
    }
    
    val builder = DictionaryBuilder()
    builder.processDirectory(inputDir, outputDir)
    
    println("Dictionary conversion completed!")
}
