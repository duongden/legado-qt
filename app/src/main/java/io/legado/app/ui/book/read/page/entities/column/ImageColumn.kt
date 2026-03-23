package io.legado.app.ui.book.read.page.entities.column

import android.graphics.Canvas
import android.graphics.RectF
import androidx.annotation.Keep
import io.legado.app.model.ImageProvider
import io.legado.app.model.ReadBook
import io.legado.app.ui.book.read.page.ContentTextView
import io.legado.app.ui.book.read.page.entities.TextLine
import io.legado.app.ui.book.read.page.entities.TextLine.Companion.emptyTextLine
import io.legado.app.utils.toastOnUi
import splitties.init.appCtx

/**
 * 图片列
 */
@Keep
data class ImageColumn(
    override var start: Float,
    override var end: Float,
    var src: String
) : BaseColumn {

    override var textLine: TextLine = emptyTextLine
    override fun draw(view: ContentTextView, canvas: Canvas) {
        val book = ReadBook.book ?: return

        val height = textLine.height

        val bitmap = ImageProvider.getImage(
            book,
            src,
            (end - start).toInt(),
            height.toInt()
        )

        val rectF = if (textLine.isImage) {
            RectF(start, 0f, end, height)
        } else {
            /*Overlay keeping original image aspect ratio based on width, allow height taller than char when div is negative*/
            val h = (end - start) / bitmap.width * bitmap.height
            val div = (height - h) / 2
            RectF(start, div, end, height - div)
        }
        kotlin.runCatching {
            canvas.drawBitmap(bitmap, null, rectF, view.imagePaint)
        }.onFailure { e ->
            appCtx.toastOnUi(e.localizedMessage)
        }
    }

}
