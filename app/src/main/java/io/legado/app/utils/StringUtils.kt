package io.legado.app.utils

import android.annotation.SuppressLint
import android.text.TextUtils.isEmpty
import android.util.Base64
import io.legado.app.R
import splitties.init.appCtx
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.abs


@Suppress("unused", "MemberVisibilityCanBePrivate")
object StringUtils {
    private const val HOUR_OF_DAY = 24
    private const val DAY_OF_YESTERDAY = 2
    private const val TIME_UNIT = 60
    private val ChnMap = chnMap
    private val wordCountFormatter by lazy {
        DecimalFormat("#.#")
    }

    private val chnMap: HashMap<Char, Int>
        get() {
            val map = HashMap<Char, Int>()
            var cnStr = "零一二三四五六七八九十"
            var c = cnStr.toCharArray()
            for (i in 0..10) {
                map[c[i]] = i
            }
            cnStr = "〇壹贰叁肆伍陆柒捌玖拾"
            c = cnStr.toCharArray()
            for (i in 0..10) {
                map[c[i]] = i
            }
            map['两'] = 2
            map['百'] = 100
            map['佰'] = 100
            map['千'] = 1000
            map['仟'] = 1000
            map['万'] = 10000
            map['亿'] = 100000000
            return map
        }

    /**
     * Convert date to yesterday, today, tomorrow
     */
    fun dateConvert(source: String, pattern: String): String {
        val format = SimpleDateFormat(pattern, Locale.getDefault())
        val calendar = Calendar.getInstance()
        kotlin.runCatching {
            val date = format.parse(source) ?: return ""
            val curTime = calendar.timeInMillis
            calendar.time = date
            // Convert MILLISEC to sec
            val difSec = abs((curTime - date.time) / 1000)
            val difMin = difSec / 60
            val difHour = difMin / 60
            val difDate = difHour / 60
            val oldHour = calendar.get(Calendar.HOUR)
            // If no time
            if (oldHour == 0) {
                // Compare dates: yesterday, today and tomorrow
                return when {
                    difDate == 0L -> appCtx.getString(R.string.today)
                    difDate < DAY_OF_YESTERDAY -> appCtx.getString(R.string.yesterday)
                    else -> {
                        @SuppressLint("SimpleDateFormat")
                        val convertFormat = SimpleDateFormat("yyyy-MM-dd")
                        convertFormat.format(date)
                    }
                }
            }

            return when {
                difSec < TIME_UNIT -> appCtx.getString(R.string.seconds_ago, difSec.toString())
                difMin < TIME_UNIT -> appCtx.getString(R.string.minutes_ago, difMin.toString())
                difHour < HOUR_OF_DAY -> appCtx.getString(R.string.hours_ago, difHour.toString())
                difDate < DAY_OF_YESTERDAY -> appCtx.getString(R.string.yesterday)
                else -> {
                    @SuppressLint("SimpleDateFormat")
                    val convertFormat = SimpleDateFormat("yyyy-MM-dd")
                    convertFormat.format(date)
                }
            }
        }.onFailure {
            it.printOnDebug()
        }
        return ""
    }

    /**
     * First letter to uppercase
     */
    @SuppressLint("DefaultLocale")
    fun toFirstCapital(str: String): String {
        return str.substring(0, 1).uppercase(Locale.getDefault()) + str.substring(1)
    }

    /**
     * Convert half-width characters to full-width characters
     */
    fun halfToFull(input: String): String {
        val c = input.toCharArray()
        for (i in c.indices) {
            if (c[i].code == 32)
            // Half-width space
            {
                c[i] = 12288.toChar()
                continue
            }
            // Filter symbols that don't need conversion based on actual situation
            //if (c[i] == 46) // Half-width dot, do not convert
            // continue;

            if (c[i].code in 33..126)
            // Convert other symbols to full-width
                c[i] = (c[i].code + 65248).toChar()
        }
        return String(c)
    }

    /**
     * Full-width characters to half-width characters
     */
    fun fullToHalf(input: String): String {
        val c = input.toCharArray()
        for (i in c.indices) {
            if (c[i].code == 12288)
            // Full-width space
            {
                c[i] = 32.toChar()
                continue
            }

            if (c[i].code in 65281..65374)
                c[i] = (c[i].code - 65248).toChar()
        }
        return String(c)
    }

