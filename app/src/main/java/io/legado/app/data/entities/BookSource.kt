package io.legado.app.data.entities

import android.os.Parcelable
import android.text.TextUtils
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import io.legado.app.constant.AppPattern
import io.legado.app.constant.BookSourceType
import io.legado.app.data.entities.rule.BookInfoRule
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.data.entities.rule.ExploreRule
import io.legado.app.data.entities.rule.ReviewRule
import io.legado.app.data.entities.rule.SearchRule
import io.legado.app.data.entities.rule.TocRule
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.splitNotBlank
import kotlinx.parcelize.Parcelize

@Suppress("unused")
@Parcelize
@TypeConverters(BookSource.Converters::class)
@Entity(
    tableName = "book_sources",
    indices = [(Index(value = ["bookSourceUrl"], unique = false))]
)
data class BookSource(
    // Address, including http/https
    @PrimaryKey
    var bookSourceUrl: String = "",
    // Name
    var bookSourceName: String = "",
    // Group
    var bookSourceGroup: String? = null,
    // Type, 0 Text, 1 Audio, 2 Image, 3 File (Sites like Zhixuan that only provide downloads)
    @BookSourceType.Type
    var bookSourceType: Int = 0,
    // Detail page url regex
    var bookUrlPattern: String? = null,
    // Manual sort number
    @ColumnInfo(defaultValue = "0")
    var customOrder: Int = 0,
    // Is Enabled
    @ColumnInfo(defaultValue = "1")
    var enabled: Boolean = true,
    // Enable Explore
    @ColumnInfo(defaultValue = "1")
    var enabledExplore: Boolean = true,
    // js library
    override var jsLib: String? = null,
    // Enable okhttp CookieJar auto save cookie for every request
    @ColumnInfo(defaultValue = "0")
    override var enabledCookieJar: Boolean? = true,
    // Concurrency rate
    override var concurrentRate: String? = null,
    // Request header
    override var header: String? = null,
    // Login URL
    override var loginUrl: String? = null,
    // Login UI
    override var loginUi: String? = null,
    // Login check js
    var loginCheckJs: String? = null,
    // Cover decryption js
    var coverDecodeJs: String? = null,
    // Comment
    var bookSourceComment: String? = null,
    // Custom variable description
    var variableComment: String? = null,
    // Last update time, for sorting
    var lastUpdateTime: Long = 0,
    // Response time, for sorting
    var respondTime: Long = 180000L,
    // Smart sort weight
    var weight: Int = 0,
    // Explore URL
    var exploreUrl: String? = null,
    // Explore filter rule
    var exploreScreen: String? = null,
    // Explore rule
    var ruleExplore: ExploreRule? = null,
    // Search URL
    var searchUrl: String? = null,
    // Search rule
    var ruleSearch: SearchRule? = null,
    // Book info page rule
    var ruleBookInfo: BookInfoRule? = null,
    // Catalog page rule
    var ruleToc: TocRule? = null,
    // Content page rule
    var ruleContent: ContentRule? = null,
    // Paragraph comment rule
    var ruleReview: ReviewRule? = null
) : Parcelable, BaseSource {

    override fun getTag(): String {
        return bookSourceName
    }

    override fun getKey(): String {
        return bookSourceUrl
    }

    override fun hashCode(): Int {
        return bookSourceUrl.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is BookSource) other.bookSourceUrl == bookSourceUrl else false
    }

    fun getSearchRule(): SearchRule {
        ruleSearch?.let { return it }
        val rule = SearchRule()
        ruleSearch = rule
        return rule
    }

    fun getExploreRule(): ExploreRule {
        ruleExplore?.let { return it }
        val rule = ExploreRule()
        ruleExplore = rule
        return rule
    }

    fun getBookInfoRule(): BookInfoRule {
        ruleBookInfo?.let { return it }
        val rule = BookInfoRule()
        ruleBookInfo = rule
        return rule
    }

    fun getTocRule(): TocRule {
        ruleToc?.let { return it }
        val rule = TocRule()
        ruleToc = rule
        return rule
    }

    fun getContentRule(): ContentRule {
        ruleContent?.let { return it }
        val rule = ContentRule()
        ruleContent = rule
        return rule
    }

