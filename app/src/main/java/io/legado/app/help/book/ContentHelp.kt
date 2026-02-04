package io.legado.app.help.book

import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

@Suppress("SameParameterValue", "RegExpRedundantEscape")
object ContentHelp {

    /**
     * Paragraph re-layout entry. Input full content, join broken segments, then re-split each paragraph.
     *
     * @param content     Body
     * @param chapterName Title
     * @return
     */
    fun reSegment(content: String, chapterName: String): String {
        var content1 = content
        val dict = makeDict(content1)
        var p = content1
            .replace("&quot;".toRegex(), "“")
            .replace("[:：]['\"‘”“]+".toRegex(), "：“")
            .replace("[\"”“]+\\s*[\"”“][\\s\"”“]*".toRegex(), "”\n“")
            .split("\n(\\s*)".toRegex()).toTypedArray()

        //Init StringBuilder length, redundant based on original content length
        var buffer = StringBuilder((content1.length * 1.15).toInt())
        //          Chapter text format is Chapter Title - Empty Line - First Paragraph, so skip first line when processing paragraph.
        buffer.append("  ")
        if (chapterName.trim { it <= ' ' } != p[0].trim { it <= ' ' }) {
            // Remove in-paragraph spaces. Unicode 3000 Ideographic Space (CJK symbols/punctuation), not included in \s
            buffer.append(p[0].replace("[\u3000\\s]+".toRegex(), ""))
        }

        //If original text has segmentation error, re-glue paragraphs
        for (i in 1 until p.size) {
            if (match(MARK_SENTENCES_END, buffer.last())
                || (match(MARK_QUOTATION_RIGHT, buffer.last())
                        && match(MARK_SENTENCES_END, buffer[buffer.lastIndex - 1]))
            ) {
                buffer.append("\n")
            }
            // Spaces should not exist except at paragraph start
            // Remove in-paragraph spaces. Unicode 3000 Ideographic Space (CJK symbols/punctuation), not included in \s
            buffer.append(p[i].replace("[\u3000\\s]".toRegex(), ""))
        }
        //     Pre-segmentation pre-processing
        //         Handle ”“ as ”\n“.
        //         Handle ”。“ as ”。\n“. Ignore “？” “！” cases.
        // Handle ”。xxx as ”。\n xxx
        p = buffer.toString()
            .replace("[\"”“]+\\s*[\"”“]+".toRegex(), "”\n“")
            .replace("[\"”“]+(？。！?!~)[\"”“]+".toRegex(), "”$1\n“")
            .replace("[\"”“]+(？。！?!~)([^\"”“])".toRegex(), "”$1\n$2")
            .replace(
                "([问说喊唱叫骂道着答])[\\.。]".toRegex(),
                "$1。\n"
            )
            .split("\n".toRegex()).toTypedArray()
        buffer = StringBuilder((content1.length * 1.15).toInt())
        for (s in p) {
            buffer.append("\n")
            buffer.append(findNewLines(s, dict))
        }
        buffer = reduceLength(buffer)
        content1 = (buffer.toString() //         Process chapter header spaces and newlines
            .replaceFirst("^\\s+".toRegex(), "")
            .replace("\\s*[\"”“]+\\s*[\"”“][\\s\"”“]*".toRegex(), "”\n“")
            .replace("[:：][”“\"\\s]+".toRegex(), "：“")
            .replace("\n[\"“”]([^\n\"“”]+)([,:，：][\"”“])([^\n\"“”]+)".toRegex(), "\n$1：“$3")
            .replace("\n(\\s*)".toRegex(), "\n"))
        return content1
    }

    /**
     * Forced segmentation, reduce sentences in paragraph
     * If 2 consecutive quoted paragraphs have no prompt, enter dialogue mode. Force split after last quote.
     * If content inside quotes > 5 sentences, quote state might be wrong, random split.
     * If content outside quotes > 3 sentences, random split.
     *
     * @param str
     * @return
     */
    private fun reduceLength(str: StringBuilder): StringBuilder {
        val p = str.toString().split("\n".toRegex()).toTypedArray()
        val l = p.size
        val b = BooleanArray(l)
        for (i in 0 until l) {
            b[i] = p[i].matches(PARAGRAPH_DIAGLOG)
        }
        var dialogue = 0
        for (i in 0 until l) {
            if (b[i]) {
                if (dialogue < 0) dialogue = 1 else if (dialogue < 2) dialogue++
            } else {
                if (dialogue > 1) {
                    p[i] = splitQuote(p[i])
                    dialogue--
                } else if (dialogue > 0 && i < l - 2) {
                    if (b[i + 1]) p[i] = splitQuote(p[i])
                }
            }
        }
        val string = StringBuilder()
        for (i in 0 until l) {
            string.append('\n')
            string.append(p[i])
            //System.out.print(" "+b[i]);
        }
        //System.out.println(" " + str);
        return string
    }

