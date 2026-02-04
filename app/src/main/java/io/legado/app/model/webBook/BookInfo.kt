package io.legado.app.model.webBook

import android.text.TextUtils
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.isWebFile
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeRule.Companion.setCoroutineContext
import io.legado.app.utils.DebugLog
import io.legado.app.utils.HtmlFormatter
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.StringUtils.wordCountFormat
import kotlinx.coroutines.ensureActive
import splitties.init.appCtx
import kotlin.coroutines.coroutineContext


/**
 * 获取详情
 */
object BookInfo {

    @Throws(Exception::class)
    suspend fun analyzeBookInfo(
        bookSource: BookSource,
        book: Book,
        baseUrl: String,
        redirectUrl: String,
        body: String?,
        canReName: Boolean,
    ) {
        body ?: throw NoStackTraceException(
            appCtx.getString(R.string.error_get_web_content, baseUrl)
        )
        Debug.log(bookSource.bookSourceUrl, "${appCtx.getString(R.string.log_get_success)}${baseUrl}")
        Debug.log(bookSource.bookSourceUrl, body, state = 20)
        val analyzeRule = AnalyzeRule(book, bookSource)
        analyzeRule.setContent(body).setBaseUrl(baseUrl)
        analyzeRule.setRedirectUrl(redirectUrl)
        analyzeRule.setCoroutineContext(coroutineContext)
        analyzeBookInfo(book, body, analyzeRule, bookSource, baseUrl, redirectUrl, canReName)
    }

    suspend fun analyzeBookInfo(
        book: Book,
        body: String,
        analyzeRule: AnalyzeRule,
        bookSource: BookSource,
        baseUrl: String,
        redirectUrl: String,
        canReName: Boolean,
    ) {
        val infoRule = bookSource.getBookInfoRule()
        infoRule.init?.let {
            if (it.isNotBlank()) {
                coroutineContext.ensureActive()
                Debug.log(bookSource.bookSourceUrl, appCtx.getString(R.string.log_exec_intro_init_rule))
                analyzeRule.setContent(analyzeRule.getElement(it))
            }
        }
        val mCanReName = canReName && !infoRule.canReName.isNullOrBlank()
        coroutineContext.ensureActive()
        Debug.log(bookSource.bookSourceUrl, appCtx.getString(R.string.log_get_book_name))
        BookHelp.formatBookName(analyzeRule.getString(infoRule.name)).let {
            if (it.isNotEmpty() && (mCanReName || book.name.isEmpty())) {
                book.name = it
            }
            Debug.log(bookSource.bookSourceUrl, "└${it}")
        }
        coroutineContext.ensureActive()
        Debug.log(bookSource.bookSourceUrl, appCtx.getString(R.string.log_get_author))
        BookHelp.formatBookAuthor(analyzeRule.getString(infoRule.author)).let {
            if (it.isNotEmpty() && (mCanReName || book.author.isEmpty())) {
                book.author = it
            }
            Debug.log(bookSource.bookSourceUrl, "└${it}")
        }
        coroutineContext.ensureActive()
        Debug.log(bookSource.bookSourceUrl, appCtx.getString(R.string.log_get_kind))
        try {
            analyzeRule.getStringList(infoRule.kind)
                ?.joinToString(",")
                ?.let {
                    if (it.isNotEmpty()) book.kind = it
                    Debug.log(bookSource.bookSourceUrl, "└${it}")
                } ?: Debug.log(bookSource.bookSourceUrl, "└")
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}")
            DebugLog.e(appCtx.getString(R.string.err_get_kind), e)
        }
        coroutineContext.ensureActive()
        Debug.log(bookSource.bookSourceUrl, appCtx.getString(R.string.log_get_word_count))
        try {
            wordCountFormat(analyzeRule.getString(infoRule.wordCount)).let {
                if (it.isNotEmpty()) book.wordCount = it
                Debug.log(bookSource.bookSourceUrl, "└${it}")
            }
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}")
            DebugLog.e(appCtx.getString(R.string.err_get_word_count), e)
        }
        coroutineContext.ensureActive()
        Debug.log(bookSource.bookSourceUrl, appCtx.getString(R.string.log_get_last_chapter))
        try {
            analyzeRule.getString(infoRule.lastChapter).let {
                if (it.isNotEmpty()) book.latestChapterTitle = it
                Debug.log(bookSource.bookSourceUrl, "└${it}")
            }
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}")
            DebugLog.e(appCtx.getString(R.string.err_get_last_chapter), e)
        }
        coroutineContext.ensureActive()
        Debug.log(bookSource.bookSourceUrl, appCtx.getString(R.string.log_get_intro))
        try {
            HtmlFormatter.format(analyzeRule.getString(infoRule.intro)).let {
                if (it.isNotEmpty()) book.intro = it
                Debug.log(bookSource.bookSourceUrl, "└${it}")
            }
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}")
            DebugLog.e(appCtx.getString(R.string.err_get_intro), e)
        }
        coroutineContext.ensureActive()
        Debug.log(bookSource.bookSourceUrl, appCtx.getString(R.string.log_get_cover_url))
        try {
            analyzeRule.getString(infoRule.coverUrl).let {
                if (it.isNotEmpty()) {
                    book.coverUrl =
                        NetworkUtils.getAbsoluteURL(redirectUrl, it)
                }
                Debug.log(bookSource.bookSourceUrl, "└${it}")
            }
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            Debug.log(bookSource.bookSourceUrl, "└${e.localizedMessage}")
            DebugLog.e(appCtx.getString(R.string.err_get_cover_url), e)
        }
        coroutineContext.ensureActive()
        if (!book.isWebFile) {
            Debug.log(bookSource.bookSourceUrl, appCtx.getString(R.string.log_get_toc_url))
            book.tocUrl = analyzeRule.getString(infoRule.tocUrl, isUrl = true)
            if (book.tocUrl.isEmpty()) book.tocUrl = baseUrl
            if (book.tocUrl == baseUrl) {
                book.tocHtml = body
            }
            Debug.log(bookSource.bookSourceUrl, "└${book.tocUrl}")
        } else {
            Debug.log(bookSource.bookSourceUrl, appCtx.getString(R.string.log_get_download_url))
            book.downloadUrls = analyzeRule.getStringList(infoRule.downloadUrls, isUrl = true)
            if (book.downloadUrls.isNullOrEmpty()) {
                Debug.log(bookSource.bookSourceUrl, "└")
                throw NoStackTraceException(appCtx.getString(R.string.err_download_url_empty))
            } else {
                Debug.log(
                    bookSource.bookSourceUrl,
                    "└" + TextUtils.join("，\n", book.downloadUrls!!)
                )
            }
        }
    }

}