//    fun getReviewRule(): ReviewRule {
//        ruleReview?.let { return it }
//        val rule = ReviewRule()
//        ruleReview = rule
//        return rule
//    }

    fun getDisPlayNameGroup(): String {
        return if (bookSourceGroup.isNullOrBlank()) {
            bookSourceName
        } else {
            String.format("%s (%s)", bookSourceName, bookSourceGroup)
        }
    }

    fun addGroup(groups: String): BookSource {
        bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.toHashSet()?.let {
            it.addAll(groups.splitNotBlank(AppPattern.splitGroupRegex))
            bookSourceGroup = TextUtils.join(",", it)
        }
        if (bookSourceGroup.isNullOrBlank()) bookSourceGroup = groups
        return this
    }

    fun removeGroup(groups: String): BookSource {
        bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.toHashSet()?.let {
            it.removeAll(groups.splitNotBlank(AppPattern.splitGroupRegex).toSet())
            bookSourceGroup = TextUtils.join(",", it)
        }
        return this
    }

    fun hasGroup(group: String): Boolean {
        bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.toHashSet()?.let {
            return it.indexOf(group) != -1
        }
        return false
    }

    fun removeInvalidGroups() {
        removeGroup(getInvalidGroupNames())
    }

    fun removeErrorComment() {
        bookSourceComment = bookSourceComment
            ?.split("\n\n")
            ?.filterNot {
                it.startsWith("// Error: ")
            }?.joinToString("\n")
    }

    fun addErrorComment(e: Throwable) {
        bookSourceComment =
            "// Error: ${e.localizedMessage}" + if (bookSourceComment.isNullOrBlank())
                "" else "\n\n${bookSourceComment}"
    }

    fun getCheckKeyword(default: String): String {
        ruleSearch?.checkKeyWord?.let {
            if (it.isNotBlank()) {
                return it
            }
        }
        return default
    }

    fun getInvalidGroupNames(): String {
        return bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.toHashSet()?.filter {
            "失效" in it || it == "校验超时"
        }?.joinToString() ?: ""
    }

    fun getDisplayVariableComment(otherComment: String): String {
        return if (variableComment.isNullOrBlank()) {
            otherComment
        } else {
            "${variableComment}\n$otherComment"
        }
    }

    fun equal(source: BookSource): Boolean {
        return equal(bookSourceName, source.bookSourceName)
                && equal(bookSourceUrl, source.bookSourceUrl)
                && equal(bookSourceGroup, source.bookSourceGroup)
                && bookSourceType == source.bookSourceType
                && equal(bookUrlPattern, source.bookUrlPattern)
                && equal(bookSourceComment, source.bookSourceComment)
                && customOrder == source.customOrder
                && enabled == source.enabled
                && enabledExplore == source.enabledExplore
                && enabledCookieJar == source.enabledCookieJar
                && equal(variableComment, source.variableComment)
                && equal(concurrentRate, source.concurrentRate)
                && equal(jsLib, source.jsLib)
                && equal(header, source.header)
                && equal(loginUrl, source.loginUrl)
                && equal(loginUi, source.loginUi)
                && equal(loginCheckJs, source.loginCheckJs)
                && equal(coverDecodeJs, source.coverDecodeJs)
                && equal(exploreUrl, source.exploreUrl)
                && equal(searchUrl, source.searchUrl)
                && getSearchRule() == source.getSearchRule()
                && getExploreRule() == source.getExploreRule()
                && getBookInfoRule() == source.getBookInfoRule()
                && getTocRule() == source.getTocRule()
                && getContentRule() == source.getContentRule()
    }

    private fun equal(a: String?, b: String?) = a == b || (a.isNullOrEmpty() && b.isNullOrEmpty())

    class Converters {

        @TypeConverter
        fun exploreRuleToString(exploreRule: ExploreRule?): String =
            GSON.toJson(exploreRule)

        @TypeConverter
        fun stringToExploreRule(json: String?) =
            GSON.fromJsonObject<ExploreRule>(json).getOrNull()

        @TypeConverter
        fun searchRuleToString(searchRule: SearchRule?): String =
            GSON.toJson(searchRule)

        @TypeConverter
        fun stringToSearchRule(json: String?) =
            GSON.fromJsonObject<SearchRule>(json).getOrNull()

        @TypeConverter
        fun bookInfoRuleToString(bookInfoRule: BookInfoRule?): String =
            GSON.toJson(bookInfoRule)

        @TypeConverter
        fun stringToBookInfoRule(json: String?) =
            GSON.fromJsonObject<BookInfoRule>(json).getOrNull()

        @TypeConverter
        fun tocRuleToString(tocRule: TocRule?): String =
            GSON.toJson(tocRule)

        @TypeConverter
        fun stringToTocRule(json: String?) =
            GSON.fromJsonObject<TocRule>(json).getOrNull()

        @TypeConverter
        fun contentRuleToString(contentRule: ContentRule?): String =
            GSON.toJson(contentRule)

        @TypeConverter
        fun stringToContentRule(json: String?) =
            GSON.fromJsonObject<ContentRule>(json).getOrNull()

        @TypeConverter
        fun stringToReviewRule(json: String?): ReviewRule? = null

        @TypeConverter
        fun reviewRuleToString(reviewRule: ReviewRule?): String = "null"

    }
}