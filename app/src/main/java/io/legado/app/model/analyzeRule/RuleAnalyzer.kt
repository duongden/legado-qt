package io.legado.app.model.analyzeRule

//Common rule split processing
class RuleAnalyzer(data: String, code: Boolean = false) {

    private var queue: String = data //Processed string
    private var pos = 0 //Current processed position
    private var start = 0 //Current field start
    private var startX = 0 //Current rule start

    private var rule = ArrayList<String>()  //Split rule list
    private var step: Int = 0 //Split character length
    var elementsType = "" //Current split string

    fun trim() { // Trim "@" or whitespace before current rule
        if (queue[pos] == '@' || queue[pos] < '!') { //Repeatedly setting start/startX in while slows down, so check if trimming needed first, set start/startX once at end
            pos++
            while (queue[pos] == '@' || queue[pos] < '!') pos++
            start = pos //Start point move
            startX = pos //Rule start point move
        }
    }

    //Reset pos to 0, facilitate reuse
    fun reSetPos() {
        pos = 0
        startX = 0
    }

    /**
     * Pull string from remaining until matching sequence (exclusive)
     * @param seq Search string **Case Sensitive**
     * @return Whether found.
     */
    private fun consumeTo(seq: String): Boolean {
        start = pos //Set processed position as rule start
        val offset = queue.indexOf(seq, pos)
        return if (offset != -1) {
            pos = offset
            true
        } else false
    }

    /**
     * Pull string from remaining until matching sequence (any match in list), or remaining used up.
     * @param seq Match string sequence
     * @return True if success and set interval, otherwise false
     */
    private fun consumeToAny(vararg seq: String): Boolean {

        var pos = pos //Declare new var to record match pos, do not change class pos

        while (pos != queue.length) {

            for (s in seq) {
                if (queue.regionMatches(pos, s, 0, s.length)) {
                    step = s.length //Interval count
                    this.pos = pos //Match success, sync position to class
                    return true //Match return true
                }
            }

            pos++ //Probe one by one
        }
        return false
    }

    /**
     * Pull string from remaining until matching sequence (any match in list), or remaining used up.
     * @param seq Match char sequence
     * @return Match position
     */
    private fun findToAny(vararg seq: Char): Int {

        var pos = pos //Declare new var to record match pos, do not change class pos

        while (pos != queue.length) {

            for (s in seq) if (queue[pos] == s) return pos //Match return position

            pos++ //Probe one by one

        }

        return -1
    }

    /**
     * Extract non-inline code balanced group, escape text exists
     */
    private fun chompCodeBalanced(open: Char, close: Char): Boolean {

        var pos = pos //Declare temp var to record match pos, sync to class pos only on success

        var depth = 0 //Nesting depth
        var otherDepth = 0 //Other symmetric symbol nesting depth

        var inSingleQuote = false //Single quote
        var inDoubleQuote = false //Double quote

        do {
            if (pos == queue.length) break
            val c = queue[pos++]
            if (c != ESC) { //Non-escape char
                if (c == '\'' && !inDoubleQuote) inSingleQuote = !inSingleQuote //Match syntactic single quote
                else if (c == '"' && !inSingleQuote) inDoubleQuote = !inDoubleQuote //Match syntactic double quote

                if (inSingleQuote || inDoubleQuote) continue //Syntax unit not match end, continue next loop

                if (c == '[') depth++ //Start nest one layer
                else if (c == ']') depth-- //Close one layer nesting
                else if (depth == 0) {
                    //Non-default char in default nesting needs no balance, only nest this char when depth 0 (default nesting closed)
                    if (c == open) otherDepth++
                    else if (c == close) otherDepth--
                }

            } else pos++

        } while (depth > 0 || otherDepth > 0) //Pull balanced string

        return if (depth > 0 || otherDepth > 0) false else {
            this.pos = pos //Sync position
            true
        }
    }