    // Force split entry into dialogue mode, paragraph not forming “xxx” format
    private fun splitQuote(str: String): String {
        val length = str.length
        if (length < 3) return str
        if (match(MARK_QUOTATION, str[0])) {
            val i = seekIndex(str, MARK_QUOTATION, 1, length - 2, true) + 1
            if (i > 1) if (!match(MARK_QUOTATION_BEFORE, str[i - 1])) {
                return "${str.take(i)}\n${str.substring(i)}"
            }
        } else if (match(MARK_QUOTATION, str[length - 1])) {
            val i = length - 1 - seekIndex(str, MARK_QUOTATION, 1, length - 2, false)
            if (i > 1) {
                if (!match(MARK_QUOTATION_BEFORE, str[i - 1])) {
                    return "${str.take(i)}\n${str.substring(i)}"
                }
            }
        }
        return str
    }

    /**
     * Probability of inserting newline is 1 / gain, larger gain means less likely to insert newline
     * @return
     */
    private fun forceSplit(
        str: String,
        offset: Int,
        min: Int,
        gain: Int,
        tigger: Int
    ): ArrayList<Int> {
        val result = ArrayList<Int>()
        val arrayEnd = seekIndexes(str, MARK_SENTENCES_END_P, 0, str.length - 2, true)
        val arrayMid = seekIndexes(str, MARK_SENTENCES_MID, 0, str.length - 2, true)
        if (arrayEnd.size < tigger && arrayMid.size < tigger * 3) return result
        var j = 0
        var i = min
        while (i < arrayEnd.size) {
            var k = 0
            while (j < arrayMid.size) {
                if (arrayMid[j] < arrayEnd[i]) k++
                j++
            }
            if (Math.random() * gain < 0.8 + k / 2.5) {
                result.add(arrayEnd[i] + offset)
                i = max(i + min, i)
            }
            i++
        }
        return result
    }

