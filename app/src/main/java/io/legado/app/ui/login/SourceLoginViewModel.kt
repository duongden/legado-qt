package io.legado.app.ui.login

import android.app.Application
import android.content.Intent
import com.script.rhino.runScriptWithContext
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.constant.BookType
import io.legado.app.data.appDb
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.exception.NoStackTraceException
import io.legado.app.model.AudioPlay
import io.legado.app.model.ReadBook
import io.legado.app.model.VideoPlay
import io.legado.app.utils.toastOnUi

class SourceLoginViewModel(application: Application) : BaseViewModel(application) {

    var source: BaseSource? = null
    var headerMap: Map<String, String> = emptyMap()
    var book: Book? = null
    var bookType: Int = 0
    var chapter: BookChapter? = null
    var loginInfo: MutableMap<String, String> = mutableMapOf()

    fun initData(intent: Intent, success: (bookSource: BaseSource) -> Unit, error: () -> Unit) {
        execute {
            bookType = intent.getIntExtra("bookType", 0)
            when (bookType) {
                BookType.text -> {
                    source = ReadBook.bookSource
                    book = ReadBook.book?.also {
                        chapter = appDb.bookChapterDao.getChapter(it.bookUrl, ReadBook.durChapterIndex)
                    }
                }

                BookType.audio -> {
                    source = AudioPlay.bookSource
                    book = AudioPlay.book
                    chapter = AudioPlay.durChapter
                }

                BookType.video -> {
                    source = VideoPlay.source
                    book = VideoPlay.book
                    chapter = VideoPlay.chapter
                }

                else -> {
                    val sourceKey = intent.getStringExtra("key")
                        ?: throw NoStackTraceException("Không có tham số")
                    val type = intent.getStringExtra("type")
                    source = when (type) {
                        "bookSource" ->  appDb.bookSourceDao.getBookSource(sourceKey)
                        "rssSource" -> appDb.rssSourceDao.getByKey(sourceKey)
                        "httpTts" -> appDb.httpTTSDao.get(sourceKey.toLong())
                        else -> null
                    }
                    val bookUrl = intent.getStringExtra("bookUrl")
                    book = bookUrl?.let {
                        appDb.bookDao.getBook(it) ?: appDb.searchBookDao.getSearchBook(it)?.toBook()
                    }
                }
            }
            headerMap = runScriptWithContext {
                source?.getHeaderMap(true) ?: emptyMap()
            }
            source?.let{ loginInfo = it.getLoginInfoMap() }
            source
        }.onSuccess {
            if (it != null) {
                success.invoke(it)
            } else {
                context.toastOnUi("Không tìm thấy nguồn sách")
            }
        }.onError {
            error.invoke()
            AppLog.put("Khởi tạo giao diện đăng nhập thất bại\n$it", it, true)
        }
    }

}