    /**
     * Extract rule balanced group. Escape chars invalid in quotes in xpath/jsoup.
     */
    private fun chompRuleBalanced(open: Char, close: Char): Boolean {

        var pos = pos //Declare temp var to record match pos, sync to class pos only on success
        var depth = 0 //Nesting depth
        var inSingleQuote = false //Single quote
        var inDoubleQuote = false //Double quote

        do {
            if (pos == queue.length) break
            val c = queue[pos++]
            if (c == '\'' && !inDoubleQuote) inSingleQuote = !inSingleQuote //Match syntactic single quote
            else if (c == '"' && !inSingleQuote) inDoubleQuote = !inDoubleQuote //Match syntactic double quote

            if (inSingleQuote || inDoubleQuote) continue //Syntax unit not match end, continue next loop
            else if (c == '\\') { //Only escape char outside quotes escapes next char
                pos++
                continue
            }

            if (c == open) depth++ //Start nest one layer
            else if (c == close) depth-- //Close one layer nesting

        } while (depth > 0) //Pull balanced string

        return if (depth > 0) false else {
            this.pos = pos //Sync position
            true
        }
    }

    /**
     * No regex, no slicing until end, no intermediate storage, only mark start/end in sequence, slice at return, efficient fast accurate splitting rule
     * Solve conflict between jsonPath's "&&" "||" and reading rules, and conflicts caused by "&&", "||", "%%", "@" in rules regex or string
     */
    tailrec fun splitRule(vararg split: String): ArrayList<String> { //First segment match, elementsType empty

        if (split.size == 1) {
            elementsType = split[0] //Set split string
            return if (!consumeTo(elementsType)) {
                rule += queue.substring(startX)
                rule
            } else {
                step = elementsType.length //Set separator length
                splitRule()
            } //Recursive match
        } else if (!consumeToAny(* split)) { //Separator not found
            rule += queue.substring(startX)
            return rule
        }

        val end = pos //Record split position
        pos = start //Return to start, start another search

        do {
            val st = findToAny('[', '(') //Find filter position

            if (st == -1) {

                rule = arrayListOf(queue.substring(startX, end)) //Push separated first segment rule to array

                elementsType = queue.substring(end, end + step) //Set composite type
                pos = end + step //Skip separator

                while (consumeTo(elementsType)) { //Loop split rule push to array
                    rule += queue.substring(start, pos)
                    pos += step //Skip separator
                }

                rule += queue.substring(pos) //Push remaining fields to array end

                return rule
            }

            if (st > end) { //Match st1pos first, means split string not in selector, push fields split by separator before selector into array

                rule = arrayListOf(queue.substring(startX, end)) //Push separated first segment rule to array

                elementsType = queue.substring(end, end + step) //Set composite type
                pos = end + step //Skip separator

                while (consumeTo(elementsType) && pos < st) { //Loop split rule push to array
                    rule += queue.substring(start, pos)
                    pos += step //Skip separator
                }

                return if (pos > st) {
                    startX = start
                    splitRule() //First segment matched, current match incomplete, call 2nd stage
                } else { //Executed here means no more separators behind
                    rule += queue.substring(pos) //Push remaining fields to array end
                    rule
                }
            }

            pos = st //Position move to filter
            val next = if (queue[pos] == '[') ']' else ')' //Balanced group end char

            if (!chompBalanced(queue[pos], next)) throw Error(
                queue.substring(0, start) + "后未平衡"
            ) //Pull filter, error if unbalanced

        } while (end > pos)

        start = pos //Set start position for finding filter position

        return splitRule(* split) //Recursive call first segment match
    }