    // Re-segment content. Input str already pre-split by newline
    private fun findNewLines(str: String, dict: List<String>): String {
        val string = StringBuilder(str)
        // Mark each quote position in string. Specifically, lists in quotes treated as one pair. e.g. "Pot", "Bowl" treated as "Pot, Bowl", avoiding mis-segmentation.
        val arrayQuote: MutableList<Int> = ArrayList()
        //  Mark position to insert newline, int is insert position (str char index)
        var insN = ArrayList<Int>()

        //mod[i] marks if each segment of str is inside/outside quotes. Range: State of str.substring( array_quote.get(i), array_quote.get(i+1) ).
        //Length: array_quote.size(), but initial estimate missing, space for time
        //0 unknown, positive inside quotes, negative outside.
        //If adjacent two marks are +1, add 1 quote.
        //No segmentation inside quotes
        val mod = IntArray(str.length)
        var waitClose = false
        for (i in str.indices) {
            val c = str[i]
            if (match(MARK_QUOTATION, c)) {
                val size = arrayQuote.size

                //        Merge “xxx”、“yy” into “xxx_yy” for processing
                if (size > 0) {
                    val quotePre = arrayQuote[size - 1]
                    if (i - quotePre == 2) {
                        var remove = false
                        if (waitClose) {
                            if (match(",，、/", str[i - 1])) {
                                // Consider special case "and"
                                remove = true
                            }
                        } else if (match(",，、/和与或", str[i - 1])) {
                            remove = true
                        }
                        if (remove) {
                            string.setCharAt(i, '“')
                            string.setCharAt(i - 2, '”')
                            arrayQuote.removeAt(size - 1)
                            mod[size - 1] = 1
                            mod[size] = -1
                            continue
                        }
                    }
                }
                arrayQuote.add(i)

                //  Mark xxx: “xxx”
                if (i > 1) {
                    // Previous char of current speech's opening quote
                    val charB1 = str[i - 1]
                    // Previous char of last speech's opening quote
                    var charB2 = 0.toChar()
                    if (match(MARK_QUOTATION_BEFORE, charB1)) {
                        // If not first quote, find previous break, segment
                        if (arrayQuote.size > 1) {
                            val lastQuote = arrayQuote[arrayQuote.size - 2]
                            var p = 0
                            if (charB1 == ',' || charB1 == '，') {
                                if (arrayQuote.size > 2) {
                                    p = arrayQuote[arrayQuote.size - 3]
                                    if (p > 0) {
                                        charB2 = str[p - 1]
                                    }
                                }
                            }
                            //if(char_b2=='.' || char_b2=='。')
                            if (match(MARK_SENTENCES_END_P, charB2)) {
                                insN.add(p - 1)
                            } else if (!match("的", charB2)) {
                                val lastEnd = seekLast(str, MARK_SENTENCES_END, i, lastQuote)
                                if (lastEnd > 0) insN.add(lastEnd) else insN.add(lastQuote)
                            }
                        }
                        waitClose = true
                        mod[size] = 1
                        if (size > 0) {
                            mod[size - 1] = -1
                            if (size > 1) {
                                mod[size - 2] = 1
                            }
                        }
                    } else if (waitClose) {
                        run {
                            waitClose = false
                            insN.add(i)
                        }
                    }
                }
            }
        }
        val size = arrayQuote.size


        //Mark loop state, are quotes before this position paired
        var opend = false
        if (size > 0) {
            //1st traverse array_quote, make element value non-zero
            for (i in 0 until size) {
                if (mod[i] > 0) {
                    opend = true
                } else if (mod[i] < 0) {
                    //2 consecutive backticks mean conflict, force previous to opening quote
                    if (!opend) {
                        if (i > 0) mod[i] = 3
                    }
                    opend = false
                } else {
                    opend = !opend
                    if (opend) mod[i] = 2 else mod[i] = -2
                }
            }
            //        Correction, trailing must close quote
            if (opend) {
                if (arrayQuote[size - 1] - string.length > -3) {
                    //if((match(MARK_QUOTATION,string.charAt(string.length()-1)) || match(MARK_QUOTATION,string.charAt(string.length()-2)))){
                    if (size > 1) mod[size - 2] = 4
                    // i<size, so no need to check size>=1
                    mod[size - 1] = -4
                } else if (!match(MARK_SENTENCES_SAY, string[string.length - 2])) string.append(
                    "”"
                )
            }


            //2nd loop, mod[i] neg to pos, if prev char is sentence end, insert newline
            var loop2Mod1 = -1 //State of content following previous quote
            var loop2Mod2: Int //State of content following current quote
            var i = 0
            var j = arrayQuote[0] - 1 //Index of char before current quote
            if (j < 0) {
                i = 1
                loop2Mod1 = 0
            }
            while (i < size) {
                j = arrayQuote[i] - 1
                loop2Mod2 = mod[i]
                if (loop2Mod1 < 0 && loop2Mod2 > 0) {
                    if (match(MARK_SENTENCES_END, string[j])) insN.add(j)
                }
                loop2Mod1 = loop2Mod2
                i++
            }
        }

        //3rd loop, match and insert newline.
        //"xxxx" xxxx。\n xxx“xxxx”
        //Not implemented

        // Use dictionary to verify ins_n, avoid unnecessary newlines.
        // formatting list not available, cannot solve "xx", "xx""xx" newline insertion issue
        val insN1 = ArrayList<Int>()
        for (i in insN) {
            if (match("\"'”“", string[i])) {
                val start: Int = seekLast(
                    str,
                    "\"'”“",
                    i - 1,
                    i - WORD_MAX_LENGTH
                )
                if (start > 0) {
                    val word = str.substring(start + 1, i)
                    if (dict.contains(word)) {
                        //System.out.println("Dict verify Skip\tins_n=" + i + "  word=" + word);
                        //If inside quotes is dict entry, no newline after (front needs no optimization)
                        continue
                    } else {
                        //System.out.println("Dict verify Insert\tins_n=" + i + "  word=" + word);
                        if (match("的地得", str[start])) {
                            //xx's "xx", no newline inserted after (no optimization needed before)
                            continue
                        }
                    }
                }
            }
            insN1.add(i)
        }
        insN = insN1

//        Randomly insert newline at sentence end
        insN = ArrayList(HashSet(insN))
        insN.sort()
        run {
            var subs: String
            var j = 0
            var progress = 0
            var nextLine = -1
            if (insN.isNotEmpty()) nextLine = insN[j]
            var gain = 3
            var min = 0
            var trigger = 2
            for (i in arrayQuote.indices) {
                val qutoe = arrayQuote[i]
                if (qutoe > 0) {
                    gain = 4
                    min = 2
                    trigger = 4
                } else {
                    gain = 3
                    min = 0
                    trigger = 2
                }

//            Insert newline before quote interleaved with content
                while (j < insN.size) {

//                If next newline is before current quote, need processing. If adjacent to current quote, consider inserting quote
                    if (nextLine >= qutoe) break
                    nextLine = insN[j]
                    if (progress < nextLine) {
                        subs = string.substring(progress, nextLine)
                        insN.addAll(forceSplit(subs, progress, min, gain, trigger))
                        progress = nextLine + 1
                    }
                    j++
                }
                if (progress < qutoe) {
                    subs = string.substring(progress, qutoe + 1)
                    insN.addAll(forceSplit(subs, progress, min, gain, trigger))
                    progress = qutoe + 1
                }
            }
            while (j < insN.size) {
                nextLine = insN[j]
                if (progress < nextLine) {
                    subs = string.substring(progress, nextLine)
                    insN.addAll(forceSplit(subs, progress, min, gain, trigger))
                    progress = nextLine + 1
                }
                j++
            }
            if (progress < string.length) {
                subs = string.substring(progress, string.length)
                insN.addAll(forceSplit(subs, progress, min, gain, trigger))
            }
        }

//     Correct quote direction based on paragraph state, calculate position to insert quote
//     If opend == 0, need to insert '"' before array_quote.get(i)
        val insQuote = BooleanArray(size)
        opend = false
        for (i in 0 until size) {
            val p = arrayQuote[i]
            if (mod[i] > 0) {
                string.setCharAt(p, '“')
                if (opend) insQuote[i] = true
                opend = true
            } else if (mod[i] < 0) {
                string.setCharAt(p, '”')
                opend = false
            } else {
                opend = !opend
                if (opend) string.setCharAt(p, '“') else string.setCharAt(p, '”')
            }
        }
        insN = ArrayList(HashSet(insN))
        insN.sort()

//     Complete string concatenation (copy from string, insert quotes and newlines
//     If opend == 0, need to insert '"' before array_quote.get(i)
//     ins_n Insert newline. Array value indicates position to insert newline
        val buffer = StringBuilder((str.length * 1.15).toInt())
        var j = 0
        var progress = 0
        var nextLine = -1
        if (insN.isNotEmpty()) nextLine = insN[j]
        for (i in arrayQuote.indices) {
            val quote = arrayQuote[i]

//            Insert newline before quote interleaved with content
            while (j < insN.size) {

//                If next newline is before current quote, need processing. If adjacent to current quote, consider inserting quote
                if (nextLine >= quote) break
                nextLine = insN[j]
                buffer.append(string, progress, nextLine + 1)
                buffer.append('\n')
                progress = nextLine + 1
                j++
            }
            if (progress < quote) {
                buffer.append(string, progress, quote + 1)
                progress = quote + 1
            }
            if (insQuote[i] && buffer.length > 2) {
                if (buffer[buffer.length - 1] == '\n') buffer.append('“') else buffer.insert(
                    buffer.length - 1,
                    "”\n"
                )
            }
        }
        while (j < insN.size) {
            nextLine = insN[j]
            if (progress <= nextLine) {
                buffer.append(string, progress, nextLine + 1)
                buffer.append('\n')
                progress = nextLine + 1
            }
            j++
        }
        if (progress < string.length) {
            buffer.append(string, progress, string.length)
        }
        return buffer.toString()
    }

