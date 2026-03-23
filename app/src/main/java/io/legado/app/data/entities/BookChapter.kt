package io.legado.app.data.entities

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import io.legado.app.constant.AppLog
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
import io.legado.app.exception.RegexTimeoutException
import io.legado.app.help.RuleBigDataHelp
import io.legado.app.help.config.AppConfig
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.analyzeRule.RuleDataInterface
import io.legado.app.utils.ChineseUtils
import io.legado.app.utils.GSON
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.replace
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.CancellationException
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import splitties.init.appCtx

@Parcelize
@Entity(
    tableName = "chapters",
    primaryKeys = ["url", "bookUrl"],
    indices = [(Index(value = ["bookUrl"], unique = false)),
        (Index(value = ["bookUrl", "index"], unique = true))],
    foreignKeys = [(ForeignKey(
        entity = Book::class,
        parentColumns = ["bookUrl"],
        childColumns = ["bookUrl"],
        onDelete = ForeignKey.CASCADE
    ))]
)    // Auto delete chapters when deleting book
data class BookChapter(
    var url: String = "",               // Chapter URL
    var title: String = "",             // Chapter Title
    var isVolume: Boolean = false,      // Is Volume Name
    var baseUrl: String = "",           // Used to splice relative url
    var bookUrl: String = "",           // Book URL
    var index: Int = 0,                 // Chapter Order
    var isVip: Boolean = false,         // Is VIP
    var isPay: Boolean = false,         // Is Purchased
    var resourceUrl: String? = null,    // Audio real URL
    var tag: String? = null,            // Update time or other chapter extra info
    var wordCount: String? = null,      // Current chapter word count
    var start: Long? = null,            // Chapter start position
    var end: Long? = null,              // Chapter end position
    var startFragmentId: String? = null,  //EPUB book current chapter fragmentId
    var endFragmentId: String? = null,    //EPUB book next chapter fragmentId
    var variable: String? = null        //Variable
) : Parcelable, RuleDataInterface {

    @delegate:Transient
    @delegate:Ignore
    @IgnoredOnParcel
    override val variableMap: HashMap<String, String> by lazy {
        GSON.fromJsonObject<HashMap<String, String>>(variable).getOrNull() ?: hashMapOf()
    }

    @Ignore
    @IgnoredOnParcel
    var titleMD5: String? = null

    override fun putVariable(key: String, value: String?): Boolean {
        if (super.putVariable(key, value)) {
            variable = GSON.toJson(variableMap)
        }
        return true
    }

    override fun putBigVariable(key: String, value: String?) {
        RuleBigDataHelp.putChapterVariable(bookUrl, url, key, value)
    }

    override fun getBigVariable(key: String): String? {
        return RuleBigDataHelp.getChapterVariable(bookUrl, url, key)
    }

    override fun hashCode() = url.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other is BookChapter) {
            return other.url == url
        }
        return false
    }

    fun primaryStr(): String {
        return bookUrl + url
    }

    fun getDisplayTitle(
        replaceRules: List<ReplaceRule>? = null,
        useReplace: Boolean = true,
        chineseConvert: Boolean = true,
    ): String {
        var displayTitle = title.replace(AppPattern.rnRegex, "")
        if (chineseConvert) {
            when (AppConfig.chineseConverterType) {
                1 -> displayTitle = ChineseUtils.t2s(displayTitle)
                2 -> displayTitle = ChineseUtils.s2t(displayTitle)
            }
        }
        if (useReplace && replaceRules != null) kotlin.run {
            replaceRules.forEach { item ->
                if (item.pattern.isNotEmpty()) {
                    try {
                        val mDisplayTitle = if (item.isRegex) {
                            displayTitle.replace(
                                item.regex,
                                item.replacement,
                                item.getValidTimeoutMillisecond()
                            )
                        } else {
                            displayTitle.replace(item.pattern, item.replacement)
                        }
                        if (mDisplayTitle.isNotBlank()) {
                            displayTitle = mDisplayTitle
                        }
                    } catch (e: RegexTimeoutException) {
                        item.isEnabled = false
                        appDb.replaceRuleDao.update(item)
                    } catch (e: CancellationException) {
                        return@run
                    } catch (e: Exception) {
                        AppLog.put("${item.name}替换出错\n替换内容\n${displayTitle}", e)
                        appCtx.toastOnUi("${item.name}替换出错")
                    }
                }
            }
        }
        return displayTitle
    }

    fun getAbsoluteURL(): String {
        //Volume link null in secondary catalog parse, return catalog page link
        if (url.startsWith(title) && isVolume) return baseUrl
        val urlMatcher = AnalyzeUrl.paramPattern.matcher(url)
        val urlBefore = if (urlMatcher.find()) url.substring(0, urlMatcher.start()) else url
        val urlAbsoluteBefore = NetworkUtils.getAbsoluteURL(baseUrl, urlBefore)
        return if (urlBefore.length == url.length) {
            urlAbsoluteBefore
        } else {
            "$urlAbsoluteBefore," + url.substring(urlMatcher.end())
        }
    }

    private fun ensureTitleMD5Init() {
        if (titleMD5 == null) {
            titleMD5 = MD5Utils.md5Encode16(title)
        }
    }

    @SuppressLint("DefaultLocale")
    @Suppress("unused")
    fun getFileName(suffix: String = "nb"): String {
        ensureTitleMD5Init()
        return String.format("%05d-%s.%s", index, titleMD5, suffix)
    }

    @SuppressLint("DefaultLocale")
    @Suppress("unused")
    fun getFontName(): String {
        ensureTitleMD5Init()
        return String.format("%05d-%s.ttf", index, titleMD5)
    }
}

