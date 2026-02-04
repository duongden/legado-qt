package io.legado.app.model.analyzeRule

import androidx.annotation.Keep
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.jsoup.select.Collector
import org.jsoup.select.Elements
import org.jsoup.select.Evaluator
import org.seimicrawler.xpath.JXNode

/**
 * Created by GKF on 2018/1/25.
 * 书源规则解析
 */
@Keep
class AnalyzeByJSoup(doc: Any) {

    companion object {
        private val nullSet = setOf(null)
    }

    private var element: Element = parse(doc)

    private fun parse(doc: Any): Element {
        if (doc is Element) {
            return doc
        }
        if (doc is JXNode) {
            return if (doc.isElement) doc.asElement() else Jsoup.parse(doc.toString())
        }
        kotlin.runCatching {
            if (doc.toString().startsWith("<?xml", true)) {
                return Jsoup.parse(doc.toString(), Parser.xmlParser())
            }
        }
        return Jsoup.parse(doc.toString())
    }

    /**
     * Get list
     */
    internal fun getElements(rule: String) = getElements(element, rule)

    /**
     * Merge content list to get content
     */
    internal fun getString(ruleStr: String): String? {
        if (ruleStr.isEmpty()) {
            return null
        }
        val list = getStringList(ruleStr)
        if (list.isEmpty()) {
            return null
        }
        if (list.size == 1) {
            return list.first()
        }
        return list.joinToString("\n")
    }


    /**
     * Get a string
     */
    internal fun getString0(ruleStr: String) =
        getStringList(ruleStr).let { if (it.isEmpty()) "" else it[0] }

    /**
     * Get all content list
     */
    internal fun getStringList(ruleStr: String): List<String> {

        val textS = ArrayList<String>()

        if (ruleStr.isEmpty()) return textS

        //Split rule
        val sourceRule = SourceRule(ruleStr)

        if (sourceRule.elementsRule.isEmpty()) {

            textS.add(element.data() ?: "")

        } else {

            val ruleAnalyzes = RuleAnalyzer(sourceRule.elementsRule)
            val ruleStrS = ruleAnalyzes.splitRule("&&", "||", "%%")

            val results = ArrayList<List<String>>()
            for (ruleStrX in ruleStrS) {

                val temp: ArrayList<String>? =
                    if (sourceRule.isCss) {
                        val lastIndex = ruleStrX.lastIndexOf('@')
                        getResultLast(
                            element.select(ruleStrX.substring(0, lastIndex)),
                            ruleStrX.substring(lastIndex + 1)
                        )
                    } else {
                        getResultList(ruleStrX)
                    }

                if (!temp.isNullOrEmpty()) {
                    results.add(temp)
                    if (ruleAnalyzes.elementsType == "||") break
                }
            }
            if (results.size > 0) {
                if ("%%" == ruleAnalyzes.elementsType) {
                    for (i in results[0].indices) {
                        for (temp in results) {
                            if (i < temp.size) {
                                textS.add(temp[i])
                            }
                        }
                    }
                } else {
                    for (temp in results) {
                        textS.addAll(temp)
                    }
                }
            }
        }
        return textS
    }

    /**
     * Get Elements
     */
    private fun getElements(temp: Element?, rule: String): Elements {

        if (temp == null || rule.isEmpty()) return Elements()

        val elements = Elements()

        val sourceRule = SourceRule(rule)
        val ruleAnalyzes = RuleAnalyzer(sourceRule.elementsRule)
        val ruleStrS = ruleAnalyzes.splitRule("&&", "||", "%%")

        val elementsList = ArrayList<Elements>()
        if (sourceRule.isCss) {
            for (ruleStr in ruleStrS) {
                val tempS = temp.select(ruleStr)
                elementsList.add(tempS)
                if (tempS.size > 0 && ruleAnalyzes.elementsType == "||") {
                    break
                }
            }
        } else {
            for (ruleStr in ruleStrS) {

                val rsRule = RuleAnalyzer(ruleStr)

                rsRule.trim()  // Trim "@" or whitespace before current rule

                val rs = rsRule.splitRule("@")

                val el = if (rs.size > 1) {
                    val el = Elements()
                    el.add(temp)
                    for (rl in rs) {
                        val es = Elements()
                        for (et in el) {
                            es.addAll(getElements(et, rl))
                        }
                        el.clear()
                        el.addAll(es)
                    }
                    el
                } else ElementsSingle().getElementsSingle(temp, ruleStr)

                elementsList.add(el)
                if (el.size > 0 && ruleAnalyzes.elementsType == "||") {
                    break
                }
            }
        }
        if (elementsList.size > 0) {
            if ("%%" == ruleAnalyzes.elementsType) {
                for (i in 0 until elementsList[0].size) {
                    for (es in elementsList) {
                        if (i < es.size) {
                            elements.add(es[i])
                        }
                    }
                }
            } else {
                for (es in elementsList) {
                    elements.addAll(es)
                }
            }
        }
        return elements
    }

