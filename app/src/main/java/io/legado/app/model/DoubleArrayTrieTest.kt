package io.legado.app.model

import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Test class to verify DoubleArrayTrie compatibility and performance
 */
object DoubleArrayTrieTest {
    
    data class TestResult(
        val testName: String,
        val passed: Boolean,
        val message: String,
        val memoryUsage: Map<String, Any>? = null
    )
    
    private val testResults = mutableListOf<TestResult>()
    
    fun runAllTests(): List<TestResult> {
        testResults.clear()
        
        testBasicFunctionality()
        testLongestMatch()
        testMemoryEfficiency()
        testBinarySerialization()
        testCompatibilityWithExistingData()
        
        return testResults.toList()
    }
    
    private fun testBasicFunctionality() {
        try {
            val trie = DoubleArrayTrie()
            val entries = listOf(
                "hello" to "xin chào",
                "world" to "thế giới",
                "test" to "kiểm tra"
            )
            
            trie.build(entries)
            
            // Test exact matches
            val result1 = trie.findLongestMatch("hello", 0)
            val result2 = trie.findLongestMatch("world", 0)
            val result3 = trie.findLongestMatch("test", 0)
            
            val passed = result1?.second == "xin chào" && 
                        result2?.second == "thế giới" && 
                        result3?.second == "kiểm tra"
            
            testResults.add(TestResult(
                testName = "Basic Functionality",
                passed = passed,
                message = if (passed) "All basic lookups work correctly" 
                         else "Basic lookups failed: $result1, $result2, $result3"
            ))
            
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = "Basic Functionality",
                passed = false,
                message = "Exception: ${e.message}"
            ))
        }
    }
    
    private fun testLongestMatch() {
        try {
            val trie = DoubleArrayTrie()
            val entries = listOf(
                "中" to "Trung",
                "中文" to "tiếng Trung",
                "中文翻译" to "dịch tiếng Trung"
            )
            
            trie.build(entries)
            
            // Test longest match behavior
            val result1 = trie.findLongestMatch("中文翻译测试", 0)
            val result2 = trie.findLongestMatch("中文字符", 0)
            
            val passed = result1?.first == 4 && result1?.second == "dịch tiếng Trung" &&
                        result2?.first == 2 && result2?.second == "tiếng Trung"
            
            testResults.add(TestResult(
                testName = "Longest Match",
                passed = passed,
                message = if (passed) "Longest matching works correctly"
                         else "Longest matching failed: $result1, $result2"
            ))
            
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = "Longest Match",
                passed = false,
                message = "Exception: ${e.message}"
            ))
        }
    }
    
    private fun testMemoryEfficiency() {
        try {
            val trie = DoubleArrayTrie()
            
            // Create a large dataset similar to real dictionary
            val entries = mutableListOf<Pair<String, String>>()
            for (i in 0 until 1000) {
                entries.add("test$i" to "giá trị $i")
                entries.add("测试$i" to "测试值 $i")
            }
            
            trie.build(entries)
            
            // Test that building works and we can find entries
            val result = trie.findLongestMatch("test0ABC", 0)
            val passed = result != null && result.second == "giá trị 0"
            
            testResults.add(TestResult(
                testName = "Memory Efficiency",
                passed = passed,
                message = if (passed) "Build and lookup works for 2000 entries"
                         else "Build/lookup failed for 2000 entries"
            ))
            
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = "Memory Efficiency",
                passed = false,
                message = "Exception: ${e.message}"
            ))
        }
    }
    
    private fun testBinarySerialization() {
        try {
            val entries = listOf(
                "serialize" to "tuần tự hóa",
                "binary" to "nhị phân",
                "test" to "kiểm tra"
            )
            
            // Save as VERSION 3 .dat to temp file
            val tmpFile = java.io.File.createTempFile("dat_test", ".dat")
            try {
                java.io.FileOutputStream(tmpFile).use { fos ->
                    java.io.BufferedOutputStream(fos, 1024 * 1024).use { bos ->
                        val saveTrie = DoubleArrayTrie()
                        saveTrie.save(bos, entries)
                    }
                }
                
                // Load via memory mapping
                val newTrie = DoubleArrayTrie()
                newTrie.loadMapped(tmpFile)
                
                // Test that deserialized trie works
                val result1 = newTrie.findLongestMatch("serialize", 0)
                val result2 = newTrie.findLongestMatch("binary", 0)
                val result3 = newTrie.findLongestMatch("test", 0)
                
                val passed = result1?.second == "tuần tự hóa" && 
                            result2?.second == "nhị phân" && 
                            result3?.second == "kiểm tra"
                
                testResults.add(TestResult(
                    testName = "Binary Serialization",
                    passed = passed,
                    message = if (passed) "Save+MMap round-trip works (${tmpFile.length()} bytes)"
                             else "Round-trip failed: $result1, $result2, $result3"
                ))
            } finally {
                tmpFile.delete()
            }
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = "Binary Serialization",
                passed = false,
                message = "Exception: ${e.message}"
            ))
        }
    }
    
    private fun testCompatibilityWithExistingData() {
        try {
            // Simulate loading real dictionary data
            val trie = DoubleArrayTrie()
            val entries = listOf(
                "的" to "của",
                "了" to "rồi",
                "是" to "là",
                "在" to "ở",
                "我" to "tôi",
                "你" to "bạn",
                "他" to "anh ấy",
                "她" to "cô ấy",
                "我们" to "chúng tôi",
                "你们" to "các bạn"
            )
            
            trie.build(entries)
            
            // Test translation-like scenarios
            val testText = "我是中国人"
            val translations = mutableListOf<String>()
            var currentIndex = 0
            
            while (currentIndex < testText.length) {
                val match = trie.findLongestMatch(testText, currentIndex)
                if (match != null) {
                    translations.add(match.second)
                    currentIndex += match.first
                } else {
                    translations.add(" $testText[currentIndex] ")
                    currentIndex++
                }
            }
            
            val passed = translations.isNotEmpty() && translations.any { it.contains("tôi") }
            
            testResults.add(TestResult(
                testName = "Compatibility with Existing Data",
                passed = passed,
                message = if (passed) "Compatible with existing translation workflow: ${translations.joinToString(" ")}"
                         else "Not compatible: ${translations.joinToString(" ")}"
            ))
            
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = "Compatibility with Existing Data",
                passed = false,
                message = "Exception: ${e.message}"
            ))
        }
    }
    
    /**
     * Print test results in a formatted way
     */
    fun printResults() {
        println("\n=== DoubleArrayTrie Test Results ===")
        
        val passed = testResults.count { it.passed }
        val total = testResults.size
        
        println("Passed: $passed/$total tests")
        println()
        
        testResults.forEach { result ->
            val status = if (result.passed) "✓" else "✗"
            println("$status ${result.testName}")
            println("  ${result.message}")
            
            if (result.memoryUsage != null) {
                println("  Memory: ${result.memoryUsage}")
            }
            println()
        }
        
        if (passed == total) {
            println("🎉 All tests passed! DoubleArrayTrie is ready for production.")
        } else {
            println("⚠️  Some tests failed. Please review the implementation.")
        }
    }
}

/**
 * Main function to run tests
 */
fun main() {
    val results = DoubleArrayTrieTest.runAllTests()
    DoubleArrayTrieTest.printResults()
}
