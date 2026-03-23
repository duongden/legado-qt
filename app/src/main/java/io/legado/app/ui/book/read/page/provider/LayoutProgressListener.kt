package io.legado.app.ui.book.read.page.provider

import io.legado.app.ui.book.read.page.entities.TextPage

interface LayoutProgressListener {

    /**
     * Single page layout complete
     */
    fun onLayoutPageCompleted(index: Int, page: TextPage) {}

    /**
     * Layout complete
     */
    fun onLayoutCompleted() {}

    /**
     * Layout exception
     */
    fun onLayoutException(e: Throwable) {}

}
