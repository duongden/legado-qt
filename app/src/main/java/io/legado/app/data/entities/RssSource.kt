package io.legado.app.data.entities

import android.os.Parcelable
import android.text.TextUtils
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.legado.app.constant.AppPattern
import io.legado.app.utils.splitNotBlank
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "rssSources", indices = [(Index(value = ["sourceUrl"], unique = false))])
data class RssSource(
    @PrimaryKey
    var sourceUrl: String = "",
    // Name
    var sourceName: String = "",
    // Icon
    var sourceIcon: String = "",
    // Group
    var sourceGroup: String? = null,
    // Comment
    var sourceComment: String? = null,
    // Is Enabled
    var enabled: Boolean = true,
    // Custom variable description
    var variableComment: String? = null,
    // js library
    override var jsLib: String? = null,
    // Enable okhttp CookieJar auto save cookie for every request
    @ColumnInfo(defaultValue = "0")
    override var enabledCookieJar: Boolean? = true,
    /**Concurrency rate**/
    override var concurrentRate: String? = null,
    /**Request header**/
    override var header: String? = null,
    /**Login Url**/
    override var loginUrl: String? = null,
    /**Login Ui**/
    override var loginUi: String? = null,
    /**Login check js**/
    var loginCheckJs: String? = null,
    /**Cover decryption js**/
    var coverDecodeJs: String? = null,
    /**Category Url**/
    var sortUrl: String? = null,
    /**Is single url source**/
    var singleUrl: Boolean = false,
    /*List rule*/
    /**List style,0,1,2**/
    @ColumnInfo(defaultValue = "0")
    var articleStyle: Int = 0,
    /**List rule**/
    var ruleArticles: String? = null,
    /**Next page rule**/
    var ruleNextPage: String? = null,
    /**Title rule**/
    var ruleTitle: String? = null,
    /**Publish date rule**/
    var rulePubDate: String? = null,
    /*webView rule*/
    /**Description rule**/
    var ruleDescription: String? = null,
    /**Image rule**/
    var ruleImage: String? = null,
    /**Link rule**/
    var ruleLink: String? = null,
    /**Content rule**/
    var ruleContent: String? = null,
    /**Content url whitelist**/
    var contentWhitelist: String? = null,
    /**Content url blacklist**/
    var contentBlacklist: String? = null,
    /**
     * 跳转url拦截,
     * js, 返回true拦截,js变量url,可以通过js打开url,比如调用阅读搜索,添加书架等,简化规则写法,不用webView js注入
     * **/
    var shouldOverrideUrlLoading: String? = null,
    /**webView style**/
    var style: String? = null,
    @ColumnInfo(defaultValue = "1")
    var enableJs: Boolean = true,
    @ColumnInfo(defaultValue = "1")
    var loadWithBaseUrl: Boolean = true,
    /**Inject js**/
    var injectJs: String? = null,
    /*Other rules*/
    /**Last update time, for sorting**/
    @ColumnInfo(defaultValue = "0")
    var lastUpdateTime: Long = 0,
    @ColumnInfo(defaultValue = "0")
    var customOrder: Int = 0
) : Parcelable, BaseSource {

    override fun getTag(): String {
        return sourceName
    }

    override fun getKey(): String {
        return sourceUrl
    }

    override fun equals(other: Any?): Boolean {
        if (other is RssSource) {
            return other.sourceUrl == sourceUrl
        }
        return false
    }

    override fun hashCode() = sourceUrl.hashCode()

    fun equal(source: RssSource): Boolean {
        return equal(sourceUrl, source.sourceUrl)
                && equal(sourceName, source.sourceName)
                && equal(sourceIcon, source.sourceIcon)
                && enabled == source.enabled
                && equal(sourceGroup, source.sourceGroup)
                && enabledCookieJar == source.enabledCookieJar
                && equal(sourceComment, source.sourceComment)
                && equal(concurrentRate, source.concurrentRate)
                && equal(header, source.header)
                && equal(loginUrl, source.loginUrl)
                && equal(loginUi, source.loginUi)
                && equal(loginCheckJs, source.loginCheckJs)
                && equal(coverDecodeJs, source.coverDecodeJs)
                && equal(sortUrl, source.sortUrl)
                && singleUrl == source.singleUrl
                && articleStyle == source.articleStyle
                && equal(ruleArticles, source.ruleArticles)
                && equal(ruleNextPage, source.ruleNextPage)
                && equal(ruleTitle, source.ruleTitle)
                && equal(rulePubDate, source.rulePubDate)
                && equal(ruleDescription, source.ruleDescription)
                && equal(ruleLink, source.ruleLink)
                && equal(ruleContent, source.ruleContent)
                && enableJs == source.enableJs
                && loadWithBaseUrl == source.loadWithBaseUrl
                && equal(variableComment, source.variableComment)
                && equal(style, source.style)
                && equal(injectJs, source.injectJs)
    }

    private fun equal(a: String?, b: String?): Boolean {
        return a == b || (a.isNullOrEmpty() && b.isNullOrEmpty())
    }

    fun getDisplayNameGroup(): String {
        return if (sourceGroup.isNullOrBlank()) {
            sourceName
        } else {
            String.format("%s (%s)", sourceName, sourceGroup)
        }
    }

    fun addGroup(groups: String): RssSource {
        sourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.toHashSet()?.let {
            it.addAll(groups.splitNotBlank(AppPattern.splitGroupRegex))
            sourceGroup = TextUtils.join(",", it)
        }
        if (sourceGroup.isNullOrBlank()) sourceGroup = groups
        return this
    }

    fun removeGroup(groups: String): RssSource {
        sourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.toHashSet()?.let {
            it.removeAll(groups.splitNotBlank(AppPattern.splitGroupRegex).toSet())
            sourceGroup = TextUtils.join(",", it)
        }
        return this
    }

    fun getDisplayVariableComment(otherComment: String): String {
        return if (variableComment.isNullOrBlank()) {
            otherComment
        } else {
            "${variableComment}\n$otherComment"
        }
    }

}
