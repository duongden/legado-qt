package io.legado.app.model.remote

import android.net.Uri
import io.legado.app.data.entities.Book

abstract class RemoteBookManager {

    /**
     * Get book list
     */
    @Throws(Exception::class)
    abstract suspend fun getRemoteBookList(path: String): MutableList<RemoteBook>

    /**
     * Get book info by url
     */
    @Throws(Exception::class)
    abstract suspend fun getRemoteBook(path: String): RemoteBook?

    /**
     * @return Uri: Local download path
     */
    @Throws(Exception::class)
    abstract suspend fun downloadRemoteBook(remoteBook: RemoteBook): Uri

    /**
     * Upload book
     */
    @Throws(Exception::class)
    abstract suspend fun upload(book: Book)

    /**
     * Delete book
     */
    @Throws(Exception::class)
    abstract suspend fun delete(remoteBookUrl: String)

}