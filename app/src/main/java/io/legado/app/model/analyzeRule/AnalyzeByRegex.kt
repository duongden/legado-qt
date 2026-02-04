package io.legado.app.model.analyzeRule

import androidx.annotation.Keep
import java.util.regex.Pattern

@Keep
object AnalyzeByRegex {

    fun getElement(res: String, regs: Array<String>, index: Int = 0): List<String>? {
        var vIndex = index
        val resM = Pattern.compile(regs[vIndex]).matcher(res)
        if (!resM.find()) {
            return null
        }
        // Rule to check index is the last rule
        return if (vIndex + 1 == regs.size) {
            // Create container
            val info = arrayListOf<String>()
            for (groupIndex in 0..resM.groupCount()) {
                info.add(resM.group(groupIndex)!!)
            }
            info
        } else {
            val result = StringBuilder()
            do {
                result.append(resM.group())
            } while (resM.find())
            getElement(result.toString(), regs, ++vIndex)
        }
    }

    fun getElements(res: String, regs: Array<String>, index: Int = 0): List<List<String>> {
        var vIndex = index
        val resM = Pattern.compile(regs[vIndex]).matcher(res)
        if (!resM.find()) {
            return arrayListOf()
        }
        // Rule to check index is the last rule
        if (vIndex + 1 == regs.size) {
            // Create book info cache array
            val books = ArrayList<List<String>>()
            // Extract list
            do {
                // Create container
                val info = arrayListOf<String>()
                for (groupIndex in 0..resM.groupCount()) {
                    info.add(resM.group(groupIndex) ?: "")
                }
                books.add(info)
            } while (resM.find())
            return books
        } else {
            val result = StringBuilder()
            do {
                result.append(resM.group())
            } while (resM.find())
            return getElements(result.toString(), regs, ++vIndex)
        }
    }
}