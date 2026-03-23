package io.legado.app.web.socket

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import io.legado.app.R
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.config.AppConfig
import io.legado.app.model.webBook.SearchModel
import io.legado.app.ui.book.search.SearchScope
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.isJson
import io.legado.app.utils.TranslateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import splitties.init.appCtx
import java.io.IOException

class BookSearchWebSocket(handshakeRequest: NanoHTTPD.IHTTPSession) :
    NanoWSD.WebSocket(handshakeRequest),
    CoroutineScope by MainScope(),
    SearchModel.CallBack {

    private val normalClosure = NanoWSD.WebSocketFrame.CloseCode.NormalClosure
    private val searchModel = SearchModel(this, this)

    private val SEARCH_FINISH = "Search finish"
    private var shouldTranslate = false

    override fun onOpen() {
        launch(IO) {
            kotlin.runCatching {
                while (isOpen) {
                    ping("ping".toByteArray())
                    delay(30000)
                }
            }
        }
    }

    override fun onClose(
        code: NanoWSD.WebSocketFrame.CloseCode,
        reason: String,
        initiatedByRemote: Boolean
    ) {
        cancel()
        searchModel.close()
    }

    override fun onMessage(message: NanoWSD.WebSocketFrame) {
        launch(IO) {
            kotlin.runCatching {
                if (!message.textPayload.isJson()) {
                    send("数据必须为Json格式")
                    close(normalClosure, SEARCH_FINISH, false)
                    return@launch
                }
                val searchMap =
                    GSON.fromJsonObject<Map<String, String>>(message.textPayload).getOrNull()
                if (searchMap != null) {
                    val key = searchMap["key"]
                    shouldTranslate = searchMap["translate"]?.toBoolean() ?: false
                    if (key.isNullOrBlank()) {
                        send(appCtx.getString(R.string.cannot_empty))
                        close(normalClosure, SEARCH_FINISH, false)
                        return@launch
                    }
                    searchModel.search(System.currentTimeMillis(), key)
                }
            }
        }
    }

    override fun onPong(pong: NanoWSD.WebSocketFrame) {

    }

    override fun onException(exception: IOException) {

    }

    override fun getSearchScope(): SearchScope = SearchScope(AppConfig.searchScope)

    override fun onSearchStart() {

    }

    override fun onSearchSuccess(searchBooks: List<SearchBook>) {
        if (shouldTranslate) {
            runBlocking {
                searchBooks.forEach {
                    it.name = TranslateUtils.translateMeta(it.name)
                    it.author = TranslateUtils.translateMeta(it.author)
                    it.intro = TranslateUtils.translateMeta(it.intro)
                    it.latestChapterTitle = TranslateUtils.translateMeta(it.latestChapterTitle)
                    it.kind = TranslateUtils.translateMeta(it.kind)
                }
            }
        }
        send(GSON.toJson(searchBooks))
    }

    override fun onSearchFinish(isEmpty: Boolean, hasMore: Boolean) = close(normalClosure, SEARCH_FINISH, false)

    override fun onSearchCancel(exception: Throwable?) = close(normalClosure, exception?.toString() ?: SEARCH_FINISH, false)

}

