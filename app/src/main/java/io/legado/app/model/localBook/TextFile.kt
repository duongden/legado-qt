package io.legado.app.model.localBook

import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.exception.EmptyFileException
import io.legado.app.help.DefaultData
import io.legado.app.help.book.isLocalModified
import io.legado.app.utils.EncodingDetect
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.StringUtils
import io.legado.app.utils.Utf8BomUtils
import java.io.FileNotFoundException
import java.nio.charset.Charset
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.math.min

class TextFile(private var book: Book) {

    @Suppress("ConstPropertyName")
    companion object {
        private val padRegex = "^[\\n\\s]+".toRegex()
        private const val txtBufferSize = 8 * 1024 * 1024
        private var textFile: TextFile? = null

        @Synchronized
        private fun getTextFile(book: Book): TextFile {
            if (textFile == null || textFile?.book?.bookUrl != book.bookUrl || book.isLocalModified()) {
                textFile = TextFile(book)
                return textFile!!
            }
            textFile?.book = book
            return textFile!!
        }

        @Throws(FileNotFoundException::class)
        fun getChapterList(book: Book): ArrayList<BookChapter> {
            return getTextFile(book).getChapterList()
        }

        @Synchronized
        @Throws(FileNotFoundException::class)
        fun getContent(book: Book, bookChapter: BookChapter): String {
            return getTextFile(book).getContent(bookChapter)
        }

        fun clear() {
            textFile = null
        }

    }

    private val blank: Byte = 0x0a

    //Default get length from file
    private val bufferSize = 512000

    //Max length per chapter when no title
    private val maxLengthWithNoToc = 10 * 1024

    //Use regex to split catalog, max allowed length per chapter
    private val maxLengthWithToc = 102400

    private var charset: Charset = book.fileCharset()

    private var txtBuffer: ByteArray? = null
    private var bufferStart = -1L
    private var bufferEnd = -1L

    /**
     * Get catalog
     */
    @Throws(FileNotFoundException::class, SecurityException::class, EmptyFileException::class)
    fun getChapterList(): ArrayList<BookChapter> {
        val modified = book.isLocalModified()
        if (book.charset == null || book.tocUrl.isBlank() || modified) {
            LocalBook.getBookInputStream(book).use { bis ->
                val buffer = ByteArray(bufferSize)
                val length = bis.read(buffer)
                if (length == -1) throw EmptyFileException("Unexpected Empty Txt File")
                if (book.charset.isNullOrBlank() || modified) {
                    book.charset = EncodingDetect.getEncode(buffer.copyOf(length))
                }
                charset = book.fileCharset()
                if (book.tocUrl.isBlank() || modified) {
                    val blockContent = String(buffer, 0, length, charset)
                    book.tocUrl = getTocRule(blockContent)?.pattern() ?: ""
                }
            }
        }
        val (toc, wordCount) = analyze(book.tocUrl.toPattern(Pattern.MULTILINE))
        book.wordCount = StringUtils.wordCountFormat(wordCount)
        toc.forEachIndexed { index, bookChapter ->
            bookChapter.index = index
            bookChapter.bookUrl = book.bookUrl
            bookChapter.url = MD5Utils.md5Encode16(book.originName + index + bookChapter.title)
        }
        return toc
    }

    fun getContent(chapter: BookChapter): String {
        val start = chapter.start!!
        val end = chapter.end!!
        if (txtBuffer == null || start > bufferEnd || end < bufferStart) {
            LocalBook.getBookInputStream(book).use { bis ->
                bufferStart = txtBufferSize * (start / txtBufferSize)
                txtBuffer = ByteArray(min(txtBufferSize, bis.available() - bufferStart.toInt()))
                bufferEnd = bufferStart + txtBuffer!!.size
                bis.skip(bufferStart)
                bis.read(txtBuffer)
            }
        }

        val count = (end - start).toInt()
        val buffer = ByteArray(count)

        @Suppress("ConvertTwoComparisonsToRangeCheck")
        if (start < bufferEnd && end > bufferEnd || start < bufferStart && end > bufferStart) {
            /** Chapter content at buffer boundary */
            LocalBook.getBookInputStream(book).use { bis ->
                bis.skip(start)
                bis.read(buffer)
            }
        } else {
            /** Chapter content inside buffer */
            txtBuffer!!.copyInto(
                buffer,
                0,
                (start - bufferStart).toInt(),
                (end - bufferStart).toInt()
            )
        }

        return String(buffer, charset)
            .substringAfter(chapter.title)
            .replace(padRegex, "　　")
    }

