package io.legado.app.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import io.legado.app.constant.AppPattern
import io.legado.app.constant.BookType
import io.legado.app.constant.PageAnim
import io.legado.app.data.appDb
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.book.getFolderNameNoCache
import io.legado.app.help.book.isEpub
import io.legado.app.help.book.isImage
import io.legado.app.help.book.simulatedTotalChapterNum
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.model.ReadBook
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.nio.charset.Charset
import java.time.LocalDate
import kotlin.math.max

@Parcelize
@TypeConverters(Book.Converters::class)
@Entity(
    tableName = "books",
    indices = [Index(value = ["name", "author"], unique = true)]
)
data class Book(
    // Detail page Url (Local source stores full path)
    @PrimaryKey
    @ColumnInfo(defaultValue = "")
    override var bookUrl: String = "",
    // Catalog page Url (toc=table of Contents)
    @ColumnInfo(defaultValue = "")
    var tocUrl: String = "",
    // Source URL (Default BookType.local)
    @ColumnInfo(defaultValue = BookType.localTag)
    var origin: String = BookType.localTag,
    //Source name or local book filename
    @ColumnInfo(defaultValue = "")
    var originName: String = "",
    // Book title (from source)
    @ColumnInfo(defaultValue = "")
    override var name: String = "",
    // Author name (from source)
    @ColumnInfo(defaultValue = "")
    override var author: String = "",
    // Category info (from source)
    override var kind: String? = null,
    // Category info (user modified)
    var customTag: String? = null,
    // Cover Url (from source)
    var coverUrl: String? = null,
    // Cover Url (user modified)
    var customCoverUrl: String? = null,
    // Intro content (from source)
    var intro: String? = null,
    // Intro content (user modified)
    var customIntro: String? = null,
    // Custom charset name (Local books only)
    var charset: String? = null,
    // Type, see BookType
    @ColumnInfo(defaultValue = "0")
    var type: Int = BookType.text,
    // Custom group index
    @ColumnInfo(defaultValue = "0")
    var group: Long = 0,
    // Latest chapter title
    var latestChapterTitle: String? = null,
    // Latest chapter title update time
    @ColumnInfo(defaultValue = "0")
    var latestChapterTime: Long = System.currentTimeMillis(),
    // Time of last book info update
    @ColumnInfo(defaultValue = "0")
    var lastCheckTime: Long = System.currentTimeMillis(),
    // Count of new chapters found last time
    @ColumnInfo(defaultValue = "0")
    var lastCheckCount: Int = 0,
    // Total book chapters
    @ColumnInfo(defaultValue = "0")
    var totalChapterNum: Int = 0,
    // Current chapter name
    var durChapterTitle: String? = null,
    // Current chapter index
    @ColumnInfo(defaultValue = "0")
    var durChapterIndex: Int = 0,
    // Current reading progress (index position of first line char)
    @ColumnInfo(defaultValue = "0")
    var durChapterPos: Int = 0,
    // Time of last book read (open content time)
    @ColumnInfo(defaultValue = "0")
    var durChapterTime: Long = System.currentTimeMillis(),
    //Word count
    override var wordCount: String? = null,
    // Update book info when refreshing bookshelf
    @ColumnInfo(defaultValue = "1")
    var canUpdate: Boolean = true,
    // Manual sort
    @ColumnInfo(defaultValue = "0")
    var order: Int = 0,
    //Source sort
    @ColumnInfo(defaultValue = "0")
    var originOrder: Int = 0,
    // Custom book variable info (for source rule book info retrieval)
    override var variable: String? = null,
    //Reading settings
    var readConfig: ReadConfig? = null,
    //Sync time
    @ColumnInfo(defaultValue = "0")
    var syncTime: Long = 0L
) : Parcelable, BaseBook {

    override fun equals(other: Any?): Boolean {
        if (other is Book) {
            return other.bookUrl == bookUrl
        }
        return false
    }

    override fun hashCode(): Int {
        return bookUrl.hashCode()
    }

    @delegate:Transient
    @delegate:Ignore
    @IgnoredOnParcel
    override val variableMap: HashMap<String, String> by lazy {
        GSON.fromJsonObject<HashMap<String, String>>(variable).getOrNull() ?: hashMapOf()
    }

    @Ignore
    @IgnoredOnParcel
    override var infoHtml: String? = null

    @Ignore
    @IgnoredOnParcel
    override var tocHtml: String? = null

    @Ignore
    @IgnoredOnParcel
    var downloadUrls: List<String>? = null

    @Ignore
    @IgnoredOnParcel
    private var folderName: String? = null

    @get:Ignore
    @IgnoredOnParcel
    val lastChapterIndex get() = totalChapterNum - 1

    fun getRealAuthor() = author.replace(AppPattern.authorRegex, "")

    fun getUnreadChapterNum() = max(simulatedTotalChapterNum() - durChapterIndex - 1, 0)

    fun getDisplayCover() = if (customCoverUrl.isNullOrEmpty()) coverUrl else customCoverUrl

    fun getDisplayIntro() = if (customIntro.isNullOrEmpty()) intro else customIntro

    //When custom intro needs auto update, update intro then call upCustomIntro()
    @Suppress("unused")
    fun upCustomIntro() {
        customIntro = intro
    }

    fun fileCharset(): Charset {
        return charset(charset ?: "UTF-8")
    }

    @IgnoredOnParcel
    val config: ReadConfig
        get() {
            if (readConfig == null) {
                readConfig = ReadConfig()
            }
            return readConfig!!
        }

    fun setReverseToc(reverseToc: Boolean) {
        config.reverseToc = reverseToc
    }

    fun getReverseToc(): Boolean {
        return config.reverseToc
    }

    fun setUseReplaceRule(useReplaceRule: Boolean) {
        config.useReplaceRule = useReplaceRule
    }

    fun getUseReplaceRule(): Boolean {
        val useReplaceRule = config.useReplaceRule
        if (useReplaceRule != null) {
            return useReplaceRule
        }
        //Image source epub local default disable purification
        if (isImage || isEpub) {
            return false
        }
        return AppConfig.replaceEnableDefault
    }

    fun setReSegment(reSegment: Boolean) {
        config.reSegment = reSegment
    }

    fun getReSegment(): Boolean {
        return config.reSegment
    }

    fun setPageAnim(pageAnim: Int?) {
        config.pageAnim = pageAnim
    }

    fun getPageAnim(): Int {
        var pageAnim = config.pageAnim
            ?: if (isImage) PageAnim.scrollPageAnim else ReadBookConfig.pageAnim
        if (pageAnim < 0) {
            pageAnim = ReadBookConfig.pageAnim
        }
        return pageAnim
    }

    fun setImageStyle(imageStyle: String?) {
        config.imageStyle = imageStyle
    }

    fun getImageStyle(): String? {
        return config.imageStyle
    }

    fun setTtsEngine(ttsEngine: String?) {
        config.ttsEngine = ttsEngine
    }

    fun getTtsEngine(): String? {
        return config.ttsEngine
    }

    fun setSplitLongChapter(limitLongContent: Boolean) {
        config.splitLongChapter = limitLongContent
    }

    fun getSplitLongChapter(): Boolean {
        return config.splitLongChapter
    }

    // readSimulating setter and getter
    fun setReadSimulating(readSimulating: Boolean) {
        config.readSimulating = readSimulating
    }

    fun getReadSimulating(): Boolean {
        return config.readSimulating
    }

    // startDate setter and getter
    fun setStartDate(startDate: LocalDate?) {
        config.startDate = startDate
    }

    fun getStartDate(): LocalDate? {
        if (!config.readSimulating || config.startDate == null) {
            return LocalDate.now()
        }
        return config.startDate
    }

    // startChapter setter and getter
    fun setStartChapter(startChapter: Int) {
        config.startChapter = startChapter
    }

    fun getStartChapter(): Int {
        if (config.readSimulating) return config.startChapter ?: 0
        return this.durChapterIndex
    }

    // dailyChapters setter and getter
    fun setDailyChapters(dailyChapters: Int) {
        config.dailyChapters = dailyChapters
    }

    fun getDailyChapters(): Int {
        return config.dailyChapters
    }

    fun getDelTag(tag: Long): Boolean {
        return config.delTag and tag == tag
    }

    fun addDelTag(tag: Long) {
        config.delTag = config.delTag and tag
    }

    fun removeDelTag(tag: Long) {
        config.delTag = config.delTag and tag.inv()
    }

    fun getFolderName(): String {
        folderName?.let {
            return it
        }
        //Prevent title too long, take 9 chars
        folderName = getFolderNameNoCache()
        return folderName!!
    }

    fun toSearchBook() = SearchBook(
        name = name,
        author = author,
        kind = kind,
        bookUrl = bookUrl,
        origin = origin,
        originName = originName,
        type = type,
        wordCount = wordCount,
        latestChapterTitle = latestChapterTitle,
        coverUrl = coverUrl,
        intro = intro,
        tocUrl = tocUrl,
        originOrder = originOrder,
        variable = variable
    ).apply {
        this.infoHtml = this@Book.infoHtml
        this.tocHtml = this@Book.tocHtml
    }

    /**
     * 迁移旧的书籍的一些信息到新的书籍中
     */
    fun migrateTo(newBook: Book, toc: List<BookChapter>): Book {
        newBook.durChapterIndex = BookHelp
            .getDurChapter(durChapterIndex, durChapterTitle, toc, totalChapterNum)
        newBook.durChapterTitle = toc[newBook.durChapterIndex].getDisplayTitle(
            ContentProcessor.get(newBook.name, newBook.origin).getTitleReplaceRules(),
            getUseReplaceRule()
        )
        newBook.durChapterPos = durChapterPos
        newBook.durChapterTime = durChapterTime
        newBook.group = group
        newBook.order = order
        newBook.customCoverUrl = customCoverUrl
        newBook.customIntro = customIntro
        newBook.customTag = customTag
        newBook.canUpdate = canUpdate
        newBook.readConfig = readConfig
        return newBook
    }

    fun createBookMark(): Bookmark {
        return Bookmark(
            bookName = name,
            bookAuthor = author,
        )
    }

    fun save() {
        if (appDb.bookDao.has(bookUrl)) {
            appDb.bookDao.update(this)
        } else {
            appDb.bookDao.insert(this)
        }
    }

    fun delete() {
        if (ReadBook.book?.bookUrl == bookUrl) {
            ReadBook.book = null
        }
        appDb.bookDao.delete(this)
    }

    @Suppress("ConstPropertyName")
    companion object {
        const val hTag = 2L
        const val rubyTag = 4L
        const val imgStyleDefault = "DEFAULT"
        const val imgStyleFull = "FULL"
        const val imgStyleText = "TEXT"
        const val imgStyleSingle = "SINGLE"
    }

    @Parcelize
    data class ReadConfig(
        var reverseToc: Boolean = false,
        var pageAnim: Int? = null,
        var reSegment: Boolean = false,
        var imageStyle: String? = null,
        var useReplaceRule: Boolean? = null,// Content uses purification replacement rules
        var delTag: Long = 0L,//Remove tags
        var ttsEngine: String? = null,
        var splitLongChapter: Boolean = true,
        var readSimulating: Boolean = false,
        var startDate: LocalDate? = null,
        var startChapter: Int? = null,     // User set start chapter
        var dailyChapters: Int = 3    // User set daily update chapter count
    ) : Parcelable

    class Converters {

        @TypeConverter
        fun readConfigToString(config: ReadConfig?): String = GSON.toJson(config)

        @TypeConverter
        fun stringToReadConfig(json: String?) = GSON.fromJsonObject<ReadConfig>(json).getOrNull()
    }
}