    /**
     * Extract quoted content that appears more than once from string as dictionary
     *
     * @param str
     * @return Entry list
     */
    private fun makeDict(str: String): List<String> {

        // No punctuation inside quotes
        val patten = Pattern.compile(
            """
          (?<=["'”“])([^
          \p{P}]{1,$WORD_MAX_LENGTH})(?=["'”“])
          """.trimIndent()
        )
        //Pattern patten = Pattern.compile("(?<=[\"'”“])([^\n\"'”“]{1,16})(?=[\"'”“])");
        val matcher = patten.matcher(str)
        val cache: MutableList<String> = ArrayList()
        val dict: MutableList<String> = ArrayList()
        while (matcher.find()) {
            val word = matcher.group()
            if (cache.contains(word)) {
                if (!dict.contains(word)) dict.add(word)
            } else cache.add(word)
        }
        return dict
    }

    /**
     * Calc pos of each char matching dict
     *
     * @param str     String
     * @param key     Dict
     * @param from    Start index
     * @param to      End index
     * @param inOrder Match forward
     * @return ArrayList<Int> of distances
     */
    private fun seekIndexes(
        str: String,
        key: String,
        from: Int,
        to: Int,
        inOrder: Boolean
    ): ArrayList<Int> {
        val list = ArrayList<Int>()
        if (str.length - from < 1) return list
        var i = 0
        if (from > 0) i = from
        var t = str.length
        if (to > 0) t = min(t, to)
        var c: Char
        while (i < t) {
            c = if (inOrder) str[i] else str[str.length - i - 1]
            if (key.indexOf(c) != -1) {
                if (list.isNotEmpty() && i - list.last() == 1) {
                    list[list.lastIndex] = i
                } else {
                    list.add(i)
                }
            }
            i++
        }
        return list
    }

