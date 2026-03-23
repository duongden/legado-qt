package io.legado.app.utils

import io.legado.app.constant.AppLog
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.RssSource
import io.legado.app.R
import splitties.init.appCtx
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Encrypted image decryption tool
 */
object ImageUtils {

    /**
     * @param isCover Execute different decryption rules in book source based on this
     * @return Return Null if decryption failed. Do not process if decryption rule is empty
     */
    fun decode(
        src: String, bytes: ByteArray, isCover: Boolean,
        source: BaseSource?, book: Book? = null
    ): ByteArray? {
        val ruleJs = getRuleJs(source, isCover)
        if (ruleJs.isNullOrBlank()) return bytes
        //Decryption lib hutool.crypto ByteArray|InputStream -> ByteArray
        return kotlin.runCatching {
            source?.evalJS(ruleJs) {
                put("book", book)
                put("result", bytes)
                put("src", src)
            } as ByteArray
        }.onFailure {
            AppLog.putDebug("${src}${appCtx.getString(R.string.sc_decryption_error)}", it)
        }.getOrNull()
    }

    fun decode(
        src: String, inputStream: InputStream, isCover: Boolean,
        source: BaseSource?, book: Book? = null
    ): InputStream? {
        val ruleJs = getRuleJs(source, isCover)
        if (ruleJs.isNullOrBlank()) return inputStream
        //Decryption lib hutool.crypto ByteArray|InputStream -> ByteArray
        return kotlin.runCatching {
            val bytes = source?.evalJS(ruleJs) {
                put("book", book)
                put("result", inputStream)
                put("src", src)
            } as ByteArray
            ByteArrayInputStream(bytes)
        }.onFailure {
            AppLog.putDebug("${src}${appCtx.getString(R.string.sc_decryption_error)}", it)
        }.getOrNull()
    }

    fun skipDecode(source: BaseSource?, isCover: Boolean): Boolean {
        return getRuleJs(source, isCover).isNullOrBlank()
    }

    private fun getRuleJs(
        source: BaseSource?, isCover: Boolean
    ): String? {
        return when (source) {
            is BookSource ->
                if (isCover) source.coverDecodeJs
                else source.getContentRule().imageDecode

            is RssSource -> source.coverDecodeJs
            else -> null
        }
    }

}