package io.legado.app.ui.book.read.page.provider

import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.text.Layout
import android.text.TextPaint
import java.util.WeakHashMap
import kotlin.math.max

/**
 * Line break layout processing for Chinese - by hoodie13
 * Because StaticLayout punctuation handling does not suit Chinese habits, inherit Layout
 * */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class ZhLayout(
    text: CharSequence,
    textPaint: TextPaint,
    width: Int,
    words: List<String>,
    widths: List<Float>,
    indentSize: Int
) : Layout(text, textPaint, width, Alignment.ALIGN_NORMAL, 0f, 0f) {
    companion object {
        private val postPanc = hashSetOf(
            "，", "。", "：", "？", "！", "、", "”", "’", "）", "》", "}",
            "】", ")", ">", "]", "}", ",", ".", "?", "!", ":", "」", "；", ";"
        )
        private val prePanc = hashSetOf("“", "（", "《", "【", "‘", "‘", "(", "<", "[", "{", "「")
        private val cnCharWidthCache = WeakHashMap<Paint, Float>()
    }

    private val defaultCapacity = 10
    var lineStart = IntArray(defaultCapacity)
    var lineWidth = FloatArray(defaultCapacity)
    private var lineCount = 0
    private val curPaint = textPaint
    private val cnCharWidth = cnCharWidthCache[textPaint]
        ?: getDesiredWidth("我", textPaint).also {
            cnCharWidthCache[textPaint] = it
        }

    enum class BreakMod { NORMAL, BREAK_ONE_CHAR, BREAK_MORE_CHAR, CPS_1, CPS_2, CPS_3, }
    class Locate {
        var start: Float = 0f
        var end: Float = 0f
    }

    class Interval {
        var total: Float = 0f
        var single: Float = 0f
    }

    init {
        var line = 0
        var lineW = 0f
        var cwPre = 0f
        var length = 0
        words.forEachIndexed { index, s ->
            val cw = widths[index]
            var breakMod: BreakMod
            var breakLine = false
            lineW += cw
            var offset = 0f
            var breakCharCnt = 0

            if (lineW > width) {
                /*Prohibit punctuation at line end handling*/
                breakMod = if (index >= 1 && isPrePanc(words[index - 1])) {
                    if (index >= 2 && isPrePanc(words[index - 2])) BreakMod.CPS_2//Exception if another prohibited leading punctuation follows
                    else BreakMod.BREAK_ONE_CHAR //No exception scenario
                }
                /*Prohibit punctuation at line start handling*/
                else if (isPostPanc(words[index])) {
                    if (index >= 1 && isPostPanc(words[index - 1])) BreakMod.CPS_1//Exception if another prohibited leading punctuation follows, but three consecutive trailing punctuations usage not common
                    else if (index >= 2 && isPrePanc(words[index - 2])) BreakMod.CPS_3//Exception if another prohibited leading punctuation follows
                    else BreakMod.BREAK_ONE_CHAR //No exception scenario
                } else {
                    BreakMod.NORMAL //No exception scenario
                }

                /*Judge special cases not solved by above logic*/
                var reCheck = false
                var breakIndex = 0
                if (breakMod == BreakMod.CPS_1 &&
                    (inCompressible(widths[index]) || inCompressible(widths[index - 1]))
                ) reCheck = true
                if (breakMod == BreakMod.CPS_2 &&
                    (inCompressible(widths[index - 1]) || inCompressible(widths[index - 2]))
                ) reCheck = true
                if (breakMod == BreakMod.CPS_3 &&
                    (inCompressible(widths[index]) || inCompressible(widths[index - 2]))
                ) reCheck = true
                if (breakMod > BreakMod.BREAK_MORE_CHAR
                    && index < words.lastIndex && isPostPanc(words[index + 1])
                ) reCheck = true

                /*Special punctuation difficult to guarantee display effect, so ignore interval, directly find split char meeting condition*/
                var breakLength = 0
                if (reCheck && index > 2) {
                    val startPos = if (line == 0) indentSize else getLineStart(line)
                    breakMod = BreakMod.NORMAL
                    for (i in (index) downTo 1 + startPos) {
                        if (i == index) {
                            breakIndex = 0
                            cwPre = 0f
                        } else {
                            breakIndex++
                            breakLength += words[i].length
                            cwPre += widths[i]
                        }
                        if (!isPostPanc(words[i]) && !isPrePanc(words[i - 1])) {
                            breakMod = BreakMod.BREAK_MORE_CHAR
                            break
                        }
                    }
                }

                when (breakMod) {
                    BreakMod.NORMAL -> {//Mode 0 Normal break
                        offset = cw
                        lineStart[line + 1] = length
                        breakCharCnt = 1
                    }

                    BreakMod.BREAK_ONE_CHAR -> {//Mode 1 Current line shift down 1 char
                        offset = cw + cwPre
                        lineStart[line + 1] = length - words[index - 1].length
                        breakCharCnt = 2
                    }

                    BreakMod.BREAK_MORE_CHAR -> {//Mode 2 Current line shift down multiple chars
                        offset = cw + cwPre
                        lineStart[line + 1] = length - breakLength
                        breakCharCnt = breakIndex + 1
                    }

                    BreakMod.CPS_1 -> {//Mode 3 Two trailing punctuations compression
                        offset = 0f
                        lineStart[line + 1] = length + s.length
                        breakCharCnt = 0
                    }

                    BreakMod.CPS_2 -> { //Mode 4 Leading compression+Leading compression+Char
                        offset = 0f
                        lineStart[line + 1] = length + s.length
                        breakCharCnt = 0
                    }

                    BreakMod.CPS_3 -> {//Mode 5 Leading compression+Char+Trailing compression
                        offset = 0f
                        lineStart[line + 1] = length + s.length
                        breakCharCnt = 0
                    }
                }
                breakLine = true
            }

            /*Line break when current line is full*/
            if (breakLine) {
                lineWidth[line] = lineW - offset
                lineW = offset
                addLineArray(++line)
            }
            /*Reached last character*/
            if ((words.lastIndex) == index) {
                if (!breakLine) {
                    offset = 0f
                    lineStart[line + 1] = length + s.length
                    lineWidth[line] = lineW - offset
                    lineW = offset
                    addLineArray(++line)
                }
                /*Full line break, paragraph end, and need to shift char down, special case needs extra line*/
                else if (breakCharCnt > 0) {
                    lineStart[line + 1] = lineStart[line] + breakCharCnt
                    lineWidth[line] = lineW
                    addLineArray(++line)
                }
            }
            length += s.length
            cwPre = cw
        }

        lineCount = line

    }

    private fun addLineArray(line: Int) {
        if (lineStart.size <= line + 1) {
            lineStart = lineStart.copyOf(line + defaultCapacity)
            lineWidth = lineWidth.copyOf(line + defaultCapacity)
        }
    }

    private fun isPostPanc(string: String): Boolean {
        return postPanc.contains(string)
    }

    private fun isPrePanc(string: String): Boolean {
        return prePanc.contains(string)
    }

    private fun inCompressible(width: Float): Boolean {
        return width < cnCharWidth
    }

    private val gap = (cnCharWidth / 12.75).toFloat()
    private fun getPostPancOffset(string: String): Float {
        val textRect = Rect()
        curPaint.getTextBounds(string, 0, 1, textRect)
        return max(textRect.left.toFloat() - gap, 0f)
    }

    private fun getPrePancOffset(string: String): Float {
        val textRect = Rect()
        curPaint.getTextBounds(string, 0, 1, textRect)
        val d = max(cnCharWidth - textRect.right.toFloat() - gap, 0f)
        return cnCharWidth / 2 - d
    }

    fun getDesiredWidth(string: String, paint: TextPaint): Float {
        var width = paint.measureText(string)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            width += paint.letterSpacing * paint.textSize
        }
        return width
    }

    override fun getLineCount(): Int {
        return lineCount
    }

    override fun getLineTop(line: Int): Int {
        return 0
    }

    override fun getLineDescent(line: Int): Int {
        return 0
    }

    override fun getLineStart(line: Int): Int {
        return lineStart[line]
    }

    override fun getParagraphDirection(line: Int): Int {
        return 0
    }

    override fun getLineContainsTab(line: Int): Boolean {
        return true
    }

    override fun getLineDirections(line: Int): Directions? {
        return null
    }

    override fun getTopPadding(): Int {
        return 0
    }

    override fun getBottomPadding(): Int {
        return 0
    }

    override fun getLineWidth(line: Int): Float {
        return lineWidth[line]
    }

    override fun getEllipsisStart(line: Int): Int {
        return 0
    }

    override fun getEllipsisCount(line: Int): Int {
        return 0
    }

}