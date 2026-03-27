package io.legado.app.model.webBook

import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.rule.BookListRule
import io.legado.app.data.entities.rule.ExploreKind
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.book.BookHelp
import io.legado.app.help.source.exploreKindsJson
import io.legado.app.help.source.getBookType
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeRule.Companion.setCoroutineContext
import io.legado.app.model.analyzeRule.AnalyzeRule.Companion.setRuleData
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.analyzeRule.RuleData
import io.legado.app.utils.GSON
import io.legado.app.utils.GSONStrict
import io.legado.app.utils.HtmlFormatter
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.StringUtils.wordCountFormat
import io.legado.app.utils.fromJsonArray
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import splitties.init.appCtx

/**
 * 获取书籍列表
 */
object BookList {

    @Throws(Exception::class)
    suspend fun analyzeBookList(
        bookSource: BookSource,
        ruleData: RuleData,
        analyzeUrl: AnalyzeUrl,
        baseUrl: String,
        body: String?,
        isSearch: Boolean = true,
        isRedirect: Boolean = false,
        filter: ((name: String, author: String, kind: String?) -> Boolean)? = null,
        shouldBreak: ((size: Int) -> Boolean)? = null
    ): ArrayList<SearchBook> {
        body ?: throw NoStackTraceException(
            appCtx.getString(
                R.string.error_get_web_content,
                analyzeUrl.ruleUrl
            )
        )
        val bookList = ArrayList<SearchBook>()
        Debug.log(bookSource.bookSourceUrl, "≡Lấy thành công:${analyzeUrl.ruleUrl}")
        Debug.log(bookSource.bookSourceUrl, body, state = 10)
        val analyzeRule = AnalyzeRule(ruleData, bookSource)
        analyzeRule.setContent(body).setBaseUrl(baseUrl)
        analyzeRule.setRedirectUrl(baseUrl)
        analyzeRule.setCoroutineContext(currentCoroutineContext())
        if (!isSearch) {
            checkExploreJson(bookSource)
        }
        if (isSearch) bookSource.bookUrlPattern?.let {
            currentCoroutineContext().ensureActive()
            if (baseUrl.matches(it.toRegex())) {
                Debug.log(bookSource.bookSourceUrl, "≡Liên kết là trang chi tiết")
                getInfoItem(
                    bookSource,
                    analyzeRule,
                    analyzeUrl,
                    body,
                    baseUrl,
                    ruleData.getVariable(),
                    isRedirect,
                    filter
                )?.let { searchBook ->
                    searchBook.infoHtml = body
                    bookList.add(searchBook)
                }
                return bookList
            }
        }
        val collections: List<Any>
        var reverse = false
        val bookListRule: BookListRule = when {
            isSearch -> bookSource.getSearchRule()
            bookSource.getExploreRule().bookList.isNullOrBlank() -> bookSource.getSearchRule()
            else -> bookSource.getExploreRule()
        }
        var ruleList: String = bookListRule.bookList ?: ""
        if (ruleList.startsWith("-")) {
            reverse = true
            ruleList = ruleList.substring(1)
        }
        if (ruleList.startsWith("+")) {
            ruleList = ruleList.substring(1)
        }
        Debug.log(bookSource.bookSourceUrl, "┌Lấy danh sách sách")
        collections = analyzeRule.getElements(ruleList)
        currentCoroutineContext().ensureActive()
        if (collections.isEmpty() && bookSource.bookUrlPattern.isNullOrEmpty()) {
            Debug.log(bookSource.bookSourceUrl, "└Danh sách trống, phân tích theo trang chi tiết")
            getInfoItem(
                bookSource, analyzeRule, analyzeUrl, body, baseUrl, ruleData.getVariable(),
                isRedirect, filter
            )?.let { searchBook ->
                searchBook.infoHtml = body
                bookList.add(searchBook)
            }
        } else {
            val ruleName = analyzeRule.splitSourceRule(bookListRule.name)
            val ruleBookUrl = analyzeRule.splitSourceRule(bookListRule.bookUrl)
            val ruleAuthor = analyzeRule.splitSourceRule(bookListRule.author)
            val ruleCoverUrl = analyzeRule.splitSourceRule(bookListRule.coverUrl)
            val ruleIntro = analyzeRule.splitSourceRule(bookListRule.intro)
            val ruleKind = analyzeRule.splitSourceRule(bookListRule.kind)
            val ruleLastChapter = analyzeRule.splitSourceRule(bookListRule.lastChapter)
            val ruleWordCount = analyzeRule.splitSourceRule(bookListRule.wordCount)
            Debug.log(bookSource.bookSourceUrl, "└Kích thước danh sách:${collections.size}")
            for ((index, item) in collections.withIndex()) {
                getSearchItem(
                    bookSource, analyzeRule, item, baseUrl, ruleData.getVariable(),
                    index == 0,
                    filter,
                    ruleName = ruleName,
                    ruleBookUrl = ruleBookUrl,
                    ruleAuthor = ruleAuthor,
                    ruleCoverUrl = ruleCoverUrl,
                    ruleIntro = ruleIntro,
                    ruleKind = ruleKind,
                    ruleLastChapter = ruleLastChapter,
                    ruleWordCount = ruleWordCount
                )?.let { searchBook ->
                    if (baseUrl == searchBook.bookUrl) {
                        searchBook.infoHtml = body
                    }
                    bookList.add(searchBook)
                }
                if (shouldBreak?.invoke(bookList.size) == true) {
                    break
                }
            }
            val lh = LinkedHashSet(bookList)
            bookList.clear()
            bookList.addAll(lh)
            if (reverse) {
                bookList.reverse()
            }
        }
        Debug.log(bookSource.bookSourceUrl, "◇Tổng số sách:${bookList.size}")
        return bookList
    }