    /**
     * Get content list
     */
    private fun getResultList(ruleStr: String): ArrayList<String>? {

        if (ruleStr.isEmpty()) return null

        var elements = Elements()

        elements.add(element)

        val rule = RuleAnalyzer(ruleStr) //Create parser

        rule.trim() //Trim leading redundant symbols

        val rules = rule.splitRule("@") // Split into list

        val last = rules.size - 1
        for (i in 0 until last) {
            val es = Elements()
            for (elt in elements) {
                es.addAll(ElementsSingle().getElementsSingle(elt, rules[i]))
            }
            elements.clear()
            elements = es
        }
        return if (elements.isEmpty()) null else getResultLast(elements, rules[last])
    }

    /**
     * Get content by last rule
     */
    private fun getResultLast(elements: Elements, lastRule: String): ArrayList<String> {
        val textS = ArrayList<String>()
        when (lastRule) {
            "text" -> for (element in elements) {
                val text = element.text()
                if (text.isNotEmpty()) {
                    textS.add(text)
                }
            }

            "textNodes" -> for (element in elements) {
                val tn = arrayListOf<String>()
                val contentEs = element.textNodes()
                for (item in contentEs) {
                    val text = item.text().trim { it <= ' ' }
                    if (text.isNotEmpty()) {
                        tn.add(text)
                    }
                }
                if (tn.isNotEmpty()) {
                    textS.add(tn.joinToString("\n"))
                }
            }

            "ownText" -> for (element in elements) {
                val text = element.ownText()
                if (text.isNotEmpty()) {
                    textS.add(text)
                }
            }

            "html" -> {
                elements.select("script").remove()
                elements.select("style").remove()
                val html = elements.outerHtml()
                if (html.isNotEmpty()) {
                    textS.add(html)
                }
            }

            "all" -> textS.add(elements.outerHtml())
            else -> for (element in elements) {

                val url = element.attr(lastRule)

                if (url.isBlank() || textS.contains(url)) continue

                textS.add(url)
            }
        }
        return textS
    }

