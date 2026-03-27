package io.legado.app.model.rss

import androidx.annotation.Keep
import io.legado.app.R
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.exception.NoStackTraceException
import io.legado.app.model.Debug
import io.legado.app.model.analyzeRule.AnalyzeRule
import io.legado.app.model.analyzeRule.AnalyzeRule.Companion.setCoroutineContext
import io.legado.app.model.analyzeRule.AnalyzeRule.Companion.setRuleData
import io.legado.app.model.analyzeRule.RuleData
import io.legado.app.utils.NetworkUtils
import kotlinx.coroutines.currentCoroutineContext
import splitties.init.appCtx
import java.util.Locale

@Keep
object RssParserByRule {

    @Throws(Exception::class)
    suspend fun parseXML(
        sortName: String,
        sortUrl: String,
        redirectUrl: String,
        body: String?,
        rssSource: RssSource,
        ruleData: RuleData
    ): Pair<MutableList<RssArticle>, String?> {
        val sourceUrl = rssSource.sourceUrl
        var nextUrl: String? = null
        if (body.isNullOrBlank()) {
            throw NoStackTraceException(
                appCtx.getString(R.string.error_get_web_content, rssSource.sourceUrl)
            )
        }
        Debug.log(sourceUrl, body, state = 10)
        var ruleArticles = rssSource.ruleArticles
        if (ruleArticles.isNullOrBlank()) {
            Debug.log(sourceUrl, "⇒Quy tắc danh sách trống, sử dụng quy tắc mặc định để phân tích")
            return RssParserDefault.parseXML(sortName, body, sourceUrl)
        } else {
            val articleList = mutableListOf<RssArticle>()
            val analyzeRule = AnalyzeRule(ruleData, rssSource)
            analyzeRule.setCoroutineContext(currentCoroutineContext())
            analyzeRule.setContent(body).setBaseUrl(sortUrl)
            analyzeRule.setRedirectUrl(redirectUrl)
            var reverse = false
            if (ruleArticles.startsWith("-")) {
                reverse = true
                ruleArticles = ruleArticles.substring(1)
            }
            Debug.log(sourceUrl, "┌Lấy danh sách")
            val collections = analyzeRule.getElements(ruleArticles)
            Debug.log(sourceUrl, "└Kích thước danh sách:${collections.size}")
            if (!rssSource.ruleNextPage.isNullOrEmpty()) {
                Debug.log(sourceUrl, "┌Lấy liên kết trang tiếp theo")
                if (rssSource.ruleNextPage!!.uppercase(Locale.getDefault()) == "PAGE") {
                    nextUrl = sortUrl
                } else {
                    nextUrl = analyzeRule.getString(rssSource.ruleNextPage)
                    if (nextUrl.isNotEmpty()) {
                        nextUrl = NetworkUtils.getAbsoluteURL(sortUrl, nextUrl)
                    }
                }
                Debug.log(sourceUrl, "└$nextUrl")
            }
            val ruleTitle = analyzeRule.splitSourceRule(rssSource.ruleTitle)
            val rulePubDate = analyzeRule.splitSourceRule(rssSource.rulePubDate)
            val ruleDescription = analyzeRule.splitSourceRule(rssSource.ruleDescription)
            val ruleImage = analyzeRule.splitSourceRule(rssSource.ruleImage)
            val ruleLink = analyzeRule.splitSourceRule(rssSource.ruleLink)
            val variable = ruleData.getVariable()
            for ((index, item) in collections.withIndex()) {
                getItem(
                    sourceUrl, item, analyzeRule, variable,rssSource.type, index == 0,
                    ruleTitle, rulePubDate, ruleDescription, ruleImage, ruleLink
                )?.let {
                    if (io.legado.app.utils.TranslateUtils.isTranslateEnabled()) {
                        it.title = io.legado.app.utils.TranslateUtils.translateMeta(it.title)
                        it.pubDate = io.legado.app.utils.TranslateUtils.translateMeta(it.pubDate)
                    }
                    it.sort = sortName
                    it.origin = sourceUrl
                    articleList.add(it)
                }
            }
            if (reverse) {
                articleList.reverse()
            }
            return Pair(articleList, nextUrl)
        }
    }

    private fun getItem(
        sourceUrl: String,
        item: Any,
        analyzeRule: AnalyzeRule,
        variable: String?,
        type: Int,
        log: Boolean,
        ruleTitle: List<AnalyzeRule.SourceRule>,
        rulePubDate: List<AnalyzeRule.SourceRule>,
        ruleDescription: List<AnalyzeRule.SourceRule>,
        ruleImage: List<AnalyzeRule.SourceRule>,
        ruleLink: List<AnalyzeRule.SourceRule>
    ): RssArticle? {
        val rssArticle = RssArticle(variable = variable)
        analyzeRule.setRuleData(rssArticle)
        analyzeRule.setContent(item)
        Debug.log(sourceUrl, "┌获取标题", log)
        rssArticle.title = analyzeRule.getString(ruleTitle)
        Debug.log(sourceUrl, "└${rssArticle.title}", log)
        Debug.log(sourceUrl, "┌Lấy thời gian", log)
        rssArticle.pubDate = analyzeRule.getString(rulePubDate)
        Debug.log(sourceUrl, "└${rssArticle.pubDate}", log)
        Debug.log(sourceUrl, "┌Lấy mô tả", log)
        if (ruleDescription.isEmpty()) {
            rssArticle.description = null
            Debug.log(sourceUrl, "└Quy tắc mô tả trống, sẽ phân tích trang nội dung", log)
        } else {
            rssArticle.description = analyzeRule.getString(ruleDescription)
            Debug.log(sourceUrl, "└${rssArticle.description}", log)
        }
        Debug.log(sourceUrl, "┌Lấy url hình ảnh", log)
        try {
            analyzeRule.getString(ruleImage).let {
                if (it.isNotEmpty()) {
                    rssArticle.image = NetworkUtils.getAbsoluteURL(sourceUrl, it)
                }
            }
            Debug.log(sourceUrl, "└${rssArticle.image ?: ""}", log)
        } catch (e: Exception) {
            Debug.log(sourceUrl, "└${e.localizedMessage}", log)
        }
        Debug.log(sourceUrl, "┌Lấy liên kết bài viết", log)
        rssArticle.link = analyzeRule.getString(ruleLink, isUrl = true)
        Debug.log(sourceUrl, "└${rssArticle.link}", log)
        rssArticle.type = type
        if (rssArticle.title.isBlank()) {
            return null
        }
        return rssArticle
    }
}