    /**
     * Chinese uppercase number to int
     */
    fun chineseNumToInt(chNum: String): Int {
        var result = 0
        var tmp = 0
        var billion = 0
        val cn = chNum.toCharArray()

        // "One Zero Two Five" format
        if (cn.size > 1 && chNum.matches("^[〇零一二三四五六七八九壹贰叁肆伍陆柒捌玖]$".toRegex())) {
            for (i in cn.indices) {
                cn[i] = (48 + ChnMap[cn[i]]!!).toChar()
            }
            return Integer.parseInt(String(cn))
        }

        // "One Thousand Zero Twenty Five", "One Thousand Two" format
        return kotlin.runCatching {
            for (i in cn.indices) {
                val tmpNum = ChnMap[cn[i]]!!
                when {
                    tmpNum == 100000000 -> {
                        result += tmp
                        result *= tmpNum
                        billion = billion * 100000000 + result
                        result = 0
                        tmp = 0
                    }

                    tmpNum == 10000 -> {
                        result += tmp
                        result *= tmpNum
                        tmp = 0
                    }

                    tmpNum >= 10 -> {
                        if (tmp == 0)
                            tmp = 1
                        result += tmpNum * tmp
                        tmp = 0
                    }

                    else -> {
                        tmp = if (i >= 2 && i == cn.size - 1 && ChnMap[cn[i - 1]]!! > 10)
                            tmpNum * ChnMap[cn[i - 1]]!! / 10
                        else
                            tmp * 10 + tmpNum
                    }
                }
            }
            result += tmp + billion
            result
        }.getOrDefault(-1)
    }

    /**
     * String to int
     */
    fun stringToInt(str: String?): Int {
        if (str != null) {
            val num = fullToHalf(str).replace("\\s+".toRegex(), "")
            return kotlin.runCatching {
                Integer.parseInt(num)
            }.getOrElse {
                chineseNumToInt(num)
            }
        }
        return -1
    }

    /**
     * Check if contains number
     */
    fun isContainNumber(company: String): Boolean {
        val p = Pattern.compile("[0-9]+")
        val m = p.matcher(company)
        return m.find()
    }

    /**
     * Check if numeric
     */
    fun isNumeric(str: String): Boolean {
        val pattern = Pattern.compile("-?[0-9]+")
        val isNum = pattern.matcher(str)
        return isNum.matches()
    }

    fun wordCountFormat(words: Int): String {
        var wordsS = ""
        if (words > 0) {
            if (words > 10000) {
                val df = wordCountFormatter
                wordsS = df.format(words * 1.0f / 10000f.toDouble()) + appCtx.getString(R.string.ten_thousand_words)
            } else {
                wordsS = words.toString() + appCtx.getString(R.string.word_unit)
            }
        }
        return wordsS
    }

    fun wordCountFormat(wc: String?): String {
        if (wc == null) return ""
        var wordsS = ""
        if (isNumeric(wc)) {
            val words: Int = wc.toInt()
            if (words > 0) {
                if (words > 10000) {
                    val df = wordCountFormatter
                    wordsS = df.format(words * 1.0f / 10000f.toDouble()) + appCtx.getString(R.string.ten_thousand_words)
                } else {
                    wordsS = words.toString() + appCtx.getString(R.string.word_unit)
                }
            }
        } else {
            wordsS = wc
        }
        return wordsS
    }

    /**
     * Efficient trim (ASCII check, includes full-width space)
     */
    fun trim(s: String): String {
        if (isEmpty(s)) return ""
        var start = 0
        val len = s.length
        var end = len - 1
        while (start < end && (s[start].code <= 0x20 || s[start] == '　')) {
            ++start
        }
        while (start < end && (s[end].code <= 0x20 || s[end] == '　')) {
            --end
        }
        ++end
        return if (start > 0 || end < len) s.substring(start, end) else s
    }

    /**
     * 重复字符串
     */
    fun repeat(str: String, n: Int): String {
        val stringBuilder = StringBuilder()
        for (i in 0 until n) {
            stringBuilder.append(str)
        }
        return stringBuilder.toString()
    }

    /**
     * Remove UTF header
     */
    fun removeUTFCharacters(data: String?): String? {
        if (data == null) return null
        val p = Pattern.compile("\\\\u(\\p{XDigit}{4})")
        val m = p.matcher(data)
        val buf = StringBuffer(data.length)
        while (m.find()) {
            val ch = Integer.parseInt(m.group(1)!!, 16).toChar().toString()
            m.appendReplacement(buf, Matcher.quoteReplacement(ch))
        }
        m.appendTail(buf)
        return buf.toString()
    }

    /**
     * Compress string
     */
    fun compress(str: String): Result<String> {
        return kotlin.runCatching {
            if (str.isEmpty()) {
                return@runCatching str
            }
            val out = ByteArrayOutputStream()
            var gzip: GZIPOutputStream? = null
            return@runCatching try {
                gzip = GZIPOutputStream(out)
                gzip.write(str.toByteArray())
                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            } finally {
                gzip?.runCatching {
                    close()
                }
                out.runCatching {
                    close()
                }
            }
        }
    }

    /**
     * Unzip string
     */
    @Throws(IOException::class)
    fun unCompress(str: String): Result<String> {
        return kotlin.runCatching {
            val outputStream = ByteArrayOutputStream()
            var inputStream: ByteArrayInputStream? = null
            var ginZip: GZIPInputStream? = null
            return@runCatching try {
                val compressed = Base64.decode(str, Base64.NO_WRAP)
                inputStream = ByteArrayInputStream(compressed)
                ginZip = GZIPInputStream(inputStream)
                ginZip.copyTo(outputStream)
                outputStream.toString()
            } finally {
                ginZip?.runCatching {
                    close()
                }
                inputStream?.runCatching {
                    close()
                }
                outputStream.runCatching {
                    close()
                }
            }
        }
    }

}
