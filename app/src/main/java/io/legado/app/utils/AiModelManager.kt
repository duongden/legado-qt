package io.legado.app.utils

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import splitties.init.appCtx
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Manager for AI translation models (ONNX).
 * Models are downloaded from GitHub and stored in app's files directory.
 */
object AiModelManager {

    // Branch where AI_MODEL/ folder is hosted
    private const val BASE_URL =
        "https://github.com/dat-bi/legado-qt/raw/main/AI_MODEL"

    // Model file names
    val MODEL_FILES = listOf(
        "encoder_model.onnx",
        "decoder_model.onnx",
        "decoder_with_past_model.onnx"
    )

    // Total approximate size in bytes for display (~250MB)
    const val TOTAL_SIZE_MB = 250

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Gets the directory where models should be stored
     */
    fun getModelDir(context: Context = appCtx): File {
        return File(context.filesDir, "aimodel").also { it.mkdirs() }
    }

    /**
     * Checks if all model files are downloaded
     */
    fun isModelReady(context: Context = appCtx): Boolean {
        val dir = getModelDir(context)
        return MODEL_FILES.all { File(dir, it).exists() && File(dir, it).length() > 0 }
    }

    /**
     * Returns download URL for a given model file name
     */
    fun getDownloadUrl(fileName: String): String = "$BASE_URL/$fileName"

    /**
     * Delete all downloaded model files
     */
    fun deleteModels(context: Context = appCtx): Boolean {
        val dir = getModelDir(context)
        return MODEL_FILES.all { fileName ->
            val file = File(dir, fileName)
            if (file.exists()) file.delete() else true
        }
    }

    /**
     * Gets total size of downloaded model files in bytes
     */
    fun getDownloadedSizeBytes(context: Context = appCtx): Long {
        val dir = getModelDir(context)
        return MODEL_FILES.sumOf { fileName ->
            File(dir, fileName).let { if (it.exists()) it.length() else 0L }
        }
    }

    /**
     * Downloads all model files with progress reporting.
     * [onProgress] is called with (currentFileIndex, totalFiles, filePercent 0-100, overallPercent 0-100)
     * [onFileStart] is called when a new file starts downloading
     * Returns true if all files downloaded successfully.
     */
    fun downloadModels(
        context: Context = appCtx,
        onFileStart: (fileName: String, index: Int) -> Unit = { _, _ -> },
        onProgress: (fileIndex: Int, totalFiles: Int, filePercent: Int, overallPercent: Int) -> Unit = { _, _, _, _ -> },
        shouldCancel: () -> Boolean = { false }
    ): Boolean {
        val dir = getModelDir(context)
        val total = MODEL_FILES.size

        for ((index, fileName) in MODEL_FILES.withIndex()) {
            if (shouldCancel()) return false

            val destFile = File(dir, fileName)
            // Skip if already downloaded completely
            if (destFile.exists() && destFile.length() > 0) {
                onFileStart(fileName, index)
                onProgress(index, total, 100, ((index + 1) * 100) / total)
                continue
            }

            onFileStart(fileName, index)

            val url = getDownloadUrl(fileName)
            val request = Request.Builder().url(url).build()

            try {
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    destFile.delete()
                    return false
                }

                val body = response.body ?: run {
                    destFile.delete()
                    return false
                }

                val contentLength = body.contentLength()
                val tempFile = File(dir, "$fileName.tmp")

                body.byteStream().use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead = 0L
                        var len: Int

                        while (input.read(buffer).also { len = it } != -1) {
                            if (shouldCancel()) {
                                tempFile.delete()
                                return false
                            }
                            output.write(buffer, 0, len)
                            bytesRead += len

                            val filePercent = if (contentLength > 0) {
                                ((bytesRead * 100) / contentLength).toInt()
                            } else {
                                -1
                            }
                            val overallPercent = ((index * 100 + filePercent.coerceAtLeast(0)) / total)
                            onProgress(index, total, filePercent, overallPercent)
                        }
                    }
                }

                // Rename temp file to final
                tempFile.renameTo(destFile)
                onProgress(index, total, 100, ((index + 1) * 100) / total)

            } catch (e: Exception) {
                destFile.delete()
                File(dir, "$fileName.tmp").delete()
                android.util.Log.e("AiModelManager", "Download failed: $fileName", e)
                return false
            }
        }

        return true
    }
}