    /**
     * Parse catalog by rule
     */
    private fun analyze(pattern: Pattern?): Pair<ArrayList<BookChapter>, Int> {
        if (pattern == null || pattern.pattern().isNullOrEmpty()) {
            return analyze()
        }
        val toc = arrayListOf<BookChapter>()
        var bookWordCount = 0
        LocalBook.getBookInputStream(book).use { bis ->
            var blockContent: String
            //Load chapter
            var curOffset: Long = 0
            //Read length
            var length: Int
            var lastChapterWordCount = 0
            val buffer = ByteArray(bufferSize)
            var bufferStart = 3
            bis.read(buffer, 0, 3)
            if (Utf8BomUtils.hasBom(buffer)) {
                bufferStart = 0
                curOffset = 3
            }
            //Get file data to buffer until no data
            while (bis.read(
                    buffer, bufferStart, bufferSize - bufferStart
                ).also { length = it } > 0
            ) {
                var end = bufferStart + length
                if (end == bufferSize) {
                    for (i in bufferStart + length - 1 downTo 0) {
                        if (buffer[i] == blank) {
                            end = i
                            break
                        }
                    }
                }
                //Convert data to String, cannot exceed length
                blockContent = String(buffer, 0, end, charset)
                buffer.copyInto(buffer, 0, end, bufferStart + length)
                bufferStart = bufferStart + length - end
                length = end
                //Pointer to String used in current Block
                var seekPos = 0
                //Perform regex match
                val matcher: Matcher = pattern.matcher(blockContent)
                //If corresponding chapter exists
                while (matcher.find()) { //Get start position of matched char in string
                    val chapterStart = matcher.start()
                    //Get chapter content
                    val chapterContent = blockContent.substring(seekPos, chapterStart)
                    val chapterLength = chapterContent.toByteArray(charset).size.toLong()
                    val lastStart = toc.lastOrNull()?.start ?: curOffset
                    if (book.getSplitLongChapter() && curOffset + chapterLength - lastStart > maxLengthWithToc) {
                        toc.lastOrNull()?.let {
                            it.end = it.start
                            it.tag = null
                        }
                        //Split chapter if too many words
                        val lastTitle = toc.lastOrNull()?.title
                        val lastTitleLength = lastTitle?.toByteArray(charset)?.size ?: 0
                        val (chapters, wordCount) = analyze(
                            lastStart + lastTitleLength, curOffset + chapterLength
                        )
                        lastTitle?.let {
                            chapters.forEachIndexed { index, bookChapter ->
                                bookChapter.title = "$lastTitle(${index + 1})"
                            }
                        }
                        toc.addAll(chapters)
                        bookWordCount += wordCount
                        //Create current chapter
                        val curChapter = BookChapter()
                        curChapter.title = matcher.group()
                        curChapter.start = curOffset + chapterLength
                        curChapter.end = curChapter.start
                        toc.add(curChapter)
                        lastChapterWordCount = 0
                    } else if (seekPos == 0 && chapterStart != 0) {
                        /**
                         * If seekPos == 0 && chapterStart != 0 it means there is content before current block
                         * First case must be prologue, second case is content of previous chapter
                         */
                        if (toc.isEmpty()) { //If no current chapter, it's prologue
                            //Add intro
                            if (chapterContent.isNotBlank()) {
                                val qyChapter = BookChapter()
                                qyChapter.title = "前言"
                                qyChapter.start = curOffset
                                qyChapter.end = curOffset + chapterLength
                                qyChapter.wordCount =
                                    StringUtils.wordCountFormat(chapterContent.length)
                                toc.add(qyChapter)
                                book.intro = if (chapterContent.length <= 500) {
                                    chapterContent
                                } else {
                                    chapterContent.substring(0, 500)
                                }
                            }
                            //Create current chapter
                            val curChapter = BookChapter()
                            curChapter.title = matcher.group()
                            curChapter.start = curOffset + chapterLength
                            curChapter.end = curChapter.start
                            toc.add(curChapter)
                        } else { //Else remaining content of prev chapter after block split
                            //Get previous chapter
                            val lastChapter = toc.last()
                            lastChapter.isVolume =
                                chapterContent.substringAfter(lastChapter.title).isBlank()
                            //Add current paragraph to previous chapter
                            lastChapter.end = lastChapter.end!! + chapterLength
                            lastChapterWordCount += chapterContent.length
                            lastChapter.wordCount =
                                StringUtils.wordCountFormat(lastChapterWordCount)
                            //Create current chapter
                            val curChapter = BookChapter()
                            curChapter.title = matcher.group()
                            curChapter.start = lastChapter.end
                            curChapter.end = curChapter.start
                            toc.add(curChapter)
                        }
                        bookWordCount += chapterContent.length
                        lastChapterWordCount = 0
                    } else {
                        if (toc.isNotEmpty()) { //Get chapter content
                            //Get previous chapter
                            val lastChapter = toc.last()
                            lastChapter.isVolume =
                                chapterContent.substringAfter(lastChapter.title).isBlank()
                            lastChapter.end =
                                lastChapter.start!! + chapterLength
                            lastChapter.wordCount =
                                StringUtils.wordCountFormat(chapterContent.length)
                            //Create current chapter
                            val curChapter = BookChapter()
                            curChapter.title = matcher.group()
                            curChapter.start = lastChapter.end
                            curChapter.end = curChapter.start
                            toc.add(curChapter)
                        } else { //Create chapter if not exists
                            val curChapter = BookChapter()
                            curChapter.title = matcher.group()
                            curChapter.start = curOffset
                            curChapter.end = curOffset
                            curChapter.wordCount =
                                StringUtils.wordCountFormat(chapterContent.length)
                            toc.add(curChapter)
                        }
                        bookWordCount += chapterContent.length
                        lastChapterWordCount = 0
                    }
                    //Set pointer offset
                    seekPos += chapterContent.length
                }
                val wordCount = blockContent.length - seekPos
                bookWordCount += wordCount
                lastChapterWordCount += wordCount
                //block offset point
                curOffset += length.toLong()
                //Set previous chapter end
                toc.lastOrNull()?.let {
                    it.end = curOffset
                    it.wordCount = StringUtils.wordCountFormat(lastChapterWordCount)
                }
            }
            toc.lastOrNull()?.let { chapter ->
                //Split chapter if too many words
                if (book.getSplitLongChapter() && chapter.end!! - chapter.start!! > maxLengthWithToc) {
                    val end = chapter.end!!
                    chapter.end = chapter.start
                    chapter.tag = null
                    val lastTitle = chapter.title
                    val lastTitleLength = lastTitle.toByteArray(charset).size
                    val (chapters, _) = analyze(
                        chapter.start!! + lastTitleLength, end
                    )
                    chapters.forEachIndexed { index, bookChapter ->
                        bookChapter.title = "$lastTitle(${index + 1})"
                    }
                    toc.addAll(chapters)
                }
            }
        }
        System.gc()
        System.runFinalization()
        return toc to bookWordCount
    }

