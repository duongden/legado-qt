package io.legado.app.constant

import androidx.annotation.IntDef

/**
 * Start from 8 to distinguish from old version types
 */
@Suppress("ConstPropertyName")
object BookType {
    /**
     * 8 Text
     */
    const val text = 0b1000

    /**
     * 16 Update failed
     */
    const val updateError = 0b10000

    /**
     * 32 Audio
     */
    const val audio = 0b100000

    /**
     * 64 Image
     */
    const val image = 0b1000000

    /**
     * 128 Websites providing download services only
     */
    const val webFile = 0b10000000

    /**
     * 256 Local
     */
    const val local = 0b100000000

    /**
     * 512 Archive Indicates book file comes from archive
     */
    const val archive = 0b1000000000

    /**
     * 1024 Temporary reading books not officially added to bookshelf
     */
    const val notShelf = 0b100_0000_0000

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(text, updateError, audio, image, webFile, local, archive, notShelf)
    annotation class Type

    /**
     * All book types convertible from source
     */
    const val allBookType = text or image or audio or webFile

    const val allBookTypeLocal = text or image or audio or webFile or local

    /**
     * Local book source flag
     */
    const val localTag = "loc_book"

    /**
     * Books starting with webDav:: can be updated or re-downloaded from webDav
     */
    const val webDavTag = "webDav::"

}