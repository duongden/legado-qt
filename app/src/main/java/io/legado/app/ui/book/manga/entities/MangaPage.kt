package io.legado.app.ui.book.manga.entities

data class MangaPage(
    override val chapterIndex: Int = 0,//Total chapter position
    val chapterSize: Int,//Total chapter count
    val mImageUrl: String = "",//Current URL
    override val index: Int = 0,//Current chapter position
    var imageCount: Int = 0,//Total current chapter content
    val mChapterName: String = "",//Chapter Name
) : BaseMangaPage