    @Throws(Exception::class)
    private suspend fun getInfoItem(
        bookSource: BookSource,
        analyzeRule: AnalyzeRule,
        analyzeUrl: AnalyzeUrl,
        body: String,
        baseUrl: String,
        variable: String?,
        isRedirect: Boolean,
        filter: ((name: String, author: String, kind: String?) -> Boolean)?
    ): SearchBook? {
        val book = Book(variable = variable)
        book.bookUrl = if (isRedirect) {
            baseUrl
        } else {
            NetworkUtils.getAbsoluteURL(analyzeUrl.url, analyzeUrl.ruleUrl)
        }
        book.origin = bookSource.bookSourceUrl
        book.originName = bookSource.bookSourceName
        book.originOrder = bookSource.customOrder
        book.type = bookSource.getBookType()
        analyzeRule.setRuleData(book)
        BookInfo.analyzeBookInfo(
            book,
            body,
            analyzeRule,
            bookSource,
            baseUrl,
            baseUrl,
            false
        )
        if (filter?.invoke(book.name, book.author, book.kind) == false) {
            return null
        }
        if (book.name.isNotBlank()) {
            return book.toSearchBook()
        }
        return null
    }

    @Throws(Exception::class)
    private suspend fun getSearchItem(
        bookSource: BookSource,
        analyzeRule: AnalyzeRule,
        item: Any,
        baseUrl: String,
        variable: String?,
        log: Boolean,
        filter: ((name: String, author: String, kind: String?) -> Boolean)?,
        ruleName: List<AnalyzeRule.SourceRule>,
        ruleBookUrl: List<AnalyzeRule.SourceRule>,
        ruleAuthor: List<AnalyzeRule.SourceRule>,
        ruleKind: List<AnalyzeRule.SourceRule>,
        ruleCoverUrl: List<AnalyzeRule.SourceRule>,
        ruleWordCount: List<AnalyzeRule.SourceRule>,
        ruleIntro: List<AnalyzeRule.SourceRule>,
        ruleLastChapter: List<AnalyzeRule.SourceRule>
    ): SearchBook? {
        val searchBook = SearchBook(variable = variable)
        searchBook.type = bookSource.getBookType()
        searchBook.origin = bookSource.bookSourceUrl
        searchBook.originName = bookSource.bookSourceName
        searchBook.originOrder = bookSource.customOrder
        analyzeRule.setRuleData(searchBook)
        analyzeRule.setContent(item)
        currentCoroutineContext().ensureActive()
        Debug.log(bookSource.bookSourceUrl, "┌Lấy tên sách", log)
        searchBook.name = BookHelp.formatBookName(analyzeRule.getString(ruleName))
        Debug.log(bookSource.bookSourceUrl, "└${searchBook.name}", log)
        if (searchBook.name.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌Lấy tác giả", log)
            searchBook.author = BookHelp.formatBookAuthor(analyzeRule.getString(ruleAuthor))
            Debug.log(bookSource.bookSourceUrl, "└${searchBook.author}", log)
            currentCoroutineContext().ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌Lấy phân loại", log)
            try {
                searchBook.kind = analyzeRule.getStringList(ruleKind)?.joinToString(",")
                Debug.log(bookSource.bookSourceUrl, "└${searchBook.kind ?: ""}", log)
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}", log)
            }
            if (filter?.invoke(searchBook.name, searchBook.author, searchBook.kind) == false) {
                return null
            }
            currentCoroutineContext().ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌Lấy số chữ", log)
            try {
                searchBook.wordCount = wordCountFormat(analyzeRule.getString(ruleWordCount))
                Debug.log(bookSource.bookSourceUrl, "└${searchBook.wordCount}", log)
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}", log)
            }
            currentCoroutineContext().ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌Lấy chương mới nhất", log)
            try {
                searchBook.latestChapterTitle = analyzeRule.getString(ruleLastChapter)
                Debug.log(bookSource.bookSourceUrl, "└${searchBook.latestChapterTitle}", log)
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}", log)
            }
            currentCoroutineContext().ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌Lấy giới thiệu", log)
            try {
                searchBook.intro = HtmlFormatter.format(analyzeRule.getString(ruleIntro))
                Debug.log(bookSource.bookSourceUrl, "└${searchBook.intro}", log)
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}", log)
            }
            currentCoroutineContext().ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌Lấy liên kết ảnh bìa", log)
            try {
                analyzeRule.getString(ruleCoverUrl).let {
                    if (it.isNotEmpty()) {
                        searchBook.coverUrl = NetworkUtils.getAbsoluteURL(baseUrl, it)
                    }
                }
                Debug.log(bookSource.bookSourceUrl, "└${searchBook.coverUrl ?: ""}", log)
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}", log)
            }
            currentCoroutineContext().ensureActive()
            Debug.log(bookSource.bookSourceUrl, "┌Lấy liên kết trang chi tiết", log)
            searchBook.bookUrl = analyzeRule.getString(ruleBookUrl, isUrl = true)
            if (searchBook.bookUrl.isEmpty()) {
                searchBook.bookUrl = baseUrl
            }
            Debug.log(bookSource.bookSourceUrl, "└${searchBook.bookUrl}", log)
            return searchBook
        }
        return null
    }

    private fun checkExploreJson(bookSource: BookSource) {
        if (Debug.callback == null) {
            return
        }
        val json = bookSource.exploreKindsJson()
        if (json.isEmpty()) {
            return
        }
        val kinds = GSONStrict.fromJsonArray<ExploreKind>(json).getOrNull()
        if (kinds != null) {
            return
        }
        GSON.fromJsonArray<ExploreKind>(json).getOrNull()?.let {
            Debug.log("≡Định dạng JSON của quy tắc địa chỉ khám phá không chuẩn, vui lòng đổi sang định dạng chuẩn")
        }
    }

}