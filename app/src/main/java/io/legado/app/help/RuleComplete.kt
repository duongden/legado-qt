package io.legado.app.help


@Suppress("RegExpRedundantEscape")
object RuleComplete {
    // Needs completion
    private val needComplete = Regex(
        """(?<!(@|/|^|[|%&]{2})(attr|text|ownText|textNodes|href|content|html|alt|all|value|src)(\(\))?)(?<seq>\&{2}|%%|\|{2}|$)"""
    )

    // Cannot complete, complex cases with js/json/{{xx}} exist
    private val notComplete = Regex("""^:|^##|\{\{|@js:|<js>|@Json:|\$\.""")

    // Correct info fetching from image
    private val fixImgInfo =
        Regex("""(?<=(^|tag\.|[\+/@>~| &]))img(?<at>(\[@?.+\]|\.[-\w]+)?)[@/]+text(\(\))?(?<seq>\&{2}|%%|\|{2}|$)""")

    private val isXpath = Regex("^//|^@Xpath:")

    /**
     * Complete simple rules, simplify source rule writing
     * Effective for JSOUP/XPath/CSS rules
     * @author Ximi
     * @return Completed rule or original rule
     * @param rules Rule to complete
     * @param preRule Pre-process or list rule
     * @param type Result type:
     *  1 Text (Default)
     *  2 Link
     *  3 Image
     */
    fun autoComplete(
        rules: String?,
        preRule: String? = null,
        type: Int = 1
    ): String? {
        if (rules.isNullOrEmpty() || rules.contains(notComplete) || preRule?.contains(notComplete) == true) {
            return rules
        }

        /** Regex split by trailing ## or params split by , */
        val tailStr: String

        /** Split character */
        val splitStr: String

        /**  Rule added when getting text */
        val textRule: String

        /**  Rule added when getting link */
        val linkRule: String

        /**  Rule added when getting image */
        val imgRule: String

        /**  Rule added when getting image alt attribute */
        val imgText: String

        // Separate tail rule
        val regexSplit = rules.split("""##|,\{""".toRegex(), 2)
        val cleanedRule = regexSplit[0]
        if (regexSplit.size > 1) {
            splitStr = """##|,\{""".toRegex().find(rules)?.value ?: ""
            tailStr = splitStr + regexSplit[1]
        } else {
            tailStr = ""
        }
        if (cleanedRule.contains(isXpath)) {
            textRule = "//text()\${seq}"
            linkRule = "//@href\${seq}"
            imgRule = "//@src\${seq}"
            imgText = "img\${at}/@alt\${seq}"
        } else {
            textRule = "@text\${seq}"
            linkRule = "@href\${seq}"
            imgRule = "@src\${seq}"
            imgText = "img\${at}@alt\${seq}"
        }
        return when (type) {
            1 -> needComplete.replace(cleanedRule, textRule).replace(fixImgInfo, imgText) + tailStr
            2 -> needComplete.replace(cleanedRule, linkRule) + tailStr
            3 -> needComplete.replace(cleanedRule, imgRule) + tailStr
            else -> rules
        }
    }


}
