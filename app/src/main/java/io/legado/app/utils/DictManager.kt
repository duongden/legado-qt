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
            
            destFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Delete a custom dictionary file.
     */
    fun deleteCustomDict(type: DictType): Boolean {
        val file = getCustomDictFile(type)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
}
