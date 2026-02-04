package io.legado.app.utils

import android.content.Context
import android.net.Uri
import io.legado.app.help.storage.ImportOldData
import splitties.init.appCtx
import java.io.File
import java.io.FileOutputStream

object DictManager {

    private const val DICT_DIR = "translate/custom"
    
    // Define dictionary types and their filenames
    enum class DictType(val fileName: String) {
        NAMES("Names.txt"),
        VIETPHRASE("VietPhrase.txt"),
        PHIENAM("ChinesePhienAmWords.txt")
    }

    /**
     * Get the directory where custom dictionaries are stored.
     */
    fun getCustomDictDir(): File {
        val dir = File(appCtx.filesDir, DICT_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Get the file object for a specific dictionary type.
     */
    fun getCustomDictFile(type: DictType): File {
        return File(getCustomDictDir(), type.fileName)
    }

    /**
     * Check if a custom dictionary exists.
     */
    fun hasCustomDict(type: DictType): Boolean {
        return getCustomDictFile(type).exists()
    }

    /**
     * Import a dictionary file from a URI.
     * @param context Context
     * @param uri Source URI
     * @param type Dictionary Type
     * @return Boolean success
     */
    fun importDict(context: Context, uri: Uri, type: DictType): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return false
            val destFile = getCustomDictFile(type)
            
            // Basic validation: Check if file is text (optional, but good practice)
            // For now, simply copy.
            
            val result = filterAndCopyFile(inputStream, destFile)
            inputStream.close()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Filter and copy dictionary file.
     * Rules:
     * 1. Keep only the first meaning after splitting by "/"
     * 2. Remove lines that contain numbers AND chapter keywords (Quyển, Chương, Tiết, Hồi...)
     */
    private fun filterAndCopyFile(inputStream: java.io.InputStream, outputFile: File): Boolean {
        try {
            val reader = java.io.BufferedReader(java.io.InputStreamReader(inputStream))
            val writer = java.io.BufferedWriter(java.io.FileWriter(outputFile))

            val noiseRegex = Regex(".*\\d.*")
            val chapterKeywords = Regex(".*(Quyển|Chương|Tiết|Hồi|卷|回|章|幕|集|节).*")

            var line: String? = reader.readLine()
            while (line != null) {
                if (line.isNotBlank()) {
                    // Rule 2: Remove noise
                    if (line.matches(noiseRegex) && line.matches(chapterKeywords)) {
                        line = reader.readLine()
                        continue
                    }

                    // Rule 1: Filter meanings
                    val processedLine = if (line.contains("=")) {
                        val parts = line.split("=", limit = 2)
                        if (parts.size == 2) {
                            val key = parts[0]
                            val valParts = parts[1].split("/")
                            "$key=${valParts[0]}"
                        } else {
                            line
                        }
                    } else {
                        line
                    }

                    writer.write(processedLine)
                    writer.newLine()
                }
                line = reader.readLine()
            }

            writer.flush()
            writer.close()
            reader.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Delete a custom dictionary file.
     */
    fun deleteCustomDict(type: DictType): Boolean {
        val file = getCustomDictFile(type)
        // Also delete cache
        val cacheFile = File(appCtx.filesDir, "dict_cache/user_${type.fileName}.bin")
        if (cacheFile.exists()) cacheFile.delete()
        
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