    /**
     * Split catalog without rule
     */
    private fun analyze(
        fileStart: Long = 0L, fileEnd: Long = Long.MAX_VALUE
    ): Pair<ArrayList<BookChapter>, Int> {
        val toc = arrayListOf<BookChapter>()
        var bookWordCount = 0
        LocalBook.getBookInputStream(book).use { bis ->
            //block count
            var blockPos = 0
            //Load chapter
            var curOffset: Long = 0
            var chapterPos = 0
            //Read length
            var length = 0
            var lastChapterWordCount = 0
            val buffer = ByteArray(bufferSize)
            var bufferStart = 3
            if (fileStart == 0L) {
                bis.read(buffer, 0, 3)
                if (Utf8BomUtils.hasBom(buffer)) {
                    bufferStart = 0
                    curOffset = 3
                }
            } else {
                bis.skip(fileStart)
                curOffset = fileStart
                bufferStart = 0
            }
            //Get file data to buffer until no data
            while (fileEnd - curOffset - bufferStart > 0 && bis.read(
                    buffer, bufferStart, min(
                        (bufferSize - bufferStart).toLong(), fileEnd - curOffset - bufferStart
                    ).toInt()
                ).also { length = it } > 0
            ) {
                blockPos++
                //Chapter offset in buffer
                var chapterOffset = 0
                //Current remaining allocatable length
                length += bufferStart
                var strLength = length
                //Chapter split position
                chapterPos = 0
                while (strLength > 0) {
                    chapterPos++
                    //Length exceeds one chapter?
                    if (strLength > maxLengthWithNoToc) { //Chapter end point in buffer
                        var end = length
                        //Find newline as end point
                        for (i in chapterOffset + maxLengthWithNoToc until length) {
                            if (buffer[i] == blank) {
                                end = i
                                break
                            }
                        }
                        val content = String(buffer, chapterOffset, end - chapterOffset, charset)
                        bookWordCount += content.length
                        lastChapterWordCount = content.length
                        val chapter = BookChapter()
                        chapter.title = "第${blockPos}章($chapterPos)"
                        chapter.start = toc.lastOrNull()?.end ?: curOffset
                        chapter.end = chapter.start!! + end - chapterOffset
                        chapter.wordCount = StringUtils.wordCountFormat(content.length)
                        toc.add(chapter)
                        //Subtract allocated length
                        strLength -= (end - chapterOffset)
                        //Set offset position
                        chapterOffset = end
                    } else {
                        buffer.copyInto(buffer, 0, length - strLength, length)
                        length -= strLength
                        bufferStart = strLength
                        strLength = 0
                    }
                }
                //block offset point
                curOffset += length.toLong()
            }
            //Set end chapter
            val content = String(buffer, 0, bufferStart, charset)
            bookWordCount += content.length
            if (bufferStart > 100 || toc.isEmpty()) {
                val chapter = BookChapter()
                chapter.title = "第${blockPos}章(${chapterPos})"
                chapter.start = toc.lastOrNull()?.end ?: curOffset
                chapter.end = chapter.start!! + bufferStart
                chapter.wordCount = StringUtils.wordCountFormat(content.length)
                toc.add(chapter)
            } else {
                val wordCount = lastChapterWordCount + content.length
                toc.lastOrNull()?.let {
                    it.end = it.end!! + bufferStart
                    it.wordCount = StringUtils.wordCountFormat(wordCount)
                }
            }
        }
        return toc to bookWordCount
    }

    /**
     * Get suitable catalog rule
     */
    private fun getTocRule(content: String): Pattern? {
        val rules = getTocRules().reversed()
        var maxNum = 1
        var tocPattern: Pattern? = null
        for (tocRule in rules) {
            val pattern = try {
                tocRule.rule.toPattern(Pattern.MULTILINE)
            } catch (e: PatternSyntaxException) {
                AppLog.put("TXT目录规则正则语法错误:${tocRule.name}\n$e", e)
                continue
            }
            val matcher = pattern.matcher(content)
            var start = 0
            var num = 0
            while (matcher.find()) {
                if (start == 0 || matcher.start() - start > 1000) {
                    num++
                    start = matcher.end()
                }
            }
            if (num >= maxNum) {
                maxNum = num
                tocPattern = pattern
            }
        }
        return tocPattern
    }

    /**
     * Get enabled catalog rule
     */
    private fun getTocRules(): List<TxtTocRule> {
        var rules = appDb.txtTocRuleDao.enabled
        if (appDb.txtTocRuleDao.count == 0) {
            rules = DefaultData.txtTocRules.apply {
                appDb.txtTocRuleDao.insert(*this.toTypedArray())
            }.filter {
                it.enable
            }
        }
        return rules
    }

}