    @JvmName("splitRuleNext")
    private tailrec fun splitRule(): ArrayList<String> { //Second stage match called, elementsType not null (set in first stage), find by elementsType directly, faster than first stage

        val end = pos //Record split position
        pos = start //Return to start, start another search

        do {
            val st = findToAny('[', '(') //Find filter position

            if (st == -1) {

                rule += arrayOf(queue.substring(startX, end)) //Push separated first segment rule to array
                pos = end + step //Skip separator

                while (consumeTo(elementsType)) { //Loop split rule push to array
                    rule += queue.substring(start, pos)
                    pos += step //Skip separator
                }

                rule += queue.substring(pos) //Push remaining fields to array end

                return rule
            }

            if (st > end) { //Match st1pos first, means split string not in selector, push fields split by separator before selector into array

                rule += arrayListOf(queue.substring(startX, end)) //Push separated first segment rule to array
                pos = end + step //Skip separator

                while (consumeTo(elementsType) && pos < st) { //Loop split rule push to array
                    rule += queue.substring(start, pos)
                    pos += step //Skip separator
                }

                return if (pos > st) {
                    startX = start
                    splitRule() //First segment matched, current match incomplete, call 2nd stage
                } else { //Executed here means no more separators behind
                    rule += queue.substring(pos) //Push remaining fields to array end
                    rule
                }
            }

            pos = st //Position move to filter
            val next = if (queue[pos] == '[') ']' else ')' //Balanced group end char

            if (!chompBalanced(queue[pos], next)) throw Error(
                queue.substring(0, start) + "后未平衡"
            ) //Pull filter, error if unbalanced

        } while (end > pos)

        start = pos //Set start position for finding filter position

        return if (!consumeTo(elementsType)) {
            rule += queue.substring(startX)
            rule
        } else splitRule() //Recursive match

    }

    /**
     * Replace inline rule
     * @param inner Start tag, e.g. {$.
     * @param startStep Prefix len not part of rule, e.g. {, so 1
     * @param endStep Suffix len not part of rule
     * @param fr Fn to parse when inline rule found
     *
     * */
    fun innerRule(
        inner: String,
        startStep: Int = 1,
        endStep: Int = 1,
        fr: (String) -> String?
    ): String {
        val st = StringBuilder()

        while (consumeTo(inner)) { //Fetch success return true, pos in ruleAnalyzes moves, else return false, isEmpty true
            val posPre = pos //Record consumeTo match position
            if (chompCodeBalanced('{', '}')) {
                val frv = fr(queue.substring(posPre + startStep, pos - endStep))
                if (!frv.isNullOrEmpty()) {
                    st.append(queue.substring(startX, posPre) + frv) //Push content before inline rule, and string parsed from inline rule
                    startX = pos //Record next rule start
                    continue //Get content success, continue to next inline rule
                }
            }
            pos += inner.length //Pulled field unbalanced, inner is normal string, jump after inner and continue match
        }

        return if (startX == 0) "" else st.apply {
            append(queue.substring(startX))
        }.toString()
    }

    /**
     * Replace inline rule
     * @param fr Fn to parse when inline rule found
     *
     * */
    fun innerRule(
        startStr: String,
        endStr: String,
        fr: (String) -> String?
    ): String {

        val st = StringBuilder()
        while (consumeTo(startStr)) { //Fetch success return true, pos in ruleAnalyzes moves, else return false, isEmpty true
            pos += startStr.length //Skip start string
            val posPre = pos //Record consumeTo match position
            if (consumeTo(endStr)) {
                val frv = fr(queue.substring(posPre, pos))
                st.append(
                    queue.substring(
                        startX,
                        posPre - startStr.length
                    ) + frv
                ) //Push content before inline rule, and string parsed from inline rule
                pos += endStr.length //Skip end string
                startX = pos //Record next rule start
            }
        }

        return if (startX == 0) queue else st.apply {
            append(queue.substring(startX))
        }.toString()
    }

    //Set balanced group function, chompCodeBalanced if json/JS, else chompRuleBalanced
    val chompBalanced = if (code) ::chompCodeBalanced else ::chompRuleBalanced

    companion object {

        /**
         * Escape character
         */
        private const val ESC = '\\'

    }
}