    /**
     * Calc last match pos
     *
     * @param str  String
     * @param key  Dict
     * @param from Start index
     * @param to   End index
     * @return Pos
     */
    private fun seekLast(str: String, key: String, from: Int, to: Int): Int {
        if (str.length - from < 1) return -1
        var i = str.lastIndex
        if (from < i && i > 0) i = from
        var t = 0
        if (to > 0) t = to
        var c: Char
        while (i > t) {
            c = str[i]
            if (key.indexOf(c) != -1) {
                return i
            }
            i--
        }
        return -1
    }

    /**
     * Calc shortest distance between string and dict chars
     *
     * @param str     String
     * @param key     Dict
     * @param from    Start index
     * @param to      End index
     * @param inOrder Match forward
     * @return Shortest distance
     */
    private fun seekIndex(str: String, key: String, from: Int, to: Int, inOrder: Boolean): Int {
        if (str.length - from < 1) return -1
        var i = 0
        if (from > 0) i = from
        var t = str.length
        if (to > 0) t = min(t, to)
        var c: Char
        while (i < t) {
            c = if (inOrder) str[i] else str[str.length - i - 1]
            if (key.indexOf(c) != -1) {
                return i
            }
            i++
        }
        return -1
    }

    /* Search for quotation marks and fragment. Processed common cases 1, 2, and 5.
    Reference encyclopedia entry [Quotation Mark#Application Example] to correct and sentence the content of quotation marks.
    1. Full quote of speech, with punctuation inside the back quote.
    2. Partial quote, with punctuation outside the back quote.
    3. When quoting directly paragraph by paragraph, the middle paragraph only uses quotation marks at the beginning of the paragraph, but not at the end. However, orthodox literature is not considered.
    4. When quotation marks are used inside quotation marks, double quotation marks are used on the outside and single quotation marks on the inside. No need to consider for now.
    5. Irony and emphasis, with no punctuation around.
    */

    //  Sentence end punctuation. Because quotes may be misjudged, excluding quotes.
    private const val MARK_SENTENCES_END = "？。！?!~"
    private const val MARK_SENTENCES_END_P = ".？。！?!~"

    //  Mid-sentence punctuation, as some sites write “，” as ".", treat English period as mid-sentence punctuation
    private const val MARK_SENTENCES_MID = ".，、,—…"
    private const val MARK_SENTENCES_SAY = "问说喊唱叫骂道着答"

    //  XXX said: “” colon
    private const val MARK_QUOTATION_BEFORE = "，：,:"

    //  Quote
    private const val MARK_QUOTATION = "\"“”"
    private const val MARK_QUOTATION_RIGHT = "\"”"
    private val PARAGRAPH_DIAGLOG = "^[\"”“][^\"”“]+[\"”“]$".toRegex()

    //  Limit dictionary length
    private const val WORD_MAX_LENGTH = 16

    private fun match(rule: String, chr: Char): Boolean {
        return rule.indexOf(chr) != -1
    }
}