    /**
     * 1. Support original reading syntax, ':' separated index, ! or . means filter mode, index can be negative
     * e.g. tag.div.-1:10:2 or tag.div!0:3
     *
     * 2. Support [] index syntax similar to jsonPath
     * Format like [it,it,...] or [!it,it,...] where [! start means filter mode is exclude, it is single index or range.
     * Range format is start:end or start:end:step, where start 0 can be omitted, end -1 can be omitted.
     * Index, range ends and step all support negative numbers
     * e.g. tag.div[-1, 3:-2:-10, 2]
     * Special usage tag.div[-1:0] can reverse list anywhere
     * */
    @Suppress("UNCHECKED_CAST")
    data class ElementsSingle(
        var split: Char = '.',
        var beforeRule: String = "",
        val indexDefault: MutableList<Int> = mutableListOf(),
        val indexes: MutableList<Any> = mutableListOf()
    ) {
        /**
         * Get Elements by a rule
         */
        fun getElementsSingle(temp: Element, rule: String): Elements {

            findIndexSet(rule) //Execute index list processor

            /**
             * Get all elements
             * */
            var elements =
                if (beforeRule.isEmpty()) temp.children() //Allow index as root element, pre-rule empty, effect same as children
                else {
                    val rules = beforeRule.split(".")
                    when (rules[0]) {
                        "children" -> temp.children() //Allow index as root element, pre-rule empty, effect same as children
                        "class" -> temp.getElementsByClass(rules[1])
                        "tag" -> temp.getElementsByTag(rules[1])
                        "id" -> Collector.collect(Evaluator.Id(rules[1]), temp)
                        "text" -> temp.getElementsContainingOwnText(rules[1])
                        else -> temp.select(beforeRule)
                    }
                }

            val len = elements.size
            val lastIndexes = (indexDefault.size - 1).takeIf { it != -1 } ?: (indexes.size - 1)
            val indexSet = mutableSetOf<Int>()

            /**
             * Get unique and non-out-of-bound index collection
             * */
            if (indexes.isEmpty()) for (ix in lastIndexes downTo 0) { //indexes empty, means non-[] style index, collection inserted in reverse, so traverse reverse here to restore order

                val it = indexDefault[ix]
                if (it in 0 until len) indexSet.add(it) //Add positive non-out-of-bound index to collection
                else if (it < 0 && len >= -it) indexSet.add(it + len) //Add negative non-out-of-bound index to collection

            } else for (ix in lastIndexes downTo 0) { //indexes not empty, means [] style index, collection inserted in reverse, so traverse reverse here to restore order

                if (indexes[ix] is Triple<*, *, *>) { //Range
                    val (startX, endX, stepX) = indexes[ix] as Triple<Int?, Int?, Int> //Restore stored type

                    var start = startX ?: 0 // Left end omission means 0
                    if (start < 0) start += len // Convert negative index to positive

                    var end = endX ?: (len - 1) // Right end omission means len - 1
                    if (end < 0) end += len // Convert negative index to positive

                    if ((start < 0 && end < 0) || (start >= len && end >= len)) {
                        // start and end same side bound violation, invalid index
                        continue
                    }

                    if (start >= len) start = len - 1 // Right end violation, set to max index
                    else if (start < 0) start = 0 // Left end violation, set to min index

                    if (end >= len) end = len - 1 // Right end violation, set to max index
                    else if (end < 0) end = 0 // Left end violation, set to min index

                    if (start == end || stepX >= len) { //Ends same, only one number in range. Or interval too large, range effectively only first

                        indexSet.add(start)
                        continue

                    }

                    val step =
                        if (stepX > 0) stepX else if (-stepX < len) stepX + len else 1 //Min positive interval 1

                    //Expand range to collection, allow list reverse.
                    indexSet.addAll(if (end > start) start..end step step else start downTo end step step)

                } else {//Single index

                    val it = indexes[ix] as Int //Restore stored type

                    if (it in 0 until len) indexSet.add(it) //Add positive non-out-of-bound index to collection
                    else if (it < 0 && len >= -it) indexSet.add(it + len) //Add negative non-out-of-bound index to collection

                }

            }

            /**
             * Filter elements based on index collection
             * */
            if (split == '!') { //Exclude

                for (pcInt in indexSet) elements[pcInt] = null

                elements.removeAll(nullSet) //Tested, this works

            } else if (split == '.') { //Select

                val es = Elements()

                for (pcInt in indexSet) es.add(elements[pcInt])

                elements = es

            }

            return elements //Return filter result

        }

        private fun findIndexSet(rule: String) {

            val rus = rule.trim { it <= ' ' }

            var len = rus.length
            var curInt: Int? //Current number
            var curMinus = false //Current number negative?
            val curList = mutableListOf<Int?>() //Current number range
            var l = "" //Temp number string

            val head = rus.last() == ']' //Is normal index syntax

            if (head) { //Normal index syntax [index...]

                len-- //Skip trailing ']'

                while (len-- >= 0) { //Reverse traverse, can be without pre-rule

                    var rl = rus[len]
                    if (rl == ' ') continue //Skip whitespace

                    if (rl in '0'..'9') l = rl + l //Accumulate values to temp string, extract on delimiter
                    else if (rl == '-') curMinus = true
                    else {

                        curInt =
                            if (l.isEmpty()) null else if (curMinus) -l.toInt() else l.toInt() //Current number

                        when (rl) {

                            ':' -> curList.add(curInt) //Range right end or interval

                            else -> {

                                //To ensure search order, range and single index added to same collection
                                if (curList.isEmpty()) {

                                    if (curInt == null) break //Is jsoup selector not index list, break

                                    indexes.add(curInt)
                                } else {

                                    //List last pushed is range right end, if list has 2 items first pushed is interval
                                    indexes.add(
                                        Triple(
                                            curInt,
                                            curList.last(),
                                            if (curList.size == 2) curList.first() else 1
                                        )
                                    )

                                    curList.clear() //Reset temp list, avoid affecting next interval

                                }

                                if (rl == '!') {
                                    split = '!'
                                    do {
                                        rl = rus[--len]
                                    } while (len > 0 && rl == ' ')//Skip all whitespace
                                }

                                if (rl == '[') {
                                    beforeRule = rus.substring(0, len) //Hit index boundary, return result
                                    return
                                }

                                if (rl != ',') break //Non-index structure, break

                            }
                        }

                        l = "" //Clear
                        curMinus = false //Reset
                    }
                }
            } else while (len-- >= 0) { //Reading original syntax, reverse traverse, no pre-rule

                val rl = rus[len]
                if (rl == ' ') continue //Skip whitespace

                if (rl in '0'..'9') l = rl + l //Accumulate values to temp string, extract on delimiter
                else if (rl == '-') curMinus = true
                else {

                    if (rl == '!' || rl == '.' || rl == ':') { //Separator or start char

                        indexDefault.add(if (curMinus) -l.toInt() else l.toInt()) // Append current number to list

                        if (rl != ':') { //rl == '!'  || rl == '.'
                            split = rl
                            beforeRule = rus.substring(0, len)
                            return
                        }

                    } else break //Non-index structure, break loop

                    l = "" //Clear
                    curMinus = false //Reset
                }
            }

            split = ' '
            beforeRule = rus
        }
    }


    internal inner class SourceRule(ruleStr: String) {
        var isCss = false
        var elementsRule: String = if (ruleStr.startsWith("@CSS:", true)) {
            isCss = true
            ruleStr.substring(5).trim { it <= ' ' }
        } else {
            ruleStr
        